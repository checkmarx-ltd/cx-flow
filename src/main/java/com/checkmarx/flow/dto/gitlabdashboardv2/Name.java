package com.checkmarx.flow.dto.gitlabdashboardv2;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Name {
    @JsonProperty("name")
    public String dependencyname;
}

