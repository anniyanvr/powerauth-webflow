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
package io.getlime.security.powerauth.app.nextstep.service;

import io.getlime.security.powerauth.app.nextstep.configuration.NextStepServerConfiguration;
import io.getlime.security.powerauth.app.nextstep.converter.CredentialConverter;
import io.getlime.security.powerauth.app.nextstep.repository.CredentialRepository;
import io.getlime.security.powerauth.app.nextstep.repository.UserIdentityRepository;
import io.getlime.security.powerauth.app.nextstep.repository.model.entity.CredentialDefinitionEntity;
import io.getlime.security.powerauth.app.nextstep.repository.model.entity.CredentialEntity;
import io.getlime.security.powerauth.app.nextstep.repository.model.entity.CredentialPolicyEntity;
import io.getlime.security.powerauth.app.nextstep.repository.model.entity.UserIdentityEntity;
import io.getlime.security.powerauth.lib.nextstep.model.entity.CredentialDetail;
import io.getlime.security.powerauth.lib.nextstep.model.entity.CredentialSecretDetail;
import io.getlime.security.powerauth.lib.nextstep.model.entity.CredentialValue;
import io.getlime.security.powerauth.lib.nextstep.model.entity.enumeration.*;
import io.getlime.security.powerauth.lib.nextstep.model.entity.error.CredentialValidationError;
import io.getlime.security.powerauth.lib.nextstep.model.exception.*;
import io.getlime.security.powerauth.lib.nextstep.model.request.*;
import io.getlime.security.powerauth.lib.nextstep.model.response.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.*;

