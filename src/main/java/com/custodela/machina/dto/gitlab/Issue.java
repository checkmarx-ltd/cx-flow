package com.custodela.machina.dto.gitlab;

import com.custodela.machina.dto.RepoIssue;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.json.JSONException;
import org.json.JSONObject;

import javax.validation.Valid;
import java.util.List;

public class Issue extends RepoIssue {

    @JsonProperty("id")
    public Integer id;
    @JsonProperty("iid")
    public Integer iid;
    @JsonProperty("project_id")
    public Integer projectId;
    @JsonProperty("title")
    public String title;
    @JsonProperty("description")
    public String description;
    @JsonProperty("state")
    public String state;
    @JsonProperty("created_at")
    public String createdAt;
    @JsonProperty("updated_at")
    public String updatedAt;
    @JsonProperty("closed_at")
    public Object closedAt;
    @JsonProperty("closed_by")
    public Object closedBy;
    @JsonProperty("labels")
    public List<String> labels = null;
    @JsonProperty("milestone")
    public Object milestone;
    @JsonProperty("assignees")
    public List<Object> assignees = null;
    @JsonProperty("author")
    public Author author;
    @JsonProperty("assignee")
    public Object assignee;
    @JsonProperty("user_notes_count")
    public Integer userNotesCount;
    @JsonProperty("upvotes")
    public Integer upvotes;
    @JsonProperty("downvotes")
    public Integer downvotes;
    @JsonProperty("due_date")
    public Object dueDate;
    @JsonProperty("confidential")
    public Boolean confidential;
    @JsonProperty("discussion_locked")
    public Object discussionLocked;
    @JsonProperty("web_url")
    public String webUrl;
    @JsonProperty("time_stats")
    @Valid
    public TimeStats timeStats;

    public Integer getId() {
        return this.id;
    }

    public Integer getIid() {
        return this.iid;
    }

    public Integer getProjectId() {
        return this.projectId;
    }

    @Override
    public String getTitle() {
        return this.title;
    }

    public String getDescription() {
        return this.description;
    }

    public String getState() {
        return this.state;
    }

    public String getCreatedAt() {
        return this.createdAt;
    }

    public String getUpdatedAt() {
        return this.updatedAt;
    }

    public Object getClosedAt() {
        return this.closedAt;
    }

    public Object getClosedBy() {
        return this.closedBy;
    }

    public List<String> getLabels() {
        return this.labels;
    }

    public Object getMilestone() {
        return this.milestone;
    }

    public List<Object> getAssignees() {
        return this.assignees;
    }

    public Author getAuthor() {
        return this.author;
    }

    public Object getAssignee() {
        return this.assignee;
    }

    public Integer getUserNotesCount() {
        return this.userNotesCount;
    }

    public Integer getUpvotes() {
        return this.upvotes;
    }

    public Integer getDownvotes() {
        return this.downvotes;
    }

    public Object getDueDate() {
        return this.dueDate;
    }

    public Boolean getConfidential() {
        return this.confidential;
    }

    public Object getDiscussionLocked() {
        return this.discussionLocked;
    }

    public String getWebUrl() {
        return this.webUrl;
    }

