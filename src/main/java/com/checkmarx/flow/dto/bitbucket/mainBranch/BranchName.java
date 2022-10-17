
package com.checkmarx.flow.dto.bitbucket.mainBranch;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "type",
    "full_name",
    "links",
    "name",
    "slug",
    "description",
    "scm",
    "website",
    "owner",
    "workspace",
    "is_private",
    "project",
    "fork_policy",
    "created_on",
    "updated_on",
    "size",
    "language",
    "has_issues",
    "has_wiki",
    "uuid",
    "mainbranch",
    "override_settings"
})
@Generated("jsonschema2pojo")
public class BranchName {

    @JsonProperty("type")
    private String type;
    @JsonProperty("full_name")
    private String fullName;
    @JsonProperty("links")
    private Links links;
    @JsonProperty("name")
    private String name;
    @JsonProperty("slug")
    private String slug;
    @JsonProperty("description")
    private String description;
    @JsonProperty("scm")
    private String scm;
    @JsonProperty("website")
    private Object website;
    @JsonProperty("owner")
    private Owner owner;
    @JsonProperty("workspace")
    private Workspace workspace;
    @JsonProperty("is_private")
    private Boolean isPrivate;
    @JsonProperty("project")
    private Project project;
    @JsonProperty("fork_policy")
    private String forkPolicy;
    @JsonProperty("created_on")
    private String createdOn;
    @JsonProperty("updated_on")
    private String updatedOn;
    @JsonProperty("size")
    private Integer size;
    @JsonProperty("language")
    private String language;
    @JsonProperty("has_issues")
    private Boolean hasIssues;
    @JsonProperty("has_wiki")
    private Boolean hasWiki;
    @JsonProperty("uuid")
    private String uuid;
    @JsonProperty("mainbranch")
    private Mainbranch mainbranch;
    @JsonProperty("override_settings")
    private OverrideSettings overrideSettings;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("type")
    public void setType(String type) {
        this.type = type;
    }

    @JsonProperty("full_name")
    public String getFullName() {
        return fullName;
    }

    @JsonProperty("full_name")
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    @JsonProperty("links")
    public Links getLinks() {
        return links;
    }

    @JsonProperty("links")
    public void setLinks(Links links) {
        this.links = links;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("slug")
    public String getSlug() {
        return slug;
    }

    @JsonProperty("slug")
    public void setSlug(String slug) {
        this.slug = slug;
    }

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty("scm")
    public String getScm() {
        return scm;
    }

    @JsonProperty("scm")
    public void setScm(String scm) {
        this.scm = scm;
    }

    @JsonProperty("website")
    public Object getWebsite() {
        return website;
    }

    @JsonProperty("website")
    public void setWebsite(Object website) {
        this.website = website;
    }

    @JsonProperty("owner")
    public Owner getOwner() {
        return owner;
    }

    @JsonProperty("owner")
    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    @JsonProperty("workspace")
    public Workspace getWorkspace() {
        return workspace;
    }

    @JsonProperty("workspace")
    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    @JsonProperty("is_private")
    public Boolean getIsPrivate() {
        return isPrivate;
    }

    @JsonProperty("is_private")
    public void setIsPrivate(Boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    @JsonProperty("project")
    public Project getProject() {
        return project;
    }

    @JsonProperty("project")
    public void setProject(Project project) {
        this.project = project;
    }

    @JsonProperty("fork_policy")
    public String getForkPolicy() {
        return forkPolicy;
    }

    @JsonProperty("fork_policy")
    public void setForkPolicy(String forkPolicy) {
        this.forkPolicy = forkPolicy;
    }

    @JsonProperty("created_on")
    public String getCreatedOn() {
        return createdOn;
    }

    @JsonProperty("created_on")
    public void setCreatedOn(String createdOn) {
        this.createdOn = createdOn;
    }

    @JsonProperty("updated_on")
    public String getUpdatedOn() {
        return updatedOn;
    }

    @JsonProperty("updated_on")
    public void setUpdatedOn(String updatedOn) {
        this.updatedOn = updatedOn;
    }

    @JsonProperty("size")
    public Integer getSize() {
        return size;
    }

    @JsonProperty("size")
    public void setSize(Integer size) {
        this.size = size;
    }

    @JsonProperty("language")
    public String getLanguage() {
        return language;
    }

    @JsonProperty("language")
    public void setLanguage(String language) {
        this.language = language;
    }

    @JsonProperty("has_issues")
    public Boolean getHasIssues() {
        return hasIssues;
    }

    @JsonProperty("has_issues")
    public void setHasIssues(Boolean hasIssues) {
        this.hasIssues = hasIssues;
    }

    @JsonProperty("has_wiki")
    public Boolean getHasWiki() {
        return hasWiki;
    }

    @JsonProperty("has_wiki")
    public void setHasWiki(Boolean hasWiki) {
        this.hasWiki = hasWiki;
    }

    @JsonProperty("uuid")
    public String getUuid() {
        return uuid;
    }

    @JsonProperty("uuid")
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @JsonProperty("mainbranch")
    public Mainbranch getMainbranch() {
        return mainbranch;
    }

    @JsonProperty("mainbranch")
    public void setMainbranch(Mainbranch mainbranch) {
        this.mainbranch = mainbranch;
    }

    @JsonProperty("override_settings")
    public OverrideSettings getOverrideSettings() {
        return overrideSettings;
    }

    @JsonProperty("override_settings")
    public void setOverrideSettings(OverrideSettings overrideSettings) {
        this.overrideSettings = overrideSettings;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
