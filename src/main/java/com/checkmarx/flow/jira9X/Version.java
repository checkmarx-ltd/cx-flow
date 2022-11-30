
package com.checkmarx.flow.jira9X;

import java.net.URI;
import java.util.List;
import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties
@JsonPropertyOrder({
        "expand",
        "self",
        "id",
        "description",
        "name",
        "archived",
        "released",
        "overdue",
        "userStartDate",
        "userReleaseDate",
        "project",
        "projectId",
        "moveUnfixedIssuesTo",
        "operations",
        "remotelinks"
})
@Generated("jsonschema2pojo")
public class Version {

    @JsonProperty("expand")
    private String expand;
    @JsonProperty("self")
    private URI self;
    @JsonProperty("id")
    private String id;
    @JsonProperty("description")
    private String description;
    @JsonProperty("name")
    private String name;
    @JsonProperty("archived")
    private Boolean archived;
    @JsonProperty("released")
    private Boolean released;
    @JsonProperty("overdue")
    private Boolean overdue;
    @JsonProperty("userStartDate")
    private String userStartDate;
    @JsonProperty("userReleaseDate")
    private String userReleaseDate;
    @JsonProperty("project")
    private String project;
    @JsonProperty("projectId")
    private Integer projectId;
    @JsonProperty("moveUnfixedIssuesTo")
    private URI moveUnfixedIssuesTo;
    @JsonProperty("operations")
    private List<Operation> operations = null;
    @JsonProperty("remotelinks")
    private List<Item> remotelinks = null;

    @JsonProperty("expand")
    public String getExpand() {
        return expand;
    }

    @JsonProperty("expand")
    public void setExpand(String expand) {
        this.expand = expand;
    }

    @JsonProperty("self")
    public URI getSelf() {
        return self;
    }

