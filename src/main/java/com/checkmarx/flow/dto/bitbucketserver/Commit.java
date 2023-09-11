
package com.checkmarx.flow.dto.bitbucketserver;

import java.util.LinkedHashMap;
import java.util.List;
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

    "id",
    "displayId",
    "author",
    "authorTimestamp",
    "committer",
    "committerTimestamp",
    "message",
    "parents"
})
@Generated("jsonschema2pojo")
public class Commit {

    @JsonProperty("id")
    private String id;
    @JsonProperty("displayId")
    private String displayId;
    @JsonProperty("author")
    private Author author;
    @JsonProperty("authorTimestamp")
    private Long authorTimestamp;
    @JsonProperty("committer")
    private Committer committer;
    @JsonProperty("committerTimestamp")
    private Long committerTimestamp;
    @JsonProperty("message")
    private String message;
    @JsonProperty("parents")
    private List<Parent> parents;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("displayId")
    public String getDisplayId() {
        return displayId;
    }

    @JsonProperty("displayId")
    public void setDisplayId(String displayId) {
        this.displayId = displayId;
    }

    @JsonProperty("author")
    public Author getAuthor() {
        return author;
    }

    @JsonProperty("author")
    public void setAuthor(Author author) {
        this.author = author;
    }

    @JsonProperty("authorTimestamp")
    public Long getAuthorTimestamp() {
        return authorTimestamp;
    }

    @JsonProperty("authorTimestamp")
    public void setAuthorTimestamp(Long authorTimestamp) {
        this.authorTimestamp = authorTimestamp;
    }

    @JsonProperty("committer")
    public Committer getCommitter() {
        return committer;
    }

    @JsonProperty("committer")
    public void setCommitter(Committer committer) {
        this.committer = committer;
    }

    @JsonProperty("committerTimestamp")
    public Long getCommitterTimestamp() {
        return committerTimestamp;
    }

    @JsonProperty("committerTimestamp")
    public void setCommitterTimestamp(Long committerTimestamp) {
        this.committerTimestamp = committerTimestamp;
    }

    @JsonProperty("message")
    public String getMessage() {
        return message;
    }

    @JsonProperty("message")
    public void setMessage(String message) {
        this.message = message;
    }

    @JsonProperty("parents")
    public List<Parent> getParents() {
        return parents;
    }

    @JsonProperty("parents")
    public void setParents(List<Parent> parents) {
        this.parents = parents;
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
