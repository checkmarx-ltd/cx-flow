
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


/**
 * Provides information on the package where the vulnerability is located.
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "name"
})
@Generated("jsonschema2pojo")
public class Package__1 {

    /**
     * Name of the package where the vulnerability is located.
     * 
     */
    @JsonProperty("name")
    @JsonPropertyDescription("Name of the package where the vulnerability is located.")
    private String name;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * Name of the package where the vulnerability is located.
     * 
     */
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    /**
     * Name of the package where the vulnerability is located.
     * 
     */
    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
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
