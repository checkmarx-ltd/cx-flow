package com.checkmarx.flow.dto.rally;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "CreateResultAction"
})
public class CreateResultAction {

    @JsonProperty("CreateResultAction")
    private CreateResult createResult;
    @JsonIgnore
    private Map<String, java.lang.Object> additionalProperties = new HashMap<String, java.lang.Object>();

    @JsonProperty("CreateResultAction")
    public CreateResult getCreateResult() {
        return createResult;
    }

    @JsonProperty("CreateResultAction")
    public void setCreateResult(CreateResult createResult) {
        this.createResult = createResult;
    }

    @JsonAnyGetter
    public Map<String, java.lang.Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, java.lang.Object value) {
        this.additionalProperties.put(name, value);
    }

}
