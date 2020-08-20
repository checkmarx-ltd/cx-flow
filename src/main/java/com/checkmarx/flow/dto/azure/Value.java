package com.checkmarx.flow.dto.azure;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "objectId",
        "gitObjectType",
        "commitId",
        "path",
        "isFolder",
        "url"
})
public class Value {

    @JsonProperty("objectId")
    private String objectId;
    @JsonProperty("gitObjectType")
    private String gitObjectType;
    @JsonProperty("commitId")
    private String commitId;
    @JsonProperty("path")
    private String path;
    @JsonProperty("isFolder")
    private boolean isFolder;
    @JsonProperty("url")
    private String url;

    @JsonProperty("objectId")
    public String getObjectId() {
        return objectId;
    }

    @JsonProperty("objectId")
    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    @JsonProperty("gitObjectType")
    public String getGitObjectType() {
        return gitObjectType;
    }

    @JsonProperty("gitObjectType")
    public void setGitObjectType(String gitObjectType) {
        this.gitObjectType = gitObjectType;
    }

    @JsonProperty("commitId")
    public String getCommitId() {
        return commitId;
    }

    @JsonProperty("commitId")
    public void setCommitId(String commitId) {
        this.commitId = commitId;
    }

    @JsonProperty("path")
    public String getPath() {
        return path;
    }

    @JsonProperty("path")
    public void setPath(String path) {
        this.path = path;
    }

    @JsonProperty("isFolder")
    public boolean getIsFolder() {
        return isFolder;
    }

    @JsonProperty("isFolder")
    public void setIsFolder(boolean isFolder) {
        this.isFolder = isFolder;
    }

    @JsonProperty("url")
    public String getUrl() {
        return url;
    }

    @JsonProperty("url")
    public void setUrl(String url) {
        this.url = url;
    }

}