package com.checkmarx.flow.config.properties;

import com.checkmarx.flow.dto.iast.ql.utils.Severity;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "iast")
@Validated
@Getter
@Setter
public class IastProperties {

    private String url;
    private String sslCertificateFilePath;
    private String username;
    private String password;
    private String managerPort;
    private Integer updateTokenSeconds;
    private List<Severity> filterSeverity;
    private Map<Severity, Integer> thresholdsSeverity;
}
