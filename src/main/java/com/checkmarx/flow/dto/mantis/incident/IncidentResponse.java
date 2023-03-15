package com.checkmarx.flow.dto.mantis.incident;

import com.checkmarx.flow.dto.mantis.Incident;
import com.fasterxml.jackson.annotation.JsonProperty;

public class IncidentResponse {
    @JsonProperty("issue")
    private Incident incident;

    public Incident getIncident() {
        return incident;
    }

    public void setIncident(Incident incident) {
        this.incident = incident;
    }
}

