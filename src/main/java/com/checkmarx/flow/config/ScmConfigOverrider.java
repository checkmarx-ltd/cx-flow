package com.checkmarx.flow.config;

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

    public String determineConfigApiUrl(RepoProperties properties, ScanRequest scanRequest) {
        return Optional.ofNullable(scanRequest.getScmInstance())
                .map(key -> getScmOverriddenConfig(properties, ScmConfigParams.API_URI, key))
                .orElse(properties.getApiUrl());
    }

    private String getScmOverriddenConfig(RepoProperties properties, ScmConfigParams configParams, String key) {
        String value = null;
        OptionalScmInstanceProperties optionalInstanceKey = properties.getOptionalInstances().get(key);
        if (optionalInstanceKey == null) {
            log.warn("scm-instance key: {} does not exists on configuration file. Using default configuration", key);
        } else {
            switch (configParams) {
                case TOKEN:
                    value = optionalInstanceKey.getToken();
                    break;
                case API_URI:
                    value = optionalInstanceKey.getApiUrl();
                    break;
                default:
                    throw new MachinaRuntimeException("Scm key: " + key + "is not supported");
            }
        }
        return value;
    }
}