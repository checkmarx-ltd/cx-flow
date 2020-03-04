package com.checkmarx.flow.dto.rally;

import java.util.HashMap;
import java.util.List;
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
        "Errors",
        "Warnings",
        "Object"
})
public class CreateResult {

    @JsonProperty("_rallyAPIMajor")
    private String rallyAPIMajor;
    @JsonProperty("_rallyAPIMinor")
    private String rallyAPIMinor;
    @JsonProperty("Errors")
    private List<java.lang.Object> errors = null;
    @JsonProperty("Warnings")
    private List<java.lang.Object> warnings = null;
    @JsonProperty("Object")
    private com.checkmarx.flow.dto.rally.Object object;
    @JsonIgnore
    private Map<String, java.lang.Object> additionalProperties = new HashMap<String, java.lang.Object>();

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

    @JsonProperty("Errors")
    public List<java.lang.Object> getErrors() {
        return errors;
    }

    @JsonProperty("Errors")
    public void setErrors(List<java.lang.Object> errors) {
        this.errors = errors;
    }

    @JsonProperty("Warnings")
    public List<java.lang.Object> getWarnings() {
        return warnings;
    }

    @JsonProperty("Warnings")
    public void setWarnings(List<java.lang.Object> warnings) {
        this.warnings = warnings;
    }

    @JsonProperty("Object")
    public com.checkmarx.flow.dto.rally.Object getObject() {
        return object;
    }

    @JsonProperty("Object")
    public void setObject(com.checkmarx.flow.dto.rally.Object object) {
        this.object = object;
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
