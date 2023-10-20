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

package io.maestro3.diagnostic.service.impl;

import io.maestro3.diagnostic.model.MemoryInfo;
import io.maestro3.diagnostic.model.container.JmxInfoDataContainer;
import io.maestro3.diagnostic.model.container.MemoryInfoDataContainer;
import io.maestro3.diagnostic.service.DoMemoryService;
import io.maestro3.diagnostic.service.ILocalJmxProvider;
import io.maestro3.sdk.internal.util.StringUtils;
import com.sun.management.UnixOperatingSystemMXBean;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.maestro3.diagnostic.constants.JmxProviderConstants.ARCH;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.ARG;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.AVAILABLE_PROCESSORS;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.BODY_HTML_CLOSE;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.BOOT_CLASS_PATH;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.BR;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.CENTER_CLOSE;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.CLASSLOADING;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.CLASS_PATH;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.COLLECTION_COUNT;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.COLLECTION_TIME;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.COLONS;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.COMMITTED;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.COMMITTED_VIRTUAL_MEMORY;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.COUNT;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.CURRENT_PID;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.DAEMON_COUNT;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.DOT;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.EQ;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.FREE_PHYSICAL_MEMORY;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.FREE_SWAP_SPACE;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.GC;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.HEAP_COMMITTED;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.HEAP_INIT;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.HEAP_MAX;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.HEAP_USED;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.HTML_XMLNS_HEAD;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.INIT;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.JMX_VERSION;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.JVM_NAME;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.JVM_VENDOR;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.JVM_VERSION;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.K;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.LIBRARY_PATH;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.LINE;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.LOADED_COUNT;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.MAX;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.MAX_FILE_DESCRIPTORS;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.MEMORY;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.MEM_POOL;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.NAME_STR;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.NON_HEAP_COMMITTED;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.NON_HEAP_INIT;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.NON_HEAP_MAX;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.NON_HEAP_USED;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.OBJECT_PENDING_FINALIZATION_COUNT;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.OPEN_FILE_DESCRIPTORS;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.OS;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.PEAK_COUNT;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.PROCESS_UPTIME_MILLIS;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.RUNTIME;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.SPEC_NAME;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.SPEC_VENDOR;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.SPEC_VERSION;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.START_TIME;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.SYSTEM_LOAD_AVERAGE;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.THREAD;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.TITLE_JMX_INFO_TITLE_HEAD_BODY_CENTER;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.TOTAL_LOADED_COUNT;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.TOTAL_PHYSICAL_MEMORY;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.TOTAL_STARTED_COUNT;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.TOTAL_SWAP_SPACE;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.TOTAL_UNLOADED_COUNT;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.UPDATED;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.UPDATING_THE_JMX_INFORMATION;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.USED;
import static io.maestro3.diagnostic.constants.JmxProviderConstants.VERSION_STR;



@Component
public class LocalJmxProvider implements EnvironmentAware, ILocalJmxProvider {

    private static final Logger LOG = LoggerFactory.getLogger(LocalJmxProvider.class.getName());

    private final Map<String, String> generalMetrics;
    private final Map<String, String> memoryMetrics;
    private final Map<String, String> osMetrics;
    private final Map<String, String> runtimeMetrics;
    private boolean collectJmxInfo;
    private boolean useCache;
    private long lastUpdated;
    private String activeProfiles;

    @Autowired
    private DoMemoryService memoryService;

    public LocalJmxProvider() {
        this.collectJmxInfo = true;
        this.useCache = false;
        this.generalMetrics = new LinkedHashMap<>();
        this.memoryMetrics = new LinkedHashMap<>();
        this.osMetrics = new LinkedHashMap<>();
        this.runtimeMetrics = new LinkedHashMap<>();
    }

