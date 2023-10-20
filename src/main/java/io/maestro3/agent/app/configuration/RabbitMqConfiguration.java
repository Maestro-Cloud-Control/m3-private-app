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

import io.maestro3.agent.amqp.PrivateAgentAmqpConstants;
import io.maestro3.agent.amqp.model.SdkRabbitConfiguration;
import io.maestro3.agent.amqp.tracker.IAmqpMessageTracker;
import com.rabbitmq.client.MetricsCollector;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;


@Configuration
@EnableScheduling
@SuppressWarnings("unused")
public class RabbitMqConfiguration {

    @Bean
    public ConnectionFactory connectionFactory(SdkRabbitConfiguration configuration,
                                               IAmqpMessageTracker messageTracker) {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(configuration.getRabbitHost());
        connectionFactory.setUsername(configuration.getRabbitUsername());
        connectionFactory.setPassword(configuration.getRabbitPassword());
        connectionFactory.setVirtualHost(configuration.getRabbitVirtHost());
        MetricsCollector metricsCollector = messageTracker.registerMetricCollector(PrivateAgentAmqpConstants.SDK_REGION, configuration);
        connectionFactory.getRabbitConnectionFactory().setMetricsCollector(metricsCollector);
        connectionFactory.setPort(configuration.getRabbitPort());
        return connectionFactory;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         @Value("${rabbit.retry.attempts}") int retryAttempts,
                                         @Value("${rabbit.retry.initial.interval}") long retryInitialInterval,
                                         @Value("${rabbit.retry.interval.multiplier}") int retryIntervalMultiplier) {

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(retryAttempts);

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(retryInitialInterval);
        backOffPolicy.setMultiplier(retryIntervalMultiplier);

        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setBackOffPolicy(backOffPolicy);
        retryTemplate.setRetryPolicy(retryPolicy);

        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setRetryTemplate(retryTemplate);
        return rabbitTemplate;
    }

    @Bean
    public SdkRabbitConfiguration sdkRabbitConfiguration(@Value("${rabbit.host}") String host,
                                                         @Value("${rabbit.username}") String userName,
                                                         @Value("${rabbit.password}") String password,
                                                         @Value("${rabbit.virtual.host}") String virtualHost,
                                                         @Value("${rabbit.port}") int port,
                                                         @Value("${private.agent.rabbit.m3api.sync.queue}") String syncQueue,
                                                         @Value("${private.agent.rabbit.m3api.async.queue}") String asyncQueue,
                                                         @Value("${server.m3api.response.queue.name}") String responseQueue) {

        return new SdkRabbitConfiguration(syncQueue, asyncQueue, responseQueue, host, userName, password, virtualHost, port);
    }

    @Bean("integrationListenerContainer")
    public SimpleMessageListenerContainer listenerContainer(ConnectionFactory connectionFactory,
                                                            @Value("${rabbit.concurrent.private.agent.consumers.min}") int minConcurrentConsumers,
                                                            @Value("${rabbit.concurrent.private.agent.consumers.max}") int maxConcurrentConsumers,
                                                            @Value("${rabbit.prefetch.count}") int prefetchCount,
                                                            @Value("${rabbit.shutdown.timeout}") long shutdownTimeout,
                                                            @Value("${rabbit.consecutive.active.trigger}") int consecutiveActiveTrigger,
                                                            @Value("${rabbit.consecutive.idle.trigger}") int consecutiveIdleTrigger,
                                                            @Value("${rabbit.start.consumer.min.interval}") long startConsumerMinInterval,
                                                            @Value("${rabbit.stop.consumer.min.interval}") long stopConsumerMinInterval,
            /* get only queues needed to be listened*/ @Qualifier("listenedQueues") Queue... queues) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setQueues(queues);
        container.setAcknowledgeMode(AcknowledgeMode.AUTO);
        container.setConcurrentConsumers(minConcurrentConsumers);
        container.setMaxConcurrentConsumers(maxConcurrentConsumers);
        container.setDefaultRequeueRejected(false);
        container.setBatchSize(1);
        container.setPrefetchCount(prefetchCount);
        container.setShutdownTimeout(shutdownTimeout);
        container.setConsecutiveActiveTrigger(consecutiveActiveTrigger);
        container.setConsecutiveIdleTrigger(consecutiveIdleTrigger);
        container.setStartConsumerMinInterval(startConsumerMinInterval);
        container.setStopConsumerMinInterval(stopConsumerMinInterval);
        return container;
    }


}
