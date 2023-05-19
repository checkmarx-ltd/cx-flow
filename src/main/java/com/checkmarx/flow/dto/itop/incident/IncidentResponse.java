package com.checkmarx.flow.dto.itop.incident;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class IncidentResponse {

    @JsonProperty("code")
    private int code;

    @JsonProperty("message")
    private String message;

    @JsonProperty("objects")
    private Objects objects;

    public String toString() {
        return "IncidentResponse{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", objects=" + objects +
                '}';
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class Objects {

        @JsonProperty("UserRequest")
        private UserRequest userRequest;

        public String toString() {
            return "Objects{" +
                    "userRequestMap=" + userRequest +
                    '}';
        }

        @AllArgsConstructor
        @NoArgsConstructor
        @Getter
        @Setter
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class UserRequest {

            @JsonProperty("key")
            private int key;

            @JsonProperty("fields")
            private Fields fields;

            public String toString() {
                return "UserRequest{" +
                        "key=" + key +
                        ", fields=" + fields +
                        '}';
            }

            @AllArgsConstructor
            @NoArgsConstructor
            @Getter
            @Setter
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Fields {

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
                    return "Fields{" +
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
    }
}
