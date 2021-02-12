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

package io.getlime.security.powerauth.app.nextstep.exception;

import io.getlime.core.rest.model.base.entity.Error;
import io.getlime.core.rest.model.base.response.ErrorResponse;
import io.getlime.security.powerauth.lib.nextstep.model.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Controller advice responsible for default exception resolving.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
@ControllerAdvice
public class DefaultExceptionResolver {

    private static final Logger logger = LoggerFactory.getLogger(DefaultExceptionResolver.class);

    /**
     * Default exception handler, for unexpected errors.
     * @param t Throwable.
     * @return Response with error details.
     */
    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public @ResponseBody ErrorResponse handleDefaultException(Throwable t) {
        logger.error("Error occurred in Next Step server", t);
        Error error = new Error(Error.Code.ERROR_GENERIC, "error.unknown");
        return new ErrorResponse(error);
    }

    /**
     * Exception handler for operation already finished error.
     * @param ex Exception.
     * @return Response with error details.
     */
    @ExceptionHandler(OperationAlreadyFinishedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody ErrorResponse handleOperationAlreadyFinishedException(OperationAlreadyFinishedException ex) {
        logger.warn("Error occurred in Next Step server: {}", ex.getMessage());
        Error error = new Error(OperationAlreadyFinishedException.CODE, "Operation is already in DONE state.");
        return new ErrorResponse(error);
    }

    /**
     * Exception handler for operation already failed error.
     * @param ex Exception.
     * @return Response with error details.
     */
    @ExceptionHandler(OperationAlreadyFailedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody ErrorResponse handleOperationAlreadyFailedException(OperationAlreadyFailedException ex) {
        logger.warn("Error occurred in Next Step server: {}", ex.getMessage());
        Error error = new Error(OperationAlreadyFailedException.CODE, "Operation is already in FAILED state.");
        return new ErrorResponse(error);
    }

    /**
     * Exception handler for operation already canceled error.
     * @param ex Exception.
     * @return Response with error details.
     */
    @ExceptionHandler(OperationAlreadyCanceledException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody ErrorResponse handleOperationCanceledException(OperationAlreadyCanceledException ex) {
        logger.warn("Error occurred in Next Step server: {}", ex.getMessage());
        Error error = new Error(OperationAlreadyCanceledException.CODE, "Operation update attempted for CANCELED operation.");
        return new ErrorResponse(error);
    }

    /**
     * Exception handler for operation not found error.
     * @param ex Exception.
     * @return Response with error details.
     */
    @ExceptionHandler(OperationNotFoundException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody ErrorResponse handleOperationNotFoundException(OperationNotFoundException ex) {
        logger.warn("Error occurred in Next Step server: {}", ex.getMessage());
        Error error = new Error(OperationNotFoundException.CODE, "Operation not found.");
        return new ErrorResponse(error);
    }

    /**
     * Exception handler for operation not configured error.
     * @param ex Exception.
     * @return Response with error details.
     */
    @ExceptionHandler(OperationNotConfiguredException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody ErrorResponse handleOperationNotConfiguredException(OperationNotConfiguredException ex) {
        logger.warn("Error occurred in Next Step server: {}", ex.getMessage());
        Error error = new Error(OperationNotFoundException.CODE, "Operation is not configured.");
        return new ErrorResponse(error);
    }

    /**
     * Exception handler for organization not found error.
     * @param ex Exception.
     * @return Response with error details.
     */
    @ExceptionHandler(OrganizationNotFoundException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody ErrorResponse handleOrganizationNotFoundException(OrganizationNotFoundException ex) {
        logger.warn("Error occurred in Next Step server: {}", ex.getMessage());
        Error error = new Error(OrganizationNotFoundException.CODE, "Organization not found.");
        return new ErrorResponse(error);
    }

    /**
     * Exception handler for invalid operation data error.
     * @param ex Exception.
     * @return Response with error details.
     */
    @ExceptionHandler(InvalidOperationDataException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody ErrorResponse handleInvalidOperationDataException(InvalidOperationDataException ex) {
        logger.warn("Error occurred in Next Step server: {}", ex.getMessage());
        Error error = new Error(InvalidOperationDataException.CODE, "Operation contains invalid data.");
        return new ErrorResponse(error);
    }

    /**
     * Exception handler for invalid request error.
     * @param ex Exception.
     * @return Response with error details.
     */
    @ExceptionHandler(InvalidRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody ErrorResponse handleInvalidRequestException(InvalidRequestException ex) {
        logger.warn("Error occurred in Next Step server: {}", ex.getMessage());
        Error error = new Error(InvalidRequestException.CODE, "Request data is invalid.");
        return new ErrorResponse(error);
    }

    /**
     * Exception handler for invalid configuration error.
     * @param ex Exception.
     * @return Response with error details.
     */
    @ExceptionHandler(InvalidConfigurationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody ErrorResponse handleInvalidConfigurationException(InvalidConfigurationException ex) {
        logger.warn("Error occurred in Next Step server: {}", ex.getMessage());
        Error error = new Error(InvalidConfigurationException.CODE, "Next Step configuration is invalid.");
        return new ErrorResponse(error);
    }

    /**
     * Exception handler for application not found error.
     * @param ex Exception.
     * @return Response with error details.
     */
    @ExceptionHandler(ApplicationNotFoundException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody ErrorResponse handleApplicationNotFoundException(ApplicationNotFoundException ex) {
        logger.warn("Error occurred in Next Step server: {}", ex.getMessage());
        Error error = new Error(ApplicationNotFoundException.CODE, "Application not found.");
        return new ErrorResponse(error);
    }

    /**
     * Exception handler for application already exists error.
     * @param ex Exception.
     * @return Response with error details.
     */
    @ExceptionHandler(ApplicationAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody ErrorResponse handleApplicationAlreadyExistsException(ApplicationAlreadyExistsException ex) {
        logger.warn("Error occurred in Next Step server: {}", ex.getMessage());
        Error error = new Error(ApplicationAlreadyExistsException.CODE, "Application already exists.");
        return new ErrorResponse(error);
    }

    /**
     * Exception handler for hashing configuration already exists error.
     * @param ex Exception.
     * @return Response with error details.
     */
    @ExceptionHandler(HashingConfigAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody ErrorResponse handleHashingConfigAlreadyExistsException(HashingConfigAlreadyExistsException ex) {
        logger.warn("Error occurred in Next Step server: {}", ex.getMessage());
        Error error = new Error(HashingConfigAlreadyExistsException.CODE, "Hashing configuration already exists.");
        return new ErrorResponse(error);
    }

    /**
     * Exception handler for hashing configuration is not found error.
     * @param ex Exception.
     * @return Response with error details.
     */
    @ExceptionHandler(HashingConfigNotFoundException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody ErrorResponse handleHashingConfigNotFoundException(HashingConfigNotFoundException ex) {
        logger.warn("Error occurred in Next Step server: {}", ex.getMessage());
        Error error = new Error(HashingConfigNotFoundException.CODE, "Hashing configuration not found.");
        return new ErrorResponse(error);
    }

    /**
     * Exception handler for role already exists error.
     * @param ex Exception.
     * @return Response with error details.
     */
    @ExceptionHandler(RoleAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody ErrorResponse handleRoleAlreadyExistsException(RoleAlreadyExistsException ex) {
        logger.warn("Error occurred in Next Step server: {}", ex.getMessage());
        Error error = new Error(RoleAlreadyExistsException.CODE, "Role already exists.");
        return new ErrorResponse(error);
    }

    /**
     * Exception handler for role is not found error.
     * @param ex Exception.
     * @return Response with error details.
     */
    @ExceptionHandler(RoleNotFoundException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody ErrorResponse handleRoleNotFoundException(RoleNotFoundException ex) {
        logger.warn("Error occurred in Next Step server: {}", ex.getMessage());
        Error error = new Error(RoleNotFoundException.CODE, "Role not found.");
        return new ErrorResponse(error);
    }

    /**
     * Exception handler for credential policy already exists error.
     * @param ex Exception.
     * @return Response with error details.
     */
    @ExceptionHandler(CredentialPolicyAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody ErrorResponse handleCredentialPolicyAlreadyExistsException(CredentialPolicyAlreadyExistsException ex) {
        logger.warn("Error occurred in Next Step server: {}", ex.getMessage());
        Error error = new Error(CredentialPolicyAlreadyExistsException.CODE, "Credential policy already exists.");
        return new ErrorResponse(error);
    }

    /**
     * Exception handler for credential policy is not found error.
     * @param ex Exception.
     * @return Response with error details.
     */
    @ExceptionHandler(CredentialPolicyNotFoundException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody ErrorResponse handleCredentialPolicyNotFoundException(CredentialPolicyNotFoundException ex) {
        logger.warn("Error occurred in Next Step server: {}", ex.getMessage());
        Error error = new Error(CredentialPolicyNotFoundException.CODE, "Credential policy not found.");
        return new ErrorResponse(error);
    }

    /**
     * Exception handler for one time password policy already exists error.
     * @param ex Exception.
     * @return Response with error details.
     */
    @ExceptionHandler(OtpPolicyAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody ErrorResponse handleOtpPolicyAlreadyExistsException(OtpPolicyAlreadyExistsException ex) {
        logger.warn("Error occurred in Next Step server: {}", ex.getMessage());
        Error error = new Error(OtpPolicyAlreadyExistsException.CODE, "One time password policy already exists.");
        return new ErrorResponse(error);
    }

    /**
     * Exception handler for one time password policy is not found error.
     * @param ex Exception.
     * @return Response with error details.
     */
    @ExceptionHandler(OtpPolicyNotFoundException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody ErrorResponse handleOtpPolicyNotFoundException(OtpPolicyNotFoundException ex) {
        logger.warn("Error occurred in Next Step server: {}", ex.getMessage());
        Error error = new Error(OtpPolicyNotFoundException.CODE, "One time password policy not found.");
        return new ErrorResponse(error);
    }

    /**
     * Exception handler for credential definition already exists error.
     * @param ex Exception.
     * @return Response with error details.
     */
    @ExceptionHandler(CredentialDefinitionAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody ErrorResponse handleCredentialDefinitionAlreadyExistsException(CredentialDefinitionAlreadyExistsException ex) {
        logger.warn("Error occurred in Next Step server: {}", ex.getMessage());
        Error error = new Error(CredentialDefinitionAlreadyExistsException.CODE, "Credential definition already exists.");
        return new ErrorResponse(error);
    }

    /**
     * Exception handler for credential definition is not found error.
     * @param ex Exception.
     * @return Response with error details.
     */
    @ExceptionHandler(CredentialDefinitionNotFoundException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody ErrorResponse handleCredentialDefinitionNotFoundException(CredentialDefinitionNotFoundException ex) {
        logger.warn("Error occurred in Next Step server: {}", ex.getMessage());
        Error error = new Error(CredentialDefinitionNotFoundException.CODE, "Credential definition not found.");
        return new ErrorResponse(error);
    }

    /**
     * Exception handler for one time password definition already exists error.
     * @param ex Exception.
     * @return Response with error details.
     */
    @ExceptionHandler(OtpDefinitionAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody ErrorResponse handleOtpDefinitionAlreadyExistsException(OtpDefinitionAlreadyExistsException ex) {
        logger.warn("Error occurred in Next Step server: {}", ex.getMessage());
        Error error = new Error(OtpDefinitionAlreadyExistsException.CODE, "One time password definition already exists.");
        return new ErrorResponse(error);
    }

    /**
     * Exception handler for one time password definition is not found error.
     * @param ex Exception.
     * @return Response with error details.
     */
    @ExceptionHandler(OtpDefinitionNotFoundException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody ErrorResponse handleOtpDefinitionNotFoundException(OtpDefinitionNotFoundException ex) {
        logger.warn("Error occurred in Next Step server: {}", ex.getMessage());
        Error error = new Error(OtpDefinitionNotFoundException.CODE, "One time password definition not found.");
        return new ErrorResponse(error);
    }

    /**
     * Exception handler for user identity already exists error.
     * @param ex Exception.
     * @return Response with error details.
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody ErrorResponse handleUserAlreadyExistsException(UserAlreadyExistsException ex) {
        logger.warn("Error occurred in Next Step server: {}", ex.getMessage());
        Error error = new Error(UserAlreadyExistsException.CODE, "User identity already exists.");
        return new ErrorResponse(error);
    }

    /**
     * Exception handler for user identity is not found error.
     * @param ex Exception.
     * @return Response with error details.
     */
    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody ErrorResponse handleUserNotFoundException(UserNotFoundException ex) {
        logger.warn("Error occurred in Next Step server: {}", ex.getMessage());
        Error error = new Error(UserNotFoundException.CODE, "User identity not found.");
        return new ErrorResponse(error);
    }

}
