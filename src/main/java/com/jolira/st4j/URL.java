/**
 * Copyright (c) 2011 jolira. All rights reserved. This program and the accompanying materials are made available under
 * the terms of the GNU Public License 2.0 which is available at http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.jolira.st4j;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;

/**
 * A URL representation that is marshalled by its components.
 * 
 * @author jfk
 * @date Sep 17, 2011 10:08:58 PM
 * @since 1.0
 * 
 */
public class URL {
    private static class MalformedURL extends URL {
        private final String url;

        MalformedURL(final String url) {
            this.url = url;
        }

        @Override
        public String toString() {
            return url;
        }
    }

    private static class Param {
        String key;
        String value;

        Param(final String key, final String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append("Param [key=");
            builder.append(key);
            builder.append(", value=");
            builder.append(value);
            builder.append("]");
            return builder.toString();
        }
    }

    private static class WellformedURL extends URL {
        private final String protocol;
        private final String username;
        private final String password;
        private final String host;
        private final String port;
        private final String path;
        private final Collection<Param> params;

        WellformedURL(final String protocol, final String username, final String password, final String host,
                final String port, final String path, final Collection<Param> params) {
            this.protocol = protocol;
            this.username = username;
            this.password = password;
            this.host = host;
            this.port = port;
            this.path = path;
            this.params = params;
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append("{protocol=");
            builder.append(protocol);

            if (username != null) {
                builder.append(", username=");
                builder.append(username);
            }

            if (password != null) {
                builder.append(", password=");
                builder.append(password);
            }

            builder.append(", host=");
            builder.append(host);

            if (port != null) {
                builder.append(", port=");
                builder.append(port);
            }

            if (path != null) {
                builder.append(", path=");
                builder.append(path);
            }

            if (params != null) {
                builder.append(", params=");
                builder.append(params);
            }

            builder.append("}");

            return builder.toString();
        }
    }

    private static String decode(final String value) {
        if (value == null) {
            return null;
        }

        try {
            return URLDecoder.decode(value, "utf-8");
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getHost(final String value) {
        final int idx = value.indexOf('@');
        final String hostport = value.substring(idx + 1);
        final int colonPos = hostport.indexOf(':');

        if (colonPos == -1) {
            return hostport;
        }

        return hostport.substring(0, colonPos);
    }

    private static String getPassword(final String value) {
        final int idx = value.indexOf('@');

        if (idx == -1) {
            return null;
        }

        final String userpass = value.substring(0, idx);
        final int colonPos = userpass.indexOf(':');

        if (colonPos == -1) {
            return null;
        }

        return userpass.substring(colonPos + 1);
    }

    private static String getPort(final String value) {
        final int idx = value.indexOf('@');
        final String hostport = value.substring(idx + 1);
        final int colonPos = hostport.indexOf(':');

        if (colonPos == -1) {
            return null;
        }

        return hostport.substring(colonPos + 1);
    }

    private static String getUsername(final String value) {
        final int idx = value.indexOf('@');

        if (idx == -1) {
            return null;
        }

        final String userpass = value.substring(0, idx);
        final int colonPos = userpass.indexOf(':');

        if (colonPos == -1) {
            return userpass;
        }

        return userpass.substring(0, colonPos);
    }

    /**
     * Parse a URL and return a representation that makes it easy to serialize the components.
     * 
     * @param url
     *            the url to be parsed
     * 
     * @return a URL representation
     */
    public static URL parse(final String url) {
        if (url == null) {
            return null;
        }

        final int portColonPos = url.indexOf(':');

        if (portColonPos == -1) {
            return new MalformedURL(url);
        }

        final String protocol = url.substring(0, portColonPos);
        final int len = url.length();

        if (portColonPos + 3 >= len) {
            return new MalformedURL(url);
        }

        final char slash1 = url.charAt(portColonPos + 1);
        final char slash2 = url.charAt(portColonPos + 2);
        final char firstSvrQualChar = url.charAt(portColonPos + 3);

        if (slash1 != '/' || slash2 != '/' || firstSvrQualChar == '/') {
            return new MalformedURL(url);
        }

        final int slash3pos = url.indexOf('/', portColonPos + 3);
        final int pathStartPos = slash3pos == -1 ? url.indexOf('?') : slash3pos;
        final String svrQual = pathStartPos == -1 ? url.substring(portColonPos + 3) : url.substring(portColonPos + 3,
                pathStartPos);
        final String username = getUsername(svrQual);
        final String password = getPassword(svrQual);
        final String host = getHost(svrQual);
        final String port = getPort(svrQual);

        if (pathStartPos == -1) {
            return new WellformedURL(protocol, username, password, host, port, null, null);
        }

        final String urlPath = url.substring(pathStartPos);
        final int questionMarkPos = urlPath.indexOf('?');

        if (questionMarkPos == -1) {
            return new WellformedURL(protocol, username, password, host, port, urlPath, null);
        }

        final String path = urlPath.substring(0, questionMarkPos);
        final String query = urlPath.substring(questionMarkPos + 1);
        final Collection<Param> params = parseParams(query);

        return new WellformedURL(protocol, username, password, host, port, path, params);
    }

    private static Collection<Param> parseParams(final String query) {
        final StringTokenizer izer = new StringTokenizer(query, "&");
        final Collection<Param> params = new ArrayList<Param>();

        while (izer.hasMoreTokens()) {
            final String token = izer.nextToken();
            final int equalsPos = token.indexOf('=');
            final String key = equalsPos == -1 ? token : token.substring(0, equalsPos);
            final String value = equalsPos == -1 ? null : token.substring(equalsPos + 1);
            final String decodedKey = decode(key);
            final String decodedValue = decode(value);
            final Param param = new Param(decodedKey, decodedValue);

            params.add(param);
        }

        return params;
    }
}
