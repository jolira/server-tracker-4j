/**
 * Copyright (c) 2011 jolira. All rights reserved. This program and the accompanying materials are made available under
 * the terms of the GNU Public License 2.0 which is available at http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.jolira.st4j;

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

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
     * Post a metric object.
     * 
     * @param name
     *            the name of the metric
     * @param metric
     *            the metric object
     * @param unique
     *            indicates if the metric should be unique or not
     */
    public void postMetric(String name, Object metric, boolean unique);

    /**
     * Post a log record.
     * 
     * @param serverInfo
     *            a set of properties from the server that augments the data that is being forwarded. This field
     *            enables the the server to add information the the proxied event, such as the hostname and the
     *            address of the client as well as, in some cases, the session and the visitor ids.
     * @param content
     *            the content to forward
     * @return return the deserialized data structure
     * 
     */
    public Collection<Map<String, Object>> proxyEvent(Map<String, Object> serverInfo, InputStream content);

    /**
     * Submits all metrics and logs collected for this thread to the remote server tracker.
     */
    public void submit();
}
