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
        "_refObjectUUID",
        "_type"
})
public class RevisionHistory {

    @JsonProperty("_rallyAPIMajor")
    private String rallyAPIMajor;
    @JsonProperty("_rallyAPIMinor")
    private String rallyAPIMinor;
    @JsonProperty("_ref")
    private String ref;
    @JsonProperty("_refObjectUUID")
    private String refObjectUUID;
    @JsonProperty("_type")
    private String type;
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

    @JsonProperty("_ref")
    public String getRef() {
        return ref;
    }

    @JsonProperty("_ref")
    public void setRef(String ref) {
        this.ref = ref;
    }

    @JsonProperty("_refObjectUUID")
    public String getRefObjectUUID() {
        return refObjectUUID;
    }

    @JsonProperty("_refObjectUUID")
    public void setRefObjectUUID(String refObjectUUID) {
        this.refObjectUUID = refObjectUUID;
    }

    @JsonProperty("_type")
    public String getType() {
        return type;
    }

    @JsonProperty("_type")
    public void setType(String type) {
        this.type = type;
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
