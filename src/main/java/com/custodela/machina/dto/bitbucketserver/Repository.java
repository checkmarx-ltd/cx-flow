
package com.custodela.machina.dto.bitbucketserver;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "slug",
    "id",
    "name",
    "scmId",
    "state",
    "statusMessage",
    "forkable",
    "project",
    "public",
    "links"
})
public class Repository {

    @JsonProperty("slug")
    private String slug;
    @JsonProperty("id")
    private Integer id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("scmId")
    private String scmId;
    @JsonProperty("state")
    private String state;
    @JsonProperty("statusMessage")
    private String statusMessage;
    @JsonProperty("forkable")
    private Boolean forkable;
    @JsonProperty("project")
    private Project project;
    @JsonProperty("public")
    private Boolean _public;
    @JsonProperty("links")
    private Links__ links;

    @JsonProperty("slug")
    public String getSlug() {
        return slug;
    }

    @JsonProperty("slug")
    public void setSlug(String slug) {
        this.slug = slug;
    }

    @JsonProperty("id")
    public Integer getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(Integer id) {
        this.id = id;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("scmId")
    public String getScmId() {
        return scmId;
    }

    @JsonProperty("scmId")
    public void setScmId(String scmId) {
        this.scmId = scmId;
    }

    @JsonProperty("state")
    public String getState() {
        return state;
    }

    @JsonProperty("state")
    public void setState(String state) {
        this.state = state;
    }

    @JsonProperty("statusMessage")
    public String getStatusMessage() {
        return statusMessage;
    }

    @JsonProperty("statusMessage")
    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    @JsonProperty("forkable")
    public Boolean getForkable() {
        return forkable;
    }

    @JsonProperty("forkable")
    public void setForkable(Boolean forkable) {
        this.forkable = forkable;
    }

    @JsonProperty("project")
    public Project getProject() {
        return project;
    }

    @JsonProperty("project")
    public void setProject(Project project) {
        this.project = project;
    }

    @JsonProperty("public")
    public Boolean getPublic() {
        return _public;
    }

    @JsonProperty("public")
    public void setPublic(Boolean _public) {
        this._public = _public;
    }

    @JsonProperty("links")
    public Links__ getLinks() {
        return links;
    }

    @JsonProperty("links")
    public void setLinks(Links__ links) {
        this.links = links;
    }

}
