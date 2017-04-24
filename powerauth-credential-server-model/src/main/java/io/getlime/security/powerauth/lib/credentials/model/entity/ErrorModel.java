/*
 * Copyright 2017 Lime - HighTech Solutions s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.getlime.security.powerauth.lib.credentials.model.entity;

/**
 * Error model, used to represent error responses.
 *
 * @author Roman Strobl
 */
public class ErrorModel {

    /**
     * Response codes for different authentication failures.
     */
    public enum Code {
        AUTHENTICATION_FAILED,
        INPUT_INVALID,
        ERROR_GENERIC
    }

    /**
     * The response code identifies reason for the authentication failure.
     */
    private Code code;

    /**
     * A message in English which describes the error.
     */
    private String message;

    /**
     * Empty constructor.
     */
    public ErrorModel() {
    }

    /**
     * Constructor with both parameters for convenience.
     * @param code response code
     * @param message message in English
     */
    public ErrorModel(Code code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * Get the response code.
     * @return response code
     */
    public Code getCode() {
        return code;
    }

    /**
     * Set the response code.
     * @param code response code
     */
    public void setCode(Code code) {
        this.code = code;
    }

    /**
     * Get the message with error description in English.
     * @return error message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Set the message with error description in English.
     * @param message error message
     */
    public void setMessage(String message) {
        this.message = message;
    }

}
