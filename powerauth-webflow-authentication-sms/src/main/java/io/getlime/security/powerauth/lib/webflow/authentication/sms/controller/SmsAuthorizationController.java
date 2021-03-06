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
package io.getlime.security.powerauth.lib.webflow.authentication.sms.controller;

import io.getlime.security.powerauth.lib.dataadapter.model.enumeration.AfsAction;
import io.getlime.security.powerauth.lib.dataadapter.model.enumeration.AfsAuthInstrument;
import io.getlime.security.powerauth.lib.dataadapter.model.enumeration.OperationTerminationReason;
import io.getlime.security.powerauth.lib.dataadapter.model.enumeration.PasswordProtectionType;
import io.getlime.security.powerauth.lib.dataadapter.model.response.AfsResponse;
import io.getlime.security.powerauth.lib.dataadapter.model.response.AuthStepOptions;
import io.getlime.security.powerauth.lib.nextstep.client.NextStepClient;
import io.getlime.security.powerauth.lib.nextstep.client.NextStepClientException;
import io.getlime.security.powerauth.lib.nextstep.model.entity.AuthStep;
import io.getlime.security.powerauth.lib.nextstep.model.entity.enumeration.AuthenticationResult;
import io.getlime.security.powerauth.lib.nextstep.model.entity.enumeration.UserAccountStatus;
import io.getlime.security.powerauth.lib.nextstep.model.enumeration.*;
import io.getlime.security.powerauth.lib.nextstep.model.exception.CredentialNotActiveException;
import io.getlime.security.powerauth.lib.nextstep.model.exception.UserNotActiveException;
import io.getlime.security.powerauth.lib.nextstep.model.response.*;
import io.getlime.security.powerauth.lib.webflow.authentication.configuration.WebFlowServicesConfiguration;
import io.getlime.security.powerauth.lib.webflow.authentication.controller.AuthMethodController;
import io.getlime.security.powerauth.lib.webflow.authentication.encryption.AesEncryptionPasswordProtection;
import io.getlime.security.powerauth.lib.webflow.authentication.encryption.NoPasswordProtection;
import io.getlime.security.powerauth.lib.webflow.authentication.exception.*;
import io.getlime.security.powerauth.lib.webflow.authentication.model.AuthOperationResponse;
import io.getlime.security.powerauth.lib.webflow.authentication.model.AuthResultDetail;
import io.getlime.security.powerauth.lib.webflow.authentication.model.AuthorizationOtpDeliveryResult;
import io.getlime.security.powerauth.lib.webflow.authentication.model.HttpSessionAttributeNames;
import io.getlime.security.powerauth.lib.webflow.authentication.model.converter.AuthInstrumentConverter;
import io.getlime.security.powerauth.lib.webflow.authentication.repository.CertificateVerificationRepository;
import io.getlime.security.powerauth.lib.webflow.authentication.repository.model.entity.CertificateVerificationEntity;
import io.getlime.security.powerauth.lib.webflow.authentication.service.AfsIntegrationService;
import io.getlime.security.powerauth.lib.webflow.authentication.sms.model.request.SmsAuthorizationRequest;
import io.getlime.security.powerauth.lib.webflow.authentication.sms.model.response.InitSmsAuthorizationResponse;
import io.getlime.security.powerauth.lib.webflow.authentication.sms.model.response.ResendSmsAuthorizationResponse;
import io.getlime.security.powerauth.lib.webflow.authentication.sms.model.response.SmsAuthorizationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Controller which provides endpoints for SMS authorization.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@RestController
@RequestMapping(value = "/api/auth/sms")
public class SmsAuthorizationController extends AuthMethodController<SmsAuthorizationRequest, SmsAuthorizationResponse, AuthStepException> {

    private static final Logger logger = LoggerFactory.getLogger(SmsAuthorizationController.class);

    private static final Integer OPERATION_CONFIG_TEMPLATE_LOGIN = 2;
    private static final Integer OPERATION_CONFIG_TEMPLATE_APPROVAL = 1;

    private final NextStepClient nextStepClient;
    private final WebFlowServicesConfiguration configuration;
    private final AfsIntegrationService afsIntegrationService;
    private final HttpSession httpSession;
    private final CertificateVerificationRepository certificateVerificationRepository;

    private final AuthInstrumentConverter authInstrumentConverter = new AuthInstrumentConverter();

