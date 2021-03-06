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

import io.getlime.security.powerauth.lib.nextstep.model.entity.enumeration.CredentialCategory;
import io.getlime.security.powerauth.lib.nextstep.model.entity.enumeration.CredentialDefinitionStatus;
import io.getlime.security.powerauth.lib.nextstep.model.entity.enumeration.EncryptionAlgorithm;
import io.getlime.security.powerauth.lib.nextstep.model.entity.enumeration.EndToEndEncryptionAlgorithm;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * Entity which stores credential definitions.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Entity
@Table(name = "ns_credential_definition")
@Data
@EqualsAndHashCode(of = {"name"})
public class CredentialDefinitionEntity implements Serializable {

    private static final long serialVersionUID = 1113222092995641439L;

    @Id
    @SequenceGenerator(name = "ns_credential_definition", sequenceName = "ns_credential_definition_seq")
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "ns_credential_definition")
    @Column(name = "credential_definition_id", nullable = false)
    private Long credentialDefinitionId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @ManyToOne
    @JoinColumn(name = "application_id", referencedColumnName = "application_id", nullable = false)
    private ApplicationEntity application;

    @ManyToOne
    @JoinColumn(name = "organization_id", referencedColumnName = "organization_id")
    private OrganizationEntity organization;

    @ManyToOne
    @JoinColumn(name = "credential_policy_id", referencedColumnName = "credential_policy_id", nullable = false)
    private CredentialPolicyEntity credentialPolicy;

    @Column(name = "category", nullable = false)
    @Enumerated(EnumType.STRING)
    private CredentialCategory category;

    @Column(name = "encryption_enabled")
    private boolean encryptionEnabled;

    @Column(name = "encryption_algorithm")
    @Enumerated(EnumType.STRING)
    private EncryptionAlgorithm encryptionAlgorithm;

    @ManyToOne
    @JoinColumn(name = "hashing_config_id", referencedColumnName = "hashing_config_id")
    private HashConfigEntity hashingConfig;

    @Column(name = "e2e_encryption_enabled")
    private boolean e2eEncryptionEnabled;

    @Column(name = "e2e_encryption_algorithm")
    @Enumerated(EnumType.STRING)
    private EndToEndEncryptionAlgorithm e2eEncryptionAlgorithm;

    @Column(name = "e2e_encryption_transform")
    private String e2eEncryptionCipherTransformation;

    @Column(name = "e2e_encryption_temporary")
    private boolean e2eEncryptionForTemporaryCredentialEnabled;

    @Column(name = "data_adapter_proxy_enabled")
    private boolean dataAdapterProxyEnabled;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private CredentialDefinitionStatus status;

    @Column(name = "timestamp_created", nullable = false)
    private Date timestampCreated;

    @Column(name = "timestamp_last_updated")
    private Date timestampLastUpdated;

}
