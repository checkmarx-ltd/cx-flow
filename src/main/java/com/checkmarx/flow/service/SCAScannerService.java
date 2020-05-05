package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.sdk.config.ScaProperties;
import com.checkmarx.sdk.dto.sca.SCAParams;
import com.checkmarx.sdk.service.ScaClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SCAScannerService implements VulnerabilityScanner {

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
}