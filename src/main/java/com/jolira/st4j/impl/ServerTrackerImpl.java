/**
 * Copyright (c) 2011 jolira. All rights reserved. This program and the accompanying materials are made available under
 * the terms of the GNU Public License 2.0 which is available at http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.jolira.st4j.impl;

import java.io.IOException;
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
import com.google.gson.JsonIOException;
import com.jolira.st4j.LogRecord;
import com.jolira.st4j.Metric;
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
    private static final String DEFAULT = "##default";

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

    private final static ThreadLocal<Map<String, Object>> localMetrics = new ThreadLocal<Map<String, Object>>() {
        @Override
        protected Map<String, Object> initialValue() {
            return new HashMap<String, Object>();
        }
    };

    final static <T> T getLocalMetric(final String mname, final Class<T> type) {
        final String metricName = getMetricName(mname, type);
        final Map<String, Object> metricByName = localMetrics.get();
        final Object obj = metricByName.get(metricName);

        return type.cast(obj);
    }

    private static String getMetricName(final String mname, final Class<? extends Object> type) {
        if (mname != null && !DEFAULT.equals(mname)) {
            return mname;
        }

        final Metric metric = type.getAnnotation(Metric.class);

        if (metric != null) {
            final String value = metric.value();

            if (value != null && !DEFAULT.equals(value)) {
                return value;
            }
        }

        final String name = type.getName();

        return name.toLowerCase();
    }

    static void postLocalMetric(final String mname, final Object metric) {
        final Class<? extends Object> type = metric.getClass();
        final String metricName = getMetricName(mname, type);
        final Map<String, Object> metricByName = localMetrics.get();

        metricByName.put(metricName, metric);
    }

    private final Collection<Map<String, Object>> cycles = new LinkedList<Map<String, Object>>();

    private final Collection<LogRecord> logs = new LinkedList<LogRecord>();

    private final Executor executor;

    private final URL url;

    private boolean dispatcherRunning = false;

    @Inject
    ServerTrackerImpl(@Named("ServerTrackerServer") final String server, final Executor executor) {
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

        synchronized(logs) {
            logs.add(record);
        }

        execute();
    }

    private final Object lock = new Object();

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

    @Override
    public void postMetric(final Object metric) {
        postLocalMetric(null, metric);
    }

    @Override
    public void postMetric(final String name, final Object metric) {
        postLocalMetric(name, metric);
    }

    private Map<String, Object> retrievePending() {
        final Collection<Map<String, Object>> _cycles = retrieveCollectedCycles();
        final Collection<LogRecord> _logs = retrieveCollectedLogs();

        if (_cycles == null && _logs == null) {
            return null;
        }

        final Map<String, Object> pending = new HashMap<String, Object>();

        if (_cycles != null) {
            pending.put("cycles", _cycles);
        }

        if (_logs != null) {
            pending.put("logs", _logs);
        }

        final long now = System.currentTimeMillis();

        pending.put("hostname", hostname);
        pending.put("timestamp", Long.valueOf(now));

        return pending;
    }

    private Collection<Map<String, Object>> retrieveCollectedCycles() {
        synchronized (cycles) {
            final int size = cycles.size();

            if (size < 1) {
                return null;
            }

            final Collection<Map<String, Object>> pending = new ArrayList<Map<String, Object>>(size);

            pending.addAll(cycles);
            cycles.clear();

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

    @Override
    public void submit() {
        final Map<String, Object> _cycle = localMetrics.get();
        final Map<String, Object> cycle = new HashMap<String, Object>(_cycle);

        localMetrics.remove();

        final int size = cycle.size();

        if (size < 1) {
            return;
        }

        synchronized(cycles) {
            cycles.add(cycle);
        }

        execute();
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
}
