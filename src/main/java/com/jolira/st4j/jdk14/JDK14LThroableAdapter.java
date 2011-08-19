/**
 * Copyright (c) 2011 jolira. All rights reserved. This program and the accompanying materials are made available under
 * the terms of the GNU Public License 2.0 which is available at http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.jolira.st4j.jdk14;

import com.jolira.st4j.Throwable;

/**
 * An adapter for {@link java.lang.Throwable}.
 * 
 * @author jfk
 * @date Aug 19, 2011 12:44:59 PM
 * @since 1.0
 * 
 */
public class JDK14LThroableAdapter extends Throwable {
    /**
     * Create a new adapter.
     * 
     * @param e
     *            the exception that was thrown
     */
    public JDK14LThroableAdapter(final java.lang.Throwable e) {
        super(e.getClass().toString(), e.getMessage(), e.getStackTrace());
    }
}
