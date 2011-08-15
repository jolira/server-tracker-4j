package com.jolira.st4j.impl;

import static com.jolira.st4j.impl.ServerTrackerImpl.getLocalMetric;
import static com.jolira.st4j.impl.ServerTrackerImpl.postLocalMetric;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.inject.Named;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.TypeLiteral;
import com.jolira.st4j.Metric;

/**
 * Create metrics for Guice.
 * 
 * @author jfk
 * @date Aug 9, 2011 7:58:15 AM
 * @since 1.0
 * 
 */
public class MetricScope implements Scope {
    private static String getName(final Annotation annotation) {
        if (annotation == null) {
            return null;
        }

        if (annotation instanceof Metric) {
            final Metric metric = (Metric) annotation;

            return metric.value();
        }

        if (annotation instanceof Named) {
            final Named named = (Named) annotation;

            return named.value();
        }

        throw new IllegalArgumentException("only @" + Metric.class.getName() + " and @" + Named.class.getName()
                + " can be used to specify metrics.");
    }

    <T> T getScoped(final Key<T> key, final Provider<T> unscoped) {
        final TypeLiteral<T> literal = key.getTypeLiteral();
        final Type type = literal.getType();
        final Annotation annotation = key.getAnnotation();
        final String mname = getName(annotation);
        final Class<? extends Type> cls = type.getClass();
        @SuppressWarnings("unchecked")
        final T metric = (T)getLocalMetric(mname, cls);

        if (metric != null) {
            return metric;
        }

        final T _metric = unscoped.get();

        postLocalMetric(mname, _metric);

        return _metric;
    }

    @Override
    public <T> Provider<T> scope(final Key<T> key, final Provider<T> unscoped) {
        return new Provider<T>() {
            @Override
            public T get() {
                return getScoped(key, unscoped);
            }
        };
    }
}
