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

package io.getlime.security.powerauth.app.webflow.demo.oauth;

import io.getlime.security.powerauth.app.webflow.demo.model.UserResponse;
import org.springframework.social.ApiBinding;

/**
 * Interface defining the API binding.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
public interface DefaultApiBinding extends ApiBinding {

    /**
     * Get user profile.
     *
     * @return User profile.
     */
    UserResponse getProfile();

}
