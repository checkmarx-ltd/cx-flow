
package com.checkmarx.flow.dto.bitbucket.mainBranch;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "default_merge_strategy",
    "branching_model"
})
@Generated("jsonschema2pojo")
public class OverrideSettings {

    @JsonProperty("default_merge_strategy")
    private Boolean defaultMergeStrategy;
    @JsonProperty("branching_model")
    private Boolean branchingModel;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("default_merge_strategy")
    public Boolean getDefaultMergeStrategy() {
        return defaultMergeStrategy;
    }

    @JsonProperty("default_merge_strategy")
    public void setDefaultMergeStrategy(Boolean defaultMergeStrategy) {
        this.defaultMergeStrategy = defaultMergeStrategy;
    }

    @JsonProperty("branching_model")
    public Boolean getBranchingModel() {
        return branchingModel;
    }

    @JsonProperty("branching_model")
    public void setBranchingModel(Boolean branchingModel) {
        this.branchingModel = branchingModel;
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
