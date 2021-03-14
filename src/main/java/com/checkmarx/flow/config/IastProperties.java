package com.checkmarx.flow.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "iast")
@Validated
@Getter
@Setter
public class IastProperties {

    private String url;
    private String username;
    private String password;
    private String managerPort;
}
