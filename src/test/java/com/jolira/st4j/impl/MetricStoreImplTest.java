package com.jolira.st4j.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.Collection;
import java.util.Map;

import org.junit.Test;

import com.jolira.st4j.Metric;
import com.jolira.st4j.MetricStore;

@SuppressWarnings("javadoc")
public class MetricStoreImplTest {
    @Metric("x")
    static class MyMetric {
        // mothing
    }

    @Test
    public void testGetName1() {
        final MetricStore store = new MetricStoreImpl();
        final String name1 = store.getMetricName(null, MyMetric.class);

        assertEquals("x", name1);
    }

    @Test
    public void testGetName2() {
        final MetricStore store = new MetricStoreImpl();
        final String name1 = store.getMetricName(null, String.class);

        assertEquals("java.lang.string", name1);
    }

    @Test
    public void testPostNotUnique() {
        final MetricStore store = new MetricStoreImpl();

        store.postThreadLocalMeasurement("test", store, false);
        store.postThreadLocalMeasurement("test", store, false);
        store.postThreadLocalMeasurement("test", store, false);

        final Map<String, Object> metrics = store.getAndResetThreadLocalMetrics();

        assertEquals(1, metrics.size());
        @SuppressWarnings("unchecked")
        final Collection<Object> object = (Collection<Object>) metrics.get("test");

        assertEquals(3, object.size());
        assertSame(store, object.iterator().next());
        assertSame(store, object.iterator().next());
        assertSame(store, object.iterator().next());
    }

    @Test
    public void testPostUnique() {
        final MetricStore store = new MetricStoreImpl();

        store.postThreadLocalMeasurement("test", store, true);

        final Map<String, Object> metrics = store.getAndResetThreadLocalMetrics();

        assertEquals(1, metrics.size());
        final Object object = metrics.get("test");

        assertSame(store, object);
    }
}
