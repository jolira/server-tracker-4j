/**
 * Copyright (c) 2011 jolira. All rights reserved. This program and the accompanying materials are made available under
 * the terms of the GNU Public License 2.0 which is available at http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.jolira.st4j.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;

/**
 * A simple abstraction for creating network clients. The clients implement a very simple fail-over mechanism.
 * 
 * @author jfk
 * @date Sep 17, 2011 8:41:28 AM
 * @since 1.0
 * 
 */
public class ClientFactory {
    private static URL makeURL(final String server) throws IllegalArgumentException {
        try {
            return new URL("http://" + server + "/submit/events");
        } catch (final MalformedURLException e) {
            throw new IllegalArgumentException("invalid server " + server, e);
        }
    }

    private final int timeout;

    private final Collection<URL> urls;

    ClientFactory(final String servers, final int timeout) throws IllegalArgumentException {
        final Collection<URL> urls_ = new ArrayList<URL>();
        final StringTokenizer izer = new StringTokenizer(servers, ",");

        while (izer.hasMoreTokens()) {
            final String token = izer.nextToken();
            final String server = token.trim();
            final URL url = makeURL(server);

            urls_.add(url);
        }

        urls = urls_;
        this.timeout = timeout;
    }

    Client makeClient() {
        return new Client(urls, timeout);
    }
}
