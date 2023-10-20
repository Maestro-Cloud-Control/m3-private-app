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

import io.maestro3.agent.amqp.PrivateAgentAmqpConstants;
import io.maestro3.agent.amqp.model.HttpRegionStatistic;
import io.maestro3.agent.amqp.tracker.IAmqpMessageTracker;
import io.maestro3.agent.dao.IRegionRepository;
import io.maestro3.agent.dao.ITenantRepository;
import io.maestro3.agent.http.tracker.IHttpRequestTracker;
import io.maestro3.agent.model.base.IAmqpSupportedRegion;
import io.maestro3.agent.model.base.IRegion;
import io.maestro3.agent.model.base.ITenant;
import io.maestro3.diagnostic.model.healthcheck.HealthCheckReport;
import io.maestro3.diagnostic.model.healthcheck.MemoryMetrics;
import io.maestro3.diagnostic.model.healthcheck.MongoDBState;
import io.maestro3.diagnostic.model.healthcheck.RegionStateInfo;
import io.maestro3.diagnostic.service.IHealthCheckService;
import io.maestro3.diagnostic.util.HttpUtil;
import io.maestro3.diagnostic.util.MongoParametersResolver;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;


public abstract class AbstractHealthCheckService<REGION extends IRegion, TENANT extends ITenant> implements IHealthCheckService {

    protected IRegionRepository<REGION> regionRepository;
    protected ITenantRepository<TENANT> tenantRepository;
    protected MemoryMXBean memoryMXBean;
    protected MongoTemplate privateAgentMongo;
    protected String mongoUri;
    protected IAmqpMessageTracker amqpMessageTracker;
    protected IHttpRequestTracker httpRequestTracker;

    public AbstractHealthCheckService(IRegionRepository<REGION> regionRepository, ITenantRepository<TENANT> tenantRepository, MemoryMXBean memoryMXBean, MongoTemplate privateAgentMongo, String mongoUri, IAmqpMessageTracker amqpMessageTracker, IHttpRequestTracker httpRequestTracker) {
        this.regionRepository = regionRepository;
        this.tenantRepository = tenantRepository;
        this.memoryMXBean = memoryMXBean;
        this.privateAgentMongo = privateAgentMongo;
        this.mongoUri = mongoUri;
        this.amqpMessageTracker = amqpMessageTracker;
        this.httpRequestTracker = httpRequestTracker;
    }

    protected abstract void check(String tenantAlias, REGION region);

    @Override
    public void checkTenantInRegionByAliases(String tenantAlias, String regionAlias) {
        REGION cloud = regionRepository.findByAliasInCloud(regionAlias);
        if (cloud == null) {
            throw new IllegalStateException("ERROR: Region is not exist with region alias  " + regionAlias);
        }
        check(tenantAlias, cloud);
    }

    @Override
    public void checkTenantInRegion(String tenantAlias, String cloudId) {
        REGION region = regionRepository.findByIdInCloud(cloudId);
        if (region == null) {
            throw new IllegalStateException("ERROR: Region is not exist with region id  " + cloudId);
        }
        check(tenantAlias, region);
    }

    @Override
    public HealthCheckReport getReport() {
        HealthCheckReport report = new HealthCheckReport();
        report.setDate(new Date());
        report.setOrchestratorStates(convertToStateInfo());
        return report;
    }

    private List<RegionStateInfo> convertToStateInfo() {
        List<RegionStateInfo> stateInfos = new ArrayList<>();
        stateInfos.add(buildAgentState());
        stateInfos.add(buildSdkState());
        stateInfos.addAll(buildRegionsInfo());
        return stateInfos;
    }

    private RegionStateInfo buildSdkState() {
        RegionStateInfo regionStateInfo = new RegionStateInfo();
        regionStateInfo.setTitle("M3_SDK");
        regionStateInfo.setAmqpRegionStatistic(amqpMessageTracker.collectStatistic(PrivateAgentAmqpConstants.SDK_REGION));
        return regionStateInfo;
    }

    private Collection<RegionStateInfo> buildRegionsInfo() {
        List<RegionStateInfo> stateInfos = new ArrayList<>();
        List<IRegion> regions = regionRepository.findAll();
        for (IRegion region : regions) {
            RegionStateInfo regionStateInfo = new RegionStateInfo();
            regionStateInfo.setTitle(region.getRegionAlias());
            if (region instanceof IAmqpSupportedRegion) {
                regionStateInfo.setAmqpRegionStatistic(amqpMessageTracker.collectStatistic(region.getId()));
            }
            HttpRegionStatistic httpRegionStatistic = httpRequestTracker.collectStatistic(region.getId());
            regionStateInfo.setHttpRegionStatistic(httpRegionStatistic);
            stateInfos.add(regionStateInfo);
        }
        return stateInfos;
    }

    private RegionStateInfo buildAgentState() {
        RegionStateInfo regionStateInfo = new RegionStateInfo();
        Date now = new Date();
        regionStateInfo.setDate(now);
        regionStateInfo.setTitle("Private_agent");
        regionStateInfo.setIp(HttpUtil.getMyIp());
        regionStateInfo.setReportRefreshDate(now);
        regionStateInfo.setMongoDBStates(convertMongo());
        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();

        MemoryMetrics metrics = new MemoryMetrics();
        metrics.setInitHeap(heapMemoryUsage.getInit());
        metrics.setUsedHeap(heapMemoryUsage.getUsed());
        metrics.setCommittedHeap(heapMemoryUsage.getCommitted());
        metrics.setMaxHeap(heapMemoryUsage.getMax());

        regionStateInfo.setMemoryState(metrics);
        return regionStateInfo;
    }


    private List<MongoDBState> convertMongo() {
        List<MongoDBState> mongoDBStates = new ArrayList<>();
        MongoDBState privateAgent = buildState(privateAgentMongo);
        mongoDBStates.add(privateAgent);

        return mongoDBStates;
    }

    private MongoDBState buildState(MongoTemplate template) {
        MongoDBState mongoDBState = MongoParametersResolver.fromURI(mongoUri);
        mongoDBState.setLatency(getLatency(template));
        return mongoDBState;
    }

    private long getLatency(MongoTemplate template) {
        long latency = System.currentTimeMillis();
        template.getDb().runCommand(new Document("ping", 1));
        return System.currentTimeMillis() - latency;
    }

}
