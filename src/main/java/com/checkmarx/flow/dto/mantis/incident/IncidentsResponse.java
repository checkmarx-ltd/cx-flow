package com.checkmarx.flow.dto.mantis.incident;

import java.util.List;

import com.checkmarx.flow.dto.mantis.Incident;
import com.fasterxml.jackson.annotation.JsonProperty;

public class IncidentsResponse {
    @JsonProperty("issues")
    private List<Incident> incidents;

    public List<Incident> getIncidents() {
        return incidents;
    }

    public void setIncidents(List<Incident> incidents) {
        this.incidents = incidents;
    }
}