package com.jolira.st4j.impl;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.jolira.st4j.ServerTracker;

@SuppressWarnings("javadoc")
public class Demo {
    public static void main(final String[] args) {
        final Executor executor = Executors.newCachedThreadPool();
        final ServerTracker tracker = new ServerTrackerImpl("tracker.jolira.com:3080", executor);
        final DemoMetric metric = new DemoMetric();
        final long startTime = System.currentTimeMillis();

        try {
            // Do something that needs to be measured
        }
        finally {
            final long duration = System.currentTimeMillis() - startTime;

            metric.startTime = startTime;
            metric.duration = duration;
        }

        tracker.postMetric(metric);

        // send the metric (and  any other that may have been created for this thread
        // to the remote server.
        tracker.submit();
    }
}
