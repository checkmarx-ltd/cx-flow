
package com.checkmarx.flow.dto.gitlabdashboardv14.SCA;

import java.util.List;
import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    private List<Items> lstItems;





}
