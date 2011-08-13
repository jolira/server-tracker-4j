package com.jolira.st4j.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

@SuppressWarnings("javadoc")
public class MetricBeanTest {
    static abstract class MyMetric {
        abstract void set(byte b);

        abstract void setA();

        abstract void setA(int a);

        int setB(final int x) {
            return x;
        }

        void setBplus(final String x) {
        }

        void xxxBplus(final String x) {
        }
    }

    @Test
    public void testIntercept() {
        final int[] counter = new int[]{0};
        final MetricBean<MyMetric> metricBean = new MetricBean<MyMetric>(MyMetric.class) {
            @Override
            protected void add(final String name, final Object value) {
                switch(counter[0]++) {
                case 0:
                    assertEquals(Integer.valueOf(1), value);
                    assertEquals("a", name);
                    break;
                case 1:
                    assertEquals("string", value);
                    assertEquals("bplus", name);
                    break;
                default:
                    fail();
                }
            }
        };

        metricBean.metric.set((byte)'b');
        metricBean.metric.setA();
        metricBean.metric.setA(1);
        metricBean.metric.setB(6);
        metricBean.metric.setBplus("string");
        metricBean.metric.xxxBplus("string");
    }

}
