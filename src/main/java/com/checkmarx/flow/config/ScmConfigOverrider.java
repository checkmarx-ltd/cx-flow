package com.checkmarx.flow.config;

import com.checkmarx.flow.dto.ControllerRequest;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class ScmConfigOverrider {

    public String determineConfigToken(RepoProperties properties, ScanRequest scanRequest) {
        return Optional.ofNullable(scanRequest.getScmInstance())
                .map(key -> getScmOverriddenConfig(properties, ScmConfigParams.TOKEN, key))
                .orElse(properties.getToken());
    }

    public String determineConfigToken(RepoProperties properties, ControllerRequest controllerRequest) {
        return Optional.ofNullable(controllerRequest)
                .map(ControllerRequest::getScmInstance)
                .map(key -> getScmOverriddenConfig(properties, ScmConfigParams.TOKEN, key))
                .orElse(properties.getToken());
    }

    public String determineConfigApiUrl(RepoProperties properties, ScanRequest scanRequest) {
        return Optional.ofNullable(scanRequest.getScmInstance())
                .map(key -> getScmOverriddenConfig(properties, ScmConfigParams.API_URL, key))
                .orElse(properties.getApiUrl());
    }

    public String determineConfigWebhookToken(RepoProperties properties, ControllerRequest controllerRequest) {
        return Optional.ofNullable(controllerRequest)
                .map(ControllerRequest::getScmInstance)
                .map(key -> getScmOverriddenConfig(properties, ScmConfigParams.WEBHOOK_TOKEN, key))
                .orElse(properties.getWebhookToken());
    }

    private String getScmOverriddenConfig(RepoProperties properties, ScmConfigParams configParams, String key) {
        String value;
        OptionalScmInstanceProperties optionalInstanceKey = properties.getOptionalInstances().get(key);
        if (optionalInstanceKey == null) {
            log.error("scm-instance key: {} does not exists on configuration file.", key);
            throw new MachinaRuntimeException();
        } else {
            switch (configParams) {
                case TOKEN:
                    log.info("Overriding token for SCM instance key: {}", key);
                    value = optionalInstanceKey.getToken();
                    break;
                case WEBHOOK_TOKEN:
                    log.info("Overriding webhook-token for SCM instance key: {}", key);
                    value = optionalInstanceKey.getWebhookToken();
                    break;
                case API_URL:
                    log.info("Overriding api-url for SCM instance key: {}", key);
                    value = optionalInstanceKey.getApiUrl();
                    break;
                default:
                    throw new MachinaRuntimeException("Scm key: " + key + "is not supported");
            }
        }
        return value;
    }
}