    /**
     * Controller constructor.
     * @param nextStepClient Next Step client.
     * @param configuration Web Flow configuration.
     * @param afsIntegrationService Anti-fraud system integration service.
     * @param httpSession HTTP session.
     * @param certificateVerificationRepository Certificate verification repository.
     */
    @Autowired
    public SmsAuthorizationController(NextStepClient nextStepClient, WebFlowServicesConfiguration configuration, AfsIntegrationService afsIntegrationService, HttpSession httpSession, CertificateVerificationRepository certificateVerificationRepository) {
        this.nextStepClient = nextStepClient;
        this.configuration = configuration;
        this.afsIntegrationService = afsIntegrationService;
        this.httpSession = httpSession;
        this.certificateVerificationRepository = certificateVerificationRepository;
    }

    /**
     * Verifies the authorization code entered by user against code generated during initialization.
     *
     * @param request Request with authentication object information.
     * @return Authentication result with user ID and organization ID.
     * @throws AuthStepException Exception is thrown when authorization fails.
     */
    @Override
    protected AuthResultDetail authenticate(SmsAuthorizationRequest request) throws AuthStepException {
        final GetOperationDetailResponse operation = getOperation();
        final AuthMethod authMethod = getAuthMethodName(operation);
        logger.info("Step authentication started, operation ID: {}, authentication method: {}", operation.getOperationId(), authMethod);
        try {
            if (operation.getUserId() == null || operation.getAccountStatus() != UserAccountStatus.ACTIVE) {
                // Fake OTP authentication, pretend 2FA authentication failure
                logger.info("Step authentication failed with fake SMS authorization, operation ID: {}, authentication method: {}", operation.getOperationId(), authMethod);
                List<AuthInstrument> authInstruments = new ArrayList<>();
                authInstruments.add(AuthInstrument.OTP_KEY);
                authInstruments.add(AuthInstrument.CREDENTIAL);
                AuthOperationResponse response = failAuthorization(operation.getOperationId(), null, authInstruments, null);
                if (response.getAuthResult() == AuthResult.FAILED) {
                    // FAILED result instead of CONTINUE means the authentication method is failed
                    throw new MaxAttemptsExceededException("Maximum number of authentication attempts exceeded");
                }
                throw new AuthenticationFailedException("Authentication failed", "login.authenticationFailed");
            }
            final String otpId = getOtpIdFromHttpSession();
            GetOrganizationDetailResponse organization = nextStepClient.getOrganizationDetail(operation.getOrganizationId()).getResponseObject();
            String otpName = organization.getDefaultOtpName();
            String credentialName = organization.getDefaultCredentialName();
            if (otpName == null) {
                logger.warn("Default OTP name is not configured for organization: " + operation.getOrganizationId());
                throw new AuthStepException("SMS delivery failed", "error.communication");
            }
            String operationId = operation.getOperationId();
            String userId = operation.getUserId();
            UserAccountStatus accountStatus = operation.getAccountStatus();

            String authCode = request.getAuthCode();
            AuthStepOptions authStepOptions = getAuthStepOptionsFromHttpSession();
            AuthenticationResult smsAuthorizationResult = null;
            Integer remainingAttempts = null;
            String errorMessage = null;
            boolean showRemainingAttempts = false;
            if (authStepOptions != null) {
                // Authentication step options have been derived from AFS response

                if (!authStepOptions.isSmsOtpRequired() && !authStepOptions.isPasswordRequired()) {
                    // No authentication is required, approve step
                    cleanHttpSession();
                    request.setAuthInstruments(Collections.emptyList());
                    logger.info("Step authentication succeeded (NO_FA), operation ID: {}, authentication method: {}", operation.getOperationId(), authMethod);
                    return new AuthResultDetail(operation.getUserId(), operation.getOrganizationId(), false);
                } else if (!authStepOptions.isPasswordRequired()) {
                    // Only SMS authorization is required, skip password verification
                    OtpAuthenticationResponse otpResponse = nextStepClient.authenticateWithOtp(otpId, operationId, authCode, true, authMethod).getResponseObject();
                    if (otpResponse.isOperationFailed()) {
                        logger.info("Step authentication failed (1FA) due to failed operation, operation ID: {}, authentication method: {}", operation.getOperationId(), authMethod);
                        throw new MaxAttemptsExceededException("Maximum number of authentication attempts exceeded");
                    }
                    smsAuthorizationResult = otpResponse.getAuthenticationResult();
                    request.setAuthInstruments(Collections.singletonList(AuthInstrument.OTP_KEY));
                    if (smsAuthorizationResult == AuthenticationResult.SUCCEEDED) {
                        cleanHttpSession();
                        logger.info("Step authentication succeeded (1FA), operation ID: {}, authentication method: {}", operation.getOperationId(), authMethod);
                        return new AuthResultDetail(operation.getUserId(), operation.getOrganizationId(), true);
                    }
                    remainingAttempts = otpResponse.getRemainingAttempts();
                    showRemainingAttempts = otpResponse.isShowRemainingAttempts();
                    errorMessage = otpResponse.getErrorMessage();
                }
            }
            if (smsAuthorizationResult == null) {
                // Otherwise 2FA authentication is performed
                List<AuthInstrument> authInstruments = new ArrayList<>();
                authInstruments.add(AuthInstrument.OTP_KEY);
                authInstruments.add(AuthInstrument.CREDENTIAL);
                request.setAuthInstruments(authInstruments);

                PasswordProtectionType passwordProtectionType = configuration.getPasswordProtection();
                String cipherTransformation = configuration.getCipherTransformation();
                io.getlime.security.powerauth.lib.webflow.authentication.encryption.PasswordProtection passwordProtection;
                switch (passwordProtectionType) {
                    case NO_PROTECTION:
                        // Password is sent in plain text
                        passwordProtection = new NoPasswordProtection();
                        logger.info("No protection is used for protecting user password");
                        break;

                    case PASSWORD_ENCRYPTION_AES:
                        // Encrypt user password in case password encryption is configured in Web Flow
                        passwordProtection = new AesEncryptionPasswordProtection(cipherTransformation, configuration.getPasswordEncryptionKey());
                        logger.info("User password is protected using transformation: {}", cipherTransformation);
                        break;

                    default:
                        // Unsupported authentication type
                        throw new InvalidRequestException("Invalid authentication type");
                }

                String protectedPassword = passwordProtection.protect(request.getPassword());
                CombinedAuthenticationResponse authResponse = nextStepClient.authenticateCombined(credentialName, userId, protectedPassword, otpId, operationId, authCode, true, authMethod).getResponseObject();
                if (authResponse.isOperationFailed()) {
                    logger.info("Step authentication failed (2FA) due to failed operation, operation ID: {}, authentication method: {}", operation.getOperationId(), authMethod);
                    throw new MaxAttemptsExceededException("Maximum number of authentication attempts exceeded");
                }
                if (authResponse.getAuthenticationResult() == AuthenticationResult.SUCCEEDED) {
                    cleanHttpSession();
                    logger.info("Step authentication succeeded (2FA), operation ID: {}, authentication method: {}", operation.getOperationId(), authMethod);
                    return new AuthResultDetail(operation.getUserId(), operation.getOrganizationId(), true);
                }
                remainingAttempts = authResponse.getRemainingAttempts();
                showRemainingAttempts = authResponse.isShowRemainingAttempts();
                errorMessage = authResponse.getErrorMessage();
            }

            if (errorMessage == null) {
                errorMessage = "login.authenticationFailed";
            }

            if (remainingAttempts != null && remainingAttempts == 0) {
                cleanHttpSession();
                throw new MaxAttemptsExceededException("Maximum number of authentication attempts exceeded");
            }
            AuthenticationFailedException authEx = new AuthenticationFailedException("Authentication failed", errorMessage);
            if (showRemainingAttempts) {
                authEx.setRemainingAttempts(remainingAttempts);
            }
            authEx.setAccountStatus(accountStatus);
            throw authEx;
        } catch (NextStepClientException ex) {
            logger.error("Error occurred in Next Step server", ex);
            throw new AuthStepException("SMS authentication failed", ex, "error.communication");
        }
    }

