/**
 * Copyright (c) 2011 jolira. All rights reserved. This program and the accompanying materials are made available under
 * the terms of the GNU Public License 2.0 which is available at http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.jolira.st4j.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Singleton;

import com.jolira.st4j.Metric;
import com.jolira.st4j.MetricStore;

/**
 * A simple implementation of the metric store.
 * 
 * @author jfk
 * @date Aug 27, 2011 6:41:52 PM
 * @since 1.0
 * 
 */
@Singleton
public class MetricStoreImpl implements MetricStore {
    private static final String DEFAULT = "##default";
    private final static ThreadLocal<Map<String, Object>> localMetrics = new ThreadLocal<Map<String, Object>>() {
        @Override
        protected Map<String, Object> initialValue() {
            return new HashMap<String, Object>();
        }
    };

    @Override
    public Map<String, Object> getAndResetThreadLocalMetrics() {
        final Map<String, Object> result = localMetrics.get();

        localMetrics.remove();

        return result;
    }

    @Override
    public String getMetricName(@Nullable final String mname, final Class<? extends Object> type) {
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

    @Override
    public <T> T getThreadLocalMetric(final String mname, final Class<T> type) {
        final String metricName = getMetricName(mname, type);
        final Map<String, Object> metricByName = localMetrics.get();
        final Object obj = metricByName.get(metricName);

        return type.cast(obj);
    }

    @Override
    public void postThreadLocalMetric(final String mname, final Object metric, final boolean unique) {
        final Class<? extends Object> type = metric.getClass();
        final String metricName = getMetricName(mname, type);
        final Map<String, Object> metricByName = localMetrics.get();
        final boolean existis = metricByName.containsKey(metricName);

        if (unique || !existis) {
            metricByName.put(metricName, metric);
            return;
        }

        final Object existing = metricByName.get(metricName);

        if (existing instanceof Collection) {
            @SuppressWarnings("unchecked")
            final Collection<Object> collection = (Collection<Object>) existing;

            collection.add(metric);
            return;
        }

        final ArrayList<Object> collection = new ArrayList<Object>();

        collection.add(existing);
        collection.add(metric);

        metricByName.put(metricName, collection);
    }
}
