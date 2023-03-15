package com.checkmarx.flow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "mantis")

public class MantisProperties extends RepoProperties {

    private String apiToken;
    private String apiUrl;
    private String projectID;


    /* getters & setters */
    
    public String getApiToken() {
        return apiToken;
    }

    @Override
    public String getApiUrl() {
        return apiUrl;
    }

    @Override
    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public void setApiToken(String apiToken){
        this.apiToken=apiToken;
    }

    public String getProjectID() {
        return projectID;
    }

    public void setProjectID(String projectID){
        this.projectID=projectID;
    }
    
}