    /**
     * Get current authentication method.
     * @return Current authentication method.
     */
    @Override
    protected AuthMethod getAuthMethodName() {
        return AuthMethod.SMS_KEY;
    }

    /**
     * Set OTP ID in HTTP session.
     * @param otpId OTP ID.
     */
    private void updateOtpIdInHttpSession(String otpId) {
        synchronized (httpSession.getServletContext()) {
            httpSession.setAttribute(HttpSessionAttributeNames.OTP_ID, otpId);
        }
    }

    /**
     * Set last message timestamp in HTTP session.
     */
    private void updateLastMessageTimestampInHttpSession(Long timestamp) {
        synchronized (httpSession.getServletContext()) {
            httpSession.setAttribute(HttpSessionAttributeNames.LAST_MESSAGE_TIMESTAMP, timestamp);
        }
    }

    /**
     * Set initial message sent flag in HTTP session.
     */
    private void updateInitialMessageSentInHttpSession(Boolean initialMessageSent) {
        synchronized (httpSession.getServletContext()) {
            httpSession.setAttribute(HttpSessionAttributeNames.INITIAL_MESSAGE_SENT, initialMessageSent);
        }
    }

    /**
     * Set authentication step options in HTTP session.
     */
    private void updateAuthStepOptionsInHttpSession(AuthStepOptions authStepOptions) {
        synchronized (httpSession.getServletContext()) {
            httpSession.setAttribute(HttpSessionAttributeNames.AUTH_STEP_OPTIONS, authStepOptions);
        }
    }

