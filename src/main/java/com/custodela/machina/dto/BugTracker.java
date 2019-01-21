package com.custodela.machina.dto;

import java.beans.ConstructorProperties;
import java.util.List;
import java.util.Map;


public class BugTracker {
    private Type type;
    private String projectKey;
    private String issueType;
    private String openTransition;
    private String closeTransition;
    private String assignee;
    private List<String> openStatus;
    private List<String> closedStatus;
    private String closeTransitionField;
    private String closeTransitionValue;
    private Map<String, String> priorities;
    private List<Field> fields; //Field mappings in JIRA

    @ConstructorProperties({"type", "projectKey", "issueType", "openTransition", "closeTransition", "assignee", "openStatus", "closedStatus", "closeTransitionField", "closeTransitionValue", "priorities", "fields"})
    BugTracker(Type type, String projectKey, String issueType, String openTransition, String closeTransition, String assignee, List<String> openStatus, List<String> closedStatus, String closeTransitionField, String closeTransitionValue, Map<String, String> priorities, List<Field> fields) {
        this.type = type;
        this.projectKey = projectKey;
        this.issueType = issueType;
        this.openTransition = openTransition;
        this.closeTransition = closeTransition;
        this.assignee = assignee;
        this.openStatus = openStatus;
        this.closedStatus = closedStatus;
        this.closeTransitionField = closeTransitionField;
        this.closeTransitionValue = closeTransitionValue;
        this.priorities = priorities;
        this.fields = fields;
    }

    public BugTracker(BugTracker other) {
        this.type = other.type;
        this.projectKey = other.projectKey;
        this.issueType = other.issueType;
        this.openTransition = other.openTransition;
        this.closeTransition = other.closeTransition;
        this.assignee = other.assignee;
        this.openStatus = other.openStatus;
        this.closedStatus = other.closedStatus;
        this.closeTransitionField = other.closeTransitionField;
        this.closeTransitionValue = other.closeTransitionValue;
        this.priorities = other.priorities;
        this.fields = other.fields;
    }

    public static BugTrackerBuilder builder() {
        return new BugTrackerBuilder();
    }

    public Type getType() {
        return this.type;
    }

    public String getProjectKey() {
        return this.projectKey;
    }

    public String getIssueType() {
        return this.issueType;
    }

    public String getOpenTransition() {
        return this.openTransition;
    }

    public String getCloseTransition() {
        return this.closeTransition;
    }

    public String getAssignee() {
        return this.assignee;
    }

    public List<String> getOpenStatus() {
        return this.openStatus;
    }

    public List<String> getClosedStatus() {
        return this.closedStatus;
    }

    public String getCloseTransitionField() {
        return this.closeTransitionField;
    }

    public String getCloseTransitionValue() {
        return this.closeTransitionValue;
    }

    public Map<String, String> getPriorities() {
        return this.priorities;
    }

