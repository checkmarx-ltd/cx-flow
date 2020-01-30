package com.checkmarx.flow.dto.rally;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NewDefect {
    @JsonProperty("name")
    private String name;
    @JsonProperty("workspace")
    private String workspace;
    @JsonProperty("project")
    private String project;

    public String getName() {
        return this.name;
    }
    public String getWorkspace() {
        return this.workspace;
    }
    public String getProject() { return this.project; }

    public void setName(String name) { this.name = name; }
    public void setWorkspace(String workspace) { this.name = workspace; }
    public void setProject(String project) { this.name = project; }
}
