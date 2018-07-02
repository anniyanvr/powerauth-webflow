/*
 * Copyright 2017 Lime - HighTech Solutions s.r.o.
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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller advice responsible for default exception resolving.
 *
 * @author Petr Dvorak, petr@lime-company.eu
 */
@ControllerAdvice
public class DefaultExceptionResolver {

    /**
     * Default exception handler, for unexpected errors.
     * @param t Throwable.
     * @return Response with error details.
     */
    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public @ResponseBody ErrorResponse handleDefaultException(Throwable t) {
        Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Error occurred in Next Step server", t);
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
        Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Error occurred in Next Step server: {0}", ex.getMessage());
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
        Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Error occurred in Next Step server: {0}", ex.getMessage());
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
        Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Error occurred in Next Step server: {0}", ex.getMessage());
        Error error = new Error(OperationAlreadyCanceledException.CODE, "Operation update attempted for CANCELED operation.");
        return new ErrorResponse(error);
    }

    /**
     * Exception handler for invalid operation not found error.
     * @param ex Exception.
     * @return Response with error details.
     */
    @ExceptionHandler(OperationNotFoundException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody ErrorResponse handleOperationNotFoundException(OperationNotFoundException ex) {
        Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Error occurred in Next Step server: {0}", ex.getMessage());
        Error error = new Error(OperationNotFoundException.CODE, "Operation not found.");
        return new ErrorResponse(error);
    }

    /**
     * Exception handler for invalid operation not confiured error.
     * @param ex Exception.
     * @return Response with error details.
     */
    @ExceptionHandler(OperationNotConfiguredException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody ErrorResponse handleOperationNotConfiguredException(OperationNotConfiguredException ex) {
        Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Error occurred in Next Step server: {0}", ex.getMessage());
        Error error = new Error(OperationNotFoundException.CODE, "Operation is not configured.");
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
        Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Error occurred in Next Step server: {0}", ex.getMessage());
        Error error = new Error(InvalidOperationDataException.CODE, "Operation contains invalid data.");
        return new ErrorResponse(error);
    }
}
