package com.checkmarx.flow.dto.bitbucket;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "path",
        "type",
        "links",
        "commit",
        "mimetype",
        "escaped_path",
        "attributes",
        "size"
})
public class Value {

    @JsonProperty("path")
    private String path;
    @JsonProperty("type")
    private String type;
    @JsonProperty("links")
    private Links links;
    @JsonProperty("commit")
    private Commit_ commit;
    @JsonProperty("mimetype")
    private Object mimetype;
    @JsonProperty("escaped_path")
    private String escapedPath;
    @JsonProperty("attributes")
    private List<String> attributes = null;
    @JsonProperty("size")
    private Integer size;

    @JsonProperty("path")
    public String getPath() {
        return path;
    }

    @JsonProperty("path")
    public void setPath(String path) {
        this.path = path;
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("type")
    public void setType(String type) {
        this.type = type;
    }

    @JsonProperty("links")
    public Links getLinks() {
        return links;
    }

    @JsonProperty("links")
    public void setLinks(Links links) {
        this.links = links;
    }

    @JsonProperty("commit")
    public Commit_ getCommit() {
        return commit;
    }

    @JsonProperty("commit")
    public void setCommit(Commit_ commit) {
        this.commit = commit;
    }

    @JsonProperty("mimetype")
    public Object getMimetype() {
        return mimetype;
    }

    @JsonProperty("mimetype")
    public void setMimetype(Object mimetype) {
        this.mimetype = mimetype;
    }

    @JsonProperty("escaped_path")
    public String getEscapedPath() {
        return escapedPath;
    }

    @JsonProperty("escaped_path")
    public void setEscapedPath(String escapedPath) {
        this.escapedPath = escapedPath;
    }

    @JsonProperty("attributes")
    public List<String> getAttributes() {
        return attributes;
    }

    @JsonProperty("attributes")
    public void setAttributes(List<String> attributes) {
        this.attributes = attributes;
    }

    @JsonProperty("size")
    public Integer getSize() {
        return size;
    }

    @JsonProperty("size")
    public void setSize(Integer size) {
        this.size = size;
    }

}