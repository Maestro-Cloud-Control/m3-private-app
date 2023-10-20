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

import io.maestro3.diagnostic.model.container.JmxInfoDataContainer;
import io.maestro3.diagnostic.model.container.MemoryInfoDataContainer;

import java.util.Map;


public interface ILocalJmxProvider {
    void updateJmxInfo();

    void updateMemoryPoolInfo();

    void printJmxInfo();

    String getJmxHtmlView();

    JmxInfoDataContainer getJmxDetails();

    MemoryInfoDataContainer getMemoryDetails(int lastTimeMin);

    Map<String, String> getGeneralMetrics();

    void setCollectJmxInfo(boolean value);

    void setUseCache(boolean value);

    void setMemoryService(DoMemoryService value);
}
