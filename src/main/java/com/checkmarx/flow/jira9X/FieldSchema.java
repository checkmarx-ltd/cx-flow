
package com.checkmarx.flow.jira9X;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties
@JsonPropertyOrder({
        "type",
        "system",
        "items",
        "custom",
        "customId"
})
public class FieldSchema  {
    @JsonProperty("type")
    private String type;
    @JsonProperty("system")
    private String system;
    @JsonProperty("items")
    private String items;
    @JsonProperty("custom")
    private String custom;
    @JsonProperty("customId")
    private Integer customId;

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("type")
    public void setType(String type) {
        this.type = type;
    }

    @JsonProperty("system")
    public String getSystem() {
        return system;
    }

    @JsonProperty("system")
    public void setSystem(String system) {
        this.system = system;
    }

    @JsonProperty("items")
    public String getItems() {
        return items;
    }

    @JsonProperty("items")
    public void setItems(String items) {
        this.items = items;
    }

    @JsonProperty("custom")
    public String getCustom() {
        return custom;
    }

    @JsonProperty("custom")
    public void setCustom(String custom) {
        this.custom = custom;
    }

    @JsonProperty("customId")
    public Long getCustomId() {
        try {
            Long longId = new Long(this.customId);
            return longId;
        }
        catch (NumberFormatException e)
        {
            return 0l;
        }
    }

    @JsonProperty("customId")
    public void setCustomId(Integer customId) {
        this.customId = customId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(FieldSchema.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("type");
        sb.append('=');
        sb.append(((this.type == null)?"<null>":this.type));
        sb.append(',');
        sb.append("system");
        sb.append('=');
        sb.append(((this.system == null)?"<null>":this.system));
        sb.append(',');
        sb.append("items");
        sb.append('=');
        sb.append(((this.items == null)?"<null>":this.items));
        sb.append(',');
        sb.append("custom");
        sb.append('=');
        sb.append(((this.custom == null)?"<null>":this.custom));
        sb.append(',');
        sb.append("customId");
        sb.append('=');
        sb.append(((this.customId == null)?"<null>":this.customId));
        sb.append(',');
        if (sb.charAt((sb.length()- 1)) == ',') {
            sb.setCharAt((sb.length()- 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

}
