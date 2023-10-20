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

package io.maestro3.diagnostic.constants;


public interface JmxProviderConstants {
    //MxBeans prefixes
     String GC = "gc:";
     String RUNTIME = "runtime:";
     String OS = "os:";
     String MEMORY = "memory:";
     String MEM_POOL = "memory_pool:";
     String THREAD = "thread:";
     String CLASSLOADING = "classloading:";
    //String constants
     String COLONS = ":";
     String DOT = ".";
     String JMX_BK = "JMX.[";
     String CL_BK = "]";
     String TIMESTAMP_ISO8601 = "Timestamp ISO8601";
     String JMX_INFO = "jmxInfo";
     String JMX_MEMORY_INFO = "jmxMemoryInfo";
     String GC_INVOKE = "gc";
     String EQ = "=";
     String MEMORY_AFTER_GC = "Memory After GC";
     String MEMORY_BEFORE_GC = "Memory Before GC";
     String CAN_T_RUN_GC = "Can't run GC";
    //Jmx params names
     String INIT = "init";
     String K = "K";
     String USED = "used";
     String COMMITTED = "committed";
     String MAX = "max";
     String CURRENT_PID = "current.pid";
     String PROCESS_UPTIME_MILLIS = "process.uptime.millis";
     String JVM_VENDOR = "jvm.vendor";
     String JVM_NAME = "jvm.name";
     String JVM_VERSION = "jvm.version";
     String CLASS_PATH = "class.path";
     String BOOT_CLASS_PATH = "boot.class.path";
     String LIBRARY_PATH = "library.path";
     String SPEC_VENDOR = "spec.vendor";
     String SPEC_NAME = "spec.name";
     String SPEC_VERSION = "spec.version";
     String JMX_VERSION = "jmx.version";
     String START_TIME = "start.time";
     String AVAILABLE_PROCESSORS = "available.processors";
     String SYSTEM_LOAD_AVERAGE = "system.load.average";
     String ARCH = "arch";
     String NAME_STR = "name";
     String VERSION_STR = "version";
     String OPEN_FILE_DESCRIPTORS = "open.file.descriptors";
     String MAX_FILE_DESCRIPTORS = "max.file.descriptors";
     String COMMITTED_VIRTUAL_MEMORY = "committed.virtual.memory";
     String FREE_PHYSICAL_MEMORY = "free.physical.memory";
     String TOTAL_PHYSICAL_MEMORY = "total.physical.memory";
     String TOTAL_SWAP_SPACE = "total.swap.space";
     String FREE_SWAP_SPACE = "free.swap.space";
     String HEAP_COMMITTED = "heap.committed";
     String HEAP_MAX = "heap.max";
     String HEAP_INIT = "heap.init";
     String HEAP_USED = "heap.used";
     String NON_HEAP_COMMITTED = "non-heap.committed";
     String NON_HEAP_MAX = "non-heap.max";
     String NON_HEAP_INIT = "non-heap.init";
     String NON_HEAP_USED = "non-heap.used";
     String OBJECT_PENDING_FINALIZATION_COUNT = "object.pending.finalization.count";
     String COUNT = "count";
     String PEAK_COUNT = "peak.count";
     String DAEMON_COUNT = "daemon.count";
     String TOTAL_STARTED_COUNT = "total.started.count";
     String LOADED_COUNT = "loaded.count";
     String TOTAL_LOADED_COUNT = "total.loaded.count";
     String TOTAL_UNLOADED_COUNT = "total.unloaded.count";
     String COLLECTION_COUNT = ".collection.count";
     String COLLECTION_TIME = ".collection.time";
    //Static html info constants
     String UPDATING_THE_JMX_INFORMATION = "Updating the JMX information";
     String ARG = "arg";
     String HTML_XMLNS_HEAD = "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head>";
     String UPDATED = "Updated ";
     String BODY_HTML_CLOSE = "</body></html>";
     String BR = "</br>";
     String TITLE_JMX_INFO_TITLE_HEAD_BODY_CENTER = "<title>JMX Info</title></head><body><center>";
     String CENTER_CLOSE = "</center>";
     String LINE = "-------------";
}
