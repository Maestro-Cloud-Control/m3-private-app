/*
 * Copyright 2023 Maestro Cloud Control LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.maestro3.diagnostic.util;

import java.util.Locale;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;


public class MaestroThreadFactory implements ThreadFactory {

    private static final AtomicInteger generalPoolsCount = new AtomicInteger(1);
    private static final AtomicInteger generalThreadsCount = new AtomicInteger(1);

    private final int thisPoolNumber;

    private final AtomicInteger threadNumber = new AtomicInteger(1);

    private final String namePrefix;
    private final String poolNumberPrefix;

    public MaestroThreadFactory(String namePrefix) {
        this(namePrefix, "-pool-");
    }

    public MaestroThreadFactory(String namePrefix, String poolNumberPrefix) {
        this.namePrefix = namePrefix.toLowerCase(Locale.US);
        this.poolNumberPrefix = poolNumberPrefix.toLowerCase(Locale.US);
        this.thisPoolNumber = getAndIncrementOverallPoolsCount();
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r, getName());
        configureThread(thread);
        return thread;
    }

    public static int getPoolsCount() {
        return generalPoolsCount.get();
    }

    public static int getThreadsCount() {
        return generalThreadsCount.get();
    }

    protected void configureThread(Thread thread) {
        // don't know, copied from java.util.concurrent.Executors.DefaultThreadFactory, which we used everywhere before, just in case
        if (thread.isDaemon()) {
            thread.setDaemon(false);
        }
        if (thread.getPriority() != Thread.NORM_PRIORITY) {
            thread.setPriority(Thread.NORM_PRIORITY);
        }
    }

    protected int getAndIncrementOverallPoolsCount() {
        return generalPoolsCount.getAndIncrement();
    }

    private final String getName() {
        incrementTotalThreadsCount();
        return namePrefix + poolNumberPrefix + thisPoolNumber + "-thread-" + threadNumber.getAndIncrement();
    }

    protected void incrementTotalThreadsCount() {
        generalThreadsCount.incrementAndGet();
    }
}
