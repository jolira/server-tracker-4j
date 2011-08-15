A Server Tracker Client Library 4 Java
======================================

A java client library for the [Server Tracker](http://github.com/jolira/server-tracker).

The basic function of this library is to collect metrics inside a Java application
or an application server and submit these metrics to the remote Server Tracker
asynchronously.

This library has been build to be used with Google's Guice, but can also be run
without code injection. The instructions below show how to use the abstraction
standalone and with Guice.

Stand-Alone Usage
-----------------

Here is a very simple example for how to use this library (without Guice):

```
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
