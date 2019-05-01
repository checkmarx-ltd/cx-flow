
package com.checkmarx.flow.dto.github;

import com.fasterxml.jackson.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id",
    "tree_id",
    "distinct",
    "message",
    "timestamp",
    "url",
    "author",
    "committer",
    "added",
    "removed",
    "modified"
})
public class Commit {

    @JsonProperty("id")
    private String id;
    @JsonProperty("tree_id")
    private String treeId;
    @JsonProperty("distinct")
    private Boolean distinct;
    @JsonProperty("message")
    private String message;
    @JsonProperty("timestamp")
    private String timestamp;
    @JsonProperty("url")
    private String url;
    @JsonProperty("author")
    private Author author;
    @JsonProperty("committer")
    private Committer committer;
    @JsonProperty("added")
    private List<Object> added = null;
    @JsonProperty("removed")
    private List<Object> removed = null;
    @JsonProperty("modified")
    private List<String> modified = null;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("tree_id")
    public String getTreeId() {
        return treeId;
    }

    @JsonProperty("tree_id")
    public void setTreeId(String treeId) {
        this.treeId = treeId;
    }

    @JsonProperty("distinct")
    public Boolean getDistinct() {
        return distinct;
    }

    @JsonProperty("distinct")
    public void setDistinct(Boolean distinct) {
        this.distinct = distinct;
    }

    @JsonProperty("message")
    public String getMessage() {
        return message;
    }

    @JsonProperty("message")
    public void setMessage(String message) {
        this.message = message;
    }

    @JsonProperty("timestamp")
    public String getTimestamp() {
        return timestamp;
    }

    @JsonProperty("timestamp")
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @JsonProperty("url")
    public String getUrl() {
        return url;
    }

    @JsonProperty("url")
    public void setUrl(String url) {
        this.url = url;
    }

    @JsonProperty("author")
    public Author getAuthor() {
        return author;
    }

    @JsonProperty("author")
    public void setAuthor(Author author) {
        this.author = author;
    }

    @JsonProperty("committer")
    public Committer getCommitter() {
        return committer;
    }

    @JsonProperty("committer")
    public void setCommitter(Committer committer) {
        this.committer = committer;
    }

    @JsonProperty("added")
    public List<Object> getAdded() {
        return added;
    }

    @JsonProperty("added")
    public void setAdded(List<Object> added) {
        this.added = added;
    }

    @JsonProperty("removed")
    public List<Object> getRemoved() {
        return removed;
    }

    @JsonProperty("removed")
    public void setRemoved(List<Object> removed) {
        this.removed = removed;
    }

    @JsonProperty("modified")
    public List<String> getModified() {
        return modified;
    }

    @JsonProperty("modified")
    public void setModified(List<String> modified) {
        this.modified = modified;
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
