
package com.checkmarx.flow.jira9X;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties
@JsonPropertyOrder({
        "self",
        "id",
        "name",
        "description",
        "lead",
        "leadUserName",
        "assigneeType",
        "assignee",
        "realAssigneeType",
        "realAssignee",
        "isAssigneeTypeValid",
        "project",
        "projectId",
        "archived",
        "deleted"
})
@Generated("jsonschema2pojo")
public class Component {

    @JsonProperty("self")
    private URI self;
    @JsonProperty("id")
    private String id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("description")
    private String description;

    @JsonProperty("lead")
    private User lead;
    @JsonProperty("leadUserName")
    private String leadUserName;
    @JsonProperty("assigneeType")
    private Component.AssigneeType assigneeType;

    @JsonProperty("assignee")
    private User assignee;
    @JsonProperty("realAssigneeType")
    private Component.RealAssigneeType realAssigneeType;

    @JsonProperty("realAssignee")
    private User realAssignee;

    @JsonProperty("isAssigneeTypeValid")
    private Boolean isAssigneeTypeValid;
    @JsonProperty("project")
    private String project;
    @JsonProperty("projectId")
    private Integer projectId;
    @JsonProperty("archived")
    private Boolean archived;
    @JsonProperty("deleted")
    private Boolean deleted;

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

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty("lead")
    public User getLead() {
        return lead;
    }

    @JsonProperty("lead")
    public void setLead(User lead) {
        this.lead = lead;
    }

    @JsonProperty("leadUserName")
    public String getLeadUserName() {
        return leadUserName;
    }

    @JsonProperty("leadUserName")
    public void setLeadUserName(String leadUserName) {
        this.leadUserName = leadUserName;
    }

    @JsonProperty("assigneeType")
    public Component.AssigneeType getAssigneeType() {
        return assigneeType;
    }

    @JsonProperty("assigneeType")
    public void setAssigneeType(Component.AssigneeType assigneeType) {
        this.assigneeType = assigneeType;
    }

    @JsonProperty("assignee")
    public User getAssignee() {
        return assignee;
    }

    @JsonProperty("assignee")
    public void setAssignee(User assignee) {
        this.assignee = assignee;
    }

    @JsonProperty("realAssigneeType")
    public Component.RealAssigneeType getRealAssigneeType() {
        return realAssigneeType;
    }

    @JsonProperty("realAssigneeType")
    public void setRealAssigneeType(Component.RealAssigneeType realAssigneeType) {
        this.realAssigneeType = realAssigneeType;
    }

    @JsonProperty("realAssignee")
    public User getRealAssignee() {
        return realAssignee;
    }

    @JsonProperty("realAssignee")
    public void setRealAssignee(User realAssignee) {
        this.realAssignee = realAssignee;
    }

    @JsonProperty("isAssigneeTypeValid")
    public Boolean getIsAssigneeTypeValid() {
        return isAssigneeTypeValid;
    }