    @JsonProperty("self")
    public void setSelf(URI self) {
        this.self = self;
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("archived")
    public Boolean getArchived() {
        return archived;
    }

    @JsonProperty("archived")
    public void setArchived(Boolean archived) {
        this.archived = archived;
    }

    @JsonProperty("released")
    public Boolean getReleased() {
        return released;
    }

    @JsonProperty("released")
    public void setReleased(Boolean released) {
        this.released = released;
    }

    @JsonProperty("overdue")
    public Boolean getOverdue() {
        return overdue;
    }

    @JsonProperty("overdue")
    public void setOverdue(Boolean overdue) {
        this.overdue = overdue;
    }

    @JsonProperty("userStartDate")
    public String getUserStartDate() {
        return userStartDate;
    }

    @JsonProperty("userStartDate")
    public void setUserStartDate(String userStartDate) {
        this.userStartDate = userStartDate;
    }

    @JsonProperty("userReleaseDate")
    public String getUserReleaseDate() {
        return userReleaseDate;
    }

    @JsonProperty("userReleaseDate")
    public void setUserReleaseDate(String userReleaseDate) {
        this.userReleaseDate = userReleaseDate;
    }

    @JsonProperty("project")
    public String getProject() {
        return project;
    }

    @JsonProperty("project")
    public void setProject(String project) {
        this.project = project;
    }

    @JsonProperty("projectId")
    public Integer getProjectId() {
        return projectId;
    }

    @JsonProperty("projectId")
    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    @JsonProperty("moveUnfixedIssuesTo")
    public URI getMoveUnfixedIssuesTo() {
        return moveUnfixedIssuesTo;
    }

    @JsonProperty("moveUnfixedIssuesTo")
    public void setMoveUnfixedIssuesTo(URI moveUnfixedIssuesTo) {
        this.moveUnfixedIssuesTo = moveUnfixedIssuesTo;
    }

    @JsonProperty("operations")
    public List<Operation> getOperations() {
        return operations;
    }

    @JsonProperty("operations")
    public void setOperations(List<Operation> operations) {
        this.operations = operations;
    }

    @JsonProperty("remotelinks")
    public List<Item> getRemotelinks() {
        return remotelinks;
    }

    @JsonProperty("remotelinks")
    public void setRemotelinks(List<Item> remotelinks) {
        this.remotelinks = remotelinks;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Version.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("expand");
        sb.append('=');
        sb.append(((this.expand == null)?"<null>":this.expand));
        sb.append(',');
        sb.append("self");
        sb.append('=');
        sb.append(((this.self == null)?"<null>":this.self));
        sb.append(',');
        sb.append("id");
        sb.append('=');
        sb.append(((this.id == null)?"<null>":this.id));
        sb.append(',');
        sb.append("description");
        sb.append('=');
        sb.append(((this.description == null)?"<null>":this.description));
        sb.append(',');
        sb.append("name");
        sb.append('=');
        sb.append(((this.name == null)?"<null>":this.name));
        sb.append(',');
        sb.append("archived");
        sb.append('=');
        sb.append(((this.archived == null)?"<null>":this.archived));
        sb.append(',');
        sb.append("released");
        sb.append('=');
        sb.append(((this.released == null)?"<null>":this.released));
        sb.append(',');
        sb.append("overdue");
        sb.append('=');
        sb.append(((this.overdue == null)?"<null>":this.overdue));
        sb.append(',');
        sb.append("userStartDate");
        sb.append('=');
        sb.append(((this.userStartDate == null)?"<null>":this.userStartDate));
        sb.append(',');
        sb.append("userReleaseDate");
        sb.append('=');
        sb.append(((this.userReleaseDate == null)?"<null>":this.userReleaseDate));
        sb.append(',');
        sb.append("project");
        sb.append('=');
        sb.append(((this.project == null)?"<null>":this.project));
        sb.append(',');
        sb.append("projectId");
        sb.append('=');
        sb.append(((this.projectId == null)?"<null>":this.projectId));
        sb.append(',');
        sb.append("moveUnfixedIssuesTo");
        sb.append('=');
        sb.append(((this.moveUnfixedIssuesTo == null)?"<null>":this.moveUnfixedIssuesTo));
        sb.append(',');
        sb.append("operations");
        sb.append('=');
        sb.append(((this.operations == null)?"<null>":this.operations));
        sb.append(',');
        sb.append("remotelinks");
        sb.append('=');
        sb.append(((this.remotelinks == null)?"<null>":this.remotelinks));
        sb.append(',');
        if (sb.charAt((sb.length()- 1)) == ',') {
            sb.setCharAt((sb.length()- 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.description == null)? 0 :this.description.hashCode()));
        result = ((result* 31)+((this.project == null)? 0 :this.project.hashCode()));
        result = ((result* 31)+((this.archived == null)? 0 :this.archived.hashCode()));
        result = ((result* 31)+((this.expand == null)? 0 :this.expand.hashCode()));
        result = ((result* 31)+((this.operations == null)? 0 :this.operations.hashCode()));
        result = ((result* 31)+((this.overdue == null)? 0 :this.overdue.hashCode()));
        result = ((result* 31)+((this.remotelinks == null)? 0 :this.remotelinks.hashCode()));
        result = ((result* 31)+((this.name == null)? 0 :this.name.hashCode()));
        result = ((result* 31)+((this.self == null)? 0 :this.self.hashCode()));
        result = ((result* 31)+((this.moveUnfixedIssuesTo == null)? 0 :this.moveUnfixedIssuesTo.hashCode()));
        result = ((result* 31)+((this.userReleaseDate == null)? 0 :this.userReleaseDate.hashCode()));
        result = ((result* 31)+((this.id == null)? 0 :this.id.hashCode()));
        result = ((result* 31)+((this.userStartDate == null)? 0 :this.userStartDate.hashCode()));
        result = ((result* 31)+((this.projectId == null)? 0 :this.projectId.hashCode()));
        result = ((result* 31)+((this.released == null)? 0 :this.released.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Version) == false) {
            return false;
        }
        Version rhs = ((Version) other);
        return ((((((((((((((((this.description == rhs.description)||((this.description!= null)&&this.description.equals(rhs.description)))&&((this.project == rhs.project)||((this.project!= null)&&this.project.equals(rhs.project))))&&((this.archived == rhs.archived)||((this.archived!= null)&&this.archived.equals(rhs.archived))))&&((this.expand == rhs.expand)||((this.expand!= null)&&this.expand.equals(rhs.expand))))&&((this.operations == rhs.operations)||((this.operations!= null)&&this.operations.equals(rhs.operations))))&&((this.overdue == rhs.overdue)||((this.overdue!= null)&&this.overdue.equals(rhs.overdue))))&&((this.remotelinks == rhs.remotelinks)||((this.remotelinks!= null)&&this.remotelinks.equals(rhs.remotelinks))))&&((this.name == rhs.name)||((this.name!= null)&&this.name.equals(rhs.name))))&&((this.self == rhs.self)||((this.self!= null)&&this.self.equals(rhs.self))))&&((this.moveUnfixedIssuesTo == rhs.moveUnfixedIssuesTo)||((this.moveUnfixedIssuesTo!= null)&&this.moveUnfixedIssuesTo.equals(rhs.moveUnfixedIssuesTo))))&&((this.userReleaseDate == rhs.userReleaseDate)||((this.userReleaseDate!= null)&&this.userReleaseDate.equals(rhs.userReleaseDate))))&&((this.id == rhs.id)||((this.id!= null)&&this.id.equals(rhs.id))))&&((this.userStartDate == rhs.userStartDate)||((this.userStartDate!= null)&&this.userStartDate.equals(rhs.userStartDate))))&&((this.projectId == rhs.projectId)||((this.projectId!= null)&&this.projectId.equals(rhs.projectId))))&&((this.released == rhs.released)||((this.released!= null)&&this.released.equals(rhs.released))));
    }

}
