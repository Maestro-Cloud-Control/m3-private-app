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

import io.maestro3.agent.dao.IImageRepository;
import io.maestro3.agent.dao.IInstanceRunRecordDao;
import io.maestro3.agent.dao.IOpenStackTenantRepository;
import io.maestro3.agent.dao.IRegionRepository;
import io.maestro3.agent.model.base.IRegion;
import io.maestro3.agent.model.base.ITenant;
import io.maestro3.agent.model.base.InstanceRunRecord;
import io.maestro3.agent.model.base.PrivateCloudType;
import io.maestro3.diagnostic.model.EnumJMXPage;
import io.maestro3.diagnostic.model.JobModel;
import io.maestro3.diagnostic.model.MemoryInfo;
import io.maestro3.diagnostic.model.ScheduleOperation;
import io.maestro3.diagnostic.model.container.JmxInfoDataContainer;
import io.maestro3.diagnostic.model.container.MemoryInfoDataContainer;
import io.maestro3.diagnostic.model.container.StartTimeContainer;
import io.maestro3.diagnostic.model.container.TenantDataContainer;
import io.maestro3.diagnostic.model.container.TenantDataMapper;
import io.maestro3.diagnostic.service.IHealthCheckService;
import io.maestro3.diagnostic.service.ILocalJmxProvider;
import io.maestro3.diagnostic.service.IScheduleInfoProvider;
import io.maestro3.diagnostic.service.IScheduleProcessorInvoker;
import io.maestro3.diagnostic.service.impl.DefaultUrlBuilder;
import io.maestro3.sdk.internal.util.JsonUtils;
import io.maestro3.sdk.v3.model.SdkCloud;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;


@Component
public class DiagnosticPageManager implements IDiagnosticPageManager {

    private static final Logger LOG = LoggerFactory.getLogger(DiagnosticPageManager.class);
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();

    public static final String RENEW_PARAM = "renew";
    public static final String DROP_PARAM = "drop";

    private ILocalJmxProvider jmxProvider;
    private Map<String, String> jmxInfoGroups;
    private IScheduleProcessorInvoker scheduleProcessorInvoker;

    private DefaultUrlBuilder urlBuilder;
    private IScheduleInfoProvider scheduleService;
    private IOpenStackTenantRepository organizationRepository;
    private IRegionRepository<IRegion> cloudRepository;
    private IHealthCheckService healthCheckService;
    private IInstanceRunRecordDao runRecordDao;
    private Map<PrivateCloudType, IImageRepository> imageRepositoryMap;


    private final SimpleDateFormat MEMORY_RECORD_TIME_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");

    private Map<String, List<ScheduleOperation>> operationsByScopes;

    @PostConstruct
    public void init() {
        operationsByScopes = new HashMap<>();
        for (ScheduleOperation operation : ScheduleOperation.values()) {
            String scope = operation.getScope();

            List<ScheduleOperation> operations = operationsByScopes.computeIfAbsent(scope, k -> new LinkedList<>());
            operations.add(operation);
        }

        jmxInfoGroups = new TreeMap<>();
        jmxInfoGroups.put("memory:", "Memory");
        jmxInfoGroups.put("memory_pool:", "Memory");
        jmxInfoGroups.put("runtime:", "Runtime");
        jmxInfoGroups.put("os:", "Operational-system");
    }

    @Autowired
    public DiagnosticPageManager(ILocalJmxProvider jmxProvider,
                                 DefaultUrlBuilder urlBuilder,
                                 IHealthCheckService healthCheckService,
                                 IScheduleInfoProvider scheduleService,
                                 IScheduleProcessorInvoker scheduleProcessorInvoker,
                                 IRegionRepository cloudRepository,
                                 IOpenStackTenantRepository organizationRepository,
                                 IInstanceRunRecordDao runRecordDao,
                                 Set<IImageRepository> imageRepositories) {
        this.jmxProvider = jmxProvider;
        this.runRecordDao = runRecordDao;
        this.cloudRepository = cloudRepository;
        this.healthCheckService = healthCheckService;
        this.scheduleProcessorInvoker = scheduleProcessorInvoker;
        this.organizationRepository = organizationRepository;
        this.scheduleService = scheduleService;
        this.urlBuilder = urlBuilder;
        this.imageRepositoryMap = imageRepositories.stream()
            .collect(Collectors.toMap(IImageRepository::getCloud, Function.identity()));
    }


    @Override
    public JmxInfoDataContainer getJmxInfo() {
        return jmxProvider.getJmxDetails();
    }

    @Override
    public void performHealthcheck(SdkCloud cloud, String tenantAlias, String regionAlias) {
        EXECUTOR_SERVICE.execute(() -> healthCheckService.checkTenantInRegionByAliases(tenantAlias, regionAlias));
    }

    @Override
    public Map<String, Object> getOperationsModel() {
        Map<String, Object> content = new HashMap<>();
        content.put("operations", operationsByScopes);

        Map<String, String> restartLinks = new TreeMap<>();
        for (List<ScheduleOperation> operationsInScope : operationsByScopes.values()) {
            for (ScheduleOperation operation : operationsInScope) {
                String url = urlBuilder.buildJmxPageUrl(EnumJMXPage.OPERATIONS, operation);
                restartLinks.put(operation.name(), url);
            }
        }

        content.put("restartLinks", restartLinks);
        return content;
    }

