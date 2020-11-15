package com.checkmarx.flow.cucumber.integration.sca_scanner;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.service.SCAScanner;
import com.checkmarx.flow.service.ScaConfigurationOverrider;
import com.checkmarx.sdk.config.ScaProperties;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ScaCommonSteps {

    protected final FlowProperties flowProperties;
    protected final SCAScanner scaScanner;
    private final ScaConfigurationOverrider scaConfigOverrider;

    public static void initSCAConfig(ScaProperties scaProperties) {
        scaProperties.setAppUrl("https://sca.scacheckmarx.com");
        scaProperties.setApiUrl("https://api.scacheckmarx.com");
        scaProperties.setAccessControlUrl("https://platform.checkmarx.net");
    }

    protected ScanRequest getBasicScanRequest(String projectName, String repoWithAuth) {
        ScanRequest request = ScanRequest.builder()
                .project(projectName)
                .repoUrlWithAuth(repoWithAuth)
                .branch("master")
                .repoType(ScanRequest.Repository.GITHUB)
                .build();
        scaConfigOverrider.initScaConfig(request);
        return request;
    }

    protected List<String> createFiltersListFromString(String filters) {
        return Arrays.stream(filters.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }
}