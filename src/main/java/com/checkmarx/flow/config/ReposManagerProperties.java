package com.checkmarx.flow.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "repos-manager")
@Validated
@Getter
@Setter
public class ReposManagerProperties {

    private String url;
    private boolean readMultiTenantConfiguration;
}