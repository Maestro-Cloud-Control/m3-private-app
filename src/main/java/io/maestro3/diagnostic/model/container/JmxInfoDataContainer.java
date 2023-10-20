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

package io.maestro3.diagnostic.model.container;

import org.joda.time.DateTime;

import java.util.Map;


public class JmxInfoDataContainer {
    private DateTime lastUpdate;
    private Map<String, String> generalMetrics;
    private Map<String, String> memoryMetrics;
    private Map<String, String> osMetrics;
    private Map<String, String> runtimeMetrics;

    public JmxInfoDataContainer() {
        //json
    }

    public DateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(DateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public Map<String, String> getGeneralMetrics() {
        return generalMetrics;
    }

    public void setGeneralMetrics(Map<String, String> generalMetrics) {
        this.generalMetrics = generalMetrics;
    }

    public Map<String, String> getMemoryMetrics() {
        return memoryMetrics;
    }

    public void setMemoryMetrics(Map<String, String> memoryMetrics) {
        this.memoryMetrics = memoryMetrics;
    }

    public Map<String, String> getOsMetrics() {
        return osMetrics;
    }

    public void setOsMetrics(Map<String, String> osMetrics) {
        this.osMetrics = osMetrics;
    }

    public Map<String, String> getRuntimeMetrics() {
        return runtimeMetrics;
    }

    public void setRuntimeMetrics(Map<String, String> runtimeMetrics) {
        this.runtimeMetrics = runtimeMetrics;
    }
}
