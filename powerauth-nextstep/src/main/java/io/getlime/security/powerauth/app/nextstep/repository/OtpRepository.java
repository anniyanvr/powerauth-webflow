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

import io.getlime.security.powerauth.app.nextstep.repository.model.entity.OperationEntity;
import io.getlime.security.powerauth.app.nextstep.repository.model.entity.OtpEntity;
import io.getlime.security.powerauth.app.nextstep.repository.model.entity.UserIdentityEntity;
import io.getlime.security.powerauth.lib.nextstep.model.entity.enumeration.OtpStatus;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Crud repository for persistence of one time passwords.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Repository
public interface OtpRepository extends CrudRepository<OtpEntity, String> {

    /**
     * Find OTP entities by user identity.
     * @param userId User identity entity.
     * @return List of OTP entities.
     */
    List<OtpEntity> findAllByUserId(UserIdentityEntity userId);

    /**
     * Find OTP entities by operation.
     * @param operationEntity Operation entity.
     * @return List of OTP entities.
     */
    List<OtpEntity> findAllByOperationOrderByTimestampCreatedDesc(OperationEntity operationEntity);

    /**
     * Find OTP entities by operation and status.
     * @param operationEntity Operation entity.
     * @param status OTP status.
     * @return List of OTP entities.
     */
    List<OtpEntity> findAllByOperationAndStatus(OperationEntity operationEntity, OtpStatus status);

}