/**
 * Copyright (c) 2011 jolira. All rights reserved. This program and the accompanying materials are made available under
 * the terms of the GNU Public License 2.0 which is available at http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.jolira.st4j;

import java.util.Arrays;

/**
 * {@link java.lang.Throwable} has links and causes problems for GSON. Also, using this class would create objects of
 * unpredictable size.
 * 
 * @author jfk
 * @date Aug 19, 2011 12:32:07 PM
 * @since 1.0
 * 
 */
public class Throwable {
    private final String message;

    private final String type;

    private final StackTraceElement[] stackTrace;

    /**
     * Create a new one.
     * 
     * @param type
     * @param message
     * @param stackTrace
     * 
     * 
     */
    public Throwable(final String type, final String message, final StackTraceElement[] stackTrace) {
        this.type = type;
        this.message = message;
        this.stackTrace = stackTrace;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Throwable [message=");
        builder.append(message);
        builder.append(", type=");
        builder.append(type);
        builder.append(", stackTrace=");
        builder.append(Arrays.toString(stackTrace));
        builder.append("]");
        return builder.toString();
    }
}
