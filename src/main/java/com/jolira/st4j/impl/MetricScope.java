package com.jolira.st4j.impl;

import java.lang.annotation.Annotation;

import javax.inject.Named;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.TypeLiteral;
import com.jolira.st4j.Metric;
import com.jolira.st4j.MetricStore;

/**
 * Create metrics for Guice.
 * 
 * @author jfk
 * @date Aug 9, 2011 7:58:15 AM
 * @since 1.0
 * 
 */
public class MetricScope implements Scope {
    private static <T> Annotation getAnnotation(final Key<T> key) {
        final Annotation annotation = key.getAnnotation();

        if (annotation != null) {
            return annotation;
        }

        final TypeLiteral<T> literal = key.getTypeLiteral();
        final Class<? super T> raw = literal.getRawType();

        return raw.getAnnotation(Metric.class);
    }

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

    private static boolean isUnique(final Annotation annotation) {
        if (annotation == null || !(annotation instanceof Metric)) {
            return true; // default
        }

        final Metric metric = (Metric) annotation;

        return metric.unique();
    }

    private final MetricStore store;

    /**
     * Create a new scope.
     * 
     * @param store
     *            the store to use
     */
    public MetricScope(final MetricStore store) {
        this.store = store;
    }

    <T> T getScoped(final Key<T> key, final Provider<T> unscoped) {
        final TypeLiteral<T> literal = key.getTypeLiteral();
        final Class<? super T> type = literal.getRawType();
        final Annotation annotation = getAnnotation(key);
        final String mname = getName(annotation);
        final boolean unique = isUnique(annotation);

        if (unique) {
            @SuppressWarnings("unchecked")
            final T metric = (T) store.getThreadLocalMeasurement(mname, type);

            if (metric != null) {
                return metric;
            }
        }

        final T _metric = unscoped.get();

        store.postThreadLocalMeasurement(mname, _metric, unique);

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
