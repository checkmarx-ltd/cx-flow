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
    private static final String SCAN_TYPE = ScaProperties.CONFIG_PREFIX;

    private final ScaClient scaClient;
    private final FlowProperties flowProperties;

    @Override
    public ScanResults scan(ScanRequest scanRequest) {
        ScanResults result = null;
        log.info("--------------------- Initiating new {} scan ---------------------", SCAN_TYPE);
        SCAParams internalScaParams = toScaParams(scanRequest);
        try {
            SCAResults internalResults = scaClient.scanRemoteRepo(internalScaParams);
            result = toScanResults(internalResults);
        } catch (IOException e) {
            final String message = "SCA scan failed.";
            log.error(message, e);
            throw new MachinaRuntimeException(message);
        }
        return result;
    }

    @Override
    public boolean isThisScannedEnabled() {
        List<String> enabledScanners = flowProperties.getEnabledVulnerabilityScanners();
        return enabledScanners != null && enabledScanners.contains(ScaProperties.CONFIG_PREFIX);
    }

    private ScanResults toScanResults(SCAResults scaResults) {
        return ScanResults.builder()
                .scaResults(scaResults)
                .build();
    }

    private SCAParams toScaParams(ScanRequest scanRequest) {
        URL parsedUrl = getRepoUrl(scanRequest);

        return SCAParams.builder()
                .projectName(scanRequest.getProject())
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
}