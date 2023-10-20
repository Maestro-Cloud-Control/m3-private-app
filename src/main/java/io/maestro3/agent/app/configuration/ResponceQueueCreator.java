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

import io.maestro3.sdk.internal.util.StringUtils;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;


@Component
public class ResponceQueueCreator {

    private ConnectionFactory connectionFactory;
    private String responseQueue;

    @Autowired
    public ResponceQueueCreator(ConnectionFactory connectionFactory,
                                @Value("${server.m3api.response.queue.name}") String responseQueue) {
        this.connectionFactory = connectionFactory;
        this.responseQueue = responseQueue;
    }

    @PostConstruct
    public void createResponceQueue() {
        AmqpAdmin amqpAdmin = new RabbitAdmin(connectionFactory);
        if (StringUtils.isNotBlank(responseQueue)) {
            Queue queue = new Queue(responseQueue, true, false, false);
            amqpAdmin.declareQueue(queue);
        }
    }
}
