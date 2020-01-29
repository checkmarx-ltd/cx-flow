package com.checkmarx.flow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "rally")
@Validated
public class RallyProperties extends RepoProperties {
    // There's nothing here by design!
    public int testVal = 3;
}
