package com.checkmarx.flow.dto.mantis.project;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MantisProjectResponse {
    @JsonProperty("projects")
    private List<MantisProject> projects;

    public List<MantisProject> getProjects() {
        return projects;
    }

    public MantisProject getProject() {
        return projects.get(0);
    }

    public void setProjects(List<MantisProject> projects) {
        this.projects = projects;
    }

    public List<MantisProject> getSubProjects() {
        if (projects == null || projects.isEmpty()) {
            return null;
        }
        return projects.subList(1, projects.size());
    }
}

