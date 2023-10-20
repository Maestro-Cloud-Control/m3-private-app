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

import com.fasterxml.jackson.annotation.JsonInclude;
import io.maestro3.agent.model.base.RabbitNotificationConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class RabbitConfigDto {

    private String rabbitHost;
    private int rabbitPort;
    private String username;
    private String password;
    private String rabbitVirtHost;
    private int replyTimeoutMillis;
    private int shutdownTimeoutMillis;
    private int minConcurrentConsumers;
    private int maxConcurrentConsumers;
    private String novaExchangeName;
    private String cinderExchangeName;
    private String glanceExchangeName;
    private List<String> exchanges = new ArrayList<>();

    public RabbitConfigDto() {
    }

    public RabbitConfigDto(RabbitNotificationConfig config) {
        this.rabbitHost = config.getRabbitHost();
        this.rabbitPort = config.getRabbitPort();
        this.rabbitVirtHost = config.getRabbitVirtHost();
        this.replyTimeoutMillis = config.getReplyTimeoutMillis();
        this.shutdownTimeoutMillis = config.getShutdownTimeoutMillis();
        this.minConcurrentConsumers = config.getMinConcurrentConsumers();
        this.maxConcurrentConsumers = config.getMaxConcurrentConsumers();
        this.exchanges = new ArrayList<>(config.getQueueMapping().keySet());
    }

    public RabbitNotificationConfig toNotificationConfig(String novaQueueName, String cinderQueueName,
                                                         String glaceQueueName) {
        RabbitNotificationConfig config = new RabbitNotificationConfig();
        config.setRabbitHost(this.rabbitHost);
        config.setRabbitPort(this.rabbitPort);
        config.setRabbitVirtHost(this.rabbitVirtHost);
        config.setReplyTimeoutMillis(this.replyTimeoutMillis);
        config.setShutdownTimeoutMillis(this.shutdownTimeoutMillis);
        config.setMinConcurrentConsumers(this.minConcurrentConsumers);
        config.setMaxConcurrentConsumers(this.maxConcurrentConsumers);
        HashMap<String, List<String>> queueMapping = new HashMap<>();
        queueMapping.put(this.novaExchangeName, Collections.singletonList(novaQueueName));
        queueMapping.put(this.cinderExchangeName, Collections.singletonList(cinderQueueName));
        queueMapping.put(this.glanceExchangeName, Collections.singletonList(glaceQueueName));
        config.setQueueMapping(queueMapping);
        config.setRabbitUsername(this.username);
        config.setRabbitPassword(this.password);
        return config;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public List<String> getExchanges() {
        return exchanges;
    }

    public void setExchanges(List<String> exchanges) {
        this.exchanges = exchanges;
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

    public String getNovaExchangeName() {
        return novaExchangeName;
    }

    public void setNovaExchangeName(String novaExchangeName) {
        this.novaExchangeName = novaExchangeName;
    }

    public String getCinderExchangeName() {
        return cinderExchangeName;
    }

    public void setCinderExchangeName(String cinderExchangeName) {
        this.cinderExchangeName = cinderExchangeName;
    }

    public String getGlanceExchangeName() {
        return glanceExchangeName;
    }

    public void setGlanceExchangeName(String glanceExchangeName) {
        this.glanceExchangeName = glanceExchangeName;
    }
}
