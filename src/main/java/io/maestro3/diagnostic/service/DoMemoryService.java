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

package io.maestro3.diagnostic.service;

import io.maestro3.diagnostic.model.MemoryInfo;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


@Component
public class DoMemoryService implements InitializingBean {

    // processing policy
    private int fCheckTimeInSec;
    private int fStoreTimeInSec;
    private MemoryInfo[] memoryInfoList;
    private Timer fMemoryTimer;

    /**
     * Constructor
     */
    public DoMemoryService() {
        super();
        fCheckTimeInSec = 5; // 5 sec
        fStoreTimeInSec = 7200; // 2 h
        memoryInfoList = null;
        fMemoryTimer = new Timer("app-memory");
    }

    public void setCheckTimeInSec(int aValue) {
        fCheckTimeInSec = aValue;
    }

    public void setStoreTimeInSec(int aValue) {
        fStoreTimeInSec = aValue;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // fill ui
        Runtime runtime = Runtime.getRuntime();
        int iCount = (fStoreTimeInSec / fCheckTimeInSec) + 1;
        memoryInfoList = new MemoryInfo[iCount];
        for (int i = 0; i < iCount; i++) {
            memoryInfoList[i] = new MemoryInfo(runtime);
        }
        // start scheduler task
        int timeMsec = fCheckTimeInSec * 1000;
        TimerTask task = new MemoryInfoCollector();
        fMemoryTimer.scheduleAtFixedRate(task, timeMsec, timeMsec);
    }

    public List<MemoryInfo> getMemoryInfo(int lastTimeMin) {
        if (memoryInfoList == null)
            return null;
        int iCount = memoryInfoList.length - 1;
        if (iCount < 2)
            return null;
        long timestamp = System.currentTimeMillis();
        int maxVal = (lastTimeMin) > 0 ? lastTimeMin * 60 * 1000 : fStoreTimeInSec * 1000;
        int minVal = fCheckTimeInSec * 1000 * 2;  // skip last value
        List<MemoryInfo> memoryInfoListForReturn = new ArrayList<>();
        for (int i = 0; i < iCount; i++) {
            long val = timestamp - memoryInfoList[i].getTimestamp();
            if ((val > minVal) && val < maxVal) {
                memoryInfoListForReturn.add(memoryInfoList[i]);
            }
        }
        memoryInfoListForReturn.sort(Comparator.comparingLong(MemoryInfo::getTimestamp).reversed());
        return memoryInfoListForReturn;
    }

    /**
     * MemoryInfoCollector class
     */
    protected class MemoryInfoCollector extends TimerTask {

        public void run() {
            // init
            int max = Integer.MIN_VALUE;
            long timestamp = System.currentTimeMillis();
            int iCount = memoryInfoList.length; // ignore last: for app initialization state info;
            // find oldest
            int index = 0;
            for (int i = 0; i < iCount; i++) {
                MemoryInfo info = memoryInfoList[i];
                int val = (int) (timestamp - info.getTimestamp());
                if (max < val) {
                    index = i;
                    max = val;
                }
            }
            // save current value
            Runtime r = Runtime.getRuntime();
            if (r != null)
                memoryInfoList[index].set(r);
        }
    }

}