    /**
     * Get OTP ID from HTTP session.
     */
    private String getOtpIdFromHttpSession() {
        synchronized (httpSession.getServletContext()) {
            return (String) httpSession.getAttribute(HttpSessionAttributeNames.OTP_ID);
        }
    }

    /**
     * Get username from HTTP session.
     */
    private String getUsernameFromHttpSession() {
        synchronized (httpSession.getServletContext()) {
            return (String) httpSession.getAttribute(HttpSessionAttributeNames.USERNAME);
        }
    }

    /**
     * Get last message timestamp from HTTP session.
     */
    private Long getLastMessageTimestampFromHttpSession() {
        synchronized (httpSession.getServletContext()) {
            return (Long) httpSession.getAttribute(HttpSessionAttributeNames.LAST_MESSAGE_TIMESTAMP);
        }
    }

    /**
     * Get initial message sent flag from HTTP session.
     */
    private Boolean getInitialMessageSentFromHttpSession() {
        synchronized (httpSession.getServletContext()) {
            return (Boolean) httpSession.getAttribute(HttpSessionAttributeNames.INITIAL_MESSAGE_SENT);
        }
    }

    /**
     * Get authentication step options from HTTP session.
     */
    private AuthStepOptions getAuthStepOptionsFromHttpSession() {
        synchronized (httpSession.getServletContext()) {
            return (AuthStepOptions) httpSession.getAttribute(HttpSessionAttributeNames.AUTH_STEP_OPTIONS);
        }
    }

    /**
     * Clean HTTP session.
     */
    private void cleanHttpSession() {
        synchronized (httpSession.getServletContext()) {
            httpSession.removeAttribute(HttpSessionAttributeNames.OTP_ID);
            httpSession.removeAttribute(HttpSessionAttributeNames.LAST_MESSAGE_TIMESTAMP);
            httpSession.removeAttribute(HttpSessionAttributeNames.INITIAL_MESSAGE_SENT);
            httpSession.removeAttribute(HttpSessionAttributeNames.AUTH_STEP_OPTIONS);
            httpSession.removeAttribute(HttpSessionAttributeNames.USERNAME);
        }
    }

