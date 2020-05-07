package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaRuntimeException;
import com.checkmarx.sdk.config.ScaProperties;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.sca.SCAParams;
import com.checkmarx.sdk.dto.sca.SCAResults;
import com.checkmarx.sdk.service.ScaClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SCAScanner implements VulnerabilityScanner {
    private final ScaClient scaClient;
    private final FlowProperties flowProperties;

    @Override
    public ScanResults scan(ScanRequest scanRequest, String projectName) {
        ScanResults result = null;
        if (isThisScannerEnabled()) {
            SCAParams internalScaParams = toScaParams(scanRequest, projectName);
            try {
                SCAResults internalResults = scaClient.scanRemoteRepo(internalScaParams);
                result = toScanResults(internalResults);
            } catch (IOException e) {
                final String message = "SCA scan failed.";
                log.error(message, e);
                throw new MachinaRuntimeException(message);
            }
        }
        return result;
    }

    private boolean isThisScannerEnabled() {
        List<String> enabledScanners = flowProperties.getEnabledVulnerabilityScanners();
        return enabledScanners != null && enabledScanners.contains(ScaProperties.CONFIG_PREFIX);
    }

    private ScanResults toScanResults(SCAResults scaResults) {
        return ScanResults.builder()
                .scaResults(scaResults)
                .build();
    }

    private SCAParams toScaParams(ScanRequest scanRequest, String projectName) {
        URL parsedUrl = getRepoUrl(scanRequest);

        return SCAParams.builder()
                .projectName(projectName)
                .remoteRepoUrl(parsedUrl)
                .build();
    }

    private URL getRepoUrl(ScanRequest scanRequest) {
        URL parsedUrl;
        try {
            parsedUrl = new URL(scanRequest.getRepoUrl());
        } catch (MalformedURLException e) {
            log.error("Failed to parse repository URL: '{}'", scanRequest.getRepoUrl());
            throw new MachinaRuntimeException("Invalid repository URL.");
        }
        return parsedUrl;
    }

    /*
        Logic to determine the project name according to SAST project name determination.
        In this case project name is trying to be built as not as a multi tenant project
     */
    private String getProjectName(ScanRequest scanRequest) {
        StringBuilder projectName = new StringBuilder();

        String repoName = scanRequest.getRepoName();
        String namespace = scanRequest.getNamespace();
        String branch = scanRequest.getBranch();

        if (!StringUtils.isEmpty(namespace)) {
            projectName.append(namespace);
        }

        if (!StringUtils.isEmpty(repoName)) {
            projectName.append("-").append(repoName);
        }

        if (!StringUtils.isEmpty(branch)) {
            projectName.append("-").append(branch);
        }
        return projectName.toString();
    }
}