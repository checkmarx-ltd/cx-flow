
package com.checkmarx.flow.gitlabdashboardfifteen.sca;

import com.checkmarx.flow.gitlabdashboardfifteen.sca.Items;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Generated;
import java.util.List;


/**
 * Describes how this vulnerability should be tracked as the project changes.
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "type"
})
@Generated("jsonschema2pojo")
@Builder
@Data
public class Tracking {

    /**
     * Each tracking type must declare its own type.
     * 
     */
    @JsonProperty("items")
    @Getter
    @Setter
    private List<Items> lstItems;

    @JsonProperty("type")
    @JsonPropertyDescription("Each tracking type must declare its own type.")
    private String type;

    /**
     * Each tracking type must declare its own type.
     * 
     */
    @JsonProperty("type")
    public String getType() {
        return type;
    }

    /**
     * Each tracking type must declare its own type.
     * 
     */
    @JsonProperty("type")
    public void setType(String type) {
        this.type = type;
    }


}
