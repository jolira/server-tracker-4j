/**
 * Copyright (c) 2011 jolira. All rights reserved. This program and the accompanying materials are made available under
 * the terms of the GNU Public License 2.0 which is available at http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.jolira.st4j;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.google.inject.ScopeAnnotation;

/**
 * Used to annotate any metric instance.
 * 
 * @author jfk
 * @date Aug 12, 2011 8:54:14 PM
 * @since 1.0
 * 
 */
@Target({ TYPE, FIELD, PARAMETER })
@Retention(RUNTIME)
@ScopeAnnotation
@Documented
public @interface Metric {
    /**
     * @return the name to be for the metric when marshalling
     */
    String value() default "##default";
}
