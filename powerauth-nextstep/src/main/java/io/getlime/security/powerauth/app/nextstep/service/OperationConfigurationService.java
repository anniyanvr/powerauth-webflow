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
package io.getlime.security.powerauth.app.nextstep.service;

import io.getlime.security.powerauth.app.nextstep.converter.OperationConfigConverter;
import io.getlime.security.powerauth.app.nextstep.repository.OperationConfigRepository;
import io.getlime.security.powerauth.app.nextstep.repository.model.entity.OperationConfigEntity;
import io.getlime.security.powerauth.lib.nextstep.model.exception.OperationConfigAlreadyExists;
import io.getlime.security.powerauth.lib.nextstep.model.exception.OperationConfigNotFoundException;
import io.getlime.security.powerauth.lib.nextstep.model.request.CreateOperationConfigRequest;
import io.getlime.security.powerauth.lib.nextstep.model.request.DeleteOperationConfigRequest;
import io.getlime.security.powerauth.lib.nextstep.model.response.CreateOperationConfigResponse;
import io.getlime.security.powerauth.lib.nextstep.model.response.DeleteOperationConfigResponse;
import io.getlime.security.powerauth.lib.nextstep.model.response.GetOperationConfigDetailResponse;
import io.getlime.security.powerauth.lib.nextstep.model.response.GetOperationConfigListResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Optional;

/**
 * Service which handles persistence of operation configurations.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Service
public class OperationConfigurationService {

    private final OperationConfigRepository operationConfigRepository;
    private final OperationConfigConverter configConverter = new OperationConfigConverter();

    /**
     * Service constructor.
     *
     * @param operationConfigRepository Operation configuration repository.
     */
    @Autowired
    public OperationConfigurationService(OperationConfigRepository operationConfigRepository) {
        this.operationConfigRepository = operationConfigRepository;
    }

    /**
     * Create an operation configuration.
     * @param request Create operation configuration request.
     * @return Create operation configuration response.
     * @throws OperationConfigAlreadyExists Thrown when operation configuration already exists.
     */
    @Transactional
    public CreateOperationConfigResponse createOperationConfig(CreateOperationConfigRequest request) throws OperationConfigAlreadyExists {
        Optional<OperationConfigEntity> operationConfigOptional = operationConfigRepository.findById(request.getOperationName());
        if (operationConfigOptional.isPresent()) {
            throw new OperationConfigAlreadyExists("Operation configuration already exists for operation: " + request.getOperationName());
        }
        OperationConfigEntity operationConfig = new OperationConfigEntity();
        operationConfig.setOperationName(request.getOperationName());
        operationConfig.setTemplateVersion(request.getTemplateVersion());
        operationConfig.setTemplateId(request.getTemplateId());
        operationConfig.setMobileTokenEnabled(request.isMobileTokenEnabled());
        operationConfig.setMobileTokenMode(request.getMobileTokenMode());
        operationConfig.setAfsEnabled(request.isAfsEnabled());
        operationConfig.setAfsConfigId(request.getAfsConfigId());
        operationConfigRepository.save(operationConfig);
        CreateOperationConfigResponse response = new CreateOperationConfigResponse();
        response.setOperationName(operationConfig.getOperationName());
        response.setTemplateVersion(operationConfig.getTemplateVersion());
        response.setTemplateId(operationConfig.getTemplateId());
        response.setMobileTokenEnabled(operationConfig.isMobileTokenEnabled());
        response.setMobileTokenMode(operationConfig.getMobileTokenMode());
        response.setAfsEnabled(operationConfig.isAfsEnabled());
        response.setAfsConfigId(operationConfig.getAfsConfigId());
        return response;
    }

    /**
     * Get operation configuration.
     * @param operationName Operation name.
     * @return Operation configuration.
     * @throws OperationConfigNotFoundException Thrown when operation is not configured.
     */
    @Transactional
    public GetOperationConfigDetailResponse getOperationConfig(String operationName) throws OperationConfigNotFoundException {
        Optional<OperationConfigEntity> operationConfigOptional = operationConfigRepository.findById(operationName);
        if (!operationConfigOptional.isPresent()) {
            throw new OperationConfigNotFoundException("Operation not configured, operation name: " + operationName);
        }
        OperationConfigEntity operationConfig = operationConfigOptional.get();
        return configConverter.fromOperationConfigEntity(operationConfig);
    }

    /**
     * Get all operation configurations.
     * @return All operation configurations.
     */
    @Transactional
    public GetOperationConfigListResponse getOperationConfigs() {
        GetOperationConfigListResponse configsResponse = new GetOperationConfigListResponse();
        Iterable<OperationConfigEntity> allConfigs = operationConfigRepository.findAll();
        for (OperationConfigEntity operationConfig: allConfigs) {
            GetOperationConfigDetailResponse config = configConverter.fromOperationConfigEntity(operationConfig);
            configsResponse.getOperationConfigs().add(config);
        }
        return configsResponse;
    }

    /**
     * Delete an operation configuration.
     * @param request Delete operation configuration request.
     * @return Delete operation configuration response.
     * @throws OperationConfigNotFoundException Thrown when operation configuration is not configured.
     */
    @Transactional
    public DeleteOperationConfigResponse deleteOperationConfig(DeleteOperationConfigRequest request) throws OperationConfigNotFoundException {
        Optional<OperationConfigEntity> operationConfigOptional = operationConfigRepository.findById(request.getOperationName());
        if (!operationConfigOptional.isPresent()) {
            throw new OperationConfigNotFoundException("Operation configuration not found, operation name: " + request.getOperationName());
        }
        OperationConfigEntity operationConfig = operationConfigOptional.get();
        operationConfigRepository.delete(operationConfig);
        DeleteOperationConfigResponse response = new DeleteOperationConfigResponse();
        response.setOperationName(operationConfig.getOperationName());
        return response;
    }

}