    /**
     * Initializes the SMS authorization process by creating authorization SMS using Data Adapter.
     *
     * @return Authorization response.
     * @throws AuthStepException Thrown when operation is invalid or not available.
     */
    @RequestMapping(value = "/init", method = RequestMethod.POST)
    public InitSmsAuthorizationResponse initSmsAuthorization() throws AuthStepException {
        final GetOperationDetailResponse operation = getOperation();
        final AuthMethod authMethod = getAuthMethodName(operation);
        logger.info("Init step started, operation ID: {}, authentication method: {}", operation.getOperationId(), authMethod);
        InitSmsAuthorizationResponse initResponse = new InitSmsAuthorizationResponse();

        // By default enable both SMS authorization and password verification (2FA)
        initResponse.setSmsOtpEnabled(true);
        initResponse.setPasswordEnabled(true);

        String username = null;
        if (authMethod == AuthMethod.LOGIN_SCA) {
            // Add username for LOGIN_SCA method
            username = getUsernameFromHttpSession();
            initResponse.setUsername(username);
        }

        if (operation.getUserId() == null || operation.getAccountStatus() != UserAccountStatus.ACTIVE) {
            // Operation is anonymous or user account is blocked, perform fake SMS authorization, return default response
            initResponse.setResendDelay(configuration.getSmsResendDelay());
            initResponse.setResult(AuthStepResult.CONFIRMED);
            return initResponse;
        }

        if (isCertificateUsedForAuthentication(operation.getOperationId())) {

            // Currently AFS integration and client certificate authentication are exclusive
            logger.debug("Disabling password verification due to client TLS certificate usage in INIT step of authentication method: {}, operation ID: {}", authMethod, operation.getOperationId());
            AuthStepOptions authStepOptions = new AuthStepOptions();
            authStepOptions.setPasswordRequired(false);
            authStepOptions.setSmsOtpRequired(true);
            updateAuthStepOptionsInHttpSession(authStepOptions);
            initResponse.setSmsOtpEnabled(true);
            initResponse.setPasswordEnabled(false);

        } else if (configuration.isAfsEnabled()) {

            AfsAction afsAction = determineAfsActionInit(authMethod, operation.getOperationName());

            if (afsAction != null) {
                // Execute an AFS action
                AfsResponse afsResponse = afsIntegrationService.executeInitAction(operation.getOperationId(), username, afsAction);

                // Save authentication step options derived from AFS response for authenticate step
                updateAuthStepOptionsInHttpSession(afsResponse.getAuthStepOptions());

                // Process AFS response
                if (afsResponse.isAfsResponseApplied()) {
                    if (afsResponse.getAuthStepOptions() != null) {
                        if (!afsResponse.getAuthStepOptions().isPasswordRequired()) {
                            logger.debug("Disabling password verification based on AFS response in INIT step of authentication method: {}, operation ID: {}", authMethod, operation.getOperationId());
                            // Step-down for password verification
                            initResponse.setPasswordEnabled(false);
                        }
                        if (!afsResponse.getAuthStepOptions().isSmsOtpRequired()) {
                            logger.debug("Disabling SMS authorization due based on AFS response in INIT step of authentication method: {}, operation ID: {}", authMethod, operation.getOperationId());
                            // Step-down for SMS authorization
                            initResponse.setSmsOtpEnabled(false);
                        }
                    }
                }
            }
        }

        try {
            if (initResponse.isSmsOtpEnabled()) {
                initResponse.setResendDelay(configuration.getSmsResendDelay());
                Boolean initialMessageSent = getInitialMessageSentFromHttpSession();
                if (initialMessageSent != null && initialMessageSent) {
                    initResponse.setResult(AuthStepResult.CONFIRMED);
                    return initResponse;
                }
                AuthorizationOtpDeliveryResult result = sendAuthorizationSms(operation);
                if (result.isDelivered()) {
                    updateOtpIdInHttpSession(result.getOtpId());
                    updateLastMessageTimestampInHttpSession(System.currentTimeMillis());
                    updateInitialMessageSentInHttpSession(true);
                    initResponse.setResult(AuthStepResult.CONFIRMED);
                    logger.info("Init step result: CONFIRMED, operation ID: {}, authentication method: {}", operation.getOperationId(), authMethod);
                } else {
                    initResponse.setResult(AuthStepResult.AUTH_FAILED);
                    initResponse.setMessage(result.getErrorMessage());
                    logger.info("Init step result: AUTH_FAILED, operation ID: {}, authentication method: {}", operation.getOperationId(), authMethod);
                }
            }

            return initResponse;
        } catch (NextStepClientException ex) {
            logger.error("Error when sending SMS message.", ex);
            initResponse.setResult(AuthStepResult.AUTH_FAILED);
            logger.info("Init step result: AUTH_FAILED, operation ID: {}, authentication method: {}", operation.getOperationId(), authMethod);
            initResponse.setMessage("smsAuthorization.deliveryFailed");
            return initResponse;
        }
    }

