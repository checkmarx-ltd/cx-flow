package com.checkmarx.flow.cucumber.integration.sca_scanner;

import com.checkmarx.flow.config.properties.FlowProperties;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.service.SCAScanner;
import com.checkmarx.flow.service.ScaConfigurationOverrider;
import com.checkmarx.sdk.config.RestClientConfig;
import com.checkmarx.sdk.config.ScaProperties;

import com.checkmarx.sdk.dto.RemoteRepositoryInfo;
import com.checkmarx.sdk.dto.sca.ScaConfig;
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
        scaProperties.setApiUrl("https://api-sca.checkmarx.net");
        scaProperties.setAccessControlUrl("https://platform.checkmarx.net");
    }

    protected ScanRequest getBasicScanRequest(String projectName, String repoWithAuth) {
        BugTracker bt = BugTracker.builder()
                .type(BugTracker.Type.JIRA)
                .customBean("JIRA")
                .build();
        ScanRequest request = ScanRequest.builder()
                .project(projectName)
                .repoUrlWithAuth(repoWithAuth)
                .branch("master")
                .repoType(ScanRequest.Repository.GITHUB)
                .bugTracker(bt)
                .build();
        scaConfigOverrider.initScaConfig(request);
        return request;
    }

    protected List<String> createFiltersListFromString(String filters) {
        return Arrays.stream(filters.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    protected RestClientConfig createRestClientConfig(ScaProperties scaProperties, String projectName) {
        ScaConfig scaConfig = new ScaConfig();
        scaConfig.setTenant(scaProperties.getTenant());
        scaConfig.setApiUrl(scaProperties.getApiUrl());
        scaConfig.setUsername(scaProperties.getUsername());
        scaConfig.setPassword(scaProperties.getPassword());
        scaConfig.setAccessControlUrl(scaProperties.getAccessControlUrl());
        scaConfig.setRemoteRepositoryInfo(new RemoteRepositoryInfo());

        RestClientConfig restClientConfig = new RestClientConfig();
        restClientConfig.setScaConfig(scaConfig);
        restClientConfig.setProjectName(projectName);

        return restClientConfig;
    }
}