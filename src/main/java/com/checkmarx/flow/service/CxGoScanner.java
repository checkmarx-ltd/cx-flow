package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.sastscanning.ScanRequestConverter;
import com.checkmarx.sdk.config.CxGoProperties;
import com.checkmarx.sdk.service.CxOsaClient;
import com.cx.restclient.CxGoClientImpl;
import com.cx.restclient.ScannerClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Service
@Slf4j
public class CxGoScanner extends AbstractVulnerabilityScanner {
    
    private final CxGoClientImpl scannerClient;
    private final String scanType = CxGoProperties.CONFIG_PREFIX;
    protected final ScanRequestConverter scanRequestConverter;
    
    public CxGoScanner(ResultsService resultsService, HelperService helperService, CxGoProperties cxProperties, FlowProperties flowProperties, CxOsaClient osaService, EmailService emailService, ScanRequestConverter scanRequestConverter, BugTrackerEventTrigger bugTrackerEventTrigger, ProjectNameGenerator projectNameGenerator, CxGoClientImpl scannerClient, ScanRequestConverter scanRequestConverter1) {
        super(resultsService, helperService, cxProperties, flowProperties, emailService, bugTrackerEventTrigger, projectNameGenerator);
        this.scannerClient = scannerClient;
        this.scanRequestConverter = scanRequestConverter1;
        this.scanRequestConverter.setScannerClient(scannerClient);
    }
    
    @Override
    protected void cxBatch(ScanRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected String createOsaScan(ScanRequest request, Integer projectId) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected ScanRequestConverter getScanRequestConverter() {
        return scanRequestConverter;
    }

    @Override
    protected void cxParseResults(ScanRequest request, File file){
        throw new UnsupportedOperationException();
    }
    
    public ScannerClient getScannerClient() {
        return scannerClient;
    }

    @Override
    public boolean isEnabled() {
        List<String> enabledScanners = flowProperties.getEnabledVulnerabilityScanners();

        return enabledScanners != null
                && enabledScanners.stream().anyMatch(scanner -> scanner.equalsIgnoreCase(scanType));

    }
}