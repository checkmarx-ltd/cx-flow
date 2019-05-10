
package com.checkmarx.flow.dto.gitlab;

import com.fasterxml.jackson.annotation.*;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "access_level",
    "notification_level"
})
public class GroupAccess {

    @JsonProperty("access_level")
    private Integer accessLevel;
    @JsonProperty("notification_level")
    private Integer notificationLevel;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<>();

    @JsonProperty("access_level")
    public Integer getAccessLevel() {
        return accessLevel;
    }

    @JsonProperty("access_level")
    public void setAccessLevel(Integer accessLevel) {
        this.accessLevel = accessLevel;
    }

    @JsonProperty("notification_level")
    public Integer getNotificationLevel() {
        return notificationLevel;
    }

    @JsonProperty("notification_level")
    public void setNotificationLevel(Integer notificationLevel) {
        this.notificationLevel = notificationLevel;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