    /**
     * Resend the SMS using Data Adapter.
     *
     * @return Authorization response.
     * @throws AuthStepException Thrown when operation is invalid or not available.
     */
    @RequestMapping(value = "/resend", method = RequestMethod.POST)
    public ResendSmsAuthorizationResponse resendSmsAuthorization() throws AuthStepException {
        final GetOperationDetailResponse operation = getOperation();
        final AuthMethod authMethod = getAuthMethodName(operation);
        logger.info("Resend step started, operation ID: {}, authentication method: {}", operation.getOperationId(), authMethod);
        ResendSmsAuthorizationResponse resendResponse = new ResendSmsAuthorizationResponse();
        resendResponse.setResendDelay(configuration.getSmsResendDelay());
        if (operation.getUserId() == null) {
            // Operation is anonymous, e.g. for fake SMS authorization, return default response
            logger.info("Resend step result: CONFIRMED (fake SMS), operation ID: {}, authentication method: {}", operation.getOperationId(), authMethod);
            resendResponse.setResult(AuthStepResult.CONFIRMED);
            return resendResponse;
        }
        try {
            AuthorizationOtpDeliveryResult response = sendAuthorizationSms(operation);
            if (response.isDelivered()) {
                updateOtpIdInHttpSession(response.getOtpId());
                updateLastMessageTimestampInHttpSession(System.currentTimeMillis());
                resendResponse.setResult(AuthStepResult.CONFIRMED);
                logger.info("Resend step result: CONFIRMED, operation ID: {}, authentication method: {}", operation.getOperationId(), authMethod);
            } else {
                resendResponse.setResult(AuthStepResult.AUTH_FAILED);
                resendResponse.setMessage(response.getErrorMessage());
                logger.info("Resend step result: AUTH_FAILED, operation ID: {}, authentication method: {}", operation.getOperationId(), authMethod);
            }
            return resendResponse;
        } catch (NextStepClientException ex) {
            logger.error("Error when sending SMS message.", ex);
            resendResponse.setResult(AuthStepResult.AUTH_FAILED);
            logger.info("Resend step result: AUTH_FAILED, operation ID: {}, authentication method: {}", operation.getOperationId(), authMethod);
            resendResponse.setMessage("smsAuthorization.deliveryFailed");
            return resendResponse;
        }
    }

    /**
     * Performs the authorization and resolves the next step.
     *
     * @param request Authorization request which includes the authorization code.
     * @return Authorization response.
     * @throws AuthStepException In case authentication fails.
     */
    @RequestMapping(value = "/authenticate", method = RequestMethod.POST)
    public SmsAuthorizationResponse authenticateHandler(@Valid @RequestBody SmsAuthorizationRequest request) throws AuthStepException {
        final GetOperationDetailResponse operation = getOperation();
        final AuthMethod authMethod = getAuthMethodName(operation);
        // Extract username for LOGIN_SCA
        final String username;
        if (authMethod == AuthMethod.LOGIN_SCA) {
            username = getUsernameFromHttpSession();
        } else {
            // In other methods user ID is already available
            username = null;
        }
        final AfsAction afsAction;
        if (configuration.isAfsEnabled()) {
            afsAction = determineAfsActionAuth(authMethod, operation.getOperationName());
        } else {
            afsAction = null;
        }

        try {
            return buildAuthorizationResponse(request, new AuthResponseProvider() {

                final List<AfsAuthInstrument> authInstruments = authInstrumentConverter.fromAuthInstruments(request.getAuthInstruments());

                @Override
                public SmsAuthorizationResponse doneAuthentication(String userId) {
                    if (afsAction != null) {
                        afsIntegrationService.executeAuthAction(operation.getOperationId(), afsAction, username, authInstruments,  AuthStepResult.CONFIRMED);
                    }
                    authenticateCurrentBrowserSession();
                    final SmsAuthorizationResponse response = new SmsAuthorizationResponse();
                    response.setResult(AuthStepResult.CONFIRMED);
                    response.setMessage("authentication.success");
                    logger.info("Step result: CONFIRMED, authentication method: {}", authMethod);
                    return response;
                }

                @Override
                public SmsAuthorizationResponse failedAuthentication(String userId, String failedReason) {
                    clearCurrentBrowserSession();
                    final SmsAuthorizationResponse response = new SmsAuthorizationResponse();
                    response.setResult(AuthStepResult.AUTH_FAILED);
                    response.setMessage(failedReason);
                    logger.info("Step result: AUTH_FAILED, authentication method: {}", authMethod);
                    return response;
                }

                @Override
                public SmsAuthorizationResponse continueAuthentication(String operationId, String userId, List<AuthStep> steps) {
                    if (afsAction != null) {
                        afsIntegrationService.executeAuthAction(operation.getOperationId(), afsAction, username, authInstruments, AuthStepResult.CONFIRMED);
                    }
                    final SmsAuthorizationResponse response = new SmsAuthorizationResponse();
                    response.setResult(AuthStepResult.CONFIRMED);
                    response.setMessage("authentication.success");
                    response.getNext().addAll(steps);
                    logger.info("Step result: CONFIRMED, operation ID: {}, authentication method: {}", operationId, authMethod);
                    return response;
                }
            });
        } catch (AuthStepException e) {
            logger.warn("Error occurred while verifying authorization code from SMS message: {}", e.getMessage());
            if (afsAction != null) {
                final List<AfsAuthInstrument> authInstruments = authInstrumentConverter.fromAuthInstruments(request.getAuthInstruments());
                if (e instanceof AuthenticationFailedException) {
                    AuthenticationFailedException authEx = (AuthenticationFailedException) e;
                    if (authEx.getAccountStatus() != UserAccountStatus.ACTIVE) {
                        // notify AFS about failed authentication method due to the fact that user account is not active
                        afsIntegrationService.executeAuthAction(operation.getOperationId(), afsAction, username, authInstruments, AuthStepResult.AUTH_METHOD_FAILED);
                    } else {
                        // notify AFS about failed authentication
                        afsIntegrationService.executeAuthAction(operation.getOperationId(), afsAction, username, authInstruments, AuthStepResult.AUTH_FAILED);
                    }
                } else if (e instanceof MaxAttemptsExceededException) {
                    // notify AFS about failed authentication method due to last attempt
                    afsIntegrationService.executeAuthAction(operation.getOperationId(), afsAction, username, authInstruments, AuthStepResult.AUTH_METHOD_FAILED);
                    // notify AFS about logout
                    afsIntegrationService.executeLogoutAction(operation.getOperationId(), OperationTerminationReason.FAILED);
                }
            }
            final SmsAuthorizationResponse response = new SmsAuthorizationResponse();
            response.setResult(AuthStepResult.AUTH_FAILED);
            logger.info("Step result: AUTH_FAILED, authentication method: {}", authMethod);
            if (e.getMessageId() != null) {
                // prefer localized message over regular message string
                response.setMessage(e.getMessageId());
            } else {
                response.setMessage(e.getMessage());
            }
            response.setRemainingAttempts(e.getRemainingAttempts());
            return response;
        }

    }

