/**
 * Copyright (c) 2011 jolira. All rights reserved. This program and the accompanying materials are made available under
 * the terms of the GNU Public License 2.0 which is available at http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.jolira.st4j.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executor;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jolira.st4j.LogRecord;
import com.jolira.st4j.MetricStore;
import com.jolira.st4j.ServerTracker;
import com.jolira.st4j.ServerUnavailableException;

/**
 * Creates measurement and stores them in the thread-local field and send measurement to the remote server.
 * 
 * @author jfk
 * @date Aug 12, 2011 9:09:31 PM
 * @since 1.0
 * 
 */
@Singleton
public class ServerTrackerImpl implements ServerTracker {
    private static final String CLASS_NAME = ServerTrackerImpl.class.getName();
    static final Logger LOG = LoggerFactory.getLogger(ServerTrackerImpl.class);

    private final static String hostname;
    private static final int LOGS_BATCH_SIZE = 100;

    static {
        try {
            final java.net.InetAddress host = java.net.InetAddress.getLocalHost();

            hostname = host.getHostName();
        } catch (final java.net.UnknownHostException ex) {
            throw new Error("unable to determine the hostname", ex);
        }
    }

    private final Collection<Map<String, Object>> events = new LinkedList<Map<String, Object>>();

    private final Collection<Object> logs = new LinkedList<Object>();

    private final Executor executor;

    private final ClientFactory factory;

    private final MetricStore store;

    private long lastSubmit = 0;

    private final int SUMBIT_INTERVAL = 1000;

    /**
     * Create a new instance.
     * 
     * @param server
     *            the name or names of the remote measurements servers. If there is more than one, the different names
     *            have to be comma delimited. Every name can include a port number (separated from the servername with a
     *            column, such as "localhost:3080").
     * @param timeout
     *            used as the connect timeout
     * @param store
     *            the store to be used
     * @param executor
     *            the executor for uploading the measurements and logs in the background
     * @throws IllegalArgumentException
     *             thrown if the server cannot be used to form a valid URL
     */
    @Inject
    public ServerTrackerImpl(@Named("ServerTrackerServer") final String server,
            @Named("ServerTrackerTimeout") final int timeout, final MetricStore store, final Executor executor)
                    throws IllegalArgumentException {
        this.store = store;
        this.executor = executor;
        factory = new ClientFactory(server, timeout){
            @Override
            protected void postMeasurment(final Object measurement) {
                ServerTrackerImpl.this.postMeasurment(measurement);
            }
        };
    }

    private void addEvent(final Map<String, Object> event) {
        synchronized (events) {
            events.add(event);
        }

        transmit();
    }

    private void addLog(final Map<String, Object> log) {
        synchronized (logs) {
            logs.add(log);
        }
    }

    private void merge(final Map<String, Object> source, final Map<String, Object> target) {
        final Collection<Entry<String, Object>> entries = source.entrySet();

        for (final Entry<String, Object> entry : entries) {
            final String key = entry.getKey();

            if (target.containsKey(key)) {
                continue;
            }

            final Object value = entry.getValue();

            target.put(key, value);

        }
    }

    @Override
    public void post(final LogRecord record) {
        if (CLASS_NAME.equals(record.sourceClassName)) {
            return;
        }

        synchronized (logs) {
            logs.add(record);
        }

        transmit();
    }

    @Override
    public void postMeasurment(final Object measurement) {
        store.postThreadLocalMeasurement(null, measurement, true);
    }

    @Override
    public void postMeasurment(final String name, final Object measurement) {
        store.postThreadLocalMeasurement(null, measurement, true);
    }

    @Override
    public void postMeasurment(final String name, final Object measurement, final boolean unique) {
        store.postThreadLocalMeasurement(null, measurement, unique);
    }

