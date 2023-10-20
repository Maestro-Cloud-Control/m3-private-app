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

package io.maestro3.diagnostic.util;

import io.maestro3.diagnostic.model.healthcheck.MongoDBState;
import io.maestro3.sdk.internal.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public final class MongoParametersResolver {

    private static final Logger LOG = LoggerFactory.getLogger(MongoParametersResolver.class);

    private static final Pattern URI_PATTERN = Pattern.compile(
            "mongodb://((.*):(.*)@)?(.*):(\\d{0,6})/([a-zA-Z-_]*)\\??(.*)");

    private MongoParametersResolver() {
        throw new UnsupportedOperationException("Instantiation is forbidden.");
    }

    public static MongoDBState fromURI(String uri) {
        MongoDBState mongoDBState = new MongoDBState();
        try {
            Matcher matcher = URI_PATTERN.matcher(uri);
            if (!matcher.matches()) {
                return mongoDBState;
            }
            mongoDBState.setHost(matcher.group(4));
            mongoDBState.setPort(matcher.group(5));
            mongoDBState.setUsername(matcher.group(2));
            mongoDBState.setDatabase(matcher.group(6));
            mongoDBState.setAdditionalParams(resolveParams(matcher.group(7)));
        } catch (Exception ex) {
            LOG.error("Error during resolving mongo parameters from uri", ex);
        }
        return mongoDBState;
    }

    private static Map<String, String> resolveParams(String matcher) {
        if (StringUtils.isBlank(matcher)) {
            return Collections.emptyMap();
        }
        String[] params = matcher.split("&");
        return Arrays.stream(params)
                .map(param -> param.split("="))
                .collect(Collectors.toMap(array -> array[0], array -> array[1]));
    }
}
