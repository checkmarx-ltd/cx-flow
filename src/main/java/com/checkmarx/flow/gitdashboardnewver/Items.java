
package com.checkmarx.flow.gitdashboardnewver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;

import com.checkmarx.sdk.dto.ScanResults;
import com.fasterxml.jackson.annotation.*;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({

})
@Generated("jsonschema2pojo")
@Data
@Builder
public class Items {


    @JsonProperty("signatures")
    @JsonPropertyDescription("Each tracking type must declare its own type.")
    @Getter
    @Setter
    @Builder.Default
    private List<Signature> signatures;

    @JsonProperty("file")
    @Getter
    @Setter
    @Builder.Default
    private String file="NA";

    @JsonProperty("end_line")
    @JsonPropertyDescription("Each tracking type must declare its own type.")
    @Getter
    @Setter
    @Builder.Default
    private int end_line=0;

    @JsonProperty("start_line")
    @JsonPropertyDescription("Each tracking type must declare its own type.")
    @Getter
    @Setter
    @Builder.Default
    private int start_line=0;


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