    @JsonProperty("isAssigneeTypeValid")
    public void setIsAssigneeTypeValid(Boolean isAssigneeTypeValid) {
        this.isAssigneeTypeValid = isAssigneeTypeValid;
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

    @JsonProperty("archived")
    public Boolean getArchived() {
        return archived;
    }

    @JsonProperty("archived")
    public void setArchived(Boolean archived) {
        this.archived = archived;
    }

    @JsonProperty("deleted")
    public Boolean getDeleted() {
        return deleted;
    }

    @JsonProperty("deleted")
    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Component.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("self");
        sb.append('=');
        sb.append(((this.self == null)?"<null>":this.self));
        sb.append(',');
        sb.append("id");
        sb.append('=');
        sb.append(((this.id == null)?"<null>":this.id));
        sb.append(',');
        sb.append("name");
        sb.append('=');
        sb.append(((this.name == null)?"<null>":this.name));
        sb.append(',');
        sb.append("description");
        sb.append('=');
        sb.append(((this.description == null)?"<null>":this.description));
        sb.append(',');
        sb.append("lead");
        sb.append('=');
        sb.append(((this.lead == null)?"<null>":this.lead));
        sb.append(',');
        sb.append("leadUserName");
        sb.append('=');
        sb.append(((this.leadUserName == null)?"<null>":this.leadUserName));
        sb.append(',');
        sb.append("assigneeType");
        sb.append('=');
        sb.append(((this.assigneeType == null)?"<null>":this.assigneeType));
        sb.append(',');
        sb.append("assignee");
        sb.append('=');
        sb.append(((this.assignee == null)?"<null>":this.assignee));
        sb.append(',');
        sb.append("realAssigneeType");
        sb.append('=');
        sb.append(((this.realAssigneeType == null)?"<null>":this.realAssigneeType));
        sb.append(',');
        sb.append("realAssignee");
        sb.append('=');
        sb.append(((this.realAssignee == null)?"<null>":this.realAssignee));
        sb.append(',');
        sb.append("isAssigneeTypeValid");
        sb.append('=');
        sb.append(((this.isAssigneeTypeValid == null)?"<null>":this.isAssigneeTypeValid));
        sb.append(',');
        sb.append("project");
        sb.append('=');
        sb.append(((this.project == null)?"<null>":this.project));
        sb.append(',');
        sb.append("projectId");
        sb.append('=');
        sb.append(((this.projectId == null)?"<null>":this.projectId));
        sb.append(',');
        sb.append("archived");
        sb.append('=');
        sb.append(((this.archived == null)?"<null>":this.archived));
        sb.append(',');
        sb.append("deleted");
        sb.append('=');
        sb.append(((this.deleted == null)?"<null>":this.deleted));
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
        result = ((result* 31)+((this.leadUserName == null)? 0 :this.leadUserName.hashCode()));
        result = ((result* 31)+((this.description == null)? 0 :this.description.hashCode()));
        result = ((result* 31)+((this.project == null)? 0 :this.project.hashCode()));
        result = ((result* 31)+((this.lead == null)? 0 :this.lead.hashCode()));
        result = ((result* 31)+((this.archived == null)? 0 :this.archived.hashCode()));
        result = ((result* 31)+((this.isAssigneeTypeValid == null)? 0 :this.isAssigneeTypeValid.hashCode()));
        result = ((result* 31)+((this.deleted == null)? 0 :this.deleted.hashCode()));
        result = ((result* 31)+((this.realAssigneeType == null)? 0 :this.realAssigneeType.hashCode()));
        result = ((result* 31)+((this.name == null)? 0 :this.name.hashCode()));
        result = ((result* 31)+((this.self == null)? 0 :this.self.hashCode()));
        result = ((result* 31)+((this.realAssignee == null)? 0 :this.realAssignee.hashCode()));
        result = ((result* 31)+((this.id == null)? 0 :this.id.hashCode()));
        result = ((result* 31)+((this.assigneeType == null)? 0 :this.assigneeType.hashCode()));
        result = ((result* 31)+((this.assignee == null)? 0 :this.assignee.hashCode()));
        result = ((result* 31)+((this.projectId == null)? 0 :this.projectId.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Component) == false) {
            return false;
        }
        Component rhs = ((Component) other);
        return ((((((((((((((((this.leadUserName == rhs.leadUserName)||((this.leadUserName!= null)&&this.leadUserName.equals(rhs.leadUserName)))&&((this.description == rhs.description)||((this.description!= null)&&this.description.equals(rhs.description))))&&((this.project == rhs.project)||((this.project!= null)&&this.project.equals(rhs.project))))&&((this.lead == rhs.lead)||((this.lead!= null)&&this.lead.equals(rhs.lead))))&&((this.archived == rhs.archived)||((this.archived!= null)&&this.archived.equals(rhs.archived))))&&((this.isAssigneeTypeValid == rhs.isAssigneeTypeValid)||((this.isAssigneeTypeValid!= null)&&this.isAssigneeTypeValid.equals(rhs.isAssigneeTypeValid))))&&((this.deleted == rhs.deleted)||((this.deleted!= null)&&this.deleted.equals(rhs.deleted))))&&((this.realAssigneeType == rhs.realAssigneeType)||((this.realAssigneeType!= null)&&this.realAssigneeType.equals(rhs.realAssigneeType))))&&((this.name == rhs.name)||((this.name!= null)&&this.name.equals(rhs.name))))&&((this.self == rhs.self)||((this.self!= null)&&this.self.equals(rhs.self))))&&((this.realAssignee == rhs.realAssignee)||((this.realAssignee!= null)&&this.realAssignee.equals(rhs.realAssignee))))&&((this.id == rhs.id)||((this.id!= null)&&this.id.equals(rhs.id))))&&((this.assigneeType == rhs.assigneeType)||((this.assigneeType!= null)&&this.assigneeType.equals(rhs.assigneeType))))&&((this.assignee == rhs.assignee)||((this.assignee!= null)&&this.assignee.equals(rhs.assignee))))&&((this.projectId == rhs.projectId)||((this.projectId!= null)&&this.projectId.equals(rhs.projectId))));
    }

    @Generated("jsonschema2pojo")
    public enum AssigneeType {

        PROJECT_DEFAULT("PROJECT_DEFAULT"),
        COMPONENT_LEAD("COMPONENT_LEAD"),
        PROJECT_LEAD("PROJECT_LEAD"),
        UNASSIGNED("UNASSIGNED");
        private final String value;
        private final static Map<String, Component.AssigneeType> CONSTANTS = new HashMap<String, Component.AssigneeType>();

        static {
            for (Component.AssigneeType c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        AssigneeType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static Component.AssigneeType fromValue(String value) {
            Component.AssigneeType constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

    @Generated("jsonschema2pojo")
    public enum RealAssigneeType {

        PROJECT_DEFAULT("PROJECT_DEFAULT"),
        COMPONENT_LEAD("COMPONENT_LEAD"),
        PROJECT_LEAD("PROJECT_LEAD"),
        UNASSIGNED("UNASSIGNED");
        private final String value;
        private final static Map<String, Component.RealAssigneeType> CONSTANTS = new HashMap<String, Component.RealAssigneeType>();

        static {
            for (Component.RealAssigneeType c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        RealAssigneeType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static Component.RealAssigneeType fromValue(String value) {
            Component.RealAssigneeType constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
