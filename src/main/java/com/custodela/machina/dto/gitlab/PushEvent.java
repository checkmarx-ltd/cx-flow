
package com.custodela.machina.dto.gitlab;

import com.custodela.machina.dto.Event;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.validation.Valid;
import java.util.List;

@JsonIgnoreProperties
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "object_kind",
    "before",
    "after",
    "ref",
    "checkout_sha",
    "user_id",
    "user_name",
    "user_username",
    "user_email",
    "user_avatar",
    "project_id",
    "project",
    "repository",
    "commits",
    "total_commits_count"
})
public class PushEvent extends Event {

    @JsonProperty("object_kind")
    private String objectKind;
    @JsonProperty("before")
    private String before;
    @JsonProperty("after")
    private String after;
    @JsonProperty("ref")
    private String ref;
    @JsonProperty("checkout_sha")
    private String checkoutSha;
    @JsonProperty("user_id")
    private Integer userId;
    @JsonProperty("user_name")
    private String userName;
    @JsonProperty("user_username")
    private String userUsername;
    @JsonProperty("user_email")
    private String userEmail;
    @JsonProperty("user_avatar")
    private String userAvatar;
    @JsonProperty("project_id")
    private Integer projectId;
    @JsonProperty("project")
    @Valid
    private Project project;
    @JsonProperty("repository")
    @Valid
    private Repository repository;
    @JsonProperty("commits")
    @Valid
    private List<Commit> commits = null;
    @JsonProperty("total_commits_count")
    private Integer totalCommitsCount;

    @JsonProperty("object_kind")
    public String getObjectKind() {
        return objectKind;
    }

    @JsonProperty("object_kind")
    public void setObjectKind(String objectKind) {
        this.objectKind = objectKind;
    }

    public PushEvent withObjectKind(String objectKind) {
        this.objectKind = objectKind;
        return this;
    }

    @JsonProperty("before")
    public String getBefore() {
        return before;
    }

    @JsonProperty("before")
    public void setBefore(String before) {
        this.before = before;
    }

    public PushEvent withBefore(String before) {
        this.before = before;
        return this;
    }

    @JsonProperty("after")
    public String getAfter() {
        return after;
    }

    @JsonProperty("after")
    public void setAfter(String after) {
        this.after = after;
    }

    public PushEvent withAfter(String after) {
        this.after = after;
        return this;
    }

    @JsonProperty("ref")
    public String getRef() {
        return ref;
    }

    @JsonProperty("ref")
    public void setRef(String ref) {
        this.ref = ref;
    }

    public PushEvent withRef(String ref) {
        this.ref = ref;
        return this;
    }

    @JsonProperty("checkout_sha")
    public String getCheckoutSha() {
        return checkoutSha;
    }

    @JsonProperty("checkout_sha")
    public void setCheckoutSha(String checkoutSha) {
        this.checkoutSha = checkoutSha;
    }

    public PushEvent withCheckoutSha(String checkoutSha) {
        this.checkoutSha = checkoutSha;
        return this;
    }

    @JsonProperty("user_id")
    public Integer getUserId() {
        return userId;
    }

    @JsonProperty("user_id")
    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public PushEvent withUserId(Integer userId) {
        this.userId = userId;
        return this;
    }

    @JsonProperty("user_name")
    public String getUserName() {
        return userName;
    }

    @JsonProperty("user_name")
    public void setUserName(String userName) {
        this.userName = userName;
    }

    public PushEvent withUserName(String userName) {
        this.userName = userName;
        return this;
    }

    @JsonProperty("user_username")
    public String getUserUsername() {
        return userUsername;
    }

    @JsonProperty("user_username")
    public void setUserUsername(String userUsername) {
        this.userUsername = userUsername;
    }

    public PushEvent withUserUsername(String userUsername) {
        this.userUsername = userUsername;
        return this;
    }

    @JsonProperty("user_email")
    public String getUserEmail() {
        return userEmail;
    }

    @JsonProperty("user_email")
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public PushEvent withUserEmail(String userEmail) {
        this.userEmail = userEmail;
        return this;
    }

    @JsonProperty("user_avatar")
    public String getUserAvatar() {
        return userAvatar;
    }

    @JsonProperty("user_avatar")
    public void setUserAvatar(String userAvatar) {
        this.userAvatar = userAvatar;
    }

    public PushEvent withUserAvatar(String userAvatar) {
        this.userAvatar = userAvatar;
        return this;
    }

    @JsonProperty("project_id")
    public Integer getProjectId() {
        return projectId;
    }

    @JsonProperty("project_id")
    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public PushEvent withProjectId(Integer projectId) {
        this.projectId = projectId;
        return this;
    }

    @JsonProperty("project")
    public Project getProject() {
        return project;
    }

    @JsonProperty("project")
    public void setProject(Project project) {
        this.project = project;
    }

    public PushEvent withProject(Project project) {
        this.project = project;
        return this;
    }

    @JsonProperty("repository")
    public Repository getRepository() {
        return repository;
    }

    @JsonProperty("repository")
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public PushEvent withRepository(Repository repository) {
        this.repository = repository;
        return this;
    }

    @JsonProperty("commits")
    public List<Commit> getCommits() {
        return commits;
    }

    @JsonProperty("commits")
    public void setCommits(List<Commit> commits) {
        this.commits = commits;
    }

    public PushEvent withCommits(List<Commit> commits) {
        this.commits = commits;
        return this;
    }

    @JsonProperty("total_commits_count")
    public Integer getTotalCommitsCount() {
        return totalCommitsCount;
    }

    @JsonProperty("total_commits_count")
    public void setTotalCommitsCount(Integer totalCommitsCount) {
        this.totalCommitsCount = totalCommitsCount;
    }

    public PushEvent withTotalCommitsCount(Integer totalCommitsCount) {
        this.totalCommitsCount = totalCommitsCount;
        return this;
    }

}
