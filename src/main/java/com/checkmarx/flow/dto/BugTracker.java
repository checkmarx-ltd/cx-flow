package com.checkmarx.flow.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.beans.ConstructorProperties;
import java.util.List;
import java.util.Map;

/**
 * Issue tracker properties stored inside {@link ScanRequest}.
 */
@Data
@Builder
@NoArgsConstructor
public class BugTracker {
    private Type type;

    /**
     * Used when {@link  #type} is set to {@link BugTracker.Type#CUSTOM}.
     */
    private String customBean;

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

    @ConstructorProperties({"type", "customBean", "projectKey", "issueType", "openTransition", "closeTransition", "assignee", "openStatus", "closedStatus", "closeTransitionField", "closeTransitionValue", "priorities", "fields"})
    BugTracker(Type type, String customBean, String projectKey, String issueType, String openTransition, String closeTransition, String assignee, List<String> openStatus, List<String> closedStatus, String closeTransitionField, String closeTransitionValue, Map<String, String> priorities, List<Field> fields) {
        this.type = type;
        this.customBean = customBean;
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
        this.customBean = other.customBean;
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

    public enum Type {
        JIRA("JIRA"),
        BITBUCKETCOMMIT("BITBUCKETCOMMIT"),
        BITBUCKETPULL("BITBUCKETPULL"),
        BITBUCKETSERVERPULL("BITBUCKETSERVERPULL"),
        bitbucketserverpull("bitbucketserverpull"),
        ADOPULL("ADOPULL"),
        adopull("adopull"),
        GITHUBPULL("GITHUBPULL"),
        githubpull("githubpull"),
        GITHUBCOMMIT("GITHUBCOMMIT"),
        GITLABCOMMIT("GITLABCOMMIT"),
        GITLABMERGE("GITLABMERGE"),
        gitlabmerge("gitlabmerge"),
        EMAIL("EMAIL"),
        CUSTOM("CUSTOM"),
        NONE("NONE"),
        WAIT("WAIT"),
        wait("wait");

        private String type;

        Type(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }
}
