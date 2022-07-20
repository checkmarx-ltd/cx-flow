
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
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "iid",
        "name",
        "version",
        "isResolved",
        "isDevelopment"
})
@Generated("jsonschema2pojo")
@Data
@Builder
@Getter
@Setter
public class DependencyPath {

    @JsonProperty("iid")
    @Builder.Default
    @JsonPropertyDescription("ID that is unique in the scope of a parent object, and specific to the resource type.")
    private int iid=123;


    @JsonProperty("name")
    private String name;


    @JsonProperty("version")
    private String version;


    @JsonProperty("isResolved")
    private String isResolved;

    @JsonProperty("isDevelopment")
    private String isDevelopment;

    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();



    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
