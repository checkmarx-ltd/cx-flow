package com.checkmarx.flow.custom;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "azure")
@Validated
public class AzureProperties {
    private String webhookToken;
    private String token;
    private String url;
    private String apiPath;
    private String falsePositiveLabel;

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

}
