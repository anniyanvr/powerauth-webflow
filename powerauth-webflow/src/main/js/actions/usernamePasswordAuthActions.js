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
import axios from "axios";
import {dispatchAction, dispatchError} from "../dispatcher/dispatcher";

/**
 * Username and password authentication.
 * @param username Username.
 * @param password Password.
 * @param organizationId Organization ID.
 * @returns {Function} No return value.
 */
export function authenticate(username, password, organizationId) {
    return function (dispatch) {
        dispatch({
            type: "SHOW_SCREEN_LOGIN",
            payload: {
                loading: true,
                error: false,
                message: ""
            }
        });
        axios.post("./api/auth/form/authenticate", {
            username: username,
            password: password,
            organizationId: organizationId
        }, {
            headers: {
                'X-OPERATION-HASH': operationHash,
            }
        }).then((response) => {
            switch (response.data.result) {
                case 'CONFIRMED': {
                    dispatchAction(dispatch, response);
                    break;
                }
                case 'AUTH_FAILED': {
                    // handle timeout - login action can not succeed anymore, do not show login screen, show error instead
                    if (response.data.message === "operation.timeout") {
                        dispatchAction(dispatch, response);
                        break;
                    }
                    // if the operation has been interrupted by new operation, show an error
                    if (response.data.message === "operation.interrupted") {
                        dispatchAction(dispatch, response);
                        break;
                    }
                    // if the maximum number of attempts has been exceeded, show an error, the method cannot continue
                    if (response.data.message === "authentication.maxAttemptsExceeded") {
                        dispatchAction(dispatch, response);
                        break;
                    }
                    // if there is no supported auth method, show error, there is no point in continuing
                    // TODO - handle fallback - see issue #32
                    if (response.data.message === "error.noAuthMethod") {
                        dispatchAction(dispatch, response);
                        break;
                    }
                    dispatch({
                        type: "SHOW_SCREEN_LOGIN",
                        payload: {
                            loading: false,
                            error: true,
                            message: response.data.message,
                            remainingAttempts: response.data.remainingAttempts
                        }
                    });
                    break;
                }
            }
            return null;
        }).catch((error) => {
            dispatchError(dispatch, error);
        })
    }
}

export function cancel() {
    return function (dispatch) {
        axios.post("./api/auth/form/cancel", {}, {
            headers: {
                'X-OPERATION-HASH': operationHash,
            }
        }).then((response) => {
            dispatch({
                type: "SHOW_SCREEN_ERROR",
                payload: {
                    message: response.data.message
                }
            });
            return null;
        }).catch((error) => {
            dispatchError(dispatch, error);
        })
    }
}

export function initLogin(callback) {
    return function (dispatch) {
        dispatch({
            type: "SHOW_SCREEN_LOGIN",
            payload: {
                loading: true,
                error: false,
                message: ""
            }
        });
        axios.post("./api/auth/form/init", {}).then((response) => {
            dispatch({
                type: "SHOW_SCREEN_LOGIN",
                payload: response.data
            });
            callback(true);
            return null;
        }).catch((error) => {
            dispatchError(dispatch, error);
        })
    }
}

export function selectOrganization(organizationId) {
    return function (dispatch) {
        dispatch({
            type: "SHOW_SCREEN_LOGIN",
            payload: {
                chosenOrganizationId: organizationId
            }
        });
    }
}

/**
 * Interrupt operation and show unexpected error about missing organization configuration.
 * @returns {Function} Missing organization configuration error is dispatched.
 */
export function organizationConfigurationError() {
    return function (dispatch) {
        dispatch({
            type: "SHOW_SCREEN_ERROR",
            payload: {
                message: "organization.configurationError"
            }
        })
    }
}