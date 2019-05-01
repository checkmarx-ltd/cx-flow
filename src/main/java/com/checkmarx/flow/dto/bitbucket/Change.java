
package com.checkmarx.flow.dto.bitbucket;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "forced",
    "old",
    "links",
    "truncated",
    "commits",
    "created",
    "closed",
    "new"
})
public class Change {

    @JsonProperty("forced")
    private Boolean forced;
    @JsonProperty("old")
    private Old old;
    @JsonProperty("links")
    private Links links;
    @JsonProperty("truncated")
    private Boolean truncated;
    @JsonProperty("commits")
    private List<Commit> commits = null;
    @JsonProperty("created")
    private Boolean created;
    @JsonProperty("closed")
    private Boolean closed;
    @JsonProperty("new")
    private New _new;

    @JsonProperty("forced")
    public Boolean getForced() {
        return forced;
    }

    @JsonProperty("forced")
    public void setForced(Boolean forced) {
        this.forced = forced;
    }

    @JsonProperty("old")
    public Old getOld() {
        return old;
    }

    @JsonProperty("old")
    public void setOld(Old old) {
        this.old = old;
    }

    @JsonProperty("links")
    public Links getLinks() {
        return links;
    }

    @JsonProperty("links")
    public void setLinks(Links links) {
        this.links = links;
    }

    @JsonProperty("truncated")
    public Boolean getTruncated() {
        return truncated;
    }

    @JsonProperty("truncated")
    public void setTruncated(Boolean truncated) {
        this.truncated = truncated;
    }

    @JsonProperty("commits")
    public List<Commit> getCommits() {
        return commits;
    }

    @JsonProperty("commits")
    public void setCommits(List<Commit> commits) {
        this.commits = commits;
    }

    @JsonProperty("created")
    public Boolean getCreated() {
        return created;
    }

    @JsonProperty("created")
    public void setCreated(Boolean created) {
        this.created = created;
    }

    @JsonProperty("closed")
    public Boolean getClosed() {
        return closed;
    }

    @JsonProperty("closed")
    public void setClosed(Boolean closed) {
        this.closed = closed;
    }

    @JsonProperty("new")
    public New getNew() {
        return _new;
    }

    @JsonProperty("new")
    public void setNew(New _new) {
        this._new = _new;
    }

}
