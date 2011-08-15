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

This example creates one metric that captures start time and performance of
some piece of code and sends the results to the server using a background
thread.

The full source code of this example can be found at 
[Demo.java](http://raw.github.com/jolira/server-tracker-4j/master/src/test/java/com/jolira/st4j/impl/Demo.java).

Metrics
-------

The client is very flexible when it comes to metrics. Users may define arbitrary
metric interfaces or objects. An example for a metric object can be found at
[DemoMetric.java](http://raw.github.com/jolira/server-tracker-4j/master/src/test/java/com/jolira/st4j/impl/DemoMetric.java).

```
@Metric("demo")
public interface DemoMetric {
    public void setStartTime(final long startTime);
    public void setDuration(final long duration);
}
```

There is no need to create an implementation for the interface. The ServerTracker
will automatically generate one. All values that are set using the method that
follow the conventions for standard bean-pattern setters will be recorded and
send to the remote server when ``ServerTracker#submit()`` is called.

In other words, any value set on a metric object will be recorded if the following
conditions are met:

1. The metric object is instantiated by calling ``ServerTracker#getMetric(Class<T>)``
   or ``ServerTracker#getMetric(String, Class<T>)``
2. The value is set on the metric object by a valid setter. Valid setter method 
   return ``void``, their name starts with the prefix ``set`` and they have
   one parameter. 

Any class or interface used as a metric object should also be tagged with the
``@Metric`` annotation. This annotation is required when using the metric with
Guice, but is also advantageous when running the library stand-alone as it
allows the user to specify an alias for identifying the metric in the
collected data.

