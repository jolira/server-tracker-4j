/**
 * Copyright (c) 2011 jolira. All rights reserved. This program and the accompanying materials are made available under
 * the terms of the GNU Public License 2.0 which is available at http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.jolira.st4j;

/**
 * A very simple timer. The timer starts when the object is created and stops when {@link #stop()} is called. Timers can
 * have associated categories and urls.
 * 
 * @author jfk
 * @date Sep 17, 2011 9:46:19 PM
 * @since 1.0
 * 
 */
@Metric(value = "timer", unique = false)
public class Timer {
    /**
     * The system time when the timer object was created.
     */
    public final long timestamp = System.currentTimeMillis();

    private long duration;

    /**
     * The category of event being timed
     */
    public String category;

    /**
     * The url associated with the timer
     */
    public URL url;

    /**
     * An additional source identifier, if available
     */
    public String source;

    /**
     * Stop the timer.
     * 
     * @return the duration in ms
     */
    public long stop() {
        final long current = System.currentTimeMillis();

        return duration = current - timestamp;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();

        builder.append("Timer [timestamp=");
        builder.append(timestamp);
        builder.append(", duration=");
        builder.append(getDuration());
        builder.append(", category=");
        builder.append(category);
        builder.append(", source=");
        builder.append(source);
        builder.append(", url=");
        builder.append(url);
        builder.append("]");

        return builder.toString();
    }

    /**
     * @return the duration
     */
    public long getDuration() {
        return duration;
    }
}
