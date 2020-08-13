package com.checkmarx.flow.dto.bitbucketserver;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "path",
        "revision",
        "children"
})
public class Content {

    @JsonProperty("path")
    private Path path;
    @JsonProperty("revision")
    private String revision;
    @JsonProperty("children")
    private Children children;

    @JsonProperty("path")
    public Path getPath() {
        return path;
    }

    @JsonProperty("path")
    public void setPath(Path path) {
        this.path = path;
    }

    @JsonProperty("revision")
    public String getRevision() {
        return revision;
    }

    @JsonProperty("revision")
    public void setRevision(String revision) {
        this.revision = revision;
    }

    @JsonProperty("children")
    public Children getChildren() {
        return children;
    }

    @JsonProperty("children")
    public void setChildren(Children children) {
        this.children = children;
    }
}