    /**
     * Collecting fresh JMX information.
     * <p/>
     * This job could be scheduled:
     * 1. Set useCache=true
     * 2. Schedule method execution with @Scheduled(fixedRate=5000)
     */
    @Override
    public void updateJmxInfo() {
        try {
            if (collectJmxInfo) {
                LOG.debug(UPDATING_THE_JMX_INFORMATION);
                synchronized (generalMetrics) {
                    generalMetrics.clear();
                    memoryMetrics.clear();
                    osMetrics.clear();
                    runtimeMetrics.clear();
                    updateRuntimeInfo();
                    updateOsInfo();
                    updateMemoryInfo();
                    updateMemoryPoolInfo();
                    updateThreadInfo();
                    updateProfilesInfo();
                    updateClassLoadingInfo();
                    updateGcInfo();
                    lastUpdated = System.currentTimeMillis();
                }
            }
        } catch (Throwable t) {
            LOG.error(t.getMessage(), t);
        }
    }

    @Override
    public void setEnvironment(Environment environment) {
        // we do not need to save link to environment or update active profiles later
        // active profiles will not change after app start
        activeProfiles = String.join(" ", environment.getActiveProfiles());
    }

    // actually we don't need update this parameter, main purpose to not change common approach
    private void updateProfilesInfo() {
        generalMetrics.put("spring.active.profiles", activeProfiles);
    }

    private void updateRuntimeInfo() {
        //Runtime info
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        runtimeMetrics.put(RUNTIME + CURRENT_PID, runtimeMxBean.getName());
        runtimeMetrics.put(RUNTIME + PROCESS_UPTIME_MILLIS, Long.valueOf(runtimeMxBean.getUptime()).toString());
        runtimeMetrics.put(RUNTIME + JVM_VENDOR, runtimeMxBean.getVmVendor());
        runtimeMetrics.put(RUNTIME + JVM_NAME, runtimeMxBean.getVmName());
        runtimeMetrics.put(RUNTIME + JVM_VERSION, runtimeMxBean.getVmVersion());
        runtimeMetrics.put(RUNTIME + CLASS_PATH, runtimeMxBean.getClassPath());
        runtimeMetrics.put(RUNTIME + BOOT_CLASS_PATH, runtimeMxBean.getBootClassPath());
        runtimeMetrics.put(RUNTIME + LIBRARY_PATH, runtimeMxBean.getLibraryPath());
        runtimeMetrics.put(RUNTIME + SPEC_VENDOR, runtimeMxBean.getSpecVendor());
        runtimeMetrics.put(RUNTIME + SPEC_NAME, runtimeMxBean.getSpecName());
        runtimeMetrics.put(RUNTIME + SPEC_VERSION, runtimeMxBean.getSpecVersion());
        runtimeMetrics.put(RUNTIME + JMX_VERSION, runtimeMxBean.getManagementSpecVersion());
        runtimeMetrics.put(RUNTIME + START_TIME, Long.valueOf(runtimeMxBean.getStartTime()).toString());
        List<String> inputArguments = runtimeMxBean.getInputArguments();
        int i = 0;
        for (String argument : inputArguments) {
            runtimeMetrics.put(RUNTIME + ARG + i, argument);
            i++;
        }
    }

    private void updateOsInfo() {
        //OS Info
        OperatingSystemMXBean osMxBean = ManagementFactory.getOperatingSystemMXBean();
        osMetrics.put(OS + AVAILABLE_PROCESSORS, Integer.valueOf(osMxBean.getAvailableProcessors()).toString());
        osMetrics.put(OS + SYSTEM_LOAD_AVERAGE, Double.valueOf(osMxBean.getSystemLoadAverage()).toString());
        osMetrics.put(OS + ARCH, osMxBean.getArch());
        osMetrics.put(OS + NAME_STR, osMxBean.getName());
        osMetrics.put(OS + VERSION_STR, osMxBean.getVersion());
        //Extended info for Unix OS
        if (osMxBean instanceof UnixOperatingSystemMXBean) {
            UnixOperatingSystemMXBean unixMxbean = (UnixOperatingSystemMXBean) osMxBean;
            osMetrics.put(OS + OPEN_FILE_DESCRIPTORS, Long.valueOf(unixMxbean.getOpenFileDescriptorCount()).toString());
            osMetrics.put(OS + MAX_FILE_DESCRIPTORS, Long.valueOf(unixMxbean.getMaxFileDescriptorCount()).toString());
            osMetrics.put(OS + COMMITTED_VIRTUAL_MEMORY, Long.valueOf(unixMxbean.getCommittedVirtualMemorySize()).toString());
            osMetrics.put(OS + FREE_PHYSICAL_MEMORY, Long.valueOf(unixMxbean.getFreePhysicalMemorySize()).toString());
            osMetrics.put(OS + TOTAL_PHYSICAL_MEMORY, Long.valueOf(unixMxbean.getTotalPhysicalMemorySize()).toString());
            osMetrics.put(OS + TOTAL_SWAP_SPACE, Long.valueOf(unixMxbean.getTotalSwapSpaceSize()).toString());
            osMetrics.put(OS + FREE_SWAP_SPACE, Long.valueOf(unixMxbean.getFreeSwapSpaceSize()).toString());
        }
    }

