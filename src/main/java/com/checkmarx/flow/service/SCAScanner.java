package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.sdk.config.ScaProperties;
import com.checkmarx.sdk.dto.sca.SCAParams;
import com.checkmarx.sdk.service.ScaClient;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SCAScanner implements VulnerabilityScanner {

    private final ScaClient scaClient;
    private final ScaProperties scaProperties;
    private final FlowProperties flowProperties;

    private SCAParams scaParams;

    @Override
    public void scan(ScanRequest scanRequest) {
        if (isScaScanConfigured()) {
        }
    }

    private boolean isScaScanConfigured() {
        return flowProperties.getEnabledVulnerabilityScanners().contains(ScannerType.SCA.getScanner());
    }

    /*
        Logic to determine the project name according to SAST project name determination.
        In this case project name is trying to be built as not as a multi tenant project
     */
    private String setProjectName(ScanRequest scanRequest) {
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