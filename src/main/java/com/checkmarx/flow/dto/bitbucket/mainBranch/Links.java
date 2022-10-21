
package com.checkmarx.flow.dto.bitbucket.mainBranch;

import java.util.HashMap;
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
    "self",
    "html",
    "avatar",
    "pullrequests",
    "commits",
    "forks",
    "watchers",
    "branches",
    "tags",
    "downloads",
    "source",
    "clone",
    "hooks"
})
@Generated("jsonschema2pojo")
public class Links {

    @JsonProperty("self")
    private Self self;
    @JsonProperty("html")
    private Html html;
    @JsonProperty("avatar")
    private Avatar avatar;
    @JsonProperty("pullrequests")
    private Pullrequests pullrequests;
    @JsonProperty("commits")
    private Commits commits;
    @JsonProperty("forks")
    private Forks forks;
    @JsonProperty("watchers")
    private Watchers watchers;
    @JsonProperty("branches")
    private Branches branches;
    @JsonProperty("tags")
    private Tags tags;
    @JsonProperty("downloads")
    private Downloads downloads;
    @JsonProperty("source")
    private Source source;
    @JsonProperty("clone")
    private List<Clone> clone = null;
    @JsonProperty("hooks")
    private Hooks hooks;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("self")
    public Self getSelf() {
        return self;
    }

    @JsonProperty("self")
    public void setSelf(Self self) {
        this.self = self;
    }

    @JsonProperty("html")
    public Html getHtml() {
        return html;
    }

    @JsonProperty("html")
    public void setHtml(Html html) {
        this.html = html;
    }

    @JsonProperty("avatar")
    public Avatar getAvatar() {
        return avatar;
    }

    @JsonProperty("avatar")
    public void setAvatar(Avatar avatar) {
        this.avatar = avatar;
    }

    @JsonProperty("pullrequests")
    public Pullrequests getPullrequests() {
        return pullrequests;
    }

    @JsonProperty("pullrequests")
    public void setPullrequests(Pullrequests pullrequests) {
        this.pullrequests = pullrequests;
    }

    @JsonProperty("commits")
    public Commits getCommits() {
        return commits;
    }

    @JsonProperty("commits")
    public void setCommits(Commits commits) {
        this.commits = commits;
    }

    @JsonProperty("forks")
    public Forks getForks() {
        return forks;
    }

    @JsonProperty("forks")
    public void setForks(Forks forks) {
        this.forks = forks;
    }

    @JsonProperty("watchers")
    public Watchers getWatchers() {
        return watchers;
    }

    @JsonProperty("watchers")
    public void setWatchers(Watchers watchers) {
        this.watchers = watchers;
    }

    @JsonProperty("branches")
    public Branches getBranches() {
        return branches;
    }

    @JsonProperty("branches")
    public void setBranches(Branches branches) {
        this.branches = branches;
    }

    @JsonProperty("tags")
    public Tags getTags() {
        return tags;
    }

    @JsonProperty("tags")
    public void setTags(Tags tags) {
        this.tags = tags;
    }

    @JsonProperty("downloads")
    public Downloads getDownloads() {
        return downloads;
    }

    @JsonProperty("downloads")
    public void setDownloads(Downloads downloads) {
        this.downloads = downloads;
    }

    @JsonProperty("source")
    public Source getSource() {
        return source;
    }

    @JsonProperty("source")
    public void setSource(Source source) {
        this.source = source;
    }

    @JsonProperty("clone")
    public List<Clone> getClone() {
        return clone;
    }

    @JsonProperty("clone")
    public void setClone(List<Clone> clone) {
        this.clone = clone;
    }

    @JsonProperty("hooks")
    public Hooks getHooks() {
        return hooks;
    }

    @JsonProperty("hooks")
    public void setHooks(Hooks hooks) {
        this.hooks = hooks;
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
