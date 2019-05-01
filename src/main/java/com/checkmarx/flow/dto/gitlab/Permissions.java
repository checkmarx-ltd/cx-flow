
package com.checkmarx.flow.dto.gitlab;

import com.fasterxml.jackson.annotation.*;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "project_access",
    "group_access"
})
public class Permissions {

    @JsonProperty("project_access")
    private Object projectAccess;
    @JsonProperty("group_access")
    private GroupAccess groupAccess;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("project_access")
    public Object getProjectAccess() {
        return projectAccess;
    }

    @JsonProperty("project_access")
    public void setProjectAccess(Object projectAccess) {
        this.projectAccess = projectAccess;
    }

    @JsonProperty("group_access")
    public GroupAccess getGroupAccess() {
        return groupAccess;
    }

    @JsonProperty("group_access")
    public void setGroupAccess(GroupAccess groupAccess) {
        this.groupAccess = groupAccess;
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
