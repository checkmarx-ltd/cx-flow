
package com.checkmarx.flow.gitdashboardnewver.SCA;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "package",
        "version",
        "iid",
        "direct",
        "dependency_path"
})
@Generated("jsonschema2pojo")
public class DependencyPath__1 {

    /**
     * ID that is unique in the scope of a parent object, and specific to the resource type.
     * (Required)
     * 
     */
    @JsonProperty("iid")
    @JsonPropertyDescription("ID that is unique in the scope of a parent object, and specific to the resource type.")
    private Double iid;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * ID that is unique in the scope of a parent object, and specific to the resource type.
     * (Required)
     * 
     */
    @JsonProperty("iid")
    public Double getIid() {
        return iid;
    }

    /**
     * ID that is unique in the scope of a parent object, and specific to the resource type.
     * (Required)
     * 
     */
    @JsonProperty("iid")
    public void setIid(Double iid) {
        this.iid = iid;
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
