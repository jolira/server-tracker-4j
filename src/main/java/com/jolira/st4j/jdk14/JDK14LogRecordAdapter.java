/**
 * Copyright (c) 2011 jolira. All rights reserved. This program and the accompanying materials are made available under
 * the terms of the GNU Public License 2.0 which is available at http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.jolira.st4j.jdk14;

import com.jolira.st4j.LogRecord;

/**
 * An adapter for JDK 1.5 logging.
 * 
 * @author jfk
 * @date Aug 14, 2011 9:25:54 PM
 * @since 1.0
 * @see java.util.logging.LogRecord
 * 
 */
public class JDK14LogRecordAdapter extends LogRecord {
    /**
     * Create a new one.
     * 
     * @param record
     *            the record to be adapted
     */
    public JDK14LogRecordAdapter(final java.util.logging.LogRecord record) {
        super(record.getLevel().getName(), record.getMessage(), record.getParameters(), record.getSequenceNumber(),
                record.getSourceClassName(), record.getSourceMethodName(), record.getThreadID(),
                record.getThrown() != null ? new JDK14LThrowableAdapter(record.getThrown()) : null, record.getMillis());
    }
}
