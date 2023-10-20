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

import java.util.Map;


public class MongoDBState {
    private String host;
    private String username;
    private String port;
    private String database;
    private Map<String, String> additionalParams;
    private long latency;

    public MongoDBState() {
    }

    public MongoDBState(String host, String port, String database, Map<String, String> additionalParams) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.additionalParams = additionalParams;
    }

    public long getLatency() {
        return latency;
    }

    public void setLatency(long latency) {
        this.latency = latency;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public Map<String, String> getAdditionalParams() {
        return additionalParams;
    }

    public void setAdditionalParams(Map<String, String> additionalParams) {
        this.additionalParams = additionalParams;
    }


    @Override
    public String toString() {
        return "MongoDBState{" +
                "host='" + host + '\'' +
                ", username='" + username + '\'' +
                ", port='" + port + '\'' +
                ", database='" + database + '\'' +
                ", additionalParams=" + additionalParams +
                ", latency=" + latency +
                '}';
    }
}
