package com.checkmarx.flow.dto.servicenow;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "description",
        "short_description",
        "number",
        "sys_updated_by",
        "sys_created_on",
        "state",
        "sys_created_by",
        "impact",
        "closed_at",
        "active",
        "priority",
        "subcategory",
        "work_notes",
        "close_code",
        "sys_id",
        "contact_type",
        "incident_state",
        "urgency",
        "severity",
        "category",
        "sys_tags",
        "comments",
        "location",
        "close_notes",
})
public class Incident {
    @JsonProperty("comments")
    private String comments;
    @JsonProperty("close_notes")
    private String closeNotes;
    @JsonProperty("location")
    private String location;
    @JsonProperty("description")
    private String description;
    @JsonProperty("sys_tags")
    private String sysTags;
    @JsonProperty("short_description")
    private String shortDescription;
    @JsonProperty("number")
    private String number;
    @JsonProperty("sys_updated_by")
    private String sysUpdatedBy;
    @JsonProperty("sys_created_on")
    private String sysCreatedOn;
    @JsonProperty("state")
    private String state;
    @JsonProperty("sys_created_by")
    private String sysCreatedBy;
    @JsonProperty("impact")
    private String impact;
    @JsonProperty("closed_at")
    private String closedAt;
    @JsonProperty("active")
    private String active;
    @JsonProperty("priority")
    private String priority;
    @JsonProperty("subcategory")
    private String subcategory;
    @JsonProperty("work_notes")
    private String workNotes;
    @JsonProperty("close_code")
    private String closeCode;
    @JsonProperty("sys_id")
    private String sysId;
    @JsonProperty("contact_type")
    private String contactType;
    @JsonProperty("incident_state")
    private String incidentState;
    @JsonProperty("urgency")
    private String urgency;
    @JsonProperty("severity")
    private String severity;
    @JsonProperty("category")
    private String category;

    public String getCloseNotes() {
        return closeNotes;
    }

    public void setCloseNotes(String closeNotes) {
        this.closeNotes = closeNotes;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNumber() {
        return number;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public String getSysTags() {
        return sysTags;
    }

    public void setSysTags(String sysTags) {
        this.sysTags = sysTags;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getSysUpdatedBy() {
        return sysUpdatedBy;
    }

    public void setSysUpdatedBy(String sysUpdatedBy) {
        this.sysUpdatedBy = sysUpdatedBy;
    }

    public String getSysCreatedOn() {
        return sysCreatedOn;
    }

    public void setSysCreatedOn(String sysCreatedOn) {
        this.sysCreatedOn = sysCreatedOn;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getSysCreatedBy() {
        return sysCreatedBy;
    }

    public void setSysCreatedBy(String sysCreatedBy) {
        this.sysCreatedBy = sysCreatedBy;
    }

    public String getImpact() {
        return impact;
    }

    public void setImpact(String impact) {
        this.impact = impact;
    }

    public String getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(String closedAt) {
        this.closedAt = closedAt;
    }

    public String getActive() {
        return active;
    }

    public void setActive(String active) {
        this.active = active;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public void setSubcategory(String subcategory) {
        this.subcategory = subcategory;
    }

    public String getWorkNotes() {
        return workNotes;
    }

    public void setWorkNotes(String workNotes) {
        this.workNotes = workNotes;
    }

    public String getCloseCode() {
        return closeCode;
    }

    public void setCloseCode(String closeCode) {
        this.closeCode = closeCode;
    }

    public String getSysId() {
        return sysId;
    }

    public void setSysId(String sysId) {
        this.sysId = sysId;
    }

    public String getContactType() {
        return contactType;
    }

    public void setContactType(String contactType) {
        this.contactType = contactType;
    }

    public String getIncidentState() {
        return incidentState;
    }

    public void setIncidentState(String incidentState) {
        this.incidentState = incidentState;
    }

    public String getUrgency() {
        return urgency;
    }

    public void setUrgency(String urgency) {
        this.urgency = urgency;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