    public @Valid TimeStats getTimeStats() {
        return this.timeStats;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setIid(Integer iid) {
        this.iid = iid;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setClosedAt(Object closedAt) {
        this.closedAt = closedAt;
    }

    public void setClosedBy(Object closedBy) {
        this.closedBy = closedBy;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public void setMilestone(Object milestone) {
        this.milestone = milestone;
    }

    public void setAssignees(List<Object> assignees) {
        this.assignees = assignees;
    }

    public void setAuthor(Author author) {
        this.author = author;
    }

    public void setAssignee(Object assignee) {
        this.assignee = assignee;
    }

    public void setUserNotesCount(Integer userNotesCount) {
        this.userNotesCount = userNotesCount;
    }

    public void setUpvotes(Integer upvotes) {
        this.upvotes = upvotes;
    }

    public void setDownvotes(Integer downvotes) {
        this.downvotes = downvotes;
    }

    public void setDueDate(Object dueDate) {
        this.dueDate = dueDate;
    }

    public void setConfidential(Boolean confidential) {
        this.confidential = confidential;
    }

    public void setDiscussionLocked(Object discussionLocked) {
        this.discussionLocked = discussionLocked;
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }

    public void setTimeStats(@Valid TimeStats timeStats) {
        this.timeStats = timeStats;
    }

    public class Author {

        @JsonProperty("id")
        public Integer id;
        @JsonProperty("name")
        public String name;
        @JsonProperty("username")
        public String username;
        @JsonProperty("state")
        public String state;
        @JsonProperty("avatar_url")
        public String avatarUrl;
        @JsonProperty("web_url")
        public String webUrl;

        public Author() {
        }

        public Integer getId() {
            return this.id;
        }

        public String getName() {
            return this.name;
        }

        public String getUsername() {
            return this.username;
        }

        public String getState() {
            return this.state;
        }

        public String getAvatarUrl() {
            return this.avatarUrl;
        }

        public String getWebUrl() {
            return this.webUrl;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public void setState(String state) {
            this.state = state;
        }

        public void setAvatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
        }

        public void setWebUrl(String webUrl) {
            this.webUrl = webUrl;
        }

        public boolean equals(final Object o) {
            if (o == this) return true;
            if (!(o instanceof Author)) return false;
            final Author other = (Author) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            final Object this$name = this.getName();
            final Object other$name = other.getName();
            if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
            final Object this$username = this.getUsername();
            final Object other$username = other.getUsername();
            if (this$username == null ? other$username != null : !this$username.equals(other$username)) return false;
            final Object this$state = this.getState();
            final Object other$state = other.getState();
            if (this$state == null ? other$state != null : !this$state.equals(other$state)) return false;
            final Object this$avatarUrl = this.getAvatarUrl();
            final Object other$avatarUrl = other.getAvatarUrl();
            if (this$avatarUrl == null ? other$avatarUrl != null : !this$avatarUrl.equals(other$avatarUrl))
                return false;
            final Object this$webUrl = this.getWebUrl();
            final Object other$webUrl = other.getWebUrl();
            if (this$webUrl == null ? other$webUrl != null : !this$webUrl.equals(other$webUrl)) return false;
            return true;
        }

        protected boolean canEqual(final Object other) {
            return other instanceof Author;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            final Object $name = this.getName();
            result = result * PRIME + ($name == null ? 43 : $name.hashCode());
            final Object $username = this.getUsername();
            result = result * PRIME + ($username == null ? 43 : $username.hashCode());
            final Object $state = this.getState();
            result = result * PRIME + ($state == null ? 43 : $state.hashCode());
            final Object $avatarUrl = this.getAvatarUrl();
            result = result * PRIME + ($avatarUrl == null ? 43 : $avatarUrl.hashCode());
            final Object $webUrl = this.getWebUrl();
            result = result * PRIME + ($webUrl == null ? 43 : $webUrl.hashCode());
            return result;
        }

        public String toString() {
            return "Issue.Author(id=" + this.getId() + ", name=" + this.getName() + ", username=" + this.getUsername() + ", state=" + this.getState() + ", avatarUrl=" + this.getAvatarUrl() + ", webUrl=" + this.getWebUrl() + ")";
        }
    }
    public class TimeStats {

        @JsonProperty("time_estimate")
        public Integer timeEstimate;
        @JsonProperty("total_time_spent")
        public Integer totalTimeSpent;
        @JsonProperty("human_time_estimate")
        public Object humanTimeEstimate;
        @JsonProperty("human_total_time_spent")
        public Object humanTotalTimeSpent;

        public TimeStats() {
        }

        public Integer getTimeEstimate() {
            return this.timeEstimate;
        }

        public Integer getTotalTimeSpent() {
            return this.totalTimeSpent;
        }

        public Object getHumanTimeEstimate() {
            return this.humanTimeEstimate;
        }

        public Object getHumanTotalTimeSpent() {
            return this.humanTotalTimeSpent;
        }

        public void setTimeEstimate(Integer timeEstimate) {
            this.timeEstimate = timeEstimate;
        }

        public void setTotalTimeSpent(Integer totalTimeSpent) {
            this.totalTimeSpent = totalTimeSpent;
        }

        public void setHumanTimeEstimate(Object humanTimeEstimate) {
            this.humanTimeEstimate = humanTimeEstimate;
        }

        public void setHumanTotalTimeSpent(Object humanTotalTimeSpent) {
            this.humanTotalTimeSpent = humanTotalTimeSpent;
        }
        public String toString() {
            return "Issue.TimeStats(timeEstimate=" + this.getTimeEstimate() + ", totalTimeSpent=" + this.getTotalTimeSpent() + ", humanTimeEstimate=" + this.getHumanTimeEstimate() + ", humanTotalTimeSpent=" + this.getHumanTotalTimeSpent() + ")";
        }
    }
}
