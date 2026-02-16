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

import io.maestro3.agent.amqp.IntegrationChannels;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.MessageChannel;


@Configuration
@EnableIntegration
@IntegrationComponentScan("io.maestro3.agent.amqp")
public class VmwareIntegrationConfiguration {

    @Bean(IntegrationChannels.Inbound.VMWARE)
    public MessageChannel vmwareIn() {
        return new DirectChannel();
    }

    @Bean(IntegrationChannels.Inbound.VMWARE_DESCRIBER)
    public MessageChannel vmwareDescriber() {
        return new DirectChannel();
    }
}
