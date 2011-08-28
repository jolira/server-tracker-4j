package com.jolira.st4j.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jolira.st4j.Metric;
import com.jolira.st4j.MetricStore;
import com.jolira.st4j.jdk14.JDK14LogRecordAdapter;
import com.jolira.testing.WebServerEmulator;

@SuppressWarnings("javadoc")
public class ServerTrackerImplTest {
    static class MyMetric {
        long valueA;
        String valueB;
    }

    @Metric
    static class MySecondMetric {
        long valueC;
    }

    @Metric("third")
    static class MyThirdMetric {
        String valueD;
    }

    final static Logger LOG = LoggerFactory.getLogger(ServerTrackerImplTest.class);

    @Test
    public void testGetMetric() {
        final MetricStore store = new MetricStoreImpl();
        final ServerTrackerImpl tracker = new ServerTrackerImpl("localhost:3080", store, null);
        final MyMetric m1 = new MyMetric();
        final MySecondMetric m2 = new MySecondMetric();
        final MyThirdMetric m3 = new MyThirdMetric();

        m1.valueA = 1234567890l;
        m1.valueB = "test";
        m2.valueC = 0;
        m3.valueD = "jolira";

        tracker.postMetric(m1);
        tracker.postMetric(m2);
        tracker.postMetric(m3);

        final MyMetric r1 = store.getThreadLocalMetric(null, MyMetric.class);
        final MySecondMetric r2 = store.getThreadLocalMetric("com.jolira.st4j.impl.servertrackerimpltest$mysecondmetric",
                MySecondMetric.class);
        final MyThirdMetric r3 = store.getThreadLocalMetric("third", MyThirdMetric.class);

        assertSame(m1, r1);
        assertSame(m2, r2);
        assertSame(m3, r3);
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

                if (!body.contains("\"third\":{\"valueD\":\"jolira\"}")) {
                    fail("body does not contain third: " + body);
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
        final MetricStore store = new MetricStoreImpl();

        try {
            final ServerTrackerImpl tracker = new ServerTrackerImpl(name, store, new Executor() {
                @Override
                public void execute(final Runnable command) {
                    command.run();
                }
            });
            final MyMetric m1 = new MyMetric();
            final MySecondMetric m2 = new MySecondMetric();
            final MyThirdMetric m3 = new MyThirdMetric();

            m1.valueA = 1234567890l;
            m1.valueB = "test";
            m2.valueC = 0;
            m3.valueD = "jolira";

            tracker.postMetric(m1);
            tracker.postMetric(m2);
            tracker.postMetric(m3);
            tracker.post(new JDK14LogRecordAdapter(new LogRecord(Level.SEVERE, "")));
            tracker.submit();
            tracker.submit();
        } finally {
            server.stop();
        }
    }
}
