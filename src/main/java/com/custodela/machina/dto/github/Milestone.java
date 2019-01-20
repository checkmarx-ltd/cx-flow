package com.custodela.machina.dto.github;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.beans.ConstructorProperties;

public class Milestone{

	@JsonProperty("creator")
	private Creator creator;

	@JsonProperty("closed_at")
	private String closedAt;

	@JsonProperty("description")
	private String description;

	@JsonProperty("created_at")
	private String createdAt;

	@JsonProperty("title")
	private String title;

	@JsonProperty("closed_issues")
	private int closedIssues;

	@JsonProperty("url")
	private String url;

	@JsonProperty("due_on")
	private String dueOn;

	@JsonProperty("labels_url")
	private String labelsUrl;

	@JsonProperty("number")
	private int number;

	@JsonProperty("updated_at")
	private String updatedAt;

	@JsonProperty("html_url")
	private String htmlUrl;

	@JsonProperty("id")
	private int id;

	@JsonProperty("state")
	private String state;

	@JsonProperty("open_issues")
	private int openIssues;

    @ConstructorProperties({"creator", "closedAt", "description", "createdAt", "title", "closedIssues", "url", "dueOn", "labelsUrl", "number", "updatedAt", "htmlUrl", "id", "state", "openIssues"})
    public Milestone(Creator creator, String closedAt, String description, String createdAt, String title, int closedIssues, String url, String dueOn, String labelsUrl, int number, String updatedAt, String htmlUrl, int id, String state, int openIssues) {
        this.creator = creator;
        this.closedAt = closedAt;
        this.description = description;
        this.createdAt = createdAt;
        this.title = title;
        this.closedIssues = closedIssues;
        this.url = url;
        this.dueOn = dueOn;
        this.labelsUrl = labelsUrl;
        this.number = number;
        this.updatedAt = updatedAt;
        this.htmlUrl = htmlUrl;
        this.id = id;
        this.state = state;
        this.openIssues = openIssues;
    }

    public Milestone() {
    }

    public static MilestoneBuilder builder() {
        return new MilestoneBuilder();
    }

    public Creator getCreator() {
        return this.creator;
    }

    public String getClosedAt() {
        return this.closedAt;
    }

    public String getDescription() {
        return this.description;
    }

    public String getCreatedAt() {
        return this.createdAt;
    }

    public String getTitle() {
        return this.title;
    }

    public int getClosedIssues() {
        return this.closedIssues;
    }

    public String getUrl() {
        return this.url;
    }

    public String getDueOn() {
        return this.dueOn;
    }

    public String getLabelsUrl() {
        return this.labelsUrl;
    }

    public int getNumber() {
        return this.number;
    }

    public String getUpdatedAt() {
        return this.updatedAt;
    }

    public String getHtmlUrl() {
        return this.htmlUrl;
    }

    public int getId() {
        return this.id;
    }

    public String getState() {
        return this.state;
    }

    public int getOpenIssues() {
        return this.openIssues;
    }

    public void setCreator(Creator creator) {
        this.creator = creator;
    }

    public void setClosedAt(String closedAt) {
        this.closedAt = closedAt;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setClosedIssues(int closedIssues) {
        this.closedIssues = closedIssues;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setDueOn(String dueOn) {
        this.dueOn = dueOn;
    }

    public void setLabelsUrl(String labelsUrl) {
        this.labelsUrl = labelsUrl;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setOpenIssues(int openIssues) {
        this.openIssues = openIssues;
    }

    public String toString() {
        return "Milestone(creator=" + this.getCreator() + ", closedAt=" + this.getClosedAt() + ", description=" + this.getDescription() + ", createdAt=" + this.getCreatedAt() + ", title=" + this.getTitle() + ", closedIssues=" + this.getClosedIssues() + ", url=" + this.getUrl() + ", dueOn=" + this.getDueOn() + ", labelsUrl=" + this.getLabelsUrl() + ", number=" + this.getNumber() + ", updatedAt=" + this.getUpdatedAt() + ", htmlUrl=" + this.getHtmlUrl() + ", id=" + this.getId() + ", state=" + this.getState() + ", openIssues=" + this.getOpenIssues() + ")";
    }

    public static class MilestoneBuilder {
        private Creator creator;
        private String closedAt;
        private String description;
        private String createdAt;
        private String title;
        private int closedIssues;
        private String url;
        private String dueOn;
        private String labelsUrl;
        private int number;
        private String updatedAt;
        private String htmlUrl;
        private int id;
        private String state;
        private int openIssues;

        MilestoneBuilder() {
        }

        public Milestone.MilestoneBuilder creator(Creator creator) {
            this.creator = creator;
            return this;
        }

        public Milestone.MilestoneBuilder closedAt(String closedAt) {
            this.closedAt = closedAt;
            return this;
        }

        public Milestone.MilestoneBuilder description(String description) {
            this.description = description;
            return this;
        }

        public Milestone.MilestoneBuilder createdAt(String createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Milestone.MilestoneBuilder title(String title) {
            this.title = title;
            return this;
        }

        public Milestone.MilestoneBuilder closedIssues(int closedIssues) {
            this.closedIssues = closedIssues;
            return this;
        }

        public Milestone.MilestoneBuilder url(String url) {
            this.url = url;
            return this;
        }

        public Milestone.MilestoneBuilder dueOn(String dueOn) {
            this.dueOn = dueOn;
            return this;
        }

        public Milestone.MilestoneBuilder labelsUrl(String labelsUrl) {
            this.labelsUrl = labelsUrl;
            return this;
        }

        public Milestone.MilestoneBuilder number(int number) {
            this.number = number;
            return this;
        }

        public Milestone.MilestoneBuilder updatedAt(String updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Milestone.MilestoneBuilder htmlUrl(String htmlUrl) {
            this.htmlUrl = htmlUrl;
            return this;
        }

        public Milestone.MilestoneBuilder id(int id) {
            this.id = id;
            return this;
        }

        public Milestone.MilestoneBuilder state(String state) {
            this.state = state;
            return this;
        }

        public Milestone.MilestoneBuilder openIssues(int openIssues) {
            this.openIssues = openIssues;
            return this;
        }

        public Milestone build() {
            return new Milestone(creator, closedAt, description, createdAt, title, closedIssues, url, dueOn, labelsUrl, number, updatedAt, htmlUrl, id, state, openIssues);
        }

        public String toString() {
            return "Milestone.MilestoneBuilder(creator=" + this.creator + ", closedAt=" + this.closedAt + ", description=" + this.description + ", createdAt=" + this.createdAt + ", title=" + this.title + ", closedIssues=" + this.closedIssues + ", url=" + this.url + ", dueOn=" + this.dueOn + ", labelsUrl=" + this.labelsUrl + ", number=" + this.number + ", updatedAt=" + this.updatedAt + ", htmlUrl=" + this.htmlUrl + ", id=" + this.id + ", state=" + this.state + ", openIssues=" + this.openIssues + ")";
        }
    }
}