    /**
     * Cancels the SMS authorization.
     *
     * @return Authorization response.
     * @throws AuthStepException Thrown when operation is invalid or not available.
     */
    @RequestMapping(value = "/cancel", method = RequestMethod.POST)
    public SmsAuthorizationResponse cancelAuthentication() throws AuthStepException {
        try {
            final GetOperationDetailResponse operation = getOperation();
            final AuthMethod authMethod = getAuthMethodName(operation);
            cleanHttpSession();
            cancelAuthorization(operation.getOperationId(), operation.getUserId(), OperationCancelReason.UNKNOWN, null, true);
            final SmsAuthorizationResponse cancelResponse = new SmsAuthorizationResponse();
            cancelResponse.setResult(AuthStepResult.CANCELED);
            cancelResponse.setMessage("operation.canceled");
            logger.info("Step result: CANCELED, operation ID: {}, authentication method: {}", operation.getOperationId(), authMethod);
            return cancelResponse;
        } catch (CommunicationFailedException ex) {
            final SmsAuthorizationResponse cancelResponse = new SmsAuthorizationResponse();
            cancelResponse.setResult(AuthStepResult.AUTH_FAILED);
            cancelResponse.setMessage("error.communication");
            cleanHttpSession();
            logger.info("Step result: AUTH_FAILED, authentication method: {}", getAuthMethodName().toString());
            return cancelResponse;
        }
    }

