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
     * Post a log record.
     * 
     * @param record
     *            the record to post
     */
    public void post(LogRecord record);

    /**
     * Adds a metric object to thread-local storage to it can be dispatch to the server in a subsequent
     * {@link #submit()} call. The name of the metric will be derived from the name of the class or a {@link Metric}
     * annotation if one if present on the type.
     * 
     * @param metric
     *            the type of metric to be returned
     */
    public void postMetric(final Object metric);

    /**
     * Adds a metric object to thread-local storage to it can be dispatch to the server in a subsequent
     * {@link #submit()} call.
     * 
     * @param name
     *            the name to be used to store the metric
     * @param metric
     *            the type of metric to be returned
     */
    public void postMetric(String name, final Object metric);

    /**
     * Submits all metrics and logs collected for this thread to the remote server tracker.
     */
    public void submit();
}
