/*
 * Copyright 2017 Wultra s.r.o.
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
package io.getlime.security.powerauth.lib.nextstep.model.enumeration;

/**
 * Enum representing an authentication step result.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
public enum AuthResult {

    /**
     * Continue with authentication, send user to the next authentication step.
     */
    CONTINUE,

    /**
     * Authentication failed, there are no more steps to perform.
     */
    FAILED,

    /**
     * Authentication succeeded, there are no more steps to perform.
     */
    DONE
}
