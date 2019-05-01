
package com.checkmarx.flow.dto.gitlab;

import com.fasterxml.jackson.annotation.*;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "self",
    "issues",
    "merge_requests",
    "repo_branches",
    "labels",
    "events",
    "members"
})
public class Links {

    @JsonProperty("self")
    private String self;
    @JsonProperty("issues")
    private String issues;
    @JsonProperty("merge_requests")
    private String mergeRequests;
    @JsonProperty("repo_branches")
    private String repoBranches;
    @JsonProperty("labels")
    private String labels;
    @JsonProperty("events")
    private String events;
    @JsonProperty("members")
    private String members;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("self")
    public String getSelf() {
        return self;
    }

    @JsonProperty("self")
    public void setSelf(String self) {
        this.self = self;
    }

    @JsonProperty("issues")
    public String getIssues() {
        return issues;
    }

    @JsonProperty("issues")
    public void setIssues(String issues) {
        this.issues = issues;
    }

    @JsonProperty("merge_requests")
    public String getMergeRequests() {
        return mergeRequests;
    }

    @JsonProperty("merge_requests")
    public void setMergeRequests(String mergeRequests) {
        this.mergeRequests = mergeRequests;
    }

    @JsonProperty("repo_branches")
    public String getRepoBranches() {
        return repoBranches;
    }

    @JsonProperty("repo_branches")
    public void setRepoBranches(String repoBranches) {
        this.repoBranches = repoBranches;
    }

    @JsonProperty("labels")
    public String getLabels() {
        return labels;
    }

    @JsonProperty("labels")
    public void setLabels(String labels) {
        this.labels = labels;
    }

    @JsonProperty("events")
    public String getEvents() {
        return events;
    }

    @JsonProperty("events")
    public void setEvents(String events) {
        this.events = events;
    }

    @JsonProperty("members")
    public String getMembers() {
        return members;
    }

    @JsonProperty("members")
    public void setMembers(String members) {
        this.members = members;
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
