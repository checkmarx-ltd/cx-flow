package com.checkmarx.flow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "itop")

public class ITopProperties extends RepoProperties {

    private String restEndpointUrl;
    private String apiUser;
    private String apiPassword;
    private String orgName;
    private String serviceName;

    /* getters & setters */

    public String getRestEndpointUrl() {
        return restEndpointUrl;
    }

    public void setRestEndpointUrl(String restEndpointUrl) {
        this.restEndpointUrl = restEndpointUrl;
    }

    public String getApiUser() {
        return apiUser;
    }

    public void setApiUser(String apiUser) {
        this.apiUser = apiUser;
    }

    public String getApiPassword() {
        return apiPassword;
    }

    public void setApiPassword(String apiPassword) {
        this.apiPassword = apiPassword;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

}
