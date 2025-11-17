package com.checkmarx.flow.dto.gitlabdashboardv2;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Dependency {
    @JsonProperty("package")
    public Object pkg;
    @JsonProperty("version")
    public String version;
}

