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

import io.maestro3.agent.api.ApiConstants;
import io.maestro3.agent.api.HeadersValidator;
import io.maestro3.agent.api.PrivateAgentSigner;
import io.maestro3.sdk.internal.provider.IM3CredentialsProvider;
import io.maestro3.sdk.internal.provider.impl.M3StaticCredentialsProvider;
import io.maestro3.sdk.internal.signer.IM3Signer;
import io.maestro3.sdk.internal.signer.impl.M3Signer;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.ContentTypeDelegatingMessageConverter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class M3ApiConfiguration {

    @Bean("m3ApiListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory ownershipListenerContainer(ConnectionFactory connectionFactory,
                                                                           ContentTypeDelegatingMessageConverter contentTypeDelegatingMessageConverter,
                                                                           @Value("${rabbit.concurrent.private.agent.ownership.consumers.min}") int minConcurrentConsumers,
                                                                           @Value("${rabbit.concurrent.private.agent.ownership.consumers.max}") int maxConcurrentConsumers,
                                                                           @Value("${rabbit.prefetch.count}") int prefetchCount,
                                                                           @Value("${rabbit.consecutive.active.trigger}") int consecutiveActiveTrigger,
                                                                           @Value("${rabbit.consecutive.idle.trigger}") int consecutiveIdleTrigger,
                                                                           @Value("${rabbit.start.consumer.min.interval}") long startConsumerMinInterval,
                                                                           @Value("${rabbit.stop.consumer.min.interval}") long stopConsumerMinInterval) {
        SimpleRabbitListenerContainerFactory container = new SimpleRabbitListenerContainerFactory();
        container.setConnectionFactory(connectionFactory);
        container.setAcknowledgeMode(AcknowledgeMode.AUTO);
        container.setConcurrentConsumers(minConcurrentConsumers);
        container.setMaxConcurrentConsumers(maxConcurrentConsumers);
        container.setDefaultRequeueRejected(false);
        container.setBatchSize(1);
        container.setPrefetchCount(prefetchCount);
        container.setConsecutiveActiveTrigger(consecutiveActiveTrigger);
        container.setConsecutiveIdleTrigger(consecutiveIdleTrigger);
        container.setStartConsumerMinInterval(startConsumerMinInterval);
        container.setStopConsumerMinInterval(stopConsumerMinInterval);
        container.setMessageConverter(contentTypeDelegatingMessageConverter);
        container.setAutoStartup(true);
        return container;
    }

    @Bean
    public ContentTypeDelegatingMessageConverter contentTypeDelegatingMessageConverter(Jackson2JsonMessageConverter jackson2JsonMessageConverter,
                                                                                       SimpleMessageConverter simpleMessageConverter) {
        ContentTypeDelegatingMessageConverter contentTypeDelegatingMessageConverter = new ContentTypeDelegatingMessageConverter();
        contentTypeDelegatingMessageConverter.addDelegate("application/json", jackson2JsonMessageConverter);
        contentTypeDelegatingMessageConverter.addDelegate("text/plain", simpleMessageConverter);
        return contentTypeDelegatingMessageConverter;
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public SimpleMessageConverter simpleMessageConverter() {
        return new SimpleMessageConverter();
    }

    @Bean("privateAgentApiExchange")
    public DirectExchange m3ApiExchange(@Value("${private.agent.rabbit.m3api.exchange}") String privateAgentRabbitExchangeName) {
        return new DirectExchange(privateAgentRabbitExchangeName);
    }

    @Bean("privateAgentSyncQueueBinding")
    public Binding syncQueueBinding(@Qualifier("privateAgentApiExchange") DirectExchange privateAgentApiExchange,
                                    @Qualifier("privateAgentSyncIntegrationQueue") Queue m3ApiIntegrationQueue) {
        return BindingBuilder.bind(m3ApiIntegrationQueue).to(privateAgentApiExchange).withQueueName();
    }

    @Bean("privateAgentAsyncQueueBinding")
    public Binding asyncQueueBinding(@Qualifier("privateAgentApiExchange") DirectExchange privateAgentApiExchange,
                                     @Qualifier("privateAgentAsyncIntegrationQueue") Queue m3ApiIntegrationQueue) {
        return BindingBuilder.bind(m3ApiIntegrationQueue).to(privateAgentApiExchange).withQueueName();
    }

    @Bean("privateAgentSyncIntegrationQueue")
    public Queue syncIntegrationQueue(@Value("${private.agent.rabbit.m3api.sync.queue}") String privateAgentRabbitSyncQueue) {
        return new Queue(privateAgentRabbitSyncQueue);
    }

    @Bean("privateAgentAsyncIntegrationQueue")
    public Queue asyncIntegrationQueue(@Value("${private.agent.rabbit.m3api.async.queue}") String privateAgentRabbitAsyncQueue) {
        return new Queue(privateAgentRabbitAsyncQueue);
    }

    @Bean
    public IM3CredentialsProvider credentialsProvider(@Value("${server.m3api.access.key}") String privateAgentAccessKey,
                                                      @Value("${server.m3api.secret.key}") String privateAgentSecretKey) {
        return new M3StaticCredentialsProvider(privateAgentAccessKey, privateAgentSecretKey);
    }

    @Bean
    public IM3Signer signer(IM3CredentialsProvider credentialsProvider) {
        return new M3Signer(credentialsProvider);
    }

    @Bean(ApiConstants.SDK_VALIDATOR)
    public HeadersValidator headersValidatorSdk(IM3Signer signer,
                                             @Value("${maestro.security.api.request.expiration.millis:300000}") long requestExpiration) {
        return new HeadersValidator(signer, requestExpiration);
    }

    @Bean(ApiConstants.API_VALIDATOR)
    public HeadersValidator headersValidatorApi(IM3CredentialsProvider credentialsProvider,
                                             @Value("${maestro.security.api.request.expiration.millis:300000}") long requestExpiration) {
        return new HeadersValidator(new PrivateAgentSigner(credentialsProvider), requestExpiration);
    }

}
