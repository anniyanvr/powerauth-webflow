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

import io.getlime.security.powerauth.app.nextstep.repository.model.entity.CredentialPolicyEntity;
import io.getlime.security.powerauth.lib.nextstep.model.entity.enumeration.CredentialPolicyStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Crud repository for persistence of credential policies.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Repository
public interface CredentialPolicyRepository extends CrudRepository<CredentialPolicyEntity, Long> {

    /**
     * Find credential policy by name.
     * @param name Credential policy name.
     * @return Credential policy.
     */
    Optional<CredentialPolicyEntity> findByName(String name);

    /**
     * Find credential policies by status.
     * @param status Credential policy status.
     * @return List of credential policies.
     */
    @Query(value = "from CredentialPolicyEntity cp where cp.status = :status")
    List<CredentialPolicyEntity> findCredentialPolicyByStatus(@Param("status") CredentialPolicyStatus status);

}