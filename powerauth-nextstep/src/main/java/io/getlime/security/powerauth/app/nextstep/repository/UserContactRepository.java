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
package io.getlime.security.powerauth.app.nextstep.repository;

import io.getlime.security.powerauth.app.nextstep.repository.model.entity.UserContactEntity;
import io.getlime.security.powerauth.app.nextstep.repository.model.entity.UserIdentityEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Crud repository for persistence of user contacts.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Repository
public interface UserContactRepository extends CrudRepository<UserContactEntity, Long> {

    /**
     * Find user contacts for a user identity.
     * @param user User identity entity.
     * @return List of user contacts.
     */
    List<UserContactEntity> findAllByUser(UserIdentityEntity user);

    /**
     * Find user contact for a user identity with given name.
     * @param user User identity entity.
     * @param name User contact name.
     * @return User contact.
     */
    Optional<UserContactEntity> findByUserAndName(UserIdentityEntity user, String name);

}