    /**
     * Send authorization SMS using data adapter. Check SMS resend delay to protect backends from spamming.
     * @param operation Current operation.
     * @return OTP ID for the message.
     * @throws AuthStepException In case OTP configuration is invalid.
     * @throws NextStepClientException In case SMS delivery fails.
     */
    private AuthorizationOtpDeliveryResult sendAuthorizationSms(GetOperationDetailResponse operation) throws NextStepClientException, AuthStepException {
        Long lastMessageTimestamp = getLastMessageTimestampFromHttpSession();
        if (lastMessageTimestamp != null && System.currentTimeMillis() - lastMessageTimestamp < configuration.getSmsResendDelay()) {
            // SMS delivery is not allowed
            AuthorizationOtpDeliveryResult result = new AuthorizationOtpDeliveryResult();
            result.setDelivered(false);
            result.setErrorMessage("smsAuthorization.deliveryFailed");
            return result;
        }
        GetOrganizationDetailResponse organization = nextStepClient.getOrganizationDetail(operation.getOrganizationId()).getResponseObject();
        String otpName = organization.getDefaultOtpName();
        String credentialName = organization.getDefaultCredentialName();
        if (otpName == null) {
            logger.warn("Default OTP name is not configured for organization: " + operation.getOrganizationId());
            throw new AuthStepException("SMS delivery failed", "error.communication");
        }
        String userId = operation.getUserId();
        // OTP data is taken from operation
        try {
            String language = LocaleContextHolder.getLocale().getLanguage();
            CreateAndSendOtpResponse otpResponse = nextStepClient.createAndSendOtp(userId, otpName, credentialName, null, operation.getOperationId(), language).getResponseObject();
            AuthorizationOtpDeliveryResult result = new AuthorizationOtpDeliveryResult();
            result.setDelivered(otpResponse.isDelivered());
            result.setOtpId(otpResponse.getOtpId());
            result.setErrorMessage(otpResponse.getErrorMessage());
            return result;
        } catch (NextStepClientException ex) {
            if (ex.getNextStepError() != null
                    && (CredentialNotActiveException.CODE.equals(ex.getNextStepError().getCode())
                        || UserNotActiveException.CODE.equals(ex.getNextStepError().getCode()))) {
                AuthorizationOtpDeliveryResult result = new AuthorizationOtpDeliveryResult();
                result.setDelivered(false);
                result.setErrorMessage("smsAuthorization.deliveryFailed");
                return result;
            }
            throw ex;
        }
    }

    /**
     * Determine AFS action during initialization.
     * @param authMethod Current authentication method.
     * @param operationName Operation name.
     * @return AFS action.
     * @throws AuthStepException In case of any failure.
     */
    private AfsAction determineAfsActionInit(AuthMethod authMethod, String operationName) throws AuthStepException {
        AfsAction afsAction;
        switch (authMethod) {
            case LOGIN_SCA:
                afsAction = AfsAction.LOGIN_INIT;
                break;
            case APPROVAL_SCA:
                afsAction = AfsAction.APPROVAL_INIT;
                break;
            case SMS_KEY:
                GetOperationConfigDetailResponse config = getOperationConfig(operationName);
                if (config == null) {
                    throw new OperationNotConfiguredException("Operation not configured, operation name: " + operationName);
                }
                if (OPERATION_CONFIG_TEMPLATE_LOGIN.equals(config.getTemplateId())) {
                    afsAction = AfsAction.LOGIN_INIT;
                } else if (OPERATION_CONFIG_TEMPLATE_APPROVAL.equals(config.getTemplateId())) {
                    afsAction = AfsAction.APPROVAL_INIT;
                } else {
                    // Unknown template, do not execute AFS action
                    afsAction = null;
                }
                break;
            default:
                afsAction = null;
        }
        return afsAction;
    }

    /**
     * Determine AFS action during authentication.
     * @param authMethod Current authentication method.
     * @param operationName Operation name.
     * @return AFS action.
     * @throws AuthStepException In case of any failure.
     */
    private AfsAction determineAfsActionAuth(AuthMethod authMethod, String operationName) throws AuthStepException {
        AfsAction afsAction;
        switch (authMethod) {
            case LOGIN_SCA:
                afsAction = AfsAction.LOGIN_AUTH;
                break;
            case APPROVAL_SCA:
                afsAction = AfsAction.APPROVAL_AUTH;
                break;
            case SMS_KEY:
                GetOperationConfigDetailResponse config = getOperationConfig(operationName);
                if (config == null) {
                    throw new OperationNotConfiguredException("Operation not configured, operation name: " + operationName);
                }
                if (OPERATION_CONFIG_TEMPLATE_LOGIN.equals(config.getTemplateId())) {
                    afsAction = AfsAction.LOGIN_AUTH;
                } else if (OPERATION_CONFIG_TEMPLATE_APPROVAL.equals(config.getTemplateId())) {
                    afsAction = AfsAction.APPROVAL_AUTH;
                } else {
                    // Unknown template, do not execute AFS action
                    afsAction = null;
                }
                break;
            default:
                afsAction = null;
        }
        return afsAction;
    }

    /**
     * Whether client TLS certificate is used for authentication.
     * @param operationId Operation ID.
     * @return Whether client TLS certificate is used for authentication.
     */
    private boolean isCertificateUsedForAuthentication(String operationId) {
        CertificateVerificationEntity.CertificateVerificationKey key = new CertificateVerificationEntity.CertificateVerificationKey(operationId, AuthMethod.APPROVAL_SCA);
        return certificateVerificationRepository.findByCertificateVerificationKey(key).isPresent();
    }

}
