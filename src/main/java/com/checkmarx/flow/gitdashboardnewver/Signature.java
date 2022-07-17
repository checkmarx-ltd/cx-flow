package com.checkmarx.flow.gitdashboardnewver;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({

})

@Data
@Builder
public class Signature {

    @JsonProperty("algorithm")
    @Getter
    @Setter
    @Builder.Default
    private String algo="SAST-Algorithm";


    @JsonProperty("value")
    @Getter
    @Setter
    @Builder.Default
    private String value="NA";


}
