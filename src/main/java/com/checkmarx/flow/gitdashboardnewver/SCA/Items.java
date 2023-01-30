
package com.checkmarx.flow.gitdashboardnewver.SCA;

import com.fasterxml.jackson.annotation.*;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Generated;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({

})

@Builder
@Data
@Generated("jsonschema2pojo")
public class Items {


    @JsonProperty("signatures")
    @Getter
    @Setter
    private List<com.checkmarx.flow.gitdashboardnewver.SCA.Signature> signatures;

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



}
