package com.checkmarx.flow.dto.bitbucketserver;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "size",
        "limit",
        "isLastPage",
        "values",
        "start"
})
public class Children {

    @JsonProperty("size")
    private Integer size;
    @JsonProperty("limit")
    private Integer limit;
    @JsonProperty("isLastPage")
    private Boolean isLastPage;
    @JsonProperty("values")
    private List<Value> values = null;
    @JsonProperty("start")
    private Integer start;

    @JsonProperty("size")
    public Integer getSize() {
        return size;
    }

    @JsonProperty("size")
    public void setSize(Integer size) {
        this.size = size;
    }

    @JsonProperty("limit")
    public Integer getLimit() {
        return limit;
    }

    @JsonProperty("limit")
    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    @JsonProperty("isLastPage")
    public Boolean getIsLastPage() {
        return isLastPage;
    }

    @JsonProperty("isLastPage")
    public void setIsLastPage(Boolean isLastPage) {
        this.isLastPage = isLastPage;
    }

    @JsonProperty("values")
    public List<Value> getValues() {
        return values;
    }

    @JsonProperty("values")
    public void setValues(List<Value> values) {
        this.values = values;
    }

    @JsonProperty("start")
    public Integer getStart() {
        return start;
    }

    @JsonProperty("start")
    public void setStart(Integer start) {
        this.start = start;
    }

}