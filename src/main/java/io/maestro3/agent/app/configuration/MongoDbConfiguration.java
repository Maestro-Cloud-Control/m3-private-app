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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.ConnectionString;
import io.maestro3.agent.dao.converter.MongoConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import java.net.UnknownHostException;
import java.util.List;


@Configuration
public class MongoDbConfiguration {
    public final static String MONGO_MAP_DOT_REPLACEMENT = "!@#!@#";


    @Bean
    public MongoTemplate mongoTemplate(MappingMongoConverter mongoConverter,
                                       MongoDatabaseFactory mongoDatabaseFactory) throws UnknownHostException {
        return new MongoTemplate(mongoDatabaseFactory, mongoConverter);
    }

    @Bean
    @Primary
    public MappingMongoConverter mongoCustomConverter(List<MongoConverter> converters,
                                                MongoDatabaseFactory mongoDatabaseFactory) throws UnknownHostException {
        MongoCustomConversions mongoCustomConversions = new MongoCustomConversions(converters);
        MappingMongoConverter converter = new MappingMongoConverter(new DefaultDbRefResolver(mongoDatabaseFactory),
                new MongoMappingContext());
        converter.setCustomConversions(mongoCustomConversions);
        converter.setTypeMapper(
                new DefaultMongoTypeMapper(DefaultMongoTypeMapper.DEFAULT_TYPE_KEY));
        converter.setMapKeyDotReplacement(MONGO_MAP_DOT_REPLACEMENT);
        converter.afterPropertiesSet();
        return converter;
    }

    @Bean
    @Qualifier("baseConverter")
    public MappingMongoConverter mongoConverter(MongoDatabaseFactory mongoDatabaseFactory) throws UnknownHostException {
        MappingMongoConverter converter = new MappingMongoConverter(new DefaultDbRefResolver(mongoDatabaseFactory),
                new MongoMappingContext());
        converter.setTypeMapper(
                new DefaultMongoTypeMapper(DefaultMongoTypeMapper.DEFAULT_TYPE_KEY));
        converter.setMapKeyDotReplacement(MONGO_MAP_DOT_REPLACEMENT);
        converter.afterPropertiesSet();
        return converter;
    }

    @Bean
    public MongoDatabaseFactory mongoDatabaseFactory(@Value("${mongo.db.private.agent.uri}") String uri) {
        ConnectionString connectionString = new ConnectionString(uri);
        return new SimpleMongoClientDatabaseFactory(connectionString);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
