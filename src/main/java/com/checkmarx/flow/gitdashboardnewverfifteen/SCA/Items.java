
package com.checkmarx.flow.gitdashboardnewverfifteen.SCA;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.*;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({

})
@Generated("jsonschema2pojo")
@Builder
@Data
public class Items {

    @JsonProperty("signatures")
    @Getter
    @Setter
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

}
