package com.checkmarx.flow.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Component
@ConfigurationProperties(prefix = "slack")
@Validated
@Data
public class SlackProperties {
    @NotNull
    @NotBlank
    private String botToken;
    private String channelName = "random";
    private String channelScript;
    private Integer highsThreshold = 0;
    private Integer mediumsThreshold = 0;
}
