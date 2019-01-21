
package com.custodela.machina.dto.bitbucketserver;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id",
    "version",
    "title",
    "state",
    "open",
    "closed",
    "createdDate",
    "updatedDate",
    "fromRef",
    "toRef",
    "locked",
    "author",
    "reviewers",
    "participants",
    "links"
})
public class PullRequest {

    @JsonProperty("id")
    private Integer id;
    @JsonProperty("version")
    private Integer version;
    @JsonProperty("title")
    private String title;
    @JsonProperty("state")
    private String state;
    @JsonProperty("open")
    private Boolean open;
    @JsonProperty("closed")
    private Boolean closed;
    @JsonProperty("createdDate")
    private String createdDate;
    @JsonProperty("updatedDate")
    private String updatedDate;
    @JsonProperty("fromRef")
    private FromRef fromRef;
    @JsonProperty("toRef")
    private ToRef toRef;
    @JsonProperty("locked")
    private Boolean locked;
    @JsonProperty("author")
    private Author author;
    @JsonProperty("reviewers")
    private List<Object> reviewers = null;
    @JsonProperty("participants")
    private List<Object> participants = null;
    @JsonProperty("links")
    private Links links;

    @JsonProperty("id")
    public Integer getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(Integer id) {
        this.id = id;
    }

    @JsonProperty("version")
    public Integer getVersion() {
        return version;
    }

    @JsonProperty("version")
    public void setVersion(Integer version) {
        this.version = version;
    }

    @JsonProperty("title")
    public String getTitle() {
        return title;
    }

    @JsonProperty("title")
    public void setTitle(String title) {
        this.title = title;
    }

    @JsonProperty("state")
    public String getState() {
        return state;
    }

    @JsonProperty("state")
    public void setState(String state) {
        this.state = state;
    }

    @JsonProperty("open")
    public Boolean getOpen() {
        return open;
    }

    @JsonProperty("open")
    public void setOpen(Boolean open) {
        this.open = open;
    }

    @JsonProperty("closed")
    public Boolean getClosed() {
        return closed;
    }

    @JsonProperty("closed")
    public void setClosed(Boolean closed) {
        this.closed = closed;
    }

    @JsonProperty("createdDate")
    public String getCreatedDate() {
        return createdDate;
    }

    @JsonProperty("createdDate")
    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    @JsonProperty("updatedDate")
    public String getUpdatedDate() {
        return updatedDate;
    }

    @JsonProperty("updatedDate")
    public void setUpdatedDate(String updatedDate) {
        this.updatedDate = updatedDate;
    }

    @JsonProperty("fromRef")
    public FromRef getFromRef() {
        return fromRef;
    }

    @JsonProperty("fromRef")
    public void setFromRef(FromRef fromRef) {
        this.fromRef = fromRef;
    }

    @JsonProperty("toRef")
    public ToRef getToRef() {
        return toRef;
    }

    @JsonProperty("toRef")
    public void setToRef(ToRef toRef) {
        this.toRef = toRef;
    }

    @JsonProperty("locked")
    public Boolean getLocked() {
        return locked;
    }

    @JsonProperty("locked")
    public void setLocked(Boolean locked) {
        this.locked = locked;
    }

    @JsonProperty("author")
    public Author getAuthor() {
        return author;
    }

    @JsonProperty("author")
    public void setAuthor(Author author) {
        this.author = author;
    }

    @JsonProperty("reviewers")
    public List<Object> getReviewers() {
        return reviewers;
    }

    @JsonProperty("reviewers")
    public void setReviewers(List<Object> reviewers) {
        this.reviewers = reviewers;
    }

    @JsonProperty("participants")
    public List<Object> getParticipants() {
        return participants;
    }

    @JsonProperty("participants")
    public void setParticipants(List<Object> participants) {
        this.participants = participants;
    }

    @JsonProperty("links")
    public Links getLinks() {
        return links;
    }

    @JsonProperty("links")
    public void setLinks(Links links) {
        this.links = links;
    }

}
