/*
 * Copyright 2019 Wultra s.r.o.
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
package io.getlime.security.powerauth.lib.dataadapter.model.enumeration;

/**
 * Supported password protection types.
 *
 * <ul>
 * <li>NO_PROTECTION - User ID and password sent via plaintext in the request.</li>
 * <li>PASSWORD_ENCRYPTION_AES - User ID is sent in plain text and password sent encrypted by AES algorithm in the request.
 *     The encrypted password format is following: [ivBase64]:[encryptedDataBase64], without square brackets.
 *     <ul>
 *         <li>ivBase64 - Base64 encoded initialization vector bytes.</li>
 *         <li>encryptedDataBase64 - Base64 encoded encrypted password data bytes.</li>
 *     </ul>
 * </li>
 * </ul>
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
public enum PasswordProtectionType {

    /**
     * No password protection, plain text password format is used.
     */
    NO_PROTECTION,

    /**
     * Password is encrypted using AES.
     */
    PASSWORD_ENCRYPTION_AES,
}
