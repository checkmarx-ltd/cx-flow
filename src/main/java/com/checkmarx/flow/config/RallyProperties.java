package com.checkmarx.flow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "rally")
@Validated
public class RallyProperties extends RepoProperties {
    private String rallyWorkspaceId;
    private String rallyProjectId;

    public String getRallyWorkspaceId() {
        return rallyWorkspaceId;
    }

    public void setRallyWorkspaceId(String rallyWorkspaceId) {
        this.rallyWorkspaceId = rallyWorkspaceId;
    }

    public String getRallyProjectId() {
        return rallyProjectId;
    }

    public void setRallyProjectId(String rallyProjectId) {
        this.rallyProjectId = rallyProjectId;
    }
}
