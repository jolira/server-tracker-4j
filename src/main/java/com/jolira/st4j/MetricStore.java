/**
 * Copyright (c) 2011 jolira. All rights reserved. This program and the accompanying materials are made available under
 * the terms of the GNU Public License 2.0 which is available at http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.jolira.st4j;

import java.util.Map;

import javax.annotation.Nullable;

/**
 * A class for storing the data data collected by the tracker before it is shipped to the remote server. This interface
 * is most likely less useful for the common user as it does not actually trigger any remote distribution. It main
 * purpose is to let users control how, where and how much data is stored locally before transmitting it to the remote
 * server.
 * 
 * @author jfk
 * @date Aug 27, 2011 6:34:05 PM
 * @since 1.0
 * 
 */
public interface MetricStore {
    /**
     * This call returns all the metrics collected the thread and resets the storage.
     * 
     * @return all collected metric.
     */
    public Map<String, Object> getAndResetThreadLocalMetrics();

    /**
     * Normalize the name of the metric.
     * 
     * @param mname
     *            the name of the metric (may be {@literal null}).
     * @param type
     *            the metric type
     * @return the normalized name
     */
    public String getMetricName(@Nullable String mname, Class<? extends Object> type);

    /**
     * Retrieve a metric instance from thread-local storage. If the metric does not yet exist, {@literal null} will be
     * returned.
     * 
     * @param mname
     *            the name of the measurement (may be {@literal null}).
     * @param type
     *            the type of metric
     * @return the metric instance or {@literal null}
     */
    public <T> T getThreadLocalMeasurement(@Nullable String mname, Class<T> type);

    /**
     * Post a measurement instance to thread local storage.
     * 
     * @param mname
     * 
     *            the name of the measurement (may be {@literal null}).
     * @param metric
     *            the metric instance to be posted
     * @param unique
     *            {@literal true} to indicate that the metric is unique
     */
    public void postThreadLocalMeasurement(@Nullable String mname, Object metric, boolean unique);
}
