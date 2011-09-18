package com.jolira.st4j.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.jolira.st4j.MetricStore;
import com.jolira.st4j.ServerTracker;

@SuppressWarnings("javadoc")
public class Demo {
    public static void main(final String[] args) {
        // visitor IDs should identify a user as uniquely as possible
        // On a browser is should be stored as a persistent cookie or in a
        // HTML5 data-store.
        final UUID visitorID = UUID.randomUUID();

        // this identifier should remain the same for the duration of the user session
        final UUID sessionID = UUID.randomUUID();

        final Executor executor = Executors.newCachedThreadPool();
        final MetricStore store = new MetricStoreImpl();
        final ServerTracker tracker = new ServerTrackerImpl(
                "tracker1.jolira.com:3080,tracker2.jolira.com:3080,tracker3.jolira.com", 2000, store, executor);
        final DemoMetric metric = new DemoMetric();
        final long startTime = System.currentTimeMillis();

        try {
            // Do something that needs to be measured
        } finally {
            final long duration = System.currentTimeMillis() - startTime;

            metric.startTime = startTime;
            metric.duration = duration;
        }

        tracker.postMetric(metric);

        final Map<String, Object> eventInfo = new HashMap<String, Object>();

        eventInfo.put("session", sessionID);
        eventInfo.put("visitor", visitorID);

        // send the metric (and any other that may have been created for this thread
        // to the remote server.
        tracker.submit(eventInfo);
    }
}
