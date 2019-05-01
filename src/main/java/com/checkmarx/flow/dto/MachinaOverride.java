package com.checkmarx.flow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * DTO representing the Override/Property file from within repository or provided as base64 encoded JSON blob query param
 */
public class MachinaOverride {

    @JsonProperty("application")
    public String application;
    @JsonProperty("branches")
    public List<String> branches = null;
    @JsonProperty("incremental")
    public Boolean incremental;
    @JsonProperty("scan_preset")
    public String scanPreset;
    @JsonProperty("exclude_folders")
    public String excludeFolders;
    @JsonProperty("exclude_files")
    public String excludeFiles;
    @JsonProperty("emails")
    public List<String> emails = null;
    @JsonProperty("jira")
    public Jira jira;
    @JsonProperty("filters")
    public Filters filters;

    public MachinaOverride() {
    }

    public String getApplication() {
        return this.application;
    }

    public List<String> getBranches() {
        return this.branches;
    }

    public Boolean getIncremental() {
        return this.incremental;
    }

    public String getScanPreset() {
        return this.scanPreset;
    }

    public String getExcludeFolders() {
        return this.excludeFolders;
    }

    public String getExcludeFiles() {
        return this.excludeFiles;
    }

    public List<String> getEmails() {
        return this.emails;
    }

    public Jira getJira() {
        return this.jira;
    }

    public Filters getFilters() {
        return this.filters;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public void setBranches(List<String> branches) {
        this.branches = branches;
    }

    public void setIncremental(Boolean incremental) {
        this.incremental = incremental;
    }

    public void setScanPreset(String scanPreset) {
        this.scanPreset = scanPreset;
    }

    public void setExcludeFolders(String excludeFolders) {
        this.excludeFolders = excludeFolders;
    }

    public void setExcludeFiles(String excludeFiles) {
        this.excludeFiles = excludeFiles;
    }

    public void setEmails(List<String> emails) {
        this.emails = emails;
    }

    public void setJira(Jira jira) {
        this.jira = jira;
    }

    public void setFilters(Filters filters) {
        this.filters = filters;
    }


    public String toString() {
        return "MachinaOverride(application=" + this.getApplication() + ", branches=" + this.getBranches() + ", incremental=" + this.getIncremental() + ", scanPreset=" + this.getScanPreset() + ", excludeFolders=" + this.getExcludeFolders() + ", excludeFiles=" + this.getExcludeFiles() + ", emails=" + this.getEmails() + ", jira=" + this.getJira() + ", filters=" + this.getFilters() + ")";
    }

    public class Filters {

        @JsonProperty("severity")
        public List<String> severity = null;
        @JsonProperty("cwe")
        public List<String> cwe = null;
        @JsonProperty("category")
        public List<String> category = null;
        @JsonProperty("status")
        public List<String> status = null;

        public List<String> getSeverity() {
            return this.severity;
        }

        public List<String> getCwe() {
            return this.cwe;
        }

        public List<String> getCategory() {
            return this.category;
        }

        public List<String> getStatus() {
            return this.status;
        }

        public void setSeverity(List<String> severity) {
            this.severity = severity;
        }

        public void setCwe(List<String> cwe) {
            this.cwe = cwe;
        }

        public void setCategory(List<String> category) {
            this.category = category;
        }

        public void setStatus(List<String> status) {
            this.status = status;
        }
    }

    public class Jira {

        @JsonProperty("project")
        public String project;
        @JsonProperty("issue_type")
        public String issueType;
        @JsonProperty("assignee")
        public String assignee;
        @JsonProperty("opened_status")
        public List<String> openedStatus = null;
        @JsonProperty("closed_status")
        public List<String> closedStatus = null;
        @JsonProperty("open_transition")
        public String openTransition;
        @JsonProperty("close_transition")
        public String closeTransition;
        @JsonProperty("close_transition_field")
        public String closeTransitionField;
        @JsonProperty("close_transition_value")
        public String closeTransitionValue;
        @JsonProperty("priorities")
        public Map<String, String> priorities;
        @JsonProperty("fields")
        public List<Field> fields;

        public String getProject() {
            return this.project;
        }

        public String getIssueType() {
            return this.issueType;
        }

        public String getAssignee() {
            return this.assignee;
        }

        public List<String> getOpenedStatus() {
            return this.openedStatus;
        }

        public List<String> getClosedStatus() {
            return this.closedStatus;
        }

        public String getOpenTransition() {
            return this.openTransition;
        }

        public String getCloseTransition() {
            return this.closeTransition;
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

        public void setProject(String project) {
            this.project = project;
        }

        public void setIssueType(String issueType) {
            this.issueType = issueType;
        }

        public void setAssignee(String assignee) {
            this.assignee = assignee;
        }

        public void setOpenedStatus(List<String> openedStatus) {
            this.openedStatus = openedStatus;
        }

        public void setClosedStatus(List<String> closedStatus) {
            this.closedStatus = closedStatus;
        }

        public void setOpenTransition(String openTransition) {
            this.openTransition = openTransition;
        }

        public void setCloseTransition(String closeTransition) {
            this.closeTransition = closeTransition;
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
    }


}
