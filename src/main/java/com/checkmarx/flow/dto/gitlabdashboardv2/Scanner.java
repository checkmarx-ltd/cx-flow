package com.checkmarx.flow.dto.gitlabdashboardv2;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Scanner {
    @JsonProperty("id")
    @Builder.Default
    public String id = "Checkmarx";
    @JsonProperty("name")
    @Builder.Default
    public String name = "Checkmarx";
}

