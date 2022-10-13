package com.checkmarx.flow.jira9X;

import java.net.URI;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "expand",
        "self",
        "id",
        "key",
        "name",
        "avatarUrls",
        "projectTypeKey",
        "archived"
})
public class Project {

    @JsonProperty("expand")
    private String expand;
    @JsonProperty("self")
    private URI self;
    @JsonProperty("id")
    private Long id;
    @JsonProperty("key")
    private String key;
    @JsonProperty("name")
    private String name;
    @JsonProperty("avatarUrls")
    private Map<String, URI> avatarUrls;
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
        return id;
    }
    @JsonProperty("id")
    public void setId(Long id) {
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
    @JsonProperty("name")
    public String getName() {
        return name;
    }
    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }
    @JsonProperty("avatarUrls")
    public Map<String, URI> getAvatarUrls() {
        return avatarUrls;
    }
    @JsonProperty("avatarUrls")
    public void setAvatarUrls(Map<String, URI> avatarUrls) {
        this.avatarUrls = avatarUrls;
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
        sb.append("name");
        sb.append('=');
        sb.append(((this.name == null)?"<null>":this.name));
        sb.append(',');
        sb.append("avatarUrls");
        sb.append('=');
        sb.append(((this.avatarUrls == null)?"<null>":this.avatarUrls));
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

}
