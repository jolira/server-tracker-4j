server-tracker-4j
=======================

A java client library for the Server Tracker. (http://github.com/jolira/server-tracker)

The basic function of this library is to collect metrics inside a Java application
or an application server and submit these metrics to the remote Server Tracker
asynchronously.

This library has been build to be used with Google's Guice, but can also be run
without code injection. The instructions below show how to use the abstraction
standalone and with Guice.

Stand-Along Usage
--------------------

Here is a very simple example for how to use this library (without Guice):

```
        final Executor executor = Executors.newCachedThreadPool();
        final ServerTracker tracker = new ServerTrackerImpl("tracker.jolira.com:3080", executor);
        final DemoMetric metric = tracker.getMetric(DemoMetric.class);
        final long startTime = System.currentTimeMillis();

        try {
            // Do something that needs to be measured
        }
        finally {
            final long duration = System.currentTimeMillis() - startTime;

            metric.setStartTime(startTime);
            metric.setDuration(duration);
        }

        // send the metric (and  any other that may have been created for this thread
        // to the remote server.
        tracker.submit();
```

