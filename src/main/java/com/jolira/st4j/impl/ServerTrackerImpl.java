/**
 * Copyright (c) 2011 jolira.
 * All rights reserved. This program and the accompanying
 * materials are made available under the terms of the
 * GNU Public License 2.0 which is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.jolira.st4j.impl;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.jolira.st4j.ServerTracker;

/**
 * 
 * @author jfk
 * @date Aug 12, 2011 9:09:31 PM
 * @since 1.0
 *
 */
@Singleton
public class ServerTrackerImpl implements ServerTracker {
    @Inject
    ServerTrackerImpl(@Named("ServerTrackerServer") final String server) {
        //
    }
    @Override
    public <T> T getMetric(final Class<T> type) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void submit() {
        // TODO Auto-generated method stub
    }

}
