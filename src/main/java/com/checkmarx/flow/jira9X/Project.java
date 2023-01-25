package com.checkmarx.flow.jira9X;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties
@JsonPropertyOrder({
        "expand",
        "self",
        "id",
        "key",
        "description",
        "lead",
        "components",
        "issueTypes",
        "url",
        "email",
        "assigneeType",
        "versions",
        "name",
        "roles",
        "avatarUrls",
        "projectKeys",
        "projectCategory",
        "projectTypeKey",
        "archived"
})
@Generated("jsonschema2pojo")
public class Project {

    @JsonProperty("expand")
    private String expand;
    @JsonProperty("self")
    private URI self;
    @JsonProperty("id")
    private String id;
    @JsonProperty("key")
    private String key;
    @JsonProperty("description")
    private String description;

    @JsonProperty("lead")
    private User lead;
    @JsonProperty("components")
    private List<Component> components = null;
    @JsonProperty("issueTypes")
    private List<IssueType> issueTypes = null;
    @JsonProperty("url")
    private String url;
    @JsonProperty("email")
    private String email;
    @JsonProperty("assigneeType")
    private Project.AssigneeType assigneeType;
    @JsonProperty("versions")
    private List<Version> versions = null;
    @JsonProperty("name")
    private String name;
    @JsonProperty("roles")
    private Map<String, String>  roles;
    @JsonProperty("avatarUrls")
    private Map<String, String> avatarUrls;
    @JsonProperty("projectKeys")
    private List<String> projectKeys = null;
    @JsonProperty("projectCategory")
    private ProjectCategory projectCategory;
    @JsonProperty("projectTypeKey")
    private String projectTypeKey;
    @JsonProperty("archived")
    private Boolean archived;

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
    public Long getId() {
        try {
            Long longId = new Long(this.id);
            return longId;
        }
        catch (NumberFormatException e)
        {
            //default value
            return 0l;
        }
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("key")
    public String getKey() {
        return key;
    }

    @JsonProperty("key")
    public void setKey(String key) {
        this.key = key;
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

    @JsonProperty("components")
    public List<Component> getComponents() {
        return components;
    }

    @JsonProperty("components")
    public void setComponents(List<Component> components) {
        this.components = components;
    }

    @JsonProperty("issueTypes")
    public List<IssueType> getIssueTypes() {
        return issueTypes;
    }

    @JsonProperty("issueTypes")
    public void setIssueTypes(List<IssueType> issueTypes) {
        this.issueTypes = issueTypes;
    }

    @JsonProperty("url")
    public String getUrl() {
        return url;
    }

    @JsonProperty("url")
    public void setUrl(String url) {
        this.url = url;
    }

    @JsonProperty("email")
    public String getEmail() {
        return email;
    }

    @JsonProperty("email")
    public void setEmail(String email) {
        this.email = email;
    }

    @JsonProperty("assigneeType")
    public Project.AssigneeType getAssigneeType() {
        return assigneeType;
    }

    @JsonProperty("assigneeType")
    public void setAssigneeType(Project.AssigneeType assigneeType) {
        this.assigneeType = assigneeType;
    }

    @JsonProperty("versions")
    public List<Version> getVersions() {
        return versions;
    }

    @JsonProperty("versions")
    public void setVersions(List<Version> versions) {
        this.versions = versions;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("roles")
    public Map<String, String> getRoles() {
        return roles;
    }

    @JsonProperty("roles")
    public void setRoles(Map<String, String> roles) {
        this.roles = roles;
    }

    @JsonProperty("avatarUrls")
    public Map<String, URI> getAvatarUrls() {
        Map<String,URI> stringURIMap = new HashMap<>();
        URI tempUri = null;
        if(avatarUrls!=null)
        {
            Iterator iterator = avatarUrls.keySet().iterator();
            while(iterator.hasNext())
            {
                String key = (String) iterator.next();
                String tempStr = avatarUrls.get(key);
                try {
                    tempUri = new URI(tempStr);
                } catch (URISyntaxException e) {
                    //default value
                    return new HashMap<>();
                }
                stringURIMap.put(key,tempUri);
            }
        }

        return stringURIMap;
    }

    @JsonProperty("avatarUrls")
    public void setAvatarUrls(Map<String, String> avatarUrls) {
        this.avatarUrls = avatarUrls;
    }

    @JsonProperty("projectKeys")
    public List<String> getProjectKeys() {
        return projectKeys;
    }

    @JsonProperty("projectKeys")
    public void setProjectKeys(List<String> projectKeys) {
        this.projectKeys = projectKeys;
    }

    @JsonProperty("projectCategory")
    public ProjectCategory getProjectCategory() {
        return projectCategory;
    }

    @JsonProperty("projectCategory")
    public void setProjectCategory(ProjectCategory projectCategory) {
        this.projectCategory = projectCategory;
    }

    @JsonProperty("projectTypeKey")
    public String getProjectTypeKey() {
        return projectTypeKey;
    }

    @JsonProperty("projectTypeKey")
    public void setProjectTypeKey(String projectTypeKey) {
        this.projectTypeKey = projectTypeKey;
    }

    @JsonProperty("archived")
    public Boolean getArchived() {
        return archived;
    }

    @JsonProperty("archived")
    public void setArchived(Boolean archived) {
        this.archived = archived;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Project.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        sb.append("key");
        sb.append('=');
        sb.append(((this.key == null)?"<null>":this.key));
        sb.append(',');
        sb.append("description");
        sb.append('=');
        sb.append(((this.description == null)?"<null>":this.description));
        sb.append(',');
        sb.append("lead");
        sb.append('=');
        sb.append(((this.lead == null)?"<null>":this.lead));
        sb.append(',');
        sb.append("components");
        sb.append('=');
        sb.append(((this.components == null)?"<null>":this.components));
        sb.append(',');
        sb.append("issueTypes");
        sb.append('=');
        sb.append(((this.issueTypes == null)?"<null>":this.issueTypes));
        sb.append(',');
        sb.append("url");
        sb.append('=');
        sb.append(((this.url == null)?"<null>":this.url));
        sb.append(',');
        sb.append("email");
        sb.append('=');
        sb.append(((this.email == null)?"<null>":this.email));
        sb.append(',');
        sb.append("assigneeType");
        sb.append('=');
        sb.append(((this.assigneeType == null)?"<null>":this.assigneeType));
        sb.append(',');
        sb.append("versions");
        sb.append('=');
        sb.append(((this.versions == null)?"<null>":this.versions));
        sb.append(',');
        sb.append("name");
        sb.append('=');
        sb.append(((this.name == null)?"<null>":this.name));
        sb.append(',');
        sb.append("roles");
        sb.append('=');
        sb.append(((this.roles == null)?"<null>":this.roles));
        sb.append(',');
        sb.append("avatarUrls");
        sb.append('=');
        sb.append(((this.avatarUrls == null)?"<null>":this.avatarUrls));
        sb.append(',');
        sb.append("projectKeys");
        sb.append('=');
        sb.append(((this.projectKeys == null)?"<null>":this.projectKeys));
        sb.append(',');
        sb.append("projectCategory");
        sb.append('=');
        sb.append(((this.projectCategory == null)?"<null>":this.projectCategory));
        sb.append(',');
        sb.append("projectTypeKey");
        sb.append('=');
        sb.append(((this.projectTypeKey == null)?"<null>":this.projectTypeKey));
        sb.append(',');
        sb.append("archived");
        sb.append('=');
        sb.append(((this.archived == null)?"<null>":this.archived));
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
        result = ((result* 31)+((this.components == null)? 0 :this.components.hashCode()));
        result = ((result* 31)+((this.avatarUrls == null)? 0 :this.avatarUrls.hashCode()));
        result = ((result* 31)+((this.roles == null)? 0 :this.roles.hashCode()));
        result = ((result* 31)+((this.description == null)? 0 :this.description.hashCode()));
        result = ((result* 31)+((this.lead == null)? 0 :this.lead.hashCode()));
        result = ((result* 31)+((this.url == null)? 0 :this.url.hashCode()));
        result = ((result* 31)+((this.issueTypes == null)? 0 :this.issueTypes.hashCode()));
        result = ((result* 31)+((this.archived == null)? 0 :this.archived.hashCode()));
        result = ((result* 31)+((this.expand == null)? 0 :this.expand.hashCode()));
        result = ((result* 31)+((this.versions == null)? 0 :this.versions.hashCode()));
        result = ((result* 31)+((this.projectCategory == null)? 0 :this.projectCategory.hashCode()));
        result = ((result* 31)+((this.name == null)? 0 :this.name.hashCode()));
        result = ((result* 31)+((this.self == null)? 0 :this.self.hashCode()));
        result = ((result* 31)+((this.id == null)? 0 :this.id.hashCode()));
        result = ((result* 31)+((this.assigneeType == null)? 0 :this.assigneeType.hashCode()));
        result = ((result* 31)+((this.projectKeys == null)? 0 :this.projectKeys.hashCode()));
        result = ((result* 31)+((this.projectTypeKey == null)? 0 :this.projectTypeKey.hashCode()));
        result = ((result* 31)+((this.key == null)? 0 :this.key.hashCode()));
        result = ((result* 31)+((this.email == null)? 0 :this.email.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Project) == false) {
            return false;
        }
        Project rhs = ((Project) other);
        return ((((((((((((((((((((this.components == rhs.components)||((this.components!= null)&&this.components.equals(rhs.components)))&&((this.avatarUrls == rhs.avatarUrls)||((this.avatarUrls!= null)&&this.avatarUrls.equals(rhs.avatarUrls))))&&((this.roles == rhs.roles)||((this.roles!= null)&&this.roles.equals(rhs.roles))))&&((this.description == rhs.description)||((this.description!= null)&&this.description.equals(rhs.description))))&&((this.lead == rhs.lead)||((this.lead!= null)&&this.lead.equals(rhs.lead))))&&((this.url == rhs.url)||((this.url!= null)&&this.url.equals(rhs.url))))&&((this.issueTypes == rhs.issueTypes)||((this.issueTypes!= null)&&this.issueTypes.equals(rhs.issueTypes))))&&((this.archived == rhs.archived)||((this.archived!= null)&&this.archived.equals(rhs.archived))))&&((this.expand == rhs.expand)||((this.expand!= null)&&this.expand.equals(rhs.expand))))&&((this.versions == rhs.versions)||((this.versions!= null)&&this.versions.equals(rhs.versions))))&&((this.projectCategory == rhs.projectCategory)||((this.projectCategory!= null)&&this.projectCategory.equals(rhs.projectCategory))))&&((this.name == rhs.name)||((this.name!= null)&&this.name.equals(rhs.name))))&&((this.self == rhs.self)||((this.self!= null)&&this.self.equals(rhs.self))))&&((this.id == rhs.id)||((this.id!= null)&&this.id.equals(rhs.id))))&&((this.assigneeType == rhs.assigneeType)||((this.assigneeType!= null)&&this.assigneeType.equals(rhs.assigneeType))))&&((this.projectKeys == rhs.projectKeys)||((this.projectKeys!= null)&&this.projectKeys.equals(rhs.projectKeys))))&&((this.projectTypeKey == rhs.projectTypeKey)||((this.projectTypeKey!= null)&&this.projectTypeKey.equals(rhs.projectTypeKey))))&&((this.key == rhs.key)||((this.key!= null)&&this.key.equals(rhs.key))))&&((this.email == rhs.email)||((this.email!= null)&&this.email.equals(rhs.email))));
    }

    @Generated("jsonschema2pojo")
    public enum AssigneeType {

        PROJECT_LEAD("PROJECT_LEAD"),
        UNASSIGNED("UNASSIGNED");
        private final String value;
        private final static Map<String, Project.AssigneeType> CONSTANTS = new HashMap<String, Project.AssigneeType>();

        static {
            for (Project.AssigneeType c: values()) {
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
        public static Project.AssigneeType fromValue(String value) {
            Project.AssigneeType constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
