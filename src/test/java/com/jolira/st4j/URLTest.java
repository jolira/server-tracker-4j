/**
 * Copyright (c) 2011 jolira. All rights reserved. This program and the accompanying materials are made available under
 * the terms of the GNU Public License 2.0 which is available at http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.jolira.st4j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

@SuppressWarnings("javadoc")
public class URLTest {

    @Test
    public void testParse2ndSimplest() {
        final URL parsed = URL.parse("jolira://x/");
        final String value = parsed.toString();

        assertEquals("jolira://x/", value);
    }

    @Test
    public void testParseHostPort() {
        final URL parsed = URL.parse("jolira://x:a");
        final String value = parsed.toString();

        assertEquals("jolira://x:a", value);
    }

    @Test
    public void testParseMalformed1() {
        final URL parsed = URL.parse("jolira");
        final String value = parsed.toString();

        assertEquals("jolira", value);
    }

    @Test
    public void testParseMalformed2() {
        final URL parsed = URL.parse("jolira:");
        final String value = parsed.toString();

        assertEquals("jolira:", value);
    }

    @Test
    public void testParseMalformed3() {
        final URL parsed = URL.parse("jolira:xxx");
        final String value = parsed.toString();

        assertEquals("jolira:xxx", value);
    }

    @Test
    public void testParseMalformed4() {
        final URL parsed = URL.parse("jolira:/xx");
        final String value = parsed.toString();

        assertEquals("jolira:/xx", value);
    }

    @Test
    public void testParseMalformed6() {
        final URL parsed = URL.parse("jolira:///");
        final String value = parsed.toString();

        assertEquals("jolira:///", value);
    }

    @Test
    public void testParseNull() {
        final URL parsed = URL.parse(null);

        assertNull(parsed);
    }

    @Test
    public void testParseQuery1() {
        final URL parsed = URL.parse("jolira://x?a=b");
        final String value = parsed.toString();

        assertEquals("jolira://x?a=b", value);
    }

    @Test
    public void testParseQuery2() {
        final URL parsed = URL.parse("jolira://x?a");
        final String value = parsed.toString();

        assertEquals("jolira://x?a", value);
    }

    @Test
    public void testParseQuery3() {
        final URL parsed = URL.parse("jolira://x?a=b");
        final String value = parsed.toString();

        assertEquals("jolira://x?a=b", value);
    }

    @Test
    public void testParseQuery4() {
        final URL parsed = URL.parse("jolira://x/y?a=b");
        final String value = parsed.toString();

        assertEquals("jolira://x/y?a=b", value);
    }

    @Test
    public void testParseSimplest() {
        final URL parsed = URL.parse("jolira://x");
        final String value = parsed.toString();

        assertEquals("jolira://x", value);
    }

    @Test
    public void testParseUserHost() {
        final URL parsed = URL.parse("jolira://a@x");
        final String value = parsed.toString();

        assertEquals("jolira://x", value);
    }

    @Test
    public void testParseUserPasswordHost() {
        final URL parsed = URL.parse("jolira://a:b@x");
        final String value = parsed.toString();

        assertEquals("jolira://x", value);
    }

    @Test
    public void testParseUserPasswordHostPort() {
        final URL parsed = URL.parse("jolira://a:b@x:1");
        final String value = parsed.toString();

        assertEquals("jolira://x:1", value);
    }
}
