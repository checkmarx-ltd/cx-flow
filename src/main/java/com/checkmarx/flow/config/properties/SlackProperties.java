package com.checkmarx.flow.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "slack")
@Validated
@Data
public class SlackProperties {
    private String botToken;
}
