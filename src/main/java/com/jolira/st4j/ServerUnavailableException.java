/**
 * Copyright (c) 2011 jolira. All rights reserved. This program and the accompanying materials are made available under
 * the terms of the GNU Public License 2.0 which is available at http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.jolira.st4j;

/**
 * Thrown when no remote server is available to receive the collected data.
 * 
 * @author jfk
 * @date Sep 17, 2011 10:10:40 AM
 * @since 1.0
 * 
 */
public class ServerUnavailableException extends RuntimeException {
    private static final long serialVersionUID = 3587373122137791931L;

    /**
     * Create a new instance.
     */
    public ServerUnavailableException() {
        super("no server available");
    }
}
