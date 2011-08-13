/**
 * Copyright (c) 2011 jolira. All rights reserved. This program and the accompanying materials are made available under
 * the terms of the GNU Public License 2.0 which is available at http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.jolira.st4j.impl;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.jolira.st4j.Metric;
import com.jolira.st4j.ServerTracker;

/**
 * 
 * @author jfk
 * @date Aug 12, 2011 9:09:31 PM
 * @since 1.0
 * 
 */
@Singleton
public class ServerTrackerImpl implements ServerTracker {
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

    private static String getDefaultMetricName(final Class<?> type) {
        final String name = type.getName();

        return name.toLowerCase();
    }

    private final ThreadLocal<Map<String, Object>> metricCache = new ThreadLocal<Map<String, Object>>() {
        @Override
        protected Map<String, Object> initialValue() {
            return new HashMap<String, Object>();
        }
    };
    private final ThreadLocal<Map<String, Map<String, Object>>> collected = new ThreadLocal<Map<String, Map<String, Object>>>() {
        @Override
        protected Map<String, Map<String, Object>> initialValue() {
            return new HashMap<String, Map<String, Object>>();
        }
    };

    @Inject
    ServerTrackerImpl(@Named("ServerTrackerServer") final String server) {
        //
    }

    /**
     * Returns the collected values for unit testing.
     * 
     * @return the values
     */
    final Map<String, Map<String, Object>> getCollected() {
        return collected.get();
    }

    @Override
    public <T> T getMetric(final Class<T> type) {
        final String metricName = getMetricName(type);
        final Map<String, Object> _metricByName = metricCache.get();
        final Object o = _metricByName.get(metricName);

        if (o != null) {
            return type.cast(o);
        }

        final MetricBean<T> bean = new MetricBean<T>(type) {
            @Override
            protected void add(final String name, final Object value) {
                ServerTrackerImpl.this.add(metricName, name, value);
            }
        };

        _metricByName.put(metricName, bean.metric);

        return bean.metric;
    }

    final void add(final String metric, final String key, final Object value) {
        final Map<String, Map<String, Object>> map = collected.get();
        Map<String, Object> valByKey = map.get(metric);

        if (valByKey == null) {
            valByKey = new HashMap<String, Object>();

            map.put(metric, valByKey);
        }

        valByKey.put(key, value);
    }

    @Override
    public void submit() {
        // TODO Auto-generated method stub
    }

}
