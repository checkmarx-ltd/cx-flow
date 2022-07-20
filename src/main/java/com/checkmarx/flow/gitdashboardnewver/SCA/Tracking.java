
package com.checkmarx.flow.gitdashboardnewver.SCA;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;

import com.checkmarx.flow.gitdashboardnewver.Signature;
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


/**
 * Describes how this vulnerability should be tracked as the project changes.
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "type"
})
@Generated("jsonschema2pojo")
@Data
@Builder
public class Tracking {

    /**
     * Each tracking type must declare its own type.
     * 
     */

    @JsonProperty("items")
    @Getter
    @Setter
    private List<com.checkmarx.flow.gitdashboardnewver.SCA.Items> lstItems;


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