    @Override
    public String invokeOperation(ScheduleOperation scheduleOperation) {
        String result = "Ok";
        LOG.info("Manual JMX schedule invoker");
        try {
            LOG.info("Trying to invoke: {}", scheduleOperation);
            scheduleProcessorInvoker.invoke(scheduleOperation, true);
            LOG.info("Invoked: {}", scheduleOperation);
            result = "Operation \"" + scheduleOperation + "\" submitted to executor.";
        } catch (InvalidParameterException e) {
            LOG.error("Failed to invoke \"" + scheduleOperation + "\"", e);
            result = "Failed to invoke \"" + scheduleOperation + "\"." + e.getMessage();
        }
        return result;
    }

    @Override
    public Object getJmxInfoModel(JmxInfoDataContainer data) {
        Map<String, Object> contentMap = new HashMap<>();
        contentMap.put("General", data.getGeneralMetrics());
        contentMap.put("Memory", data.getMemoryMetrics());
        contentMap.put("Runtime", data.getRuntimeMetrics());
        contentMap.put("Operational-system", data.getOsMetrics());
        Map<String, Object> model = new HashMap<>();
        model.put("params", contentMap);
        return model;
    }

    @Override
    public Map getTenantInfoModel() {
        List<TenantDataContainer> data = getTenantDetails();
        Map<PrivateCloudType, List<TenantDataContainer>> contentMap = new HashMap<>();
        data.forEach(container -> {
            List<TenantDataContainer> containers = contentMap.get(container.getCloud());
            if (containers != null) {
                containers.add(container);
            } else {
                containers = new ArrayList<>();
                containers.add(container);
                contentMap.put(container.getCloud(), containers);
            }
        });
        Map<String, Object> model = new HashMap<>();
        model.put("params", contentMap);
        String json = JsonUtils.convertObjectToJson(model);
        model = JsonUtils.parseJson(json, Map.class);
        return model;
    }

    @Override
    public Object getMemoryModel(MemoryInfoDataContainer data) {
        Map<String, Object> contentMap = new HashMap<>();
        List<Map<String, String>> memoryInfoList = new ArrayList<>();

        for (MemoryInfo info : data.getMemoryInfoList()) {
            Map<String, String> memoryInfo = new HashMap<>();
            memoryInfo.put("time", MEMORY_RECORD_TIME_FORMAT.format(new Date(info.getTimestamp())));
            memoryInfo.put("usedMemoryKb", String.valueOf(info.getUsedMemKb()));
            memoryInfo.put("freeMemoryKb", String.valueOf(info.getFreeMemKb()));
            memoryInfo.put("maxMemoryKb", String.valueOf(info.getMaxMemKb()));
            memoryInfo.put("totalMemoryKb", String.valueOf(info.getTotalMemKb()));
            memoryInfo.put("memoryUsage", String.valueOf(info.getMemUsagePercent()));
            memoryInfoList.add(memoryInfo);
        }

        contentMap.put("memoryInfoList", memoryInfoList);
        return contentMap;
    }

    public MemoryInfoDataContainer getMemoryDetails(int lastTimeMin) {
        return jmxProvider.getMemoryDetails(lastTimeMin);
    }

    @Override
    public Collection<JobModel> getJobsModel() {
        return scheduleService.getScheduleInfo();
    }

    @Override
    public List<TenantDataContainer> getTenantDetails() {
        Map<String, IRegion> regionMap = cloudRepository.findAll()
            .stream()
            .collect(Collectors.toMap(IRegion::getId, Function.identity()));
        List<ITenant> tenants = organizationRepository.findAll();
        return tenants.stream()
            .map(tenant -> TenantDataMapper.map(tenant,
                regionMap.get(tenant.getRegionId())))
            .collect(Collectors.toList());
    }

    @Override
    public Map getInstanceRunDetails() {
        List<InstanceRunRecord> records = runRecordDao.findAll();
        Map<String, IRegion> regionMap = cloudRepository.findAll()
            .stream()
            .collect(Collectors.toMap(IRegion::getId, Function.identity()));
        Map<String, ITenant> tenants = organizationRepository.findAll()
            .stream()
            .collect(Collectors.toMap(ITenant::getId, Function.identity()));
        Map<PrivateCloudType, Map<String, Map<String, StartTimeContainer>>> result = new HashMap<>();
        Map<String, String> imageAliases = new HashMap<>();
        for (InstanceRunRecord record : records) {
            IRegion region = regionMap.get(record.getRegion());
            ITenant tenant = tenants.get(record.getTenant());
            if (tenant == null || region == null) {
                continue;
            }
            Map<String, Map<String, StartTimeContainer>> recordRelatedToCloud = result.computeIfAbsent(tenant.getCloud(), cloudType -> new HashMap<>());
            Map<String, StartTimeContainer> recordsRelatedToRegionInTenant = recordRelatedToCloud.computeIfAbsent(region.getRegionAlias() + " in " + tenant.getTenantAlias(),
                alias -> new HashMap<>());
            String imageAlias = imageAliases.computeIfAbsent(region.getRegionAlias() + tenant.getTenantAlias() + record.getImageId(),
                key -> {
                    String alias = imageRepositoryMap.get(tenant.getCloud()).findAlias(tenant.getId(), region.getId(), record.getImageId());
                    return alias == null ? record.getImageId() : alias;
                });
            StartTimeContainer units = recordsRelatedToRegionInTenant.computeIfAbsent(imageAlias, id -> new StartTimeContainer());
            units.update(record);
        }
        Map<String, Object> model = new HashMap<>();
        model.put("params", result);
        String json = JsonUtils.convertObjectToJson(model);
        model = JsonUtils.parseJson(json, Map.class);
        return model;
    }
}
