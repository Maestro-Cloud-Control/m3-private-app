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

package io.maestro3.diagnostic.service.impl;

import io.maestro3.diagnostic.model.EnumJMXPage;
import io.maestro3.diagnostic.model.ScheduleOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class DefaultUrlBuilder {
    private String appName;

    private static final String CUSTOM_PARAM_FORMAT = "%s=%s";
    private static final String JMX_PAGE_FORMAT = "%s/diagnostic%s";
    private static final char START_QUERY = '?';
    private static final char NEXT_PARAM = '&';
    @Autowired
    public DefaultUrlBuilder(@Value("${server.servlet.context-path}") String appName) {
        this.appName = appName;
    }

    public static String addCustomParam(String url, String key, String value) {
        String result = url;
        if (url.indexOf(START_QUERY) == -1) {
            result += START_QUERY;
        } else {
            result += NEXT_PARAM;
        }
        String customParam = String.format(CUSTOM_PARAM_FORMAT, key, value);
        result += customParam;
        return result;
    }

    public String buildJmxPageUrl(EnumJMXPage page) {
        return String.format(JMX_PAGE_FORMAT, appName, page.getPath());
    }

    public String buildJmxPageUrl(EnumJMXPage page, ScheduleOperation ops) {
        String result = String.format(JMX_PAGE_FORMAT, appName, page.getPath());
        if (ops != null) {
            result = addCustomParam(result, "restart", ops.getName());
        }
        return result;
    }

    public String buildJmxPageCustomUrl(EnumJMXPage page, String paramName, String paramValue) {
        String result = String.format(JMX_PAGE_FORMAT, appName, page.getPath());
        if (paramName != null && paramValue != null) {
            return addCustomParam(result, paramName, paramValue);
        } else {
            return result;
        }
    }
}
