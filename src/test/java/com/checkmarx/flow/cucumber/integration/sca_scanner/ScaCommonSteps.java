package com.checkmarx.flow.cucumber.integration.sca_scanner;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.service.SCAScanner;
import com.checkmarx.sdk.config.ScaProperties;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;

@RequiredArgsConstructor
public class ScaCommonSteps {

    protected final FlowProperties flowProperties;
    protected final ScaProperties scaProperties;
    protected final SCAScanner scaScanner;

    protected void initSCAConfig() {
        scaProperties.setAppUrl("https://sca.scacheckmarx.com");
        scaProperties.setApiUrl("https://api.scacheckmarx.com");
        scaProperties.setAccessControlUrl("https://platform.checkmarx.net");
    }

    protected ScanRequest getBasicScanRequest(String projectName, String repoWithAuth) {
        return ScanRequest.builder()
                .project(projectName)
                .repoUrlWithAuth(repoWithAuth)
                .branch("master")
                .repoType(ScanRequest.Repository.GITHUB)
                .build();
    }

    protected ArrayList<String> createFiltersListFromString(String filters) {
        return new ArrayList<>(Arrays.asList(filters.split(",")));
    }
}