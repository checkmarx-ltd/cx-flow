
package com.custodela.machina.dto.bitbucket;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "type",
    "description",
    "links",
    "title",
    "close_source_branch",
    "reviewers",
    "id",
    "destination",
    "comment_count",
    "summary",
    "source",
    "state",
    "author",
    "created_on",
    "participants",
    "reason",
    "updated_on",
    "merge_commit",
    "closed_by",
    "task_count"
})
public class Pullrequest {

    @JsonProperty("type")
    private String type;
    @JsonProperty("description")
    private String description;
    @JsonProperty("links")
    private Links links;
    @JsonProperty("title")
    private String title;
    @JsonProperty("close_source_branch")
    private Boolean closeSourceBranch;
    @JsonProperty("reviewers")
    private List<Object> reviewers = null;
    @JsonProperty("id")
    private Integer id;
    @JsonProperty("destination")
    private Destination destination;
    @JsonProperty("comment_count")
    private Integer commentCount;
    @JsonProperty("summary")
    private Summary summary;
    @JsonProperty("source")
    private Source source;
    @JsonProperty("state")
    private String state;
    @JsonProperty("author")
    private Author author;
    @JsonProperty("created_on")
    private String createdOn;
    @JsonProperty("participants")
    private List<Object> participants = null;
    @JsonProperty("reason")
    private String reason;
    @JsonProperty("updated_on")
    private String updatedOn;
    @JsonProperty("merge_commit")
    private MergeCommit mergeCommit;
    @JsonProperty("closed_by")
    private ClosedBy closedBy;
    @JsonProperty("task_count")
    private Integer taskCount;

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("type")
    public void setType(String type) {
        this.type = type;
    }

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty("links")
    public Links getLinks() {
        return links;
    }

    @JsonProperty("links")
    public void setLinks(Links links) {
        this.links = links;
    }

    @JsonProperty("title")
    public String getTitle() {
        return title;
    }

    @JsonProperty("title")
    public void setTitle(String title) {
        this.title = title;
    }

    @JsonProperty("close_source_branch")
    public Boolean getCloseSourceBranch() {
        return closeSourceBranch;
    }

    @JsonProperty("close_source_branch")
    public void setCloseSourceBranch(Boolean closeSourceBranch) {
        this.closeSourceBranch = closeSourceBranch;
    }

    @JsonProperty("reviewers")
    public List<Object> getReviewers() {
        return reviewers;
    }

    @JsonProperty("reviewers")
    public void setReviewers(List<Object> reviewers) {
        this.reviewers = reviewers;
    }

    @JsonProperty("id")
    public Integer getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(Integer id) {
        this.id = id;
    }

    @JsonProperty("destination")
    public Destination getDestination() {
        return destination;
    }

    @JsonProperty("destination")
    public void setDestination(Destination destination) {
        this.destination = destination;
    }

    @JsonProperty("comment_count")
    public Integer getCommentCount() {
        return commentCount;
    }

    @JsonProperty("comment_count")
    public void setCommentCount(Integer commentCount) {
        this.commentCount = commentCount;
    }

    @JsonProperty("summary")
    public Summary getSummary() {
        return summary;
    }

    @JsonProperty("summary")
    public void setSummary(Summary summary) {
        this.summary = summary;
    }

    @JsonProperty("source")
    public Source getSource() {
        return source;
    }

    @JsonProperty("source")
    public void setSource(Source source) {
        this.source = source;
    }

    @JsonProperty("state")
    public String getState() {
        return state;
    }

    @JsonProperty("state")
    public void setState(String state) {
        this.state = state;
    }

    @JsonProperty("author")
    public Author getAuthor() {
        return author;
    }

    @JsonProperty("author")
    public void setAuthor(Author author) {
        this.author = author;
    }

    @JsonProperty("created_on")
    public String getCreatedOn() {
        return createdOn;
    }

    @JsonProperty("created_on")
    public void setCreatedOn(String createdOn) {
        this.createdOn = createdOn;
    }

    @JsonProperty("participants")
    public List<Object> getParticipants() {
        return participants;
    }

    @JsonProperty("participants")
    public void setParticipants(List<Object> participants) {
        this.participants = participants;
    }

    @JsonProperty("reason")
    public String getReason() {
        return reason;
    }

    @JsonProperty("reason")
    public void setReason(String reason) {
        this.reason = reason;
    }

    @JsonProperty("updated_on")
    public String getUpdatedOn() {
        return updatedOn;
    }

    @JsonProperty("updated_on")
    public void setUpdatedOn(String updatedOn) {
        this.updatedOn = updatedOn;
    }

    @JsonProperty("merge_commit")
    public MergeCommit getMergeCommit() {
        return mergeCommit;
    }

    @JsonProperty("merge_commit")
    public void setMergeCommit(MergeCommit mergeCommit) {
        this.mergeCommit = mergeCommit;
    }

    @JsonProperty("closed_by")
    public ClosedBy getClosedBy() {
        return closedBy;
    }

    @JsonProperty("closed_by")
    public void setClosedBy(ClosedBy closedBy) {
        this.closedBy = closedBy;
    }

    @JsonProperty("task_count")
    public Integer getTaskCount() {
        return taskCount;
    }

    @JsonProperty("task_count")
    public void setTaskCount(Integer taskCount) {
        this.taskCount = taskCount;
    }

}
