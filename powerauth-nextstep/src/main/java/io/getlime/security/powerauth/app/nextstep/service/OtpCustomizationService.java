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

import io.getlime.security.powerauth.app.nextstep.converter.OperationConverter;
import io.getlime.security.powerauth.app.nextstep.converter.dataadapter.FormDataConverter;
import io.getlime.security.powerauth.app.nextstep.repository.model.entity.OperationEntity;
import io.getlime.security.powerauth.lib.dataadapter.client.DataAdapterClient;
import io.getlime.security.powerauth.lib.dataadapter.client.DataAdapterClientErrorException;
import io.getlime.security.powerauth.lib.dataadapter.model.entity.FormData;
import io.getlime.security.powerauth.lib.dataadapter.model.entity.OperationContext;
import io.getlime.security.powerauth.lib.dataadapter.model.enumeration.AccountStatus;
import io.getlime.security.powerauth.lib.dataadapter.model.enumeration.SmsDeliveryResult;
import io.getlime.security.powerauth.lib.dataadapter.model.response.CreateSmsAuthorizationResponse;
import io.getlime.security.powerauth.lib.dataadapter.model.response.SendAuthorizationSmsResponse;
import io.getlime.security.powerauth.lib.nextstep.model.entity.ApplicationContext;
import io.getlime.security.powerauth.lib.nextstep.model.entity.OtpDeliveryResult;
import io.getlime.security.powerauth.lib.nextstep.model.enumeration.AuthMethod;
import io.getlime.security.powerauth.lib.nextstep.model.response.GetOperationDetailResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * This service handles OTP customization.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Service
public class OtpCustomizationService {

    private final DataAdapterClient dataAdapterClient;

    private final Logger logger = LoggerFactory.getLogger(OtpCustomizationService.class);

    private final OperationConverter operationConverter = new OperationConverter();

    /**
     * Customization service for OTP.
     * @param dataAdapterClient Data Adapter client.
     */
    public OtpCustomizationService(DataAdapterClient dataAdapterClient) {
        this.dataAdapterClient = dataAdapterClient;
    }

    /**
     * Create and send OTP code using Data Adapter.
     * @param userId User ID.
     * @param operation Operation entity.
     * @param language Language as defined in ISO-639 with 2 characters.
     * @param resend Whether OTP code is being resent.
     * @return OTP delivery result.
     */
    public OtpDeliveryResult createAndSendOtp(String userId, OperationEntity operation, String language, boolean resend) {
        OtpDeliveryResult otpDeliveryResult = new OtpDeliveryResult();
        try {
            String operationId = operation.getOperationId();
            String operationName = operation.getOperationName();
            String operationData = operation.getOperationData();
            GetOperationDetailResponse operationDetail = operationConverter.fromEntity(operation);
            String organizationId = operationDetail.getOrganizationId();
            AuthMethod authMethod = operationDetail.getChosenAuthMethod();
            FormData formData = new FormDataConverter().fromOperationFormData(operationDetail.getFormData());
            ApplicationContext applicationContext = operationDetail.getApplicationContext();
            final String externalTransactionId = operation.getExternalTransactionId();
            OperationContext operationContext = new OperationContext(operationId, operationName, operationData, externalTransactionId, formData, applicationContext);
            CreateSmsAuthorizationResponse response = dataAdapterClient.createAuthorizationSms(userId, organizationId, AccountStatus.ACTIVE, authMethod, operationContext, language, resend).getResponseObject();
            otpDeliveryResult.setOtpId(response.getMessageId());
            otpDeliveryResult.setDelivered(response.getSmsDeliveryResult() == SmsDeliveryResult.SUCCEEDED);
            otpDeliveryResult.setErrorMessage(response.getErrorMessage());
        } catch (DataAdapterClientErrorException ex) {
            logger.warn(ex.getMessage(), ex);
            otpDeliveryResult.setDelivered(false);
            // Default error message is used
        }
        return otpDeliveryResult;
    }

    /**
     * Send OTP code using Data Adapter.
     * @param userId User ID.
     * @param operation Operation entity.
     * @param otpId OTP ID.
     * @param otpValue OTP value.
     * @param language Language as defined in ISO-639 with 2 characters.
     * @param resend Whether OTP code is being resent.
     * @return OTP delivery result.
     */
    public OtpDeliveryResult sendOtp(String userId, OperationEntity operation, String otpId, String otpValue, String language, boolean resend) {
        OtpDeliveryResult otpDeliveryResult = new OtpDeliveryResult();
        otpDeliveryResult.setOtpId(otpId);
        try {
            String operationId = operation.getOperationId();
            String operationName = operation.getOperationName();
            String operationData = operation.getOperationData();
            GetOperationDetailResponse operationDetail = operationConverter.fromEntity(operation);
            String organizationId = operationDetail.getOrganizationId();
            AuthMethod authMethod = operationDetail.getChosenAuthMethod();
            FormData formData = new FormDataConverter().fromOperationFormData(operationDetail.getFormData());
            ApplicationContext applicationContext = operationDetail.getApplicationContext();
            final String externalTransactionId = operation.getExternalTransactionId();
            OperationContext operationContext = new OperationContext(operationId, operationName, operationData, externalTransactionId, formData, applicationContext);
            SendAuthorizationSmsResponse response = dataAdapterClient.sendAuthorizationSms(userId, organizationId, AccountStatus.ACTIVE, authMethod, operationContext, otpId, otpValue, language, resend).getResponseObject();
            otpDeliveryResult.setOtpId(response.getMessageId());
            otpDeliveryResult.setDelivered(response.getSmsDeliveryResult() == SmsDeliveryResult.SUCCEEDED);
            otpDeliveryResult.setErrorMessage(response.getErrorMessage());
        } catch (DataAdapterClientErrorException ex) {
            logger.warn(ex.getMessage(), ex);
            otpDeliveryResult.setDelivered(false);
            // Default error message is used
        }
        return otpDeliveryResult;
    }

}