    public List<Field> getFields() {
        return this.fields;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setProjectKey(String projectKey) {
        this.projectKey = projectKey;
    }

    public void setIssueType(String issueType) {
        this.issueType = issueType;
    }

    public void setOpenTransition(String openTransition) {
        this.openTransition = openTransition;
    }

    public void setCloseTransition(String closeTransition) {
        this.closeTransition = closeTransition;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public void setOpenStatus(List<String> openStatus) {
        this.openStatus = openStatus;
    }

    public void setClosedStatus(List<String> closedStatus) {
        this.closedStatus = closedStatus;
    }

    public void setCloseTransitionField(String closeTransitionField) {
        this.closeTransitionField = closeTransitionField;
    }

    public void setCloseTransitionValue(String closeTransitionValue) {
        this.closeTransitionValue = closeTransitionValue;
    }

    public void setPriorities(Map<String, String> priorities) {
        this.priorities = priorities;
    }

    public void setFields(List<Field> fields) {
        this.fields = fields;
    }

    public String toString() {
        return "BugTracker(type=" + this.getType() + ", projectKey=" + this.getProjectKey() + ", issueType=" + this.getIssueType() + ", openTransition=" + this.getOpenTransition() + ", closeTransition=" + this.getCloseTransition() + ", assignee=" + this.getAssignee() + ", openStatus=" + this.getOpenStatus() + ", closedStatus=" + this.getClosedStatus() + ", closeTransitionField=" + this.getCloseTransitionField() + ", closeTransitionValue=" + this.getCloseTransitionValue() + ", priorities=" + this.getPriorities() + ", fields=" + this.getFields() + ")";
    }

    public enum Type {
        JIRA("JIRA"),
        BITBUCKET("BITBUCKET"),
        BITBUCKETCOMMIT("BITBUCKETCOMMIT"),
        BITBUCKETPULL("BITBUCKETPULL"),
        BITBUCKETSERVERPULL("BITBUCKETSERVERPULL"),
        GITHUB("GITHUB"),
        GITHUBPULL("GITHUBPULL"),
        GITHUBCOMMIT("GITHUBCOMMIT"),
        GITLAB("GITLAB"),
        GITLABCOMMIT("GITLABCOMMIT"),
        GITLABMERGE("GITLABMERGE"),
        EMAIL("EMAIL"),
        NONE("NONE");

        private String type;

        Type(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    public static class BugTrackerBuilder {
        private Type type;
        private String projectKey;
        private String issueType;
        private String openTransition;
        private String closeTransition;
        private String assignee;
        private List<String> openStatus;
        private List<String> closedStatus;
        private String closeTransitionField;
        private String closeTransitionValue;
        private Map<String, String> priorities;
        private List<Field> fields;

        BugTrackerBuilder() {
        }

        public BugTracker.BugTrackerBuilder type(Type type) {
            this.type = type;
            return this;
        }

        public BugTracker.BugTrackerBuilder projectKey(String projectKey) {
            this.projectKey = projectKey;
            return this;
        }

        public BugTracker.BugTrackerBuilder issueType(String issueType) {
            this.issueType = issueType;
            return this;
        }

        public BugTracker.BugTrackerBuilder openTransition(String openTransition) {
            this.openTransition = openTransition;
            return this;
        }

        public BugTracker.BugTrackerBuilder closeTransition(String closeTransition) {
            this.closeTransition = closeTransition;
            return this;
        }

        public BugTracker.BugTrackerBuilder assignee(String assignee) {
            this.assignee = assignee;
            return this;
        }

        public BugTracker.BugTrackerBuilder openStatus(List<String> openStatus) {
            this.openStatus = openStatus;
            return this;
        }

        public BugTracker.BugTrackerBuilder closedStatus(List<String> closedStatus) {
            this.closedStatus = closedStatus;
            return this;
        }

        public BugTracker.BugTrackerBuilder closeTransitionField(String closeTransitionField) {
            this.closeTransitionField = closeTransitionField;
            return this;
        }

        public BugTracker.BugTrackerBuilder closeTransitionValue(String closeTransitionValue) {
            this.closeTransitionValue = closeTransitionValue;
            return this;
        }

        public BugTracker.BugTrackerBuilder priorities(Map<String, String> priorities) {
            this.priorities = priorities;
            return this;
        }

        public BugTracker.BugTrackerBuilder fields(List<Field> fields) {
            this.fields = fields;
            return this;
        }

        public BugTracker build() {
            return new BugTracker(type, projectKey, issueType, openTransition, closeTransition, assignee, openStatus, closedStatus, closeTransitionField, closeTransitionValue, priorities, fields);
        }

        public String toString() {
            return "BugTracker.BugTrackerBuilder(type=" + this.type + ", projectKey=" + this.projectKey + ", issueType=" + this.issueType + ", openTransition=" + this.openTransition + ", closeTransition=" + this.closeTransition + ", assignee=" + this.assignee + ", openStatus=" + this.openStatus + ", closedStatus=" + this.closedStatus + ", closeTransitionField=" + this.closeTransitionField + ", closeTransitionValue=" + this.closeTransitionValue + ", priorities=" + this.priorities + ", fields=" + this.fields + ")";
        }
    }
}
