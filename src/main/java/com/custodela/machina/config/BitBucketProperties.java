package com.custodela.machina.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "bitbucket")
@Validated
public class BitBucketProperties {
    private String webhookToken;
    private String token;
    private String url;;
    private String apiPath;
    private String falsePositiveLabel;
    private List<String> ipAddresses;

    public String getWebhookToken() {
        return this.webhookToken;
    }

    public String getToken() {
        return this.token;
    }

    public String getUrl() {
        return this.url;
    }

    public String getApiPath() {
        return this.apiPath;
    }

    public String getFalsePositiveLabel() {
        return this.falsePositiveLabel;
    }

    public List<String> getIpAddresses() {
        return this.ipAddresses;
    }

    public void setWebhookToken(String webhookToken) {
        this.webhookToken = webhookToken;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setApiPath(String apiPath) {
        this.apiPath = apiPath;
    }

    public void setFalsePositiveLabel(String falsePositiveLabel) {
        this.falsePositiveLabel = falsePositiveLabel;
    }

    public void setIpAddresses(List<String> ipAddresses) {
        this.ipAddresses = ipAddresses;
    }
}
