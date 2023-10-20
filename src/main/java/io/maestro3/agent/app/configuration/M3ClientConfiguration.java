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

package io.maestro3.agent.app.configuration;

import io.maestro3.sdk.M3Sdk;
import io.maestro3.sdk.M3SdkVersion;
import io.maestro3.sdk.internal.provider.IM3AccessKeyProvider;
import io.maestro3.sdk.internal.provider.IM3CredentialsProvider;
import io.maestro3.sdk.internal.provider.impl.M3StaticAccessKeyProvider;
import io.maestro3.sdk.internal.provider.impl.M3StaticCredentialsProvider;
import io.maestro3.sdk.v3.client.IM3Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class M3ClientConfiguration {

    @Bean("m3ServerClient")
    public IM3Client privateAgentClient(@Value("${rabbit.host}") String host,
                                        @Value("${rabbit.username}") String userName,
                                        @Value("${rabbit.password}") String password,
                                        @Value("${rabbit.virtual.host}") String virtualHost,
                                        @Value("${rabbit.port}") int port,
                                        @Value("${rabbit.ssl.enabled}") boolean sslEnabled,
                                        @Value("${server.m3api.sync.queue.name:m3api-sync-request}") String syncQueueName,
                                        @Value("${server.m3api.async.queue.name:m3api-async-request}") String asyncQueueName,
                                        @Value("${server.m3api.response.queue.name}") String responseQueueName,
                                        @Value("${server.m3api.access.key}") String privateAgentAccessKey,
                                        @Value("${server.m3api.secret.key}") String privateAgentSecretKey) {
        IM3CredentialsProvider credentialsProvider = new M3StaticCredentialsProvider(privateAgentAccessKey, privateAgentSecretKey);
        IM3AccessKeyProvider accessKeyProvider = new M3StaticAccessKeyProvider(privateAgentAccessKey);

        return M3Sdk.clientBuilder()
            .withVersion(M3SdkVersion.V3)
            .withAccessKeyProvider(accessKeyProvider)
            .withCredentialsProvider(credentialsProvider)
            .async()
            .withRabbitExecutor()
            .withVirtualHost(virtualHost)
            .withHost(host)
            .withPort(port)
            .withVirtualHost(virtualHost)
            .withUsername(userName)
            .withPassword(password)
            .withSyncRequestQueueName(syncQueueName)
            .withAsyncRequestQueueName(asyncQueueName)
            .withResponseQueue(responseQueueName)
            .sslEnabled(sslEnabled)
            .build();
    }

}
