package com.checkmarx.flow.dto.gitlabdashboardv2;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Location {
    @JsonProperty("file")
    public String file;
    @JsonProperty("start_line")
    public Integer startLine;
    @JsonProperty("end_line")
    public Integer endLine;
    @Builder.Default
    @JsonProperty("class")
    public String clazz = "N/A";
    @Builder.Default
    @JsonProperty("method")
    public String method = "N/A";
    @JsonProperty("dependency")
    public Dependency dependency;
}