/**
 * This service handles persistence of credentials.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Service
public class CredentialService {

    private final Logger logger = LoggerFactory.getLogger(CredentialService.class);

    private final UserIdentityLookupService userIdentityLookupService;
    private final CredentialDefinitionService credentialDefinitionService;
    private final CredentialRepository credentialRepository;
    private final CredentialHistoryService credentialHistoryService;
    private final IdGeneratorService idGeneratorService;
    private final NextStepServerConfiguration nextStepServerConfiguration;
    private final CredentialGenerationService credentialGenerationService;
    private final CredentialValidationService credentialValidationService;
    private final CredentialProtectionService credentialProtectionService;
    private final EndToEndEncryptionService endToEndEncryptionService;
    private final UserIdentityRepository userIdentityRepository;

    private final CredentialConverter credentialConverter = new CredentialConverter();

    /**
     * Credential service constructor.
     * @param userIdentityLookupService User identity lookup service.
     * @param credentialDefinitionService Credential definition service.
     * @param credentialRepository Credential repository.
     * @param credentialHistoryService Credential history service.
     * @param idGeneratorService ID generator service.
     * @param nextStepServerConfiguration Next Step server configuration.
     * @param credentialGenerationService Credential generation service.
     * @param credentialValidationService Credential validation service.
     * @param credentialProtectionService Credential protection service.
     * @param endToEndEncryptionService End-to-end encryption service.
     * @param userIdentityRepository User identity repository.
     */
    @Autowired
    public CredentialService(UserIdentityLookupService userIdentityLookupService, CredentialDefinitionService credentialDefinitionService, CredentialRepository credentialRepository, CredentialHistoryService credentialHistoryService, IdGeneratorService idGeneratorService, NextStepServerConfiguration nextStepServerConfiguration, CredentialGenerationService credentialGenerationService, CredentialValidationService credentialValidationService, CredentialProtectionService credentialProtectionService, EndToEndEncryptionService endToEndEncryptionService, UserIdentityRepository userIdentityRepository) {
        this.userIdentityLookupService = userIdentityLookupService;
        this.credentialDefinitionService = credentialDefinitionService;
        this.credentialRepository = credentialRepository;
        this.credentialHistoryService = credentialHistoryService;
        this.idGeneratorService = idGeneratorService;
        this.nextStepServerConfiguration = nextStepServerConfiguration;
        this.credentialGenerationService = credentialGenerationService;
        this.credentialValidationService = credentialValidationService;
        this.credentialProtectionService = credentialProtectionService;
        this.endToEndEncryptionService = endToEndEncryptionService;
        this.userIdentityRepository = userIdentityRepository;
    }

    /**
     * Create a credential. Username and/or credential value is generated in case it is not specified.
     * @param request Create credential request.
     * @return Create credential response.
     * @throws UserNotFoundException Thrown when user identity is not found.
     * @throws CredentialDefinitionNotFoundException Thrown when credential definition is not found.
     * @throws InvalidConfigurationException Thrown when Next Step configuration is invalid.
     * @throws InvalidRequestException Thrown when request is invalid.
     * @throws CredentialValidationFailedException Thrown when credential validation fails.
     * @throws EncryptionException Thrown when encryption or decryption fails.
     */
    @Transactional
    public CreateCredentialResponse createCredential(CreateCredentialRequest request) throws UserNotFoundException, CredentialDefinitionNotFoundException, InvalidConfigurationException, InvalidRequestException, CredentialValidationFailedException, EncryptionException {
        UserIdentityEntity user = userIdentityLookupService.findUser(request.getUserId());
        final CredentialDefinitionEntity credentialDefinition = credentialDefinitionService.findActiveCredentialDefinition(request.getCredentialName());
        final CredentialType credentialType = request.getCredentialType();
        final String username = request.getUsername();
        String credentialValue = request.getCredentialValue();
        if (credentialValue != null && credentialDefinition.isE2eEncryptionEnabled()) {
            credentialValue = endToEndEncryptionService.decryptCredential(credentialValue, credentialDefinition);
        }
        CredentialValidationMode validationMode = request.getValidationMode();
        final List<CreateCredentialRequest.CredentialHistory> credentialHistory = request.getCredentialHistory();
        if (validationMode == null) {
            validationMode = CredentialValidationMode.VALIDATE_USERNAME_AND_CREDENTIAL;
        }
        final CredentialSecretDetail credentialDetail = createCredential(user, credentialDefinition, credentialType, username, credentialValue, validationMode);
        if (credentialHistory != null && !credentialHistory.isEmpty()) {
            final int dateCount = credentialHistory.size();
            // Use unique timestamps in seconds to keep order of credential history
            long createdTimestamp = new Date().getTime() - (dateCount * 1000L);
            for (CreateCredentialRequest.CredentialHistory h : credentialHistory) {
                final Date createdDate = new Date(createdTimestamp);
                String credentialValueHistory = h.getCredentialValue();
                if (credentialDefinition.isE2eEncryptionEnabled()) {
                    credentialValueHistory = endToEndEncryptionService.decryptCredential(credentialValueHistory, credentialDefinition);
                }
                importCredentialHistory(user, credentialDefinition, h.getUsername(), credentialValueHistory, createdDate);
                createdTimestamp += 1000;
            }
        }
        user = userIdentityRepository.save(user);
        final CreateCredentialResponse response = new CreateCredentialResponse();
        response.setCredentialName(credentialDetail.getCredentialName());
        response.setCredentialType(credentialDetail.getCredentialType());
        response.setUserId(user.getUserId());
        response.setCredentialStatus(credentialDetail.getCredentialStatus());
        response.setUsername(credentialDetail.getUsername());
        if (request.getCredentialValue() == null) {
            // Return generated credential value, with possible end2end encryption
            String credentialValueResponse = credentialDetail.getCredentialValue();
            if (credentialDefinition.isE2eEncryptionEnabled() &&
                    (credentialDetail.getCredentialType() == CredentialType.PERMANENT || credentialDefinition.isE2eEncryptionForTemporaryCredentialEnabled())) {
                credentialValueResponse = endToEndEncryptionService.encryptCredential(credentialValueResponse, credentialDefinition);
            }
            response.setCredentialValue(credentialValueResponse);
        }
        response.setCredentialChangeRequired(credentialDetail.isCredentialChangeRequired());
        return response;
    }

    /**
     * Update a credential.
     * @param request Update credential request.
     * @return Update credential response.
     * @throws UserNotFoundException Thrown when user is not found.
     * @throws CredentialDefinitionNotFoundException Thrown when credential definition is not found.
     * @throws CredentialNotFoundException Thrown when credential is not found.
     * @throws CredentialValidationFailedException Thrown when credential validation fails.
     * @throws InvalidRequestException Thrown when request is invalid.
     * @throws InvalidConfigurationException Thrown when Next Step configuration is invalid.
     * @throws EncryptionException Thrown when encryption or decryption fails.
     */
    @Transactional
    public UpdateCredentialResponse updateCredential(UpdateCredentialRequest request) throws UserNotFoundException, CredentialDefinitionNotFoundException, CredentialNotFoundException, CredentialValidationFailedException, InvalidRequestException, InvalidConfigurationException, EncryptionException {
        UserIdentityEntity user = userIdentityLookupService.findUser(request.getUserId());
        final CredentialDefinitionEntity credentialDefinition = credentialDefinitionService.findActiveCredentialDefinition(request.getCredentialName());
        final Optional<CredentialEntity> credentialOptional = user.getCredentials().stream().filter(c -> c.getCredentialDefinition().equals(credentialDefinition)).findFirst();
        if (!credentialOptional.isPresent()) {
            throw new CredentialNotFoundException("Credential not found: " + request.getCredentialName() + ", user ID: " + user.getUserId());
        }
        boolean updateCredentialExpiration = false;
        final CredentialEntity credential = credentialOptional.get();
        if (credential.getStatus() == CredentialStatus.REMOVED && request.getCredentialStatus() == null) {
            throw new CredentialNotFoundException("Credential is REMOVED: " + request.getCredentialName() + ", user ID: " + user.getUserId());
        }
        if (credential.getStatus() == CredentialStatus.REMOVED && request.getCredentialStatus() == CredentialStatus.REMOVED) {
            throw new CredentialNotFoundException("Credential is already REMOVED: " + request.getCredentialName() + ", user ID: " + user.getUserId());
        }
        if (request.getCredentialType() != null) {
            credential.setType(request.getCredentialType());
            updateCredentialExpiration = true;
        }
        String username = null;
        String credentialValue = request.getCredentialValue();
        if (credentialValue != null && credentialDefinition.isE2eEncryptionEnabled()) {
            credentialValue = endToEndEncryptionService.decryptCredential(credentialValue, credentialDefinition);
        }
        CredentialValidationMode validationMode = CredentialValidationMode.NO_VALIDATION;
        if (request.getUsername() != null && request.getCredentialValue() != null) {
            username = request.getUsername();
            validationMode = CredentialValidationMode.VALIDATE_USERNAME_AND_CREDENTIAL;
        } else if (request.getCredentialValue() != null) {
            username = credential.getUsername();
            validationMode = CredentialValidationMode.VALIDATE_CREDENTIAL;
        } else if (request.getUsername() != null) {
            username = request.getUsername();
            validationMode = CredentialValidationMode.VALIDATE_USERNAME;
        }
        if (request.getUsername() != null || request.getCredentialValue() != null) {
            final List<CredentialValidationFailure> validationErrors = credentialValidationService.validateCredential(user,
                    credentialDefinition, username, credentialValue, validationMode);
            if (!validationErrors.isEmpty()) {
                CredentialValidationError error = new CredentialValidationError(CredentialValidationFailedException.CODE, "Validation failed", validationErrors);
                throw new CredentialValidationFailedException("Validation failed for user ID: " + user.getUserId(), error);
            }
        }
        if (request.getUsername() != null) {
            credential.setUsername(username);
        }
        if (request.getCredentialValue() != null) {
            final CredentialValue protectedValue = credentialProtectionService.protectCredential(credentialValue, credential);
            credential.setValue(protectedValue.getValue());
            credential.setEncryptionAlgorithm(protectedValue.getEncryptionAlgorithm());
            credential.setHashingConfig(credentialDefinition.getHashingConfig());
            credential.setTimestampLastCredentialChange(new Date());
            updateCredentialExpiration = true;
        }
        if (updateCredentialExpiration) {
            updateCredentialExpirationTime(credential, credentialDefinition.getCredentialPolicy());
        }
        if (request.getCredentialStatus() != null) {
            credential.setStatus(request.getCredentialStatus());
                if (credential.getStatus() == CredentialStatus.BLOCKED_TEMPORARY || credential.getStatus() == CredentialStatus.BLOCKED_PERMANENT){
                // For blocked credentials set timestamp when credential was blocked
                credential.setTimestampBlocked(new Date());
            } else if (credential.getStatus() == CredentialStatus.ACTIVE) {
                // Reset counters for active credentials
                credential.setFailedAttemptCounterSoft(0);
                credential.setFailedAttemptCounterHard(0);
                credential.setTimestampBlocked(null);
            }
        }
        credential.setTimestampLastUpdated(new Date());
        if (request.getCredentialValue() != null) {
            // Save credential into credential history
            credentialHistoryService.createCredentialHistory(user, credential, new Date());
        }
        user = userIdentityRepository.save(user);
        final UpdateCredentialResponse response = new UpdateCredentialResponse();
        response.setUserId(user.getUserId());
        response.setCredentialName(credential.getCredentialDefinition().getName());
        response.setCredentialType(credential.getType());
        response.setCredentialStatus(credential.getStatus());
        response.setUsername(credential.getUsername());
        final boolean credentialChangeRequired;
        if (request.getCredentialValue() != null) {
            credentialChangeRequired = isCredentialChangeRequired(credential, credentialValue);
        } else {
            if (credentialDefinition.getHashingConfig() == null) {
                credentialChangeRequired = isCredentialChangeRequired(credential, credential.getValue());
            } else {
                credentialChangeRequired = isCredentialChangeRequired(credential, null);
            }
        }
        response.setCredentialChangeRequired(credentialChangeRequired);
        return response;
    }

    /**
     * Get credential list for a user identity.
     * @param request Get credential list request.
     * @return Get credential list response.
     * @throws UserNotFoundException Thrown when user identity is not found.
     * @throws InvalidConfigurationException Thrown when Next Step configuration is invalid.
     * @throws EncryptionException Thrown when decryption fails.
     */
    @Transactional
    public GetUserCredentialListResponse getCredentialList(GetUserCredentialListRequest request) throws UserNotFoundException, InvalidConfigurationException, EncryptionException {
        final UserIdentityEntity user = userIdentityLookupService.findUser(request.getUserId());
        final GetUserCredentialListResponse response = new GetUserCredentialListResponse();
        response.setUserId(user.getUserId());
        final Set<CredentialEntity> credentials = user.getCredentials();
        for (CredentialEntity credential: credentials) {
            if (credential.getStatus() == CredentialStatus.REMOVED && !request.isIncludeRemoved()) {
                continue;
            }
            final CredentialDetail credentialDetail = credentialConverter.fromEntity(credential);
            final boolean credentialChangeRequired;
            if (credential.getCredentialDefinition().getHashingConfig() == null) {
                credentialChangeRequired = isCredentialChangeRequired(credential, credential.getValue());
            } else {
                credentialChangeRequired = isCredentialChangeRequired(credential, null);
            }
            credentialDetail.setCredentialChangeRequired(credentialChangeRequired);
            response.getCredentials().add(credentialDetail);
        }
        return response;
    }

    /**
     * Validate a credential.
     * @param request Validate credential request.
     * @return Validate credential response.
     * @throws CredentialDefinitionNotFoundException Thrown when credential definition is not found.
     * @throws InvalidRequestException Thrown when request is invalid.
     * @throws UserNotFoundException Thrown when user is not found.
     * @throws InvalidConfigurationException Thrown when Next Step configuration is invalid.
     * @throws EncryptionException Thrown when decryption fails.
     */
    @Transactional
    public ValidateCredentialResponse validateCredential(ValidateCredentialRequest request) throws CredentialDefinitionNotFoundException, InvalidRequestException, UserNotFoundException, InvalidConfigurationException, EncryptionException {
        final UserIdentityEntity user = userIdentityLookupService.findUser(request.getUserId());
        final CredentialDefinitionEntity credentialDefinition = credentialDefinitionService.findActiveCredentialDefinition(request.getCredentialName());
        final String username = request.getUsername();
        String credentialValue = request.getCredentialValue();
        final CredentialValidationMode validationMode = request.getValidationMode();
        if (credentialValue != null && credentialDefinition.isE2eEncryptionEnabled()) {
            credentialValue = endToEndEncryptionService.decryptCredential(credentialValue, credentialDefinition);
        }
        final List<CredentialValidationFailure> validationErrors = credentialValidationService.validateCredential(user,
                credentialDefinition, username, credentialValue, validationMode);
        final ValidateCredentialResponse response = new ValidateCredentialResponse();
        if (validationErrors.isEmpty()) {
            response.setValidationResult(CredentialValidationResult.SUCCEEDED);
        } else {
            response.setValidationResult(CredentialValidationResult.FAILED);
        }
        response.getValidationErrors().addAll(validationErrors);
        return response;
    }

    /**
     * Check whether credential change is required.
     * @param credential Credential entity.
     * @param unprotectedCredentialValue Unprotected credential value.
     * @return Whether credential change is required.
     * @throws InvalidConfigurationException Thrown when Next Step configuration is invalid.
     * @throws EncryptionException Thrown when decryption fails.
     */
    public boolean isCredentialChangeRequired(CredentialEntity credential, String unprotectedCredentialValue) throws InvalidConfigurationException, EncryptionException {
        // Check expiration time
        final Date expirationTime = credential.getTimestampExpires();
        if (expirationTime != null && new Date().after(expirationTime)) {
            return true;
        }
        // Perform an actual check of credential expiration for case that credential policy was updated after last credential change
        final CredentialPolicyEntity credentialPolicy = credential.getCredentialDefinition().getCredentialPolicy();
        if (credentialPolicy.isRotationEnabled()) {
            final Date lastChange = credential.getTimestampLastCredentialChange();
            if (lastChange == null) {
                // Only happens when data in database is manipulated
                credential.setTimestampExpires(new Date());
                return true;
            }
            final Calendar c = GregorianCalendar.getInstance();
            c.add(Calendar.DAY_OF_YEAR, -credentialPolicy.getRotationDays());
            if (lastChange.before(c.getTime())) {
                // Last credential change occurred before time calculated by password rotation days
                credential.setTimestampExpires(new Date());
                return true;
            }
        }
        if (unprotectedCredentialValue == null) {
            return false;
        }
        final List<CredentialValidationFailure> validationFailures = credentialValidationService.validateCredentialValue(credential.getUser(),
                credential.getUsername(), unprotectedCredentialValue, credential.getCredentialDefinition(), false);
        return !validationFailures.isEmpty();
    }

    /**
     * Reset a credential. Generate a new credential value, set credential status to ACTIVE, and reset failed attempt counters.
     * @param request Reset credential request.
     * @return Reset credential response.
     * @throws UserNotFoundException Thrown when user identity is not found.
     * @throws CredentialDefinitionNotFoundException Thrown when credential definition is not found.
     * @throws CredentialNotFoundException Thrown when credential is not found.
     * @throws InvalidConfigurationException Thrown in case Next Step configuration is invalid.
     * @throws EncryptionException Thrown when encryption fails.
     */
    @Transactional
    public ResetCredentialResponse resetCredential(ResetCredentialRequest request) throws UserNotFoundException, CredentialDefinitionNotFoundException, CredentialNotFoundException, InvalidConfigurationException, EncryptionException {
        UserIdentityEntity user = userIdentityLookupService.findUser(request.getUserId());
        final CredentialDefinitionEntity credentialDefinition = credentialDefinitionService.findActiveCredentialDefinition(request.getCredentialName());
        final Optional<CredentialEntity> credentialOptional = user.getCredentials().stream().filter(c -> c.getCredentialDefinition().equals(credentialDefinition)).findFirst();
        if (!credentialOptional.isPresent()) {
            throw new CredentialNotFoundException("Credential not found: " + request.getCredentialName() + ", user ID: " + user.getUserId());
        }
        final CredentialEntity credential = credentialOptional.get();
        if (credential.getStatus() == CredentialStatus.REMOVED) {
            throw new CredentialNotFoundException("Credential is REMOVED: " + request.getCredentialName() + ", user ID: " + user.getUserId());
        }
        if (request.getCredentialType() != null) {
            credential.setType(request.getCredentialType());
        }
        updateCredentialExpirationTime(credential, credentialDefinition.getCredentialPolicy());
        final String unprotectedCredentialValue = credentialGenerationService.generateCredentialValue(credentialDefinition);
        final CredentialValue protectedCredentialValue = credentialProtectionService.protectCredential(unprotectedCredentialValue, credential);
        credential.setValue(protectedCredentialValue.getValue());
        credential.setEncryptionAlgorithm(protectedCredentialValue.getEncryptionAlgorithm());
        credential.setHashingConfig(credentialDefinition.getHashingConfig());
        credential.setTimestampLastUpdated(new Date());
        credential.setTimestampLastCredentialChange(new Date());
        credential.setFailedAttemptCounterSoft(0);
        credential.setFailedAttemptCounterHard(0);
        credential.setStatus(CredentialStatus.ACTIVE);
        credential.setTimestampBlocked(null);
        // Save credential into credential history
        credentialHistoryService.createCredentialHistory(user, credential, new Date());
        user = userIdentityRepository.save(user);
        final ResetCredentialResponse response = new ResetCredentialResponse();
        response.setUserId(user.getUserId());
        response.setCredentialName(credential.getCredentialDefinition().getName());
        response.setUsername(credential.getUsername());
        // Generated password must be returned in unprotected form
        String credentialValueResponse = unprotectedCredentialValue;
        if (credentialDefinition.isE2eEncryptionEnabled() &&
                (credential.getType() == CredentialType.PERMANENT || credentialDefinition.isE2eEncryptionForTemporaryCredentialEnabled())) {
            credentialValueResponse = endToEndEncryptionService.encryptCredential(credentialValueResponse, credentialDefinition);
        }
        response.setCredentialValue(credentialValueResponse);
        response.setCredentialStatus(credential.getStatus());
        return response;
    }

    /**
     * Delete a credential (status = REMOVED).
     * @param request Delete credential request.
     * @return Delete credential response.
     * @throws UserNotFoundException Thrown when user identity is not found.
     * @throws CredentialDefinitionNotFoundException Thrown when credential definition is not found.
     * @throws CredentialNotFoundException Thrown when credential is not found.
     * @throws CredentialNotFoundException Thrown when credential is not found.
     */
    @Transactional
    public DeleteCredentialResponse deleteCredential(DeleteCredentialRequest request) throws UserNotFoundException, CredentialDefinitionNotFoundException, CredentialNotFoundException {
        UserIdentityEntity user = userIdentityLookupService.findUser(request.getUserId());
        final CredentialDefinitionEntity credentialDefinition = credentialDefinitionService.findActiveCredentialDefinition(request.getCredentialName());
        final Optional<CredentialEntity> credentialOptional = user.getCredentials().stream().filter(c -> c.getCredentialDefinition().equals(credentialDefinition)).findFirst();
        if (!credentialOptional.isPresent()) {
            throw new CredentialNotFoundException("Credential not found: " + request.getCredentialName() + ", user ID: " + user.getUserId());
        }
        final CredentialEntity credential = credentialOptional.get();
        if (credential.getStatus() == CredentialStatus.REMOVED) {
            throw new CredentialNotFoundException("Credential is already REMOVED: " + request.getCredentialName() + ", user ID: " + user.getUserId());
        }
        credential.setStatus(CredentialStatus.REMOVED);
        user = userIdentityRepository.save(user);
        final DeleteCredentialResponse response = new DeleteCredentialResponse();
        response.setUserId(user.getUserId());
        response.setCredentialName(credential.getCredentialDefinition().getName());
        response.setCredentialStatus(credential.getStatus());
        return response;
    }

    /**
     * Block a credential (status = BLOCKED_PERMANENT).
     * @param request Block credential request.
     * @return Block credential response.
     * @throws UserNotFoundException Thrown when user identity is not found.
     * @throws CredentialDefinitionNotFoundException Thrown when credential definition is not found.
     * @throws CredentialNotFoundException Thrown when credential is not found.
     * @throws CredentialNotActiveException Thrown when credential is not active.
     */
    @Transactional
    public BlockCredentialResponse blockCredential(BlockCredentialRequest request) throws UserNotFoundException, CredentialDefinitionNotFoundException, CredentialNotFoundException, CredentialNotActiveException {
        UserIdentityEntity user = userIdentityLookupService.findUser(request.getUserId());
        final CredentialDefinitionEntity credentialDefinition = credentialDefinitionService.findActiveCredentialDefinition(request.getCredentialName());
        final Optional<CredentialEntity> credentialOptional = user.getCredentials().stream().filter(c -> c.getCredentialDefinition().equals(credentialDefinition)).findFirst();
        if (!credentialOptional.isPresent()) {
            throw new CredentialNotFoundException("Credential not found: " + request.getCredentialName() + ", user ID: " + user.getUserId());
        }
        final CredentialEntity credential = credentialOptional.get();
        if (credential.getStatus() != CredentialStatus.ACTIVE && credential.getStatus() != CredentialStatus.BLOCKED_TEMPORARY) {
            throw new CredentialNotActiveException("Credential is not ACTIVE or BLOCKED_TEMPORARY: " + request.getCredentialName() + ", user ID: " + user.getUserId());
        }
        credential.setStatus(CredentialStatus.BLOCKED_PERMANENT);
        credential.setTimestampBlocked(new Date());
        user = userIdentityRepository.save(user);
        final BlockCredentialResponse response = new BlockCredentialResponse();
        response.setUserId(user.getUserId());
        response.setCredentialName(credential.getCredentialDefinition().getName());
        response.setCredentialStatus(credential.getStatus());
        return response;
    }

    /**
     * Unblock a credential (status = ACTIVE).
     * @param request Unblock credential request.
     * @return Unblock credential response.
     * @throws UserNotFoundException Thrown in case user is not found.
     * @throws CredentialDefinitionNotFoundException Thrown in case credential definition is not found.
     * @throws CredentialNotFoundException Thrown in case credential is not found.
     * @throws CredentialNotBlockedException Thrown in case credential is not blocked.
     */
    @Transactional
    public UnblockCredentialResponse unblockCredential(UnblockCredentialRequest request) throws UserNotFoundException, CredentialDefinitionNotFoundException, CredentialNotFoundException, CredentialNotBlockedException {
        UserIdentityEntity user = userIdentityLookupService.findUser(request.getUserId());
        final CredentialDefinitionEntity credentialDefinition = credentialDefinitionService.findActiveCredentialDefinition(request.getCredentialName());
        final Optional<CredentialEntity> credentialOptional = user.getCredentials().stream().filter(c -> c.getCredentialDefinition().equals(credentialDefinition)).findFirst();
        if (!credentialOptional.isPresent()) {
            throw new CredentialNotFoundException("Credential not found: " + request.getCredentialName() + ", user ID: " + user.getUserId());
        }
        final CredentialEntity credential = credentialOptional.get();
        if (credential.getStatus() == CredentialStatus.REMOVED) {
            throw new CredentialNotFoundException("Credential is REMOVED: " + request.getCredentialName() + ", user ID: " + user.getUserId());
        }
        if (credential.getStatus() != CredentialStatus.BLOCKED_PERMANENT && credential.getStatus() != CredentialStatus.BLOCKED_TEMPORARY) {
            throw new CredentialNotBlockedException("Credential is not BLOCKED_PERMANENT or BLOCKED_TEMPORARY: " + request.getCredentialName() + ", user ID: " + user.getUserId());
        }
        credential.setFailedAttemptCounterSoft(0);
        credential.setFailedAttemptCounterHard(0);
        credential.setStatus(CredentialStatus.ACTIVE);
        credential.setTimestampBlocked(null);
        user = userIdentityRepository.save(user);
        final UnblockCredentialResponse response = new UnblockCredentialResponse();
        response.setUserId(user.getUserId());
        response.setCredentialName(credential.getCredentialDefinition().getName());
        response.setCredentialStatus(credential.getStatus());
        return response;
    }

    /**
     * Find a credential. This method is not transactional.
     * @param credentialDefinition Credential definition.
     * @param user User identity entity.
     * @return Credential.
     * @throws CredentialNotFoundException Thrown when credential is not found.
     * @throws CredentialNotActiveException Thrown when credential is not active.
     */
    public CredentialEntity findActiveCredential(CredentialDefinitionEntity credentialDefinition, UserIdentityEntity user) throws CredentialNotFoundException, CredentialNotActiveException {
        final CredentialEntity credential = findCredential(credentialDefinition, user);
        if (credential.getStatus() == CredentialStatus.REMOVED) {
            throw new CredentialNotFoundException("Credential is REMOVED: " + credentialDefinition.getName());
        }
        if (credential.getStatus() != CredentialStatus.ACTIVE) {
            throw new CredentialNotActiveException("Credential is not ACTIVE: " + credentialDefinition.getName());
        }
        return credential;
    }

    /**
     * Find a credential without status check. This method is not transactional.
     * @param credentialDefinition Credential definition.
     * @param user User identity entity.
     * @return Credential.
     * @throws CredentialNotFoundException Thrown when credential is not found.
     */
    public CredentialEntity findCredential(CredentialDefinitionEntity credentialDefinition, UserIdentityEntity user) throws CredentialNotFoundException {
        final Optional<CredentialEntity> credentialOptional = user.getCredentials().stream().filter(c -> c.getCredentialDefinition().equals(credentialDefinition)).findFirst();
        if (!credentialOptional.isPresent()) {
            throw new CredentialNotFoundException("Credential not found: " + credentialDefinition.getName());
        }
        return credentialOptional.get();
    }

    /**
     * Create a credential. In case the credential is already defined in the database, reuse the existing record.
     * Method is not transactional.
     *
     * @param user User identity entity.
     * @param credentialDefinition Credential definition entity.
     * @param credentialType Credential type.
     * @param username Username, use null for generated username.
     * @param credentialValue Credential value, use null for generated credential value.
     * @param validationMode Credential validation mode.
     * @throws InvalidConfigurationException Thrown when Next Step configuration is invalid.
     * @throws CredentialValidationFailedException Thrown when credential validation fails.
     * @throws InvalidRequestException Thrown when request is invalid.
     * @throws EncryptionException Thrown when encryption or decryption fails.
     */
    public CredentialSecretDetail createCredential(UserIdentityEntity user, CredentialDefinitionEntity credentialDefinition,
                                                   CredentialType credentialType, String username, String credentialValue,
                                                   CredentialValidationMode validationMode) throws InvalidConfigurationException, CredentialValidationFailedException, InvalidRequestException, EncryptionException {
        // Lookup credential in case it already exists
        final CredentialEntity credential;
        final Optional<CredentialEntity> credentialOptional = user.getCredentials().stream().filter(c -> c.getCredentialDefinition().equals(credentialDefinition)).findFirst();
        final boolean newCredential;
        if (credentialOptional.isPresent()) {
            // TODO - auditing
            credential = credentialOptional.get();
            credential.setTimestampLastUpdated(new Date());
            credential.setTimestampLastCredentialChange(new Date());
            newCredential = false;
        } else {
            credential = new CredentialEntity();
            credential.setCredentialId(idGeneratorService.generateCredentialId());
            credential.setCredentialDefinition(credentialDefinition);
            credential.setTimestampCreated(new Date());
            credential.setUser(user);
            newCredential = true;
        }
        if (username != null) {
            // Username has to be checked for duplicates even when username validation is disabled
            if (validationMode == CredentialValidationMode.NO_VALIDATION || validationMode == CredentialValidationMode.VALIDATE_CREDENTIAL) {
                final Optional<CredentialEntity> existingCredentialOptional = credentialRepository.findByCredentialDefinitionAndUsername(credentialDefinition, username);
                if (existingCredentialOptional.isPresent()) {
                    final CredentialEntity existingCredential = existingCredentialOptional.get();
                    if (!existingCredential.getUser().equals(user)) {
                        final CredentialValidationError error = new CredentialValidationError(CredentialValidationFailedException.CODE,
                                "Username validation failed", Collections.singletonList(CredentialValidationFailure.USERNAME_ALREADY_EXISTS));
                        throw new CredentialValidationFailedException("Username validation failed for user ID: " + user.getUserId(), error);
                    }
                }
            }
        } else {
            final boolean useOriginalUsername = nextStepServerConfiguration.isUseOriginalUsername();
            if (useOriginalUsername && credential.getUsername() != null) {
                username = credential.getUsername();
            } else {
                username = credentialGenerationService.generateUsername(credentialDefinition);
            }
        }
        credential.setType(credentialType);
        updateCredentialExpirationTime(credential, credentialDefinition.getCredentialPolicy());
        credential.setUsername(username);
        final String credentialValueRequest = credentialValue;
        if (credentialValue == null) {
            credentialValue = credentialGenerationService.generateCredentialValue(credentialDefinition);
        }
        final List<CredentialValidationFailure> validationErrors = credentialValidationService.validateCredential(user,
                credentialDefinition, username, credentialValue, validationMode);
        if (!validationErrors.isEmpty()) {
            final CredentialValidationError error = new CredentialValidationError(CredentialValidationFailedException.CODE, "Validation failed", validationErrors);
            throw new CredentialValidationFailedException("Validation failed for user ID: " + user.getUserId(), error);
        }
        final String unprotectedCredentialValue = credentialValue;
        final CredentialValue protectedCredentialValue = credentialProtectionService.protectCredential(credentialValue, credential);
        credential.setValue(protectedCredentialValue.getValue());
        credential.setEncryptionAlgorithm(protectedCredentialValue.getEncryptionAlgorithm());
        credential.setHashingConfig(credentialDefinition.getHashingConfig());
        credential.setTimestampLastCredentialChange(new Date());
        credential.setStatus(CredentialStatus.ACTIVE);
        credential.setTimestampBlocked(null);
        // Counters are reset even in case of an existing credential
        credential.setAttemptCounter(0);
        credential.setFailedAttemptCounterSoft(0);
        credential.setFailedAttemptCounterHard(0);
        if (newCredential) {
            // Credential needs to be added after all validations, otherwise JPA may save the credential prematurely
            user.getCredentials().add(credential);
        }

        // Save credential into credential history
        credentialHistoryService.createCredentialHistory(user, credential, new Date());
        userIdentityRepository.save(user);
        final CredentialSecretDetail credentialDetail = new CredentialSecretDetail();
        credentialDetail.setCredentialName(credential.getCredentialDefinition().getName());
        credentialDetail.setCredentialType(credential.getType());
        credentialDetail.setCredentialStatus(CredentialStatus.ACTIVE);
        credentialDetail.setUsername(credential.getUsername());
        final boolean credentialChangeRequired;
        if (credentialValueRequest == null) {
            // Generated credential value is returned in unprotected form, with possible e2e-encryption
            credentialChangeRequired = isCredentialChangeRequired(credential, unprotectedCredentialValue);
            String credentialValueResponse = unprotectedCredentialValue;
            if (credentialDefinition.isE2eEncryptionEnabled() &&
                    (credential.getType() == CredentialType.PERMANENT || credentialDefinition.isE2eEncryptionForTemporaryCredentialEnabled())) {
                credentialValueResponse = endToEndEncryptionService.encryptCredential(credentialValueResponse, credentialDefinition);
            }
            credentialDetail.setCredentialValue(credentialValueResponse);
        } else {
            credentialChangeRequired = isCredentialChangeRequired(credential, credentialValueRequest);
        }
        credentialDetail.setCredentialChangeRequired(credentialChangeRequired);
        credentialDetail.setTimestampCreated(credential.getTimestampCreated());
        credentialDetail.setTimestampLastUpdated(credential.getTimestampLastUpdated());
        credentialDetail.setTimestampExpires(credential.getTimestampExpires());
        credentialDetail.setTimestampBlocked(credential.getTimestampBlocked());
        credentialDetail.setTimestampLastCredentialChange(credential.getTimestampLastCredentialChange());
        return credentialDetail;
    }

    /**
     * Import credential history record.
     * @param user User identity entity.
     * @param credentialDefinition Credential definition.
     * @param username Username.
     * @param credentialValue Credential value.
     * @throws InvalidConfigurationException Thrown when Next Step configuration is invalid.
     * @throws EncryptionException Thrown when encryption fails.
     */
    public void importCredentialHistory(UserIdentityEntity user, CredentialDefinitionEntity credentialDefinition,
                                        String username, String credentialValue, Date createdDate) throws InvalidConfigurationException, EncryptionException {
        final CredentialEntity credential = new CredentialEntity();
        credential.setUser(user);
        credential.setCredentialDefinition(credentialDefinition);
        credential.setUsername(username);
        final CredentialValue protectedValue = credentialProtectionService.protectCredential(credentialValue, credential);
        credential.setValue(protectedValue.getValue());
        credential.setEncryptionAlgorithm(protectedValue.getEncryptionAlgorithm());
        credential.setHashingConfig(credentialDefinition.getHashingConfig());
        credentialHistoryService.createCredentialHistory(user, credential, createdDate);
    }

    /**
     * Update credential expiration time in case credential type is TEMPORARY or rotation is enabled.
     * @param credential Credential entity.
     * @param credentialPolicy Credential policy entity.
     */
    private void updateCredentialExpirationTime(CredentialEntity credential, CredentialPolicyEntity credentialPolicy) {
        if (credential.getType() == CredentialType.TEMPORARY) {
            final Integer expirationTime = credentialPolicy.getTemporaryCredentialExpirationTime();
            if (expirationTime != null) {
                final Calendar c = Calendar.getInstance();
                c.add(Calendar.SECOND, expirationTime);
                credential.setTimestampExpires(c.getTime());
                return;
            }
        }
        if (credentialPolicy.isRotationEnabled()) {
            final Calendar c = GregorianCalendar.getInstance();
            c.add(Calendar.DAY_OF_YEAR, credentialPolicy.getRotationDays());
            credential.setTimestampExpires(c.getTime());
            return;
        }
        credential.setTimestampExpires(null);
    }

}