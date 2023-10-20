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

package io.maestro3.diagnostic.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public enum EnumJMXPage {
    HEALTH_CHECK(
            Paths.HEALTH_CHECK,
            "Health",
            "healthcheck",
            EnumResponseFormat.HTML,
            Arrays.asList(EnumResponseFormat.HTML, EnumResponseFormat.JSON, EnumResponseFormat.XML)
    ),

    JMX_INFO(
            Paths.JMX_INFO,
            "Info",
            "jmx-info",
            EnumResponseFormat.HTML,
            Arrays.asList(EnumResponseFormat.HTML, EnumResponseFormat.JSON, EnumResponseFormat.XML)
    ),

    MEMORY(
            Paths.MEMORY,
            "Memory",
            "memory",
            EnumResponseFormat.HTML,
            Arrays.asList(EnumResponseFormat.HTML, EnumResponseFormat.JSON, EnumResponseFormat.XML)
    ),

    TENANTS(
            Paths.TENANTS,
            "Tenants",
            "tenants",
            EnumResponseFormat.HTML,
            Arrays.asList(EnumResponseFormat.HTML, EnumResponseFormat.JSON)
    ),

    INSTANCE_RUN_REPORT(
            Paths.INSTANCE_RUN_REPORT,
            "Run reports",
            "irr",
            EnumResponseFormat.HTML,
            Arrays.asList(EnumResponseFormat.HTML, EnumResponseFormat.JSON)
    ),

    JOBS(
            Paths.JOBS,
            "Jobs",
            "jobs",
            EnumResponseFormat.HTML,
            Arrays.asList(EnumResponseFormat.HTML, EnumResponseFormat.JSON)
    ),

    OPERATIONS(
        Paths.OPERATIONS,
        "Restart",
        "operations",
        EnumResponseFormat.HTML,
        Arrays.asList(EnumResponseFormat.HTML, EnumResponseFormat.JSON)
    ),

    EXTENDED_HTTP_METRICS(
        Paths.EXTENDED_HTTP,
        "Api Info",
        "api-info",
        EnumResponseFormat.HTML,
        Arrays.asList(EnumResponseFormat.HTML, EnumResponseFormat.JSON, EnumResponseFormat.XML)
    );

    private String path;

    private String label;
    private String viewName;
    private EnumResponseFormat defaultFormat;
    private List<EnumResponseFormat> supportedFormats;

    EnumJMXPage(String path, String label, String viewName, EnumResponseFormat defaultFormat, List<EnumResponseFormat> supportedFormats) {
        this.path = path;
        this.label = label;
        this.viewName = viewName;
        this.defaultFormat = defaultFormat;
        this.supportedFormats = supportedFormats;
        if (this.supportedFormats == null) {
            this.supportedFormats = Collections.emptyList();
        }
    }

    public String getPath() {
        return path;
    }

    public String getLabel() {
        return label;
    }

    public String getViewName() {
        return viewName;
    }

    public EnumResponseFormat getDefaultFormat() {
        return defaultFormat;
    }

    public List<EnumResponseFormat> getSupportedFormats() {
        return supportedFormats;
    }

    public interface Paths {
        String JMX_INFO = "/jmx-info";
        String MEMORY = "/memory";
        String HEALTH_CHECK = "/health-check";
        String OPERATIONS = "/operations";
        String EXTENDED_HTTP = "/http-extended";
        String JOBS = "/jobs";
        String TENANTS = "/tenants";
        String INSTANCE_RUN_REPORT = "/irr";
    }

    public interface RequestParams {
        String TENANT = "tenant";
        String REGION = "region";
        String CLOUD = "cloud";
    }
}
