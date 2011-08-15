/**
 * Copyright (c) 2011 jolira. All rights reserved. This program and the accompanying materials are made available under
 * the terms of the GNU Public License 2.0 which is available at http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.jolira.st4j;

import java.util.Arrays;

/**
 * @author jfk
 * @date Aug 14, 2011 8:35:01 PM
 * @since 1.0
 * 
 */
public class LogRecord {
    /**
     * the level
     */
    public final String level;

    /**
     * the log message
     */
    public final String message;
    /**
     * the parameter passed to the message
     */
    public final Object[] parameters;

    /**
     * a unique sequence number identifying the record
     */
    public final long sequenceNumber;

    /**
     * the name of the source class that produced the log entry
     */
    public final String sourceClassName;

    /**
     * the name of the method that produced the log record
     */
    public final String sourceMethodName;

    /**
     * the unique id for the thread that created the log entry
     */
    public final int threadID;

    /**
     * the exception associated with the log record, if any. {@literal null}, if there is no exception.
     */
    public final Throwable thrown;

    /**
     * the time when the log record was created
     */
    public final long timestamp;

    /**
     * Create a new record.
     * 
     * @param level
     * @param message
     * @param parameters
     * @param sequenceNumber
     * @param sourceClassName
     * @param sourceMethodName
     * @param threadID
     * @param thrown
     * @param timestamp
     */
    public LogRecord(final String level, final String message, final Object[] parameters, final long sequenceNumber,
            final String sourceClassName, final String sourceMethodName, final int threadID, final Throwable thrown,
            final long timestamp) {
        this.level = level;
        this.message = message;
        this.parameters = parameters;
        this.sequenceNumber = sequenceNumber;
        this.sourceClassName = sourceClassName;
        this.sourceMethodName = sourceMethodName;
        this.threadID = threadID;
        this.thrown = thrown;
        this.timestamp = timestamp;

    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("LogRecord [level=");
        builder.append(level);
        builder.append(", message=");
        builder.append(message);
        builder.append(", parameters=");
        builder.append(Arrays.toString(parameters));
        builder.append(", sequenceNumber=");
        builder.append(sequenceNumber);
        builder.append(", sourceClassName=");
        builder.append(sourceClassName);
        builder.append(", sourceMethodName=");
        builder.append(sourceMethodName);
        builder.append(", threadID=");
        builder.append(threadID);
        builder.append(", thrown=");
        builder.append(thrown);
        builder.append(", timestamp=");
        builder.append(timestamp);
        builder.append("]");
        return builder.toString();
    }
}
