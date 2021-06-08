package com.checkmarx.flow.dto.iast.manager.dto;


import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScanVulnerabilities {
    private long projectId;

    private Long scanId;

    @Singular
    private List<VulnerabilityInfo> vulnerabilities;
}
