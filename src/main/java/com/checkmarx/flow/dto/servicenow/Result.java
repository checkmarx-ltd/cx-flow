package com.checkmarx.flow.dto.servicenow;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.collect.Lists;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "result"
})
public class Result {
    @JsonProperty("result")
    private List<com.checkmarx.flow.dto.servicenow.Incident> incidents = Lists.newArrayList();

    @JsonProperty("result")
    public List<Incident> getIncidents() {
        return incidents;
    }

    @JsonProperty("result")
    public void setIncidents(List<Incident> incidents) {
        this.incidents = incidents;
    }
}
