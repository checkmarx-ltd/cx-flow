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
        "_rallyAPIMajor",
        "_rallyAPIMinor",
        "_ref",
        "_type",
        "Count"
})
public class DefectSuites {

    @JsonProperty("_rallyAPIMajor")
    private String rallyAPIMajor;
    @JsonProperty("_rallyAPIMinor")
    private String rallyAPIMinor;
    @JsonProperty("_ref")
    private String ref;
    @JsonProperty("_type")
    private String type;
    @JsonProperty("Count")
    private Long count;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("_rallyAPIMajor")
    public String getRallyAPIMajor() {
        return rallyAPIMajor;
    }

    @JsonProperty("_rallyAPIMajor")
    public void setRallyAPIMajor(String rallyAPIMajor) {
        this.rallyAPIMajor = rallyAPIMajor;
    }

    @JsonProperty("_rallyAPIMinor")
    public String getRallyAPIMinor() {
        return rallyAPIMinor;
    }

    @JsonProperty("_rallyAPIMinor")
    public void setRallyAPIMinor(String rallyAPIMinor) {
        this.rallyAPIMinor = rallyAPIMinor;
    }

    @JsonProperty("_ref")
    public String getRef() {
        return ref;
    }

    @JsonProperty("_ref")
    public void setRef(String ref) {
        this.ref = ref;
    }

    @JsonProperty("_type")
    public String getType() {
        return type;
    }

    @JsonProperty("_type")
    public void setType(String type) {
        this.type = type;
    }

    @JsonProperty("Count")
    public Long getCount() {
        return count;
    }

    @JsonProperty("Count")
    public void setCount(Long count) {
        this.count = count;
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