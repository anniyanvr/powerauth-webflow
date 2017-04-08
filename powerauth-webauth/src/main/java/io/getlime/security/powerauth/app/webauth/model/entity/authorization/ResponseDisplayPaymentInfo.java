/*
 * Copyright 2016 Lime - HighTech Solutions s.r.o.
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
package io.getlime.security.powerauth.app.webauth.model.entity.authorization;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.getlime.security.powerauth.app.webauth.model.entity.WebSocketJsonMessage;

import java.math.BigDecimal;

/**
 * @author Roman Strobl
 */
public class ResponseDisplayPaymentInfo extends WebSocketJsonMessage {

    @JsonProperty
    private String operationId;
    @JsonProperty
    private BigDecimal amount;
    @JsonProperty
    private String currency;

    public ResponseDisplayPaymentInfo() {
    }

    public ResponseDisplayPaymentInfo(String sessionId, String operationId, BigDecimal amount, String currency) {
        this.action = WebAuthAction.DISPLAY_PAYMENT_INFO;
        this.sessionId = sessionId;
        this.operationId = operationId;
        this.amount = amount;
        this.currency = currency;
    }

}