    private void updateMemoryInfo() {
        //Memory
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapMemory = memoryMXBean.getHeapMemoryUsage();
        memoryMetrics.put(MEMORY + HEAP_COMMITTED, Long.valueOf(heapMemory.getCommitted()).toString());
        memoryMetrics.put(MEMORY + HEAP_MAX, Long.valueOf(heapMemory.getMax()).toString());
        memoryMetrics.put(MEMORY + HEAP_INIT, Long.valueOf(heapMemory.getInit()).toString());
        memoryMetrics.put(MEMORY + HEAP_USED, Long.valueOf(heapMemory.getUsed()).toString());
        MemoryUsage nonHeapMemory = memoryMXBean.getNonHeapMemoryUsage();
        memoryMetrics.put(MEMORY + NON_HEAP_COMMITTED, Long.valueOf(nonHeapMemory.getCommitted()).toString());
        memoryMetrics.put(MEMORY + NON_HEAP_MAX, Long.valueOf(nonHeapMemory.getMax()).toString());
        memoryMetrics.put(MEMORY + NON_HEAP_INIT, Long.valueOf(nonHeapMemory.getInit()).toString());
        memoryMetrics.put(MEMORY + NON_HEAP_USED, Long.valueOf(nonHeapMemory.getUsed()).toString());
        memoryMetrics.put(MEMORY + OBJECT_PENDING_FINALIZATION_COUNT, Integer.valueOf(memoryMXBean.getObjectPendingFinalizationCount()).toString());
    }

    @Override
    public void updateMemoryPoolInfo() {
        // get and check
        List<MemoryPoolMXBean> mpList = ManagementFactory.getMemoryPoolMXBeans();
        if ((mpList == null) || mpList.size() < 1) {
            return;
        }
        // collect
        for (MemoryPoolMXBean item : mpList) {
            MemoryUsage memUsage = item.getCollectionUsage();
            if (memUsage != null) {
                String name = MEM_POOL + item.getName() + DOT;
                long val = memUsage.getInit();
                if (val > 0) {
                    memoryMetrics.put(name + INIT, (val >> 10) + K);
                }
                val = memUsage.getUsed();
                if (val > 0) {
                    memoryMetrics.put(name + USED, (val >> 10) + K);
                }
                val = memUsage.getCommitted();
                if (val > 0) {
                    memoryMetrics.put(name + COMMITTED, (val >> 10) + K);
                }
                val = memUsage.getMax();
                if (val > 0) {
                    memoryMetrics.put(name + MAX, (val >> 10) + K);
                }
            }
        }
    }

    private void updateThreadInfo() {
        //Thread info
        ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
        generalMetrics.put(THREAD + COUNT, Integer.valueOf(threadMxBean.getThreadCount()).toString());
        generalMetrics.put(THREAD + PEAK_COUNT, Integer.valueOf(threadMxBean.getPeakThreadCount()).toString());
        generalMetrics.put(THREAD + DAEMON_COUNT, Integer.valueOf(threadMxBean.getDaemonThreadCount()).toString());
        generalMetrics.put(THREAD + TOTAL_STARTED_COUNT, Long.valueOf(threadMxBean.getTotalStartedThreadCount()).toString());
    }

