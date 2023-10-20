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

package io.maestro3.diagnostic.model.healthcheck;

import io.maestro3.agent.amqp.model.AmqpRegionStatistic;
import io.maestro3.agent.amqp.model.HttpRegionStatistic;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;


public class RegionStateInfo {
    private String title;
    private Date date;
    private Date reportRefreshDate;
    private String ip;
    private String dnsName;
    private String namespace;
    private boolean outdated;
    private List<MongoDBState> mongoDBStates = Collections.emptyList();
    private AmqpRegionStatistic amqpRegionStatistic;
    private HttpRegionStatistic httpRegionStatistic;
    private MemoryMetrics memoryState;
    private Set<String> zones;

    public HttpRegionStatistic getHttpRegionStatistic() {
        return httpRegionStatistic;
    }

    public void setHttpRegionStatistic(HttpRegionStatistic httpRegionStatistic) {
        this.httpRegionStatistic = httpRegionStatistic;
    }

    public boolean isOutdated() {
        return outdated;
    }

    public void setOutdated(boolean outdated) {
        this.outdated = outdated;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Date getReportRefreshDate() {
        return reportRefreshDate;
    }

    public void setReportRefreshDate(Date reportRefreshDate) {
        this.reportRefreshDate = reportRefreshDate;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getDnsName() {
        return dnsName;
    }

    public void setDnsName(String dnsName) {
        this.dnsName = dnsName;
    }

    public List<MongoDBState> getMongoDBStates() {
        return mongoDBStates;
    }

    public void setMongoDBStates(List<MongoDBState> mongoDBStates) {
        this.mongoDBStates = mongoDBStates;
    }

    public AmqpRegionStatistic getAmqpRegionStatistic() {
        return amqpRegionStatistic;
    }

    public void setAmqpRegionStatistic(AmqpRegionStatistic amqpRegionStatistic) {
        this.amqpRegionStatistic = amqpRegionStatistic;
    }

    public MemoryMetrics getMemoryState() {
        return memoryState;
    }

    public void setMemoryState(MemoryMetrics memoryState) {
        this.memoryState = memoryState;
    }

    public Set<String> getZones() {
        return zones;
    }

    public void setZones(Set<String> zones) {
        this.zones = zones;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
}
