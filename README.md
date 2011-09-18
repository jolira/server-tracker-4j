A Server Tracker Client Library 4 Java
======================================

A java client library for the [Server Tracker](http://github.com/jolira/server-tracker).

The basic function of this library is to collect arbitrary metrics and logs inside a
Java application or an application server and submit these metrics to remote  Server 
Tracker with low-overhead so the data is available for display.

This library has been build to be used with Google's Guice, but can also be run
without code injection. The instructions below show how to use the abstraction
standalone and with Guice.

Stand-Alone Usage
-----------------

Here is a very simple example for how to use this library (without Guice):

```
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
```

This example creates one metric that captures start time and performance of
some piece of code and sends the results to the server using a background
thread.

The full source code of this example can be found at 
[Demo.java](http://raw.github.com/jolira/server-tracker-4j/master/src/test/java/com/jolira/st4j/impl/Demo.java).

Metrics
-------

This library uses [GSON](http://code.google.com/p/google-gson/) and can handle any metric object that can be serialized
using this library. Metric should be simple and do not have to have public scope, as shown in
[DemoMetric.java](http://raw.github.com/jolira/server-tracker-4j/master/src/test/java/com/jolira/st4j/impl/DemoMetric.java).

```
class DemoMetric {
    long startTime;
    long duration;
}
```
