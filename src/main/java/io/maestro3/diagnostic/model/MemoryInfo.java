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

package io.maestro3.diagnostic.model;


public class MemoryInfo {
    private long timestamp;
    private int usedMemKb;
    private int freeMemKb;
    private int maxMemKb;
    private int totalMemKb;
    private int memUsagePercent;

    /**
     * Constructor
     */
    public MemoryInfo(Runtime r) {
        set(r);
    }

    public void set(Runtime r) {
        timestamp = System.currentTimeMillis();
        if (r != null) {
            freeMemKb = (int) (r.freeMemory() / 1024);
            maxMemKb = (int) (r.maxMemory() / 1024);
            totalMemKb = (int) (r.totalMemory() / 1024);
            usedMemKb = totalMemKb - freeMemKb;
            memUsagePercent = (int) (100.0 - (100.0 * freeMemKb / totalMemKb));
        }
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getUsedMemKb() {
        return usedMemKb;
    }

    public void setUsedMemKb(int usedMemKb) {
        this.usedMemKb = usedMemKb;
    }

    public int getFreeMemKb() {
        return freeMemKb;
    }

    public void setFreeMemKb(int freeMemKb) {
        this.freeMemKb = freeMemKb;
    }

    public int getMaxMemKb() {
        return maxMemKb;
    }

    public void setMaxMemKb(int maxMemKb) {
        this.maxMemKb = maxMemKb;
    }

    public int getTotalMemKb() {
        return totalMemKb;
    }

    public void setTotalMemKb(int totalMemKb) {
        this.totalMemKb = totalMemKb;
    }

    public int getMemUsagePercent() {
        return memUsagePercent;
    }

    public void setMemUsagePercent(int memUsagePercent) {
        this.memUsagePercent = memUsagePercent;
    }

}
