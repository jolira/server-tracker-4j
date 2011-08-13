/**
 * Copyright (c) 2011 jolira. All rights reserved. This program and the accompanying materials are made available under
 * the terms of the GNU Public License 2.0 which is available at http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.jolira.st4j.impl;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

/**
 * @author jfk
 * @date Aug 12, 2011 9:23:20 PM
 * @since 1.0
 * 
 */
abstract class MetricBean<T> {
    final T metric;

    MetricBean(final Class<T> superclass) {
        final Enhancer e = new Enhancer();

        e.setSuperclass(superclass);
        e.setCallback(new MethodInterceptor() {
            @Override
            public Object intercept(final Object obj, final Method method, final Object[] args, final MethodProxy proxy)
                    throws Throwable {
                return MetricBean.this.intercept(obj, method, args, proxy);
            }
        });

        @SuppressWarnings("unchecked")
        final T _metric = (T) e.create();

        metric = _metric;
    }

    protected abstract void add(String name, Object value);

    private String getPropertyName(final Method method, final Object[] args) {
        if (args.length != 1) {
            return null;
        }

        final Class<?> returnType = method.getReturnType();

        if (!Void.TYPE.equals(returnType)) {
            return null;
        }

        final String name = method.getName();

        if (!name.startsWith("set")) {
            return null;
        }

        final int length = name.length();

        if (length < 4) {
            return null;
        }

        final char first = name.charAt(3);
        final char upperFirst = Character.toLowerCase(first);
        final String rest = name.substring(4);
        final StringBuilder buf = new StringBuilder();

        buf.append(upperFirst);
        buf.append(rest);

        return buf.toString();
    }

    final Object intercept(final Object obj, final Method method, final Object[] args, final MethodProxy proxy)
            throws Throwable {
        final String name = getPropertyName(method, args);

        if (name != null) {
            add(name, args[0]);
        }

        final int modifiers = method.getModifiers();

        if (Modifier.isAbstract(modifiers)) {
            return null;
        }

        return proxy.invokeSuper(obj, args);
    }
}
