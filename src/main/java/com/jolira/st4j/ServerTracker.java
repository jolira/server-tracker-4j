/**
 * Copyright (c) 2011 jolira. All rights reserved. This program and the accompanying materials are made available under
 * the terms of the GNU Public License 2.0 which is available at http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.jolira.st4j;

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

/**
 * A interface for submitting measurements to the server.
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
     * Adds a measurement object to thread-local storage to it can be dispatch to the server in a subsequent
     * {@link #submit(Map)} call. The name of the measurement will be derived from the name of the class or a {@link Metric}
     * annotation if one if present on the type.
     * 
     * @param measurement
     *            the type of measurement to be returned
     */
    public void postMeasurment(final Object measurement);

    /**
     * Adds a measurement object to thread-local storage to it can be dispatch to the server in a subsequent
     * {@link #submit(Map)} call.
     * 
     * @param name
     *            the name to be used to store the measurement
     * @param measurement
     *            the type of measurement to be returned
     */
    public void postMeasurment(String name, final Object measurement);

    /**
     * Post a measurement object.
     * 
     * @param name
     *            the name of the measurement
     * @param measurement
     *            the measurement object
     * @param unique
     *            indicates if the measurement should be unique or not
     */
    public void postMeasurment(String name, Object measurement, boolean unique);

    /**
     * Forward events and logs received from devices. The content has to be valid JSON in the following format:
     * 
     * <pre>
     * {
     *   "logs" : [<log1>, <log2>, ...],
     *   "events" : [<event1>, <event2>, ...]
     * }
     * </pre>
     * 
     * Both events and logs are optional. If present, an array of events and logs needs to be passed.
     * 
     * @param serverInfo
     *            a set of properties from the server that augments the data that is being forwarded. This field enables
     *            the the server to add information the the proxied event, such as the hostname and the address of the
     *            client as well as, in some cases, the session and the visitor ids.
     * @param content
     *            the content to forward
     * @return return the deserialized data structure
     * @throws IllegalArgumentException
     *             thrown if the JSON is not in the proper format.
     * 
     */
    public Collection<Map<String, Object>> proxyEvent(Map<String, Object> serverInfo, InputStream content)
            throws IllegalArgumentException;

    /**
     * Submits all measurements and logs collected for this thread to the remote server tracker.
     * 
     * @param eventInfo
     *            the event properties
     */
    public void submit(Map<String, Object> eventInfo);
}
