
package com.checkmarx.flow.jira9X;

import java.net.URI;
import java.util.Map;
import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;



@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties
@JsonPropertyOrder({
        "self",
        "key",
        "name",
        "emailAddress",
        "avatarUrls",
        "displayName",
        "active",
        "deleted",
        "timeZone",
        "locale",
        "groups",
        "applicationRoles",
        "expand"
})
@Generated("jsonschema2pojo")
public class User {

    @JsonProperty("self")
    private URI self;
    @JsonProperty("key")
    private String key;
    @JsonProperty("name")
    private String name;
    @JsonProperty("emailAddress")
    private String emailAddress;
    @JsonProperty("avatarUrls")
    private Map<String, URI> avatarUrls;
    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("active")
    private Boolean active;
    @JsonProperty("deleted")
    private Boolean deleted;
    @JsonProperty("timeZone")
    private String timeZone;
    @JsonProperty("locale")
    private String locale;

    @JsonProperty("groups")
    private SimpleListWrapper groups;

    @JsonProperty("applicationRoles")
    private SimpleListWrapper applicationRoles;
    @JsonProperty("expand")
    private String expand;

    @JsonProperty("self")
    public URI getSelf() {
        return self;
    }

    @JsonProperty("self")
    public void setSelf(URI self) {
        this.self = self;
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

    @JsonProperty("emailAddress")
    public String getEmailAddress() {
        return emailAddress;
    }

    @JsonProperty("emailAddress")
    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    @JsonProperty("avatarUrls")
    public Map<String, URI> getAvatarUrls() {
        return avatarUrls;
    }

    @JsonProperty("avatarUrls")
    public void setAvatarUrls(Map<String, URI> avatarUrls) {
        this.avatarUrls = avatarUrls;
    }

    @JsonProperty("displayName")
    public String getDisplayName() {
        return displayName;
    }

    @JsonProperty("displayName")
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @JsonProperty("active")
    public Boolean getActive() {
        return active;
    }

    @JsonProperty("active")
    public void setActive(Boolean active) {
        this.active = active;
    }

    @JsonProperty("deleted")
    public Boolean getDeleted() {
        return deleted;
    }

    @JsonProperty("deleted")
    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    @JsonProperty("timeZone")
    public String getTimeZone() {
        return timeZone;
    }

    @JsonProperty("timeZone")
    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    @JsonProperty("locale")
    public String getLocale() {
        return locale;
    }

    @JsonProperty("locale")
    public void setLocale(String locale) {
        this.locale = locale;
    }

    @JsonProperty("groups")
    public SimpleListWrapper getGroups() {
        return groups;
    }

    @JsonProperty("groups")
    public void setGroups(SimpleListWrapper groups) {
        this.groups = groups;
    }

    @JsonProperty("applicationRoles")
    public SimpleListWrapper getApplicationRoles() {
        return applicationRoles;
    }

    @JsonProperty("applicationRoles")
    public void setApplicationRoles(SimpleListWrapper applicationRoles) {
        this.applicationRoles = applicationRoles;
    }

    @JsonProperty("expand")
    public String getExpand() {
        return expand;
    }

    @JsonProperty("expand")
    public void setExpand(String expand) {
        this.expand = expand;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(User.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("self");
        sb.append('=');
        sb.append(((this.self == null)?"<null>":this.self));
        sb.append(',');
        sb.append("key");
        sb.append('=');
        sb.append(((this.key == null)?"<null>":this.key));
        sb.append(',');
        sb.append("name");
        sb.append('=');
        sb.append(((this.name == null)?"<null>":this.name));
        sb.append(',');
        sb.append("emailAddress");
        sb.append('=');
        sb.append(((this.emailAddress == null)?"<null>":this.emailAddress));
        sb.append(',');
        sb.append("avatarUrls");
        sb.append('=');
        sb.append(((this.avatarUrls == null)?"<null>":this.avatarUrls));
        sb.append(',');
        sb.append("displayName");
        sb.append('=');
        sb.append(((this.displayName == null)?"<null>":this.displayName));
        sb.append(',');
        sb.append("active");
        sb.append('=');
        sb.append(((this.active == null)?"<null>":this.active));
        sb.append(',');
        sb.append("deleted");
        sb.append('=');
        sb.append(((this.deleted == null)?"<null>":this.deleted));
        sb.append(',');
        sb.append("timeZone");
        sb.append('=');
        sb.append(((this.timeZone == null)?"<null>":this.timeZone));
        sb.append(',');
        sb.append("locale");
        sb.append('=');
        sb.append(((this.locale == null)?"<null>":this.locale));
        sb.append(',');
        sb.append("groups");
        sb.append('=');
        sb.append(((this.groups == null)?"<null>":this.groups));
        sb.append(',');
        sb.append("applicationRoles");
        sb.append('=');
        sb.append(((this.applicationRoles == null)?"<null>":this.applicationRoles));
        sb.append(',');
        sb.append("expand");
        sb.append('=');
        sb.append(((this.expand == null)?"<null>":this.expand));
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
        result = ((result* 31)+((this.avatarUrls == null)? 0 :this.avatarUrls.hashCode()));
        result = ((result* 31)+((this.displayName == null)? 0 :this.displayName.hashCode()));
        result = ((result* 31)+((this.active == null)? 0 :this.active.hashCode()));
        result = ((result* 31)+((this.timeZone == null)? 0 :this.timeZone.hashCode()));
        result = ((result* 31)+((this.groups == null)? 0 :this.groups.hashCode()));
        result = ((result* 31)+((this.locale == null)? 0 :this.locale.hashCode()));
        result = ((result* 31)+((this.emailAddress == null)? 0 :this.emailAddress.hashCode()));
        result = ((result* 31)+((this.expand == null)? 0 :this.expand.hashCode()));
        result = ((result* 31)+((this.deleted == null)? 0 :this.deleted.hashCode()));
        result = ((result* 31)+((this.name == null)? 0 :this.name.hashCode()));
        result = ((result* 31)+((this.self == null)? 0 :this.self.hashCode()));
        result = ((result* 31)+((this.key == null)? 0 :this.key.hashCode()));
        result = ((result* 31)+((this.applicationRoles == null)? 0 :this.applicationRoles.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof User) == false) {
            return false;
        }
        User rhs = ((User) other);
        return ((((((((((((((this.avatarUrls == rhs.avatarUrls)||((this.avatarUrls!= null)&&this.avatarUrls.equals(rhs.avatarUrls)))&&((this.displayName == rhs.displayName)||((this.displayName!= null)&&this.displayName.equals(rhs.displayName))))&&((this.active == rhs.active)||((this.active!= null)&&this.active.equals(rhs.active))))&&((this.timeZone == rhs.timeZone)||((this.timeZone!= null)&&this.timeZone.equals(rhs.timeZone))))&&((this.groups == rhs.groups)||((this.groups!= null)&&this.groups.equals(rhs.groups))))&&((this.locale == rhs.locale)||((this.locale!= null)&&this.locale.equals(rhs.locale))))&&((this.emailAddress == rhs.emailAddress)||((this.emailAddress!= null)&&this.emailAddress.equals(rhs.emailAddress))))&&((this.expand == rhs.expand)||((this.expand!= null)&&this.expand.equals(rhs.expand))))&&((this.deleted == rhs.deleted)||((this.deleted!= null)&&this.deleted.equals(rhs.deleted))))&&((this.name == rhs.name)||((this.name!= null)&&this.name.equals(rhs.name))))&&((this.self == rhs.self)||((this.self!= null)&&this.self.equals(rhs.self))))&&((this.key == rhs.key)||((this.key!= null)&&this.key.equals(rhs.key))))&&((this.applicationRoles == rhs.applicationRoles)||((this.applicationRoles!= null)&&this.applicationRoles.equals(rhs.applicationRoles))));
    }

}
