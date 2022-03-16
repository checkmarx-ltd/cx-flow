package com.checkmarx.flow.config.properties;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "codebashing")
@Validated
@Slf4j
@Data
public class CodebashingProperties {

    private String codebashingApiUrl = "https://api.codebashing.com/lessons";
    private String tenantBaseUrl;
    private String apiSecret;
}
