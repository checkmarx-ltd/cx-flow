
package com.checkmarx.flow.gitdashboardnewver;

import java.util.HashMap;
import java.util.List;
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


/**
 * Describes how this vulnerability should be tracked as the project changes.
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "type",
    "items"
})
@Generated("jsonschema2pojo")
@Data
@Builder
public class Tracking {

    /**
     * Each tracking type must declare its own type.
     * 
     */
//    @JsonProperty("type")
//    @JsonPropertyDescription("Each tracking type must declare its own type.")
//    private String type;


    @JsonProperty("items")
    @JsonPropertyDescription("Each tracking type must declare its own type.")
    @Getter
    @Setter
    @Builder.Default
    private List<Items> lstItems;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * Each tracking type must declare its own type.
     * 
     */
//    @JsonProperty("type")
//    public String getType() {
//        return type;
//    }
//
//    /**
//     * Each tracking type must declare its own type.
//     *
//     */
//    @JsonProperty("type")
//    public void setType(String type) {
//        this.type = type;
//    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
