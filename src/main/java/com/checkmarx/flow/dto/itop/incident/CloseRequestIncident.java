package com.checkmarx.flow.dto.itop.incident;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// {{rest_endpoint}}?version={{rest_version}}&json_data={
// "operation": "core/delete",
// "comment": "Failed space mission cover up 🙈🙉🙊",
// "class": "UserRequest",
// "key":
// {
//     "title": "Houston, got a problem!"
// },
// "simulate": false
// }

@Data
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CloseRequestIncident {

    private String operation;
    private String comment;
    @JsonProperty("class")
    private String classType;
    private CloseRequestKey key;
    private CloseRequestIncidentFields fields;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Setter
    @NoArgsConstructor
    public static class CloseRequestIncidentFields {
        private String status;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Setter
    @NoArgsConstructor
    public static class CloseRequestKey {
        private String title;
    }
    
}

