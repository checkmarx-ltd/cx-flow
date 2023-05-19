package com.checkmarx.flow.dto.itop.incident;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateRequestIncident {

    private String operation;
    private String comment;
    @JsonProperty("class")
    private String classType;
    private UpdateRequestKey key;
    private UpdateRequestIncidentFields fields;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Setter
    @NoArgsConstructor
    public static class UpdateRequestIncidentFields {
        private String title;
        private String description;
        private int priority;
        private int urgency;
        private String status;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Setter
    @NoArgsConstructor
    public static class UpdateRequestKey {
        private String title;
    }
    
}

