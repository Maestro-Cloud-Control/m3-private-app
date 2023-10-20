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

package io.maestro3.agent.admin.model;

import io.maestro3.agent.model.base.RabbitNotificationConfig;
import io.maestro3.sdk.internal.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;


public class RabbitNotificationConfigDto {

    private String rabbitHost;
    private int rabbitPort;
    private String rabbitUsername;
    private String rabbitPassword;
    private String rabbitVirtHost;
    private int replyTimeoutMillis;
    private int shutdownTimeoutMillis;
    private int minConcurrentConsumers;
    private int maxConcurrentConsumers;
    private List<String> exchangeName = new ArrayList<>();
    private String rabbitQueue;

    public RabbitNotificationConfigDto() {
    }

    public RabbitNotificationConfigDto(RabbitNotificationConfig config) {
        this.rabbitHost = config.getRabbitHost();
        this.rabbitPort = config.getRabbitPort();
        this.rabbitUsername = config.getRabbitUsername();
        this.rabbitPassword = config.getRabbitPassword();
        this.rabbitVirtHost = config.getRabbitVirtHost();
        this.replyTimeoutMillis = config.getReplyTimeoutMillis();
        this.shutdownTimeoutMillis = config.getShutdownTimeoutMillis();
        this.minConcurrentConsumers = config.getMinConcurrentConsumers();
        this.maxConcurrentConsumers = config.getMaxConcurrentConsumers();
        config.getQueueMapping().forEach((k, v) -> {
            this.exchangeName.add(k);
            this.rabbitQueue = CollectionUtils.isNotEmpty(v) ? v.get(0) : "";
        });
    }

    public RabbitNotificationConfig toConfig() {
        RabbitNotificationConfig config = new RabbitNotificationConfig();
        config.setRabbitHost(rabbitHost);
        config.setRabbitPort(rabbitPort);
        config.setRabbitUsername(rabbitUsername);
        config.setRabbitPassword(rabbitPassword);
        config.setRabbitVirtHost(rabbitVirtHost);
        config.setReplyTimeoutMillis(replyTimeoutMillis);
        config.setMinConcurrentConsumers(minConcurrentConsumers);
        config.setMaxConcurrentConsumers(maxConcurrentConsumers);
        config.setShutdownTimeoutMillis(shutdownTimeoutMillis);
        HashMap<String, List<String>> queueMapping = new HashMap<>();
        config.setQueueMapping(queueMapping);
        exchangeName.forEach(e ->
                queueMapping.put(e, Collections.singletonList(rabbitQueue))
        );
        return config;
    }

    public String getRabbitHost() {
        return rabbitHost;
    }

    public void setRabbitHost(String rabbitHost) {
        this.rabbitHost = rabbitHost;
    }

    public int getRabbitPort() {
        return rabbitPort;
    }

    public void setRabbitPort(int rabbitPort) {
        this.rabbitPort = rabbitPort;
    }

    public String getRabbitUsername() {
        return rabbitUsername;
    }

    public void setRabbitUsername(String rabbitUsername) {
        this.rabbitUsername = rabbitUsername;
    }

    public String getRabbitPassword() {
        return rabbitPassword;
    }

    public void setRabbitPassword(String rabbitPassword) {
        this.rabbitPassword = rabbitPassword;
    }

    public String getRabbitVirtHost() {
        return rabbitVirtHost;
    }

    public void setRabbitVirtHost(String rabbitVirtHost) {
        this.rabbitVirtHost = rabbitVirtHost;
    }

    public int getReplyTimeoutMillis() {
        return replyTimeoutMillis;
    }

    public void setReplyTimeoutMillis(int replyTimeoutMillis) {
        this.replyTimeoutMillis = replyTimeoutMillis;
    }

    public int getShutdownTimeoutMillis() {
        return shutdownTimeoutMillis;
    }

    public void setShutdownTimeoutMillis(int shutdownTimeoutMillis) {
        this.shutdownTimeoutMillis = shutdownTimeoutMillis;
    }

    public int getMinConcurrentConsumers() {
        return minConcurrentConsumers;
    }

    public void setMinConcurrentConsumers(int minConcurrentConsumers) {
        this.minConcurrentConsumers = minConcurrentConsumers;
    }

    public int getMaxConcurrentConsumers() {
        return maxConcurrentConsumers;
    }

    public void setMaxConcurrentConsumers(int maxConcurrentConsumers) {
        this.maxConcurrentConsumers = maxConcurrentConsumers;
    }

    public List<String> getExchangeName() {
        return exchangeName;
    }

    public void setExchangeName(List<String> exchangeName) {
        this.exchangeName = exchangeName;
    }

    public String getRabbitQueue() {
        return rabbitQueue;
    }

    public void setRabbitQueue(String rabbitQueue) {
        this.rabbitQueue = rabbitQueue;
    }
}