    @Override
    public Collection<Map<String, Object>> proxyEvent(final Map<String, Object> eventInfo, final InputStream content)
            throws IllegalArgumentException {
        final GsonBuilder gsonBuilder = new GsonBuilder();

        gsonBuilder.registerTypeAdapter(Object.class, new NaturalDeserializer());

        final Gson parser = gsonBuilder.create();
        final Object _payload = parser.fromJson(new InputStreamReader(content), Object.class);

        LOG.info("proxying {}", _payload);

        if (_payload == null || !(_payload instanceof Map)) {
            throw new IllegalArgumentException("not a map: " + _payload);
        }

        @SuppressWarnings("unchecked")
        final Map<String, Object> payload = (Map<String, Object>) _payload;

        @SuppressWarnings("unchecked")
        final Collection<Map<String, Object>> _events = (Collection<Map<String, Object>>) payload.get("events");
        @SuppressWarnings("unchecked")
        final Collection<Map<String, Object>> _logs = (Collection<Map<String, Object>>) payload.get("logs");

        payload.remove("events");
        payload.remove("logs");
        merge(eventInfo, payload);

        if (_logs != null) {
            for (final Map<String, Object> log : _logs) {
                merge(payload, log);
                addLog(log);
            }
        }

        final Collection<Map<String, Object>> result = new ArrayList<Map<String, Object>>(1);

        if (_events != null) {
            for (final Map<String, Object> event : _events) {
                merge(payload, event);
                addEvent(event);
                result.add(event);
            }
        }

        return result;
    }

    private Collection<Map<String, Object>> retrieveCollectedCycles() {
        synchronized (events) {
            final int size = events.size();

            if (size < 1) {
                return null;
            }

            final Collection<Map<String, Object>> pending = new ArrayList<Map<String, Object>>(size);

            pending.addAll(events);
            events.clear();

            return pending;
        }
    }

    private Collection<Object> retrieveCollectedLogs(final boolean willTransmitEvents) {
        final int limit = willTransmitEvents ? 1 : LOGS_BATCH_SIZE;

        synchronized (logs) {
            final int size = logs.size();

            if (size < limit) {
                return null;
            }

            final Collection<Object> pending = new ArrayList<Object>(size);

            pending.addAll(logs);
            logs.clear();

            return pending;
        }
    }

    private Map<String, Object> retrievePending() {
        final Collection<Map<String, Object>> _events = retrieveCollectedCycles();
        final Collection<Object> _logs = retrieveCollectedLogs(_events != null);

        if (_events == null && _logs == null) {
            return null;
        }

        final Map<String, Object> pending = new HashMap<String, Object>();

        if (_events != null) {
            pending.put("events", _events);
        }

        if (_logs != null) {
            pending.put("logs", _logs);
        }

        final long now = System.currentTimeMillis();

        pending.put("source", hostname);
        pending.put("timestamp", Long.valueOf(now));

        return pending;
    }

    @Override
    public void submit(final Map<String, Object> eventInfo) {
        final Map<String, Object> measurements = store.getAndResetThreadLocalMetrics();
        final int size = measurements.size();

        if (size < 1) {
            return;
        }

        final Map<String, Object> event = new HashMap<String, Object>();

        event.put("measurements", measurements);
        merge(eventInfo, event);
        addEvent(event);
    }

    private synchronized void transmit() {
        final long time = System.currentTimeMillis();

        if (time - lastSubmit < SUMBIT_INTERVAL) {
            return;
        }

        final Map<String, Object> _pending = retrievePending();

        if (_pending == null) {
            return;
        }

        final ClientFactory f = factory;
        final Gson gson = new Gson();

        lastSubmit = time;

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    transmit(gson, _pending, f);
                } catch (final Throwable e) {
                    LOG.warn("error while submitting data", e);
                }
            }
        });
    }

    /**
     * Post to the remote server. A template method that allows subclasses to pre-process JSON before submitting it to
     * the server.
     * 
     * @param gson
     *            the GSON object to be used to encode the contents
     * @param pending
     *            the events to be encoded
     * @param f
     *            the factory to be used to create the client
     * 
     * @throws IOException
     *             could not transmit
     */
    protected void transmit(final Gson gson, final Map<String, Object> pending, final ClientFactory f)
            throws IOException {
        final Client client = f.makeClient();
        final OutputStream os = client.getOutputStream();
        final OutputStreamWriter wr = new OutputStreamWriter(os);

        try {
            gson.toJson(pending, wr);
        } finally {
            wr.close();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("sending {}", gson.toJson(pending));
        }

        try {
            client.transmit();
        } catch (final ServerUnavailableException e) {
            LOG.error("no server for {}", gson.toJson(pending));
        }
    }
}
