package com.checkmarx.flow.dto.itop.incident;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Incident {

    @JsonProperty("id")
    private String id;

    @JsonProperty("ref")
    private String ref;

    @JsonProperty("org_name")
    private String orgName;

    @JsonProperty("status")
    private String status;

    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private String description;

    @JsonProperty("priority")
    private String priority;

    @JsonProperty("urgency")
    private String urgency;

    @JsonProperty("service_name")
    private String serviceName;

    public String toString() {
        return "Incident{" +
                "id='" + id + '\'' +
                ", ref='" + ref + '\'' +
                ", orgName='" + orgName + '\'' +
                ", status='" + status + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", priority='" + priority + '\'' +
                ", urgency='" + urgency + '\'' +
                ", serviceName='" + serviceName + '\'' +
                '}';
    }

}