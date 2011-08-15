/**
 * Copyright (c) 2011 jolira. All rights reserved. This program and the accompanying materials are made available under
 * the terms of the GNU Public License 2.0 which is available at http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.jolira.st4j;

/**
 * A interface for submitting metrics to the server.
 * 
 * @author jfk
 * @date Aug 12, 2011 8:47:45 PM
 * @since 1.0
 * 
 */
public interface ServerTracker {
    /**
     * Create or retrieve a metric object. If an instance of this metric was already created for this thread this
     * instance is returned. Otherwise a new instance is returned.
     * 
     * @param type
     *            the type of metric to be returned
     * @return the generated metric
     */
    public <T> T getMetric(final Class<T> type);

    /**
     * Create or retrieve a metric object. If an instance of this metric was already created for this thread this
     * instance is returned. Otherwise a new instance is returned.
     * 
     * @param name
     *            the name to be used to store the metric
     * 
     * @param type
     *            the type of metric to be returned
     * @return the generated metric
     */
    public <T> T getMetric(String name, final Class<T> type);

    /**
     * submit all metrics collectd for this thread to the remote server tracker.
     */
    public void submit();
}
