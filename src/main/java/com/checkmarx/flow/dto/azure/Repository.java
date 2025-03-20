
package com.checkmarx.flow.dto.azure;

import com.fasterxml.jackson.annotation.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id",
    "name",
    "url",
    "project",
    "size",
    "defaultBranch",
    "remoteUrl",
    "sshUrl",
    "webUrl"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class Repository {

    @JsonProperty("id")
    private String id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("url")
    private String url;
    @JsonProperty("project")
    private Project project;
    @JsonProperty("size")
    private Long size;
    @JsonProperty("defaultBranch")
    private String defaultBranch;
    @JsonProperty("remoteUrl")
    private String remoteUrl;

    @JsonSetter(nulls = Nulls.SKIP)
    @JsonProperty("sshUrl")
    private String sshUrl;

    @JsonSetter(nulls = Nulls.SKIP)
    @JsonProperty("webUrl")
    private String webUrl;

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
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

    @JsonProperty("url")
    public String getUrl() {
        return url;
    }

    @JsonProperty("url")
    public void setUrl(String url) {
        this.url = url;
    }

    @JsonProperty("project")
    public Project getProject() {
        return project;
    }

    @JsonProperty("project")
    public void setProject(Project project) {
        this.project = project;
    }

    @JsonProperty("defaultBranch")
    public String getDefaultBranch() {
        return defaultBranch;
    }

    @JsonProperty("defaultBranch")
    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }
    
    @JsonProperty("size")
    public Long getSize() {
        return size;
    }

    @JsonProperty("size")
    public void setSize(Long size) {
        this.size = size;
    }

    @JsonProperty("remoteUrl")
    public String getRemoteUrl() {
        return remoteUrl;
    }

    @JsonProperty("remoteUrl")
    public void setRemoteUrl(String remoteUrl) {
        this.remoteUrl = remoteUrl;
    }

    @JsonSetter(nulls = Nulls.SKIP)
    @JsonProperty("sshUrl")
    public String getSshUrl() {
        return sshUrl;
    }
    @JsonSetter(nulls = Nulls.SKIP)
    @JsonProperty("sshUrl")
    public void setSshUrl(String sshUrl) {
        this.sshUrl = sshUrl;
    }

    @JsonSetter(nulls = Nulls.SKIP)
    @JsonProperty("webUrl")
    public String getWebUrl() {
        return webUrl;
    }

    @JsonSetter(nulls = Nulls.SKIP)
    @JsonProperty("webUrl")
    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }
}
