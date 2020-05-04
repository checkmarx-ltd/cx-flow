package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.sdk.config.CxScaProperties;
import com.checkmarx.sdk.dto.sca.SCAParams;
import com.checkmarx.sdk.service.CxScaClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SCAScannerService implements VulnerabilityScanner {

    private final CxScaClient cxScaClient;
    private final CxScaProperties cxScaProperties;
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