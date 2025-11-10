package com.checkmarx.flow.dto.gitlabdashboardv15.SAST;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.*;
import lombok.Builder;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
public class Details {


    @JsonProperty("code_flows")
    private CodeFlows codeFlows;
    @JsonIgnore
    @Builder.Default
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    @JsonProperty("code_flows")
    public CodeFlows getCodeFlows() {
        return codeFlows;
    }

    @JsonProperty("code_flows")
    public void setCodeFlows(CodeFlows codeFlows) {
        this.codeFlows = codeFlows;
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