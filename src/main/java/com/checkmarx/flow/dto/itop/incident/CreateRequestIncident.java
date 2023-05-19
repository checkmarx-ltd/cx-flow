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
public class CreateRequestIncident {
    // {
            //     "operation": "core/create",
            //     "comment": "Synchronization from blah...",
            //     "class": "UserRequest",
            //     "fields":
            //     {
            //        "org_id": "SELECT Organization WHERE name = \"Demo\"",
            //        "service_id": "SELECT Service WHERE name = \"Software\"",
            //        "title": "Houston, got a problem!",
            //        "description": "The fridge is empty",
            //        "origin": "monitoring",
            //        "priority": 3,
            //        "urgency": 3
            //     }
            //  }

    private String operation;
    private String comment;
    @JsonProperty("class")
    private String classType;
    private CreateRequestFields fields;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Setter
    @NoArgsConstructor
    public static class CreateRequestFields {
        private String org_id;
        private String service_id;
        private String title;
        private String description;
        private String origin;
        private int priority;
        private int urgency;
    }
    
}
