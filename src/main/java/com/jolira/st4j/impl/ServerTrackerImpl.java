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
    final static Logger LOG = LoggerFactory.getLogger(ServerTrackerImpl.class);
    private final static ThreadLocal<Map<String, Object>> localMetrics = new ThreadLocal<Map<String, Object>>() {
        @Override
        protected Map<String, Object> initialValue() {
            return new HashMap<String, Object>();
        }
    };

    private final static ThreadLocal<Map<String, Map<String, Object>>> local = new ThreadLocal<Map<String, Map<String, Object>>>() {
        @Override
        protected Map<String, Map<String, Object>> initialValue() {
            return new HashMap<String, Map<String, Object>>();
        }
    };

    static final void add(final String metric, final String key, final Object value) {
        final Map<String, Map<String, Object>> map = local.get();
        Map<String, Object> valByKey = map.get(metric);

        if (valByKey == null) {
            valByKey = new HashMap<String, Object>();

            map.put(metric, valByKey);
        }

        valByKey.put(key, value);
    }

    private static String getDefaultMetricName(final Class<?> type) {
        final String name = type.getName();

        return name.toLowerCase();
    }

    final static <T> T getLocalMetric(final Class<T> type) {
        final String metricName = getMetricName(type);
        final Map<String, Object> _metricByName = localMetrics.get();
        final Object o = _metricByName.get(metricName);

        if (o != null) {
            return type.cast(o);
        }

        final MetricBean<T> bean = new MetricBean<T>(type) {
            @Override
            protected void add(final String name, final Object value) {
                ServerTrackerImpl.add(metricName, name, value);
            }
        };

        _metricByName.put(metricName, bean.metric);

        return bean.metric;
    }

    private static String getMetricName(final Class<?> type) {
        final Metric anno = type.getAnnotation(Metric.class);

        if (anno == null) {
            return getDefaultMetricName(type);
        }

        final String value = anno.value();

        if ("##default".equals(value)) {
            return getDefaultMetricName(type);
        }

        return value;
    }

    private final Collection<Map<String, Map<String, Object>>> pending = new LinkedList<Map<String, Map<String, Object>>>();

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
        final Collection<Map<String, Map<String, Object>>> _pending = retrievePending();

        if (_pending == null) {
            return false;
        }

        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setDoInput(true);

        final OutputStream os = conn.getOutputStream();
        final OutputStreamWriter wr = new OutputStreamWriter(os);

        try {
            gson.toJson(_pending, wr);
        } finally {
            wr.close();
        }

        final int status = conn.getResponseCode();

        if (status != 200) {
            LOG.error("{} response code while submitting {}", Integer.valueOf(status), gson.toJson(_pending));
        }

        return true;
    }

    /**
     * Returns the collected values for unit testing.
     * 
     * @return the values
     */
    final Map<String, Map<String, Object>> getLocal() {
        return local.get();
    }

    @Override
    public <T> T getMetric(final Class<T> type) {
        return getLocalMetric(type);
    }

    void post(final Map<String, Map<String, Object>> _collected) {
        synchronized (pending) {
            pending.add(_collected);

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
            synchronized (pending) {
                dispatcherRunning = false;
            }
        }
    }

    private Collection<Map<String, Map<String, Object>>> retrievePending() {
        synchronized (pending) {
            final int size = pending.size();

            if (size < 1) {
                return null;
            }

            final Collection<Map<String, Map<String, Object>>> _pending = new ArrayList<Map<String, Map<String, Object>>>(
                    size);

            _pending.addAll(pending);
            pending.clear();

            return _pending;
        }
    }

    @Override
    public void submit() {
        final Map<String, Map<String, Object>> _collected = getLocal();

        local.remove();

        final int size = _collected.size();

        if (size < 1) {
            return;
        }

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    post(_collected);
                } catch (final Throwable e) {
                    LOG.error("error while submitting data", e);
                }
            }

        });
    }
}
