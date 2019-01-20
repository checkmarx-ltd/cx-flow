package com.custodela.machina.dto.github;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.beans.ConstructorProperties;
import java.util.List;

public class IssueRequest {

	@JsonProperty("milestone")
	private int milestone;

	@JsonProperty("assignees")
	private List<String> assignees;

	@JsonProperty("state")
	private String state;

	@JsonProperty("title")
	private String title;

	@JsonProperty("body")
	private String body;

	@JsonProperty("labels")
	private List<String> labels;

    @ConstructorProperties({"milestone", "assignees", "state", "title", "body", "labels"})
    public IssueRequest(int milestone, List<String> assignees, String state, String title, String body, List<String> labels) {
        this.milestone = milestone;
        this.assignees = assignees;
        this.state = state;
        this.title = title;
        this.body = body;
        this.labels = labels;
    }

    public IssueRequest() {
    }

    public static IssueRequestBuilder builder() {
        return new IssueRequestBuilder();
    }

    public int getMilestone() {
        return this.milestone;
    }

    public List<String> getAssignees() {
        return this.assignees;
    }

    public String getState() {
        return this.state;
    }

    public String getTitle() {
        return this.title;
    }

    public String getBody() {
        return this.body;
    }

    public List<String> getLabels() {
        return this.labels;
    }

    public void setMilestone(int milestone) {
        this.milestone = milestone;
    }

    public void setAssignees(List<String> assignees) {
        this.assignees = assignees;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }


    public String toString() {
        return "IssueRequest(milestone=" + this.getMilestone() + ", assignees=" + this.getAssignees() + ", state=" + this.getState() + ", title=" + this.getTitle() + ", body=" + this.getBody() + ", labels=" + this.getLabels() + ")";
    }

    public static class IssueRequestBuilder {
        private int milestone;
        private List<String> assignees;
        private String state;
        private String title;
        private String body;
        private List<String> labels;

        IssueRequestBuilder() {
        }

        public IssueRequest.IssueRequestBuilder milestone(int milestone) {
            this.milestone = milestone;
            return this;
        }

        public IssueRequest.IssueRequestBuilder assignees(List<String> assignees) {
            this.assignees = assignees;
            return this;
        }

        public IssueRequest.IssueRequestBuilder state(String state) {
            this.state = state;
            return this;
        }

        public IssueRequest.IssueRequestBuilder title(String title) {
            this.title = title;
            return this;
        }

        public IssueRequest.IssueRequestBuilder body(String body) {
            this.body = body;
            return this;
        }

        public IssueRequest.IssueRequestBuilder labels(List<String> labels) {
            this.labels = labels;
            return this;
        }

        public IssueRequest build() {
            return new IssueRequest(milestone, assignees, state, title, body, labels);
        }

        public String toString() {
            return "IssueRequest.IssueRequestBuilder(milestone=" + this.milestone + ", assignees=" + this.assignees + ", state=" + this.state + ", title=" + this.title + ", body=" + this.body + ", labels=" + this.labels + ")";
        }
    }
}
