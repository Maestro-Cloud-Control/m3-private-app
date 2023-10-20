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

import com.fasterxml.jackson.annotation.JsonCreator;
import io.maestro3.sdk.internal.util.StringUtils;


public enum ScheduleOperation {
    MOCK("MOCK", "mockAction", "Mock action");

    private String scope;
    private String name;
    private String title;

    ScheduleOperation(String scope, String name, String title) {
        this.scope = scope;
        this.name = name;
        this.title = title;
    }

    @JsonCreator
    public static ScheduleOperation fromValue(String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }

        for (ScheduleOperation value : values()) {
            if (name.equalsIgnoreCase(value.getName())) {

                return value;
            }
        }

        return null;
    }

    public static String getAllScheduleOperations() {
        StringBuilder sb = new StringBuilder(80);

        ScheduleOperation[] scheduleOperations = ScheduleOperation.values();
        for (ScheduleOperation operation : scheduleOperations) {
            if (sb.length() != 0) {
                sb.append(", ");
            }
            sb.append(operation.getName());
        }

        return sb.toString();
    }

    public String getScope() {
        return scope;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

}
