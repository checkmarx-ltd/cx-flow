package com.checkmarx.flow.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "bitbucket")
@Validated
public class BitBucketProperties extends RepoProperties {
    private String apiPath;
    private List<String> ipAddresses;

    public String getApiPath() {
        return this.apiPath;
    }

    public List<String> getIpAddresses() {
        return this.ipAddresses;
    }

    public void setApiPath(String apiPath) {
        this.apiPath = apiPath;
    }

    public void setIpAddresses(List<String> ipAddresses) {
        this.ipAddresses = ipAddresses;
    }
}
