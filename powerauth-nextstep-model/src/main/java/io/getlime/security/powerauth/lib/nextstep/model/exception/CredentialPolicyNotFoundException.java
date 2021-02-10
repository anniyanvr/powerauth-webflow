package io.getlime.security.powerauth.lib.nextstep.model.exception;

import io.getlime.core.rest.model.base.entity.Error;

/**
 * Exception for case when credential policy is not found.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
public class CredentialPolicyNotFoundException extends NextStepServiceException {

    public static final String CODE = "CREDENTIAL_POLICY_NOT_FOUND";

    private Error error;

    /**
     * Constructor with error message.
     * @param message Error message.
     */
    public CredentialPolicyNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructor with cause.
     * @param cause Original exception.
     */
    public CredentialPolicyNotFoundException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor with cause and error details.
     * @param cause Original exception.
     * @param error Object with error information.
     */
    public CredentialPolicyNotFoundException(Throwable cause, Error error) {
        super(cause);
        this.error = error;
    }

    /**
     * Get error detail information.
     * @return Error detail information.
     */
    public Error getError() {
        return error;
    }
}
