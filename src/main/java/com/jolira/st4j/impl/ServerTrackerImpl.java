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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
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

    final static Logger LOG = LoggerFactory.getLogger(ServerTrackerImpl.class);

    private final static String hostname;

    static {
        try {
            final java.net.InetAddress host = java.net.InetAddress.getLocalHost();

            hostname = host.getHostName();
        } catch (final java.net.UnknownHostException ex) {
            throw new Error("unable to determine the hostname", ex);
        }
    }

    private final Collection<Map<String, Object>> events = new LinkedList<Map<String, Object>>();

    private final Collection<LogRecord> logs = new LinkedList<LogRecord>();

    private final Executor executor;

    private final URL url;

    private boolean dispatcherRunning = false;

    private final Object lock = new Object();

    private final static int LOG_QUEUE_TRIGGER_SIZE = 2048;

    private final MetricStore store;

    private static boolean shouldPost(final Collection<Map<String, Object>> events, final Collection<LogRecord> logs) {
        if (events != null) {
            return true;
        }

        if (logs == null) {
            return false;
        }

        final int size = logs.size();

        return size > LOG_QUEUE_TRIGGER_SIZE;
    }

    /**
     * Create a new instance.
     * 
     * @param server the name of the remote metrics server.
     * @param store the store to be used
     * @param executor the executor for uploading the metrics and logs in the background
     */
    @Inject
    public ServerTrackerImpl(@Named("ServerTrackerServer") final String server, final MetricStore store, final Executor executor) {
        this.store = store;
        this.executor = executor;

        try {
            url = new URL("http://" + server + "/submit/metric");
        } catch (final MalformedURLException e) {
            throw new Error("invalid server " + server, e);
        }
    }

    private boolean dispatch(final Gson gson) throws IOException {
        final Map<String, Object> _pending = retrievePending();

        if (_pending == null) {
            return false;
        }

        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setDoInput(true);

        final int status = post(gson, _pending, conn);

        if (status != 200) {
            throw new Error(Integer.toString(status) + " response code while submitting " + gson.toJson(_pending));
        }

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
     * @param pending
     * @param conn
     * @return the status code
     * @throws IOException
     * @throws JsonIOException
     */
    protected int post(final Gson gson, final Map<String, Object> pending, final HttpURLConnection conn)
            throws IOException, JsonIOException {
        final OutputStream os = conn.getOutputStream();
        final OutputStreamWriter wr = new OutputStreamWriter(os);

        try {
            gson.toJson(pending, wr);
        } finally {
            wr.close();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("sending {}", gson.toJson(pending));
        }

        return conn.getResponseCode();
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

    private Collection<LogRecord> retrieveCollectedLogs() {
        synchronized (logs) {
            final int size = logs.size();

            if (size < 1) {
                return null;
            }

            final Collection<LogRecord> pending = new ArrayList<LogRecord>(size);

            pending.addAll(logs);
            logs.clear();

            return pending;
        }
    }

    private Map<String, Object> retrievePending() {
        final Collection<Map<String, Object>> _events = retrieveCollectedCycles();
        final Collection<LogRecord> _logs = retrieveCollectedLogs();

        if (!shouldPost(_events, _logs)) {
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
    public void submit() {
        final Map<String, Object> metrics = store.getAndResetThreadLocalMetrics();
        final int size = metrics.size();

        if (size < 1) {
            return;
        }

        final Map<String, Object> event = new HashMap<String, Object>();

        event.put("metrics", metrics);

        addEvent(event);
    }

    private void addEvent(final Map<String, Object> event) {
        synchronized (events) {
            events.add(event);
        }

        execute();
    }

    @Override
    public Collection<Map<String, Object>> proxyEvent(final Map<String, Object>  eventInfo, final InputStream content) {
        final GsonBuilder gsonBuilder = new GsonBuilder();

        gsonBuilder.registerTypeAdapter(Object.class, new NaturalDeserializer());

        final Gson parser = gsonBuilder.create();
        final Object event = parser.fromJson(new InputStreamReader(content), Object.class);

        if (event == null) {
            return null;
        }

        final Class<? extends Object> eventClass = event.getClass();

        if (eventClass.isArray()) {
            final Object[] _events = (Object[]) event;

            return proxyEvents(eventInfo, _events);
        }

        final Map<String, Object> _event = proxyEvent(eventInfo, event);
        final Collection<Map<String, Object>> result = new ArrayList<Map<String, Object>>(1);

        result.add(_event);

        return result;
    }

    private Collection<Map<String, Object>> proxyEvents(final Map<String, Object> eventInfo, final Object[] _events) {
        final Object[] events_ = _events;
        final Collection<Map<String, Object>> result = new ArrayList<Map<String, Object>>(events_.length);

        for(final Object event : events_) {
            @SuppressWarnings("unchecked")
            final Map<String, Object> _event = (Map<String, Object>) event;

            mergeAndAdd(eventInfo, _event);

            result.add(_event);
        }


        return result;
    }

    private void mergeAndAdd(final Map<String, Object> eventInfo, final Map<String, Object> event) {
        event.putAll(eventInfo);
        addEvent(event);
    }

    private Map<String, Object> proxyEvent(final Map<String, Object>  eventInfo, final Object event) {
        if (!(event instanceof Map)) {
            throw new IllegalArgumentException();
        }

        @SuppressWarnings("unchecked")
        final Map<String, Object> _event = (Map<String, Object> )event;

        mergeAndAdd(eventInfo, _event);

        return _event;
    }
}
