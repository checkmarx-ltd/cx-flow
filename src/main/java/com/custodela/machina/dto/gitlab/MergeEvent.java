
package com.custodela.machina.dto.gitlab;

import com.custodela.machina.dto.Event;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.validation.Valid;
import java.util.List;
@JsonIgnoreProperties
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "object_kind",
    "user",
    "project",
    "repository",
    "object_attributes",
    "labels",
})
public class MergeEvent extends Event {

    @JsonProperty("object_kind")
    private String objectKind;
    @JsonProperty("user")
    @Valid
    private User user;
    @JsonProperty("project")
    @Valid
    private Project project;
    @JsonProperty("repository")
    @Valid
    private Repository repository;
    @JsonProperty("object_attributes")
    @Valid
    private ObjectAttributes objectAttributes;
    @JsonProperty("labels")
    @Valid
    private List<Label> labels = null;

    @JsonProperty("object_kind")
    public String getObjectKind() {
        return objectKind;
    }

    @JsonProperty("object_kind")
    public void setObjectKind(String objectKind) {
        this.objectKind = objectKind;
    }

    public MergeEvent withObjectKind(String objectKind) {
        this.objectKind = objectKind;
        return this;
    }

    @JsonProperty("user")
    public User getUser() {
        return user;
    }

    @JsonProperty("user")
    public void setUser(User user) {
        this.user = user;
    }

    public MergeEvent withUser(User user) {
        this.user = user;
        return this;
    }

    @JsonProperty("project")
    public Project getProject() {
        return project;
    }

    @JsonProperty("project")
    public void setProject(Project project) {
        this.project = project;
    }

    public MergeEvent withProject(Project project) {
        this.project = project;
        return this;
    }

    @JsonProperty("repository")
    public Repository getRepository() {
        return repository;
    }

    @JsonProperty("repository")
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public MergeEvent withRepository(Repository repository) {
        this.repository = repository;
        return this;
    }

    @JsonProperty("object_attributes")
    public ObjectAttributes getObjectAttributes() {
        return objectAttributes;
    }

    @JsonProperty("object_attributes")
    public void setObjectAttributes(ObjectAttributes objectAttributes) {
        this.objectAttributes = objectAttributes;
    }

    public MergeEvent withObjectAttributes(ObjectAttributes objectAttributes) {
        this.objectAttributes = objectAttributes;
        return this;
    }

    @JsonProperty("labels")
    public List<Label> getLabels() {
        return labels;
    }

    @JsonProperty("labels")
    public void setLabels(List<Label> labels) {
        this.labels = labels;
    }

    public MergeEvent withLabels(List<Label> labels) {
        this.labels = labels;
        return this;
    }

}
