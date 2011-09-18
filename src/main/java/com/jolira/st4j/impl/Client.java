/**
 * Copyright (c) 2011 jolira. All rights reserved. This program and the accompanying materials are made available under
 * the terms of the GNU Public License 2.0 which is available at http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.jolira.st4j.impl;

import static java.net.HttpURLConnection.HTTP_OK;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jolira.st4j.ServerUnavailableException;

/**
 * @author jfk
 * @date Sep 17, 2011 9:46:11 AM
 * @since 1.0
 * 
 */
class Client {
    private final static Logger LOG = LoggerFactory.getLogger(Client.class);
    private final int timeout;
    private final List<URL> urls;
    private final ByteArrayOutputStream buffer;

    public Client(final Collection<URL> urls, final int timeout) {
        this.urls = new ArrayList<URL>(urls);
        this.timeout = timeout;
        buffer = new ByteArrayOutputStream();
    }

    OutputStream getOutputStream() {
        return buffer;
    }

    private URL getURL(final Random random) throws ServerUnavailableException {
        final int size = urls.size();

        if (size < 1) {
            throw new ServerUnavailableException();
        }

        final int pos = random.nextInt(size);

        return urls.remove(pos);
    }

    void transmit() throws ServerUnavailableException {
        final Random random = new Random();

        for (;;) {
            final URL url = getURL(random);

            LOG.info("transmitting to {}", url);

            try {
                final int code = transmit(url);

                if (code == HTTP_OK) {
                    return;
                }

                LOG.warn("server returned {}", Integer.valueOf(code));
            } catch (final IOException e) {
                LOG.warn("exception while transmitting", e);
            }
        }
    }

    private int transmit(final URL url) throws IOException {
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setConnectTimeout(timeout);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setDoInput(true);

        final byte[] content = buffer.toByteArray();

        final OutputStream out = conn.getOutputStream();

        out.write(content);

        return conn.getResponseCode();
    }
}
