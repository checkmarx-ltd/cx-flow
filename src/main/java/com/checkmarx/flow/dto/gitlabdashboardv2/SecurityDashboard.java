package com.checkmarx.flow.dto.gitlabdashboardv2;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class SecurityDashboard {
    @JsonProperty("version")
    @Builder.Default
    public String version = "2.0";
    @JsonProperty("vulnerabilities")
    public List<Vulnerability> vulnerabilities;
    @JsonProperty("remediations")
    public List<String> remediations;
}

