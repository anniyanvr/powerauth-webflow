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
package io.getlime.security.powerauth.lib.webflow.authentication.method.form.model.request;

import io.getlime.security.powerauth.lib.nextstep.model.enumeration.AuthInstrument;
import io.getlime.security.powerauth.lib.webflow.authentication.base.AuthStepRequest;

import java.util.Collections;
import java.util.List;

/**
 * Model for a username / password authentication request from client.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
public class UsernamePasswordAuthRequest extends AuthStepRequest {

    private String username;
    private String password;
    private String organizationId;

    /**
     * Get username.
     *
     * @return Username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Set username.
     *
     * @param username Username.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Get password.
     *
     * @return Password.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Set password.
     *
     * @param password Password.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Get organization ID.
     * @return Organization ID.
     */
    public String getOrganizationId() {
        return organizationId;
    }

    /**
     * Set organization ID.
     * @param organizationId Organization ID.
     */
    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    @Override
    public List<AuthInstrument> getAuthInstruments() {
        return Collections.singletonList(AuthInstrument.CREDENTIAL);
    }
}
