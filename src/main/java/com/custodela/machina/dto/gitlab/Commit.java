
package com.custodela.machina.dto.gitlab;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.validation.Valid;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id",
    "message",
    "timestamp",
    "url",
    "author",
    "added",
    "modified",
    "removed"
})
public class Commit {

    @JsonProperty("id")
    private String id;
    @JsonProperty("message")
    private String message;
    @JsonProperty("timestamp")
    private String timestamp;
    @JsonProperty("url")
    private String url;
    @JsonProperty("author")
    @Valid
    private Author author;
    @JsonProperty("added")
    @Valid
    private List<String> added = null;
    @JsonProperty("modified")
    @Valid
    private List<String> modified = null;
    @JsonProperty("removed")
    @Valid
    private List<String> removed = null;

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    public Commit withId(String id) {
        this.id = id;
        return this;
    }

    @JsonProperty("message")
    public String getMessage() {
        return message;
    }

    @JsonProperty("message")
    public void setMessage(String message) {
        this.message = message;
    }

    public Commit withMessage(String message) {
        this.message = message;
        return this;
    }

    @JsonProperty("timestamp")
    public String getTimestamp() {
        return timestamp;
    }

    @JsonProperty("timestamp")
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Commit withTimestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    @JsonProperty("url")
    public String getUrl() {
        return url;
    }

    @JsonProperty("url")
    public void setUrl(String url) {
        this.url = url;
    }

    public Commit withUrl(String url) {
        this.url = url;
        return this;
    }

    @JsonProperty("author")
    public Author getAuthor() {
        return author;
    }

    @JsonProperty("author")
    public void setAuthor(Author author) {
        this.author = author;
    }

    public Commit withAuthor(Author author) {
        this.author = author;
        return this;
    }

    @JsonProperty("added")
    public List<String> getAdded() {
        return added;
    }

    @JsonProperty("added")
    public void setAdded(List<String> added) {
        this.added = added;
    }

    public Commit withAdded(List<String> added) {
        this.added = added;
        return this;
    }

    @JsonProperty("modified")
    public List<String> getModified() {
        return modified;
    }

    @JsonProperty("modified")
    public void setModified(List<String> modified) {
        this.modified = modified;
    }

    public Commit withModified(List<String> modified) {
        this.modified = modified;
        return this;
    }

    @JsonProperty("removed")
    public List<String> getRemoved() {
        return removed;
    }

    @JsonProperty("removed")
    public void setRemoved(List<String> removed) {
        this.removed = removed;
    }

    public Commit withRemoved(List<String> removed) {
        this.removed = removed;
        return this;
    }

}
