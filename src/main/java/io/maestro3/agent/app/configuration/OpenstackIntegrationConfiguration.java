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

import io.maestro3.agent.amqp.Headers;
import io.maestro3.agent.amqp.IntegrationChannels;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.inbound.AmqpInboundChannelAdapter;
import org.springframework.integration.amqp.outbound.AmqpOutboundEndpoint;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.router.HeaderValueRouter;
import org.springframework.messaging.MessageChannel;


@Configuration
@EnableIntegration
@IntegrationComponentScan("io.maestro3.agent.amqp")
@SuppressWarnings("unused")
public class OpenstackIntegrationConfiguration {

    @Bean(IntegrationChannels.Inbound.PLAIN)
    public MessageChannel inboundChannel() {
        return new DirectChannel();
    }

    @Bean(IntegrationChannels.Inbound.JSON)
    public MessageChannel jsonChannel() {
        return new DirectChannel();
    }

    @Bean(IntegrationChannels.Inbound.ZIP)
    public MessageChannel zipChannel() {
        return new DirectChannel();
    }

    @Bean(IntegrationChannels.Inbound.DISPATCHER)
    public MessageChannel dispatcherChannel() {
        return new DirectChannel();
    }

    @Bean(IntegrationChannels.Inbound.PRIVATE_CLOUD)
    public MessageChannel privateCloudChannel() {
        return new DirectChannel();
    }

    @Bean(IntegrationChannels.Outbound.PLAIN)
    public MessageChannel outboundChannel() {
        return new DirectChannel();
    }

    @Bean(IntegrationChannels.Outbound.ZIP)
    public MessageChannel outboundZipChannel() {
        return new DirectChannel();
    }

    @Bean(IntegrationChannels.Outbound.JSON)
    public MessageChannel outboundJsonChannel() {
        return new DirectChannel();
    }


    @Bean(IntegrationChannels.Inbound.OS_BYTES)
    public MessageChannel bytesChannel() {
        return new DirectChannel();
    }

    @Bean(IntegrationChannels.Inbound.OS_NOTIFICATIONS)
    public MessageChannel notificationsChannel() {
        return new DirectChannel();
    }

    @Bean(IntegrationChannels.Inbound.OS_NOTIFICATIONS_ENCODED)
    public MessageChannel encodedNotificationsChannel() {
        return new DirectChannel();
    }

    @Bean
    public AmqpInboundChannelAdapter inboundChannelAdapter(@Qualifier("integrationListenerContainer") SimpleMessageListenerContainer listenerContainer) {
        AmqpInboundChannelAdapter adapter = new AmqpInboundChannelAdapter(listenerContainer);
        adapter.setOutputChannelName(IntegrationChannels.Inbound.PLAIN);
        adapter.setAutoStartup(true);
        return adapter;
    }

    @Bean
    @ServiceActivator(inputChannel = IntegrationChannels.Inbound.PLAIN)
    public HeaderValueRouter startRouter() {
        HeaderValueRouter router = new HeaderValueRouter(Headers.ZIPPED);
        router.setDefaultOutputChannelName(IntegrationChannels.Inbound.JSON);
        router.setResolutionRequired(false);
        router.setChannelMapping("zipped", IntegrationChannels.Inbound.ZIP);
        return router;
    }

    @Bean
    @ServiceActivator(inputChannel = IntegrationChannels.Inbound.DISPATCHER)
    public HeaderValueRouter jsonDispatcher() {
        HeaderValueRouter router = new HeaderValueRouter(Headers.EVENT_GROUP);
        router.setDefaultOutputChannelName(IntegrationChannels.Inbound.JSON);
        router.setResolutionRequired(false);
        return router;
    }

    @Bean
    @ServiceActivator(inputChannel = IntegrationChannels.Outbound.PLAIN)
    public AmqpOutboundEndpoint amqpOutbound(AmqpTemplate amqpTemplate) {
        AmqpOutboundEndpoint outbound = new AmqpOutboundEndpoint(amqpTemplate);
        outbound.setExpectReply(false);
        outbound.setRoutingKeyExpressionString("headers.routingKey");
        return outbound;
    }
}
