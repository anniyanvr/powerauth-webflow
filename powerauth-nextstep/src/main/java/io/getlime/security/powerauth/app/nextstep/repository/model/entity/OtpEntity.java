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
package io.getlime.security.powerauth.app.nextstep.repository.model.entity;

import io.getlime.security.powerauth.lib.nextstep.model.entity.enumeration.EncryptionAlgorithm;
import io.getlime.security.powerauth.lib.nextstep.model.entity.enumeration.OtpStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * Entity which stores one time passwords.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Entity
@Table(name = "ns_otp_storage")
@Data
@EqualsAndHashCode(of = {"otpDefinition", "userId", "credentialDefinition", "timestampCreated"})
public class OtpEntity implements Serializable {

    private static final long serialVersionUID = 8483820995210446509L;

    @Id
    @Column(name = "otp_id", nullable = false)
    private String otpId;

    @ManyToOne
    @JoinColumn(name = "otp_definition_id", referencedColumnName = "otp_definition_id", updatable = false, nullable = false)
    private OtpDefinitionEntity otpDefinition;

    // User identity may not be present in Next Step, foreign key reference is optional
    @Column(name = "user_id", updatable = false)
    private String userId;

    @ManyToOne
    @JoinColumn(name = "credential_definition_id", referencedColumnName = "credential_definition_id", updatable = false)
    private CredentialDefinitionEntity credentialDefinition;

    @ManyToOne
    @JoinColumn(name = "operation_id", referencedColumnName = "operation_id", updatable = false)
    private OperationEntity operation;

    @Column(name = "value")
    private String value;

    @Column(name = "salt")
    private byte[] salt;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private OtpStatus status;

    @Column(name = "otp_data")
    private String otpData;

    @Column(name = "attempt_counter")
    private int attemptCounter;

    @Column(name = "failed_attempt_counter")
    private int failedAttemptCounter;

    @Column(name = "encryption_algorithm")
    @Enumerated(EnumType.STRING)
    private EncryptionAlgorithm encryptionAlgorithm;

    @Column(name = "timestamp_created", nullable = false)
    private Date timestampCreated;

    @Column(name = "timestamp_verified")
    private Date timestampVerified;

    @Column(name = "timestamp_blocked")
    private Date timestampBlocked;

    @Column(name = "timestamp_expires")
    private Date timestampExpires;

}
