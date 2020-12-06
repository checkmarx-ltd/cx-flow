package com.checkmarx.flow.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "codebashing")
@Validated
public class CodebashingProperties {

    @Getter
    @Setter
    private String codebashingApiUrl = "https://api.codebashing.com/lessons";
    @Getter
    @Setter
    private String tenantBaseUrl;
    @Getter
    @Setter
    private String apiSecret;
}
