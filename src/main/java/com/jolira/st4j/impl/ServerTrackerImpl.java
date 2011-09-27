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
import com.google.gson.JsonIOException;
import com.jolira.st4j.LogRecord;
import com.jolira.st4j.MetricStore;
import com.jolira.st4j.ServerTracker;
import com.jolira.st4j.ServerUnavailableException;

/**
 * Creates metric and stores them in the thread-local field and send metric to the remote server.
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

    private boolean dispatcherRunning = false;

    private final Object lock = new Object();

    private final MetricStore store;

    /**
     * Create a new instance.
     * 
     * @param server
     *            the name or names of the remote metrics servers. If there is more than one, the different names have
     *            to be comma delimited. Every name can include a port number (separated from the servername with a
     *            column, such as "localhost:3080").
     * @param timeout
     *            used as the connect timeout
     * @param store
     *            the store to be used
     * @param executor
     *            the executor for uploading the metrics and logs in the background
     * @throws IllegalArgumentException
     *             thrown if the server cannot be used to form a valid URL
     */
    @Inject
    public ServerTrackerImpl(@Named("ServerTrackerServer") final String server,
            @Named("ServerTrackerTimeout") final int timeout, final MetricStore store, final Executor executor)
                    throws IllegalArgumentException {
        this.store = store;
        this.executor = executor;
        factory = new ClientFactory(server, timeout);
    }

    private void addEvent(final Map<String, Object> event) {
        synchronized (events) {
            events.add(event);
        }

        execute();
    }

    private void addLog(final Map<String, Object> log) {
        synchronized (logs) {
            logs.add(log);
        }
    }

    private boolean dispatch(final Gson gson) throws IOException {
        final Map<String, Object> _pending = retrievePending();

        if (_pending == null) {
            return false;
        }

        post(gson, _pending, factory);

        return true;
    }

    private void execute() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    post();
                } catch (final Throwable e) {
                    LOG.error("error while submitting data", e);
                }
            }
        });
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

    void post() {
        synchronized (lock) {
            if (dispatcherRunning) {
                return;
            }

            dispatcherRunning = true;
        }

        try {
            final Gson gson = new Gson();

            while (dispatch(gson)) {
                // nothing
            }
        } catch (final IOException e) {
            LOG.error("exception while dispatching", e);
        } finally {
            synchronized (lock) {
                dispatcherRunning = false;
            }
        }
    }

    /**
     * Post to the remote server.
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
     * @throws JsonIOException
     *             could not encode
     * @throws ServerUnavailableException
     *             no server was available to receive the content
     */
    protected void post(final Gson gson, final Map<String, Object> pending, final ClientFactory f) throws IOException,
    JsonIOException, ServerUnavailableException {
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

        client.transmit();
    }

    @Override
    public void post(final LogRecord record) {
        if (CLASS_NAME.equals(record.sourceClassName)) {
            return;
        }

        synchronized (logs) {
            logs.add(record);
        }

        execute();
    }

    @Override
    public void postMetric(final Object metric) {
        store.postThreadLocalMetric(null, metric, true);
    }

    @Override
    public void postMetric(final String name, final Object metric) {
        store.postThreadLocalMetric(null, metric, true);
    }

    @Override
    public void postMetric(final String name, final Object metric, final boolean unique) {
        store.postThreadLocalMetric(null, metric, unique);
    }

    @Override
    public Collection<Map<String, Object>> proxyEvent(final Map<String, Object> eventInfo, final InputStream content) throws IllegalArgumentException {
        final GsonBuilder gsonBuilder = new GsonBuilder();

        gsonBuilder.registerTypeAdapter(Object.class, new NaturalDeserializer());

        final Gson parser = gsonBuilder.create();
        final Object _payload = parser.fromJson(new InputStreamReader(content), Object.class);

        if (_payload == null || !(_payload instanceof Map)) {
            throw new IllegalArgumentException();
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
        final Map<String, Object> metrics = store.getAndResetThreadLocalMetrics();
        final int size = metrics.size();

        if (size < 1) {
            return;
        }

        final Map<String, Object> event = new HashMap<String, Object>();

        event.put("metrics", metrics);
        merge(eventInfo, event);
        addEvent(event);
    }
}
