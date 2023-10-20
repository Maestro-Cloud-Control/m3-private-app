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


public enum EnumStatusCode {

    NA(0, "Status not recognized"),

    UNAUTHORIZED_ERROR(401, "Request requires user authentication or authorization. %s"),
    FORBIDDEN_ERROR(402, "Performing this request is forbidden. %s"),
    NOT_FOUND_ERROR(404, "Request did not match any request mapping. %s"),
    METHOD_NOT_ALLOWED(405, "Request method is not allowed. %s"),
    NOT_ACCEPT_HEADERS(406, "Accept headers sent in the request do not match identified resource. %s"),
    CONFLICT_ERROR(409, "Request cannot be completed due to some conflict. Probably resource already exists. %s"),
    RESPONSE_EMPTY_ERROR(407, "Response is empty. Probably resource not exists. %s"),
    API_GONE_ERROR(410, "Cannot compete request, because this resource is no longer accessible. %s"),
    UNSUPPORTED_TYPE_ERROR(415, "Entity of the request is in a format not supported by requested resource. %s"),

    INTERNAL_ERROR(500, "Internal error or exception: %s"),
    METHOD_NOT_IMPLEMENTED(501, "Method is not implemented. %s"),
    EXECUTION_NOT_COMPLETE(502, "Execution did not complete. %s"),
    SERVICE_UNAVAILABLE(503, "Service is currently unavailable. %s"),
    GATEWAY_ERROR(504, "Connection between API and other service layer is down. %s"),
    INCORRECT_USER_INPUT(505, "Input parameter is incorrect. %s"),

    STATUS_OK(200, "OK. %s");

    private final int code;
    private final String message;

    EnumStatusCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public boolean equalsByCode(EnumStatusCode other) {
        if (code == other.code) {
            return true;
        } else {
            return false;
        }
    }

}