    private void updateClassLoadingInfo() {
        //Class loading
        ClassLoadingMXBean classLoadingMxBean = ManagementFactory.getClassLoadingMXBean();
        generalMetrics.put(CLASSLOADING + LOADED_COUNT, Integer.valueOf(classLoadingMxBean.getLoadedClassCount()).toString());
        generalMetrics.put(CLASSLOADING + TOTAL_LOADED_COUNT, Long.valueOf(classLoadingMxBean.getTotalLoadedClassCount()).toString());
        generalMetrics.put(CLASSLOADING + TOTAL_UNLOADED_COUNT, Long.valueOf(classLoadingMxBean.getUnloadedClassCount()).toString());
    }

    private void updateGcInfo() {
        //GC
        List<GarbageCollectorMXBean> gcMxBeansList = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean gcMxBean : gcMxBeansList) {
            if (gcMxBean.isValid()) {
                generalMetrics.put(GC + gcMxBean.getName() + COLLECTION_COUNT, Long.valueOf(gcMxBean.getCollectionCount()).toString());
                generalMetrics.put(GC + gcMxBean.getName() + COLLECTION_TIME, Long.valueOf(gcMxBean.getCollectionTime()).toString());
            }
        }
    }

    @Override
    public void printJmxInfo() {
        //Print the info map
        synchronized (generalMetrics) {
            for (Map.Entry<String, String> oneEntry : generalMetrics.entrySet()) {
                LOG.info("{}{}{}", oneEntry.getKey(), EQ, oneEntry.getValue());
            }
        }
    }

    @Override
    public String getJmxHtmlView() {
        StringBuilder builder = new StringBuilder(2048);
        if (!useCache || (generalMetrics.size() == 0)) {
            updateJmxInfo();
        }
        builder.append(HTML_XMLNS_HEAD);
        builder.append(TITLE_JMX_INFO_TITLE_HEAD_BODY_CENTER);
        Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(lastUpdated);
        builder.append(UPDATED).append(calendar.getTime().toString());
        builder.append(CENTER_CLOSE).append(BR);
        String prefix = "";
        String key;
        for (Map.Entry<String, String> oneEntry : generalMetrics.entrySet()) {
            key = oneEntry.getKey();
            if (!prefix.equals(getPrefix(key))) {
                prefix = getPrefix(key);
                builder.append(LINE).append(prefix).append(LINE).append(BR);
            }
            builder.append(key).append(EQ).append(oneEntry.getValue()).append(BR);
        }
        builder.append(BODY_HTML_CLOSE);
        return builder.toString();
    }

    private String getPrefix(String data) {
        String prefix = "";
        if (StringUtils.isNotBlank(data)) {
            String[] result = data.split(COLONS);
            if (StringUtils.isNotBlank(result[0])) {
                prefix = result[0];
            }
        }
        return prefix;
    }

    @Override
    public JmxInfoDataContainer getJmxDetails() {
        if (!useCache || (lastUpdated == 0L)) {
            updateJmxInfo();
        }
        JmxInfoDataContainer container = new JmxInfoDataContainer();
        synchronized (generalMetrics) {
            container.setLastUpdate(new DateTime(lastUpdated));
            container.setGeneralMetrics(generalMetrics);
            container.setMemoryMetrics(memoryMetrics);
            container.setOsMetrics(osMetrics);
            container.setRuntimeMetrics(runtimeMetrics);
        }
        return container;
    }

    @Override
    public MemoryInfoDataContainer getMemoryDetails(int lastTimeMin) {
        List<MemoryInfo> data = memoryService.getMemoryInfo(lastTimeMin);
        return new MemoryInfoDataContainer(data);
    }

    @Override
    public Map<String, String> getGeneralMetrics() {
        if (!useCache || (generalMetrics.size() == 0)) {
            updateJmxInfo();
        }
        return generalMetrics;
    }


    @Override
    public void setCollectJmxInfo(boolean value) {
        this.collectJmxInfo = value;
    }

    @Override
    public void setUseCache(boolean value) {
        this.useCache = value;
    }

    @Override
    public void setMemoryService(DoMemoryService value) {
        this.memoryService = value;
    }
}
