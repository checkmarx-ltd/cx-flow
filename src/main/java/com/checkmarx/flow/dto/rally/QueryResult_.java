package com.checkmarx.flow.dto.rally;

import com.fasterxml.jackson.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "_rallyAPIMajor",
        "_rallyAPIMinor",
        "Errors",
        "Warnings",
        "TotalResultCount",
        "StartIndex",
        "PageSize",
        "Results"
})
public class QueryResult_ {

    @JsonProperty("_rallyAPIMajor")
    private String rallyAPIMajor;
    @JsonProperty("_rallyAPIMinor")
    private String rallyAPIMinor;
    @JsonProperty("Errors")
    private List<Object> errors = null;
    @JsonProperty("Warnings")
    private List<String> warnings = null;
    @JsonProperty("TotalResultCount")
    private Long totalResultCount;
    @JsonProperty("StartIndex")
    private Long startIndex;
    @JsonProperty("PageSize")
    private Long pageSize;
    @JsonProperty("Results")
    private List<Result> results = null;
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

    @JsonProperty("Errors")
    public List<Object> getErrors() {
        return errors;
    }

    @JsonProperty("Errors")
    public void setErrors(List<Object> errors) {
        this.errors = errors;
    }

    @JsonProperty("Warnings")
    public List<String> getWarnings() {
        return warnings;
    }

    @JsonProperty("Warnings")
    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    @JsonProperty("TotalResultCount")
    public Long getTotalResultCount() {
        return totalResultCount;
    }

    @JsonProperty("TotalResultCount")
    public void setTotalResultCount(Long totalResultCount) {
        this.totalResultCount = totalResultCount;
    }

    @JsonProperty("StartIndex")
    public Long getStartIndex() {
        return startIndex;
    }

    @JsonProperty("StartIndex")
    public void setStartIndex(Long startIndex) {
        this.startIndex = startIndex;
    }

    @JsonProperty("PageSize")
    public Long getPageSize() {
        return pageSize;
    }

    @JsonProperty("PageSize")
    public void setPageSize(Long pageSize) {
        this.pageSize = pageSize;
    }

    @JsonProperty("Results")
    public List<Result> getResults() {
        return results;
    }

    @JsonProperty("Results")
    public void setResults(List<Result> results) {
        this.results = results;
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