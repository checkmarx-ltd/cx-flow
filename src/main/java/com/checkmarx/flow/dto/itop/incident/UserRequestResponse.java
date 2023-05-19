package com.checkmarx.flow.dto.itop.incident;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class UserRequestResponse {
    private String key;
    private UserRequestFields fields;

    public String toString() {
        return "UserRequestResponse{" +
                "key='" + key + '\'' +
                ", fields=" + fields +
                '}';
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserRequestFields {

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
            return "UserRequestFields{" +
                    "ref='" + ref + '\'' +
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
}