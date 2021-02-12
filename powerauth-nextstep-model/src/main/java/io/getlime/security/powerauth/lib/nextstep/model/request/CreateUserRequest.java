/*
 * Copyright 2021 Wultra s.r.o.
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
package io.getlime.security.powerauth.lib.nextstep.model.request;

import io.getlime.security.powerauth.lib.nextstep.model.entity.enumeration.ContactType;
import io.getlime.security.powerauth.lib.nextstep.model.entity.enumeration.CredentialType;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Request object used for creating a user identity.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Data
public class CreateUserRequest {

    @NotNull
    private String userId;

    private final Map<String, Object> extras = new LinkedHashMap<>();

    private final List<String> roles = new ArrayList<>();

    private final List<NewContact> contacts = new ArrayList<>();

    private final List<NewCredential> credentials = new ArrayList<>();

    @Data
    public static class NewContact {

        @NotNull
        private String contactName;
        @NotNull
        private ContactType contactType;
        @NotNull
        private String contactValue;
        private boolean primary;

    }

    @Data
    public static class NewCredential {

        @NotNull
        private String credentialName;
        @NotNull
        private CredentialType credentialType;
        private String username;
        private String credentialValue;

    }

}
