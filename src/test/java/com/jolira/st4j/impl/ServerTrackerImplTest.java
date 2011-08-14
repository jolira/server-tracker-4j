package com.jolira.st4j.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jolira.st4j.Metric;
import com.jolira.testing.WebServerEmulator;

@SuppressWarnings("javadoc")
public class ServerTrackerImplTest {
    final static Logger LOG = LoggerFactory.getLogger(ServerTrackerImplTest.class);

    interface MyMetric {
        void setValueA(final long a);
        void setValueB(final String b);
    }

    @Metric
    interface MySecondMetric {
        void setValueC(final long a);
    }

    @Metric("thrid")
    static class MyThirdMetric {
        void setValueD(final String b){}
    }


    @Test
    public void testGetMetric() {
        final ServerTrackerImpl tracker = new ServerTrackerImpl("localhost:3080", null);
        final MyMetric m1 = tracker.getMetric(MyMetric.class);
        final MyMetric m1_2 = tracker.getMetric(MyMetric.class);
        final MySecondMetric m2 = tracker.getMetric(MySecondMetric.class);
        final MyThirdMetric m3 = tracker.getMetric(MyThirdMetric.class);

        m1.setValueA(1234567890l);
        m1.setValueB("test");
        m2.setValueC(0);
        m3.setValueD("jolira");

        final Map<String, Map<String, Object>> collected = tracker.getLocal();
        final int size = collected.size();
        final Map<String, Object> r1 = collected.get("com.jolira.st4j.impl.servertrackerimpltest$mymetric");
        final Map<String, Object> r2 = collected.get("com.jolira.st4j.impl.servertrackerimpltest$mysecondmetric");
        final Map<String, Object> r3 = collected.get("thrid");
        final Object v1 = r1.get("valueA");
        final Object v2 = r1.get("valueB");
        final Object v3 = r2.get("valueC");
        final Object v4 = r3.get("valueD");

        assertSame(m1, m1_2);
        assertEquals(3, size);
        assertEquals(Long.valueOf(1234567890l), v1);
        assertEquals("test", v2);
        assertEquals(Long.valueOf(0), v3);
        assertEquals("jolira", v4);
    }

    @Test
    public void testSubmit() throws Exception {
        final WebServerEmulator server = new WebServerEmulator() {
            @Override
            public void handle(final String target, final HttpServletRequest request, final HttpServletResponse response)
                    throws IOException, ServletException {
                final String contentType = request.getContentType();
                assertEquals("/submit/metric", target);
                assertEquals("application/json", contentType);

                final ServletInputStream in = request.getInputStream();
                final InputStreamReader _in = new InputStreamReader(in);
                final char[] buffer = new char[2048];

                _in.read(buffer);

                final String body = new String(buffer);

                LOG.info("body: {}", body);

                if (!body.contains("com.jolira.st4j.impl.servertrackerimpltest$mymetric\":{")) {
                    fail("body does not contain com.jolira.st4j.impl.servertrackerimpltest$mymetric: " + body);
                }

                if (!body.contains("\"thrid\":{\"valueD\":\"jolira\"}")) {
                    fail("body does not contain thrid: " + body);
                }

                response.setContentType(contentType);

                final ServletOutputStream out = response.getOutputStream();
                final OutputStreamWriter writer = new OutputStreamWriter(out);

                writer.append("true");
                writer.close();
            }
        };

        server.start();

        final String name = server.getName();

        try {
            final ServerTrackerImpl tracker = new ServerTrackerImpl(name, new Executor(){
                @Override
                public void execute(final Runnable command) {
                    command.run();
                }});
            final MyMetric m1 = tracker.getMetric(MyMetric.class);
            tracker.getMetric(MyMetric.class);
            final MySecondMetric m2 = tracker.getMetric(MySecondMetric.class);
            final MyThirdMetric m3 = tracker.getMetric(MyThirdMetric.class);

            m1.setValueA(1234567890l);
            m1.setValueB("test");
            m2.setValueC(0);
            m3.setValueD("jolira");

            tracker.submit();
            tracker.submit();
        }
        finally {
            server.stop();
        }
    }
}
