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

import io.maestro3.diagnostic.model.EnumResponseFormat;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

public final class HttpUtil {

    private static final String PARAM_FORMAT = "format";

    private HttpUtil() {
        throw new UnsupportedOperationException("Instantiation is forbidden.");
    }

    public static EnumResponseFormat getResponseFormat(HttpServletRequest request, EnumResponseFormat defaultFormat) {
        String format = request.getParameter(PARAM_FORMAT);
        if ("xml".equalsIgnoreCase(format)) {
            return EnumResponseFormat.XML;
        }
        if ("html".equalsIgnoreCase(format)) {
            return EnumResponseFormat.HTML;
        }
        if ("json".equalsIgnoreCase(format)) {
            return EnumResponseFormat.JSON;
        }
        return defaultFormat;
    }

    public static String getMyIp() {
        try {
            URL whatismyip = new URL("http://checkip.amazonaws.com");
            try (BufferedReader in = new BufferedReader(new InputStreamReader(
                    whatismyip.openStream()))) {
                return in.readLine();
            }
        } catch (Exception ex) {
            return null;
        }
    }
}
