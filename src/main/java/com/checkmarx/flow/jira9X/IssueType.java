
package com.checkmarx.flow.jira9X;

import java.net.URI;
import java.net.URISyntaxException;
import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties
@JsonPropertyOrder({
        "self",
        "id",
        "description",
        "iconUrl",
        "name",
        "subtask",
        "avatarId",
        "expand",
        "fields"
})


@Generated("jsonschema2pojo")
public class IssueType {

    @JsonProperty("self")
    private String self;
    @JsonProperty("id")
    private String id;
    @JsonProperty("description")
    private String description;
    @JsonProperty("iconUrl")
    private String iconUrl;
    @JsonProperty("name")
    private String name;
    @JsonProperty("subtask")
    private Boolean subtask;
    @JsonProperty("avatarId")
    private Integer avatarId;
    @JsonProperty("expand")
    private String expand;
    @JsonProperty("fields")
    private Object fields;

    @JsonProperty("self")
    public URI getSelf() {
        if(this.self!=null)
        {
            try {
                URI selfUri = new URI(this.self);
                return selfUri;
            } catch (URISyntaxException e) {
                try {
                    //default value
                    return new URI("xyz@xyz.com");
                } catch (URISyntaxException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

        return null;
    }

    @JsonProperty("self")
    public void setSelf(String self) {
        this.self = self;
    }

    @JsonProperty("id")
    public Long getId() {
        if(this.id != null)
        {
            try{
                Long longId = Long.parseLong(this.id);
                return longId;
            }catch (NumberFormatException e)
            {
                //default value
                return 0l;
            }

        }
        return null;
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

    @JsonProperty("iconUrl")
    public URI getIconUrl() {
        if(this.iconUrl!=null)
        {
            try {
                URI  icon = new URI(iconUrl);
                return icon;
            } catch (URISyntaxException e) {
                try {
                    //default value
                    return new URI("xyz@xyz.com");
                } catch (URISyntaxException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

        return null;
    }

    @JsonProperty("iconUrl")
    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("subtask")
    public Boolean getSubtask() {
        return subtask;
    }

    @JsonProperty("subtask")
    public void setSubtask(Boolean subtask) {
        this.subtask = subtask;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(IssueType.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        sb.append("iconUrl");
        sb.append('=');
        sb.append(((this.iconUrl == null)?"<null>":this.iconUrl));
        sb.append(',');
        sb.append("name");
        sb.append('=');
        sb.append(((this.name == null)?"<null>":this.name));
        sb.append(',');
        sb.append("subtask");
        sb.append('=');
        sb.append(((this.subtask == null)?"<null>":this.subtask));
        sb.append(',');
        if (sb.charAt((sb.length()- 1)) == ',') {
            sb.setCharAt((sb.length()- 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

}
