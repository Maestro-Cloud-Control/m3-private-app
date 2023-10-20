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

package io.maestro3.diagnostic.manager;

import io.maestro3.diagnostic.model.container.JmxInfoDataContainer;
import io.maestro3.diagnostic.model.container.MemoryInfoDataContainer;
import io.maestro3.diagnostic.model.container.TenantDataContainer;
import io.maestro3.sdk.v3.model.SdkCloud;

import java.util.List;
import java.util.Map;


public interface IDiagnosticPageManager {
    void performHealthcheck(SdkCloud cloud, String tenantAlias, String regionAlias);

    Map<String, Object> getOperationsModel();

    String invokeOperation(String scheduleOperation);

    Object getJmxInfoModel(JmxInfoDataContainer data);

    Map getTenantInfoModel();

    Object getMemoryModel(MemoryInfoDataContainer data);

    JmxInfoDataContainer getJmxInfo();

    MemoryInfoDataContainer getMemoryDetails(int lastTimeMin);

    Object getJobsModel();

    List<TenantDataContainer> getTenantDetails();

    Map getInstanceRunDetails();
}
