package com.checkmarx.flow.dto.mantis.project;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MantisProject {
    @JsonProperty("id")
    private String id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("description")
    private String description;
    @JsonProperty("enabled")
    private Boolean enabled;
    @JsonProperty("subProjects")
    private List<MantisProject> subProjects;

    public MantisProject() {
    }

    public MantisProject(String id, String name, String description, Boolean enabled, List<MantisProject> subProjects) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.enabled = enabled;
        this.subProjects = subProjects;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public List<MantisProject> getSubProjects() {
        return subProjects;
    }

    public void setSubProjects(List<MantisProject> subProjects) {
        this.subProjects = subProjects;
    }

    public void addSubProject(MantisProject subProject) {
        subProjects.add(subProject);
    }    

    @Override
    public String toString() {
        return "MantisProject{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", enabled=" + enabled +
                ", subProjects=" + subProjects +
                '}';
    }
}
