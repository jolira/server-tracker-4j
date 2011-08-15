package com.jolira.st4j.impl;

import com.jolira.st4j.Metric;

@SuppressWarnings("javadoc")
@Metric("demo")
public interface DemoMetric {
    public void setStartTime(final long startTime);
    public void setDuration(final long duration);
}
