package com.jolira.st4j.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.Map;

import org.junit.Test;

import com.jolira.st4j.Metric;

@SuppressWarnings("javadoc")
public class ServerTrackerImplTest {
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
        final ServerTrackerImpl tracker = new ServerTrackerImpl("localhost:3080");
        final MyMetric m1 = tracker.getMetric(MyMetric.class);
        final MyMetric m1_2 = tracker.getMetric(MyMetric.class);
        final MySecondMetric m2 = tracker.getMetric(MySecondMetric.class);
        final MyThirdMetric m3 = tracker.getMetric(MyThirdMetric.class);

        m1.setValueA(1234567890l);
        m1.setValueB("test");
        m2.setValueC(0);
        m3.setValueD("jolira");

        final Map<String, Map<String, Object>> collected = tracker.getCollected();
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

}
