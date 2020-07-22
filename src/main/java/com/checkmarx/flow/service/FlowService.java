package com.checkmarx.flow.service;

import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.sdk.dto.ScanResults;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * High level business logic for CxFlow automation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FlowService {

    private final List<VulnerabilityScanner> scanners;
    private final ProjectNameGenerator projectNameGenerator;
    private final ResultsService resultsService;

    /**
     * Main entry point for the automation process initiated by webhooks.
     * Marked as async, because we don't wait for scan completion in webhooks handler: otherwise version control
     * provider will fail the webhook request by timeout.
     */
    @Async("webHook")
    public void initiateAutomation(ScanRequest scanRequest) {
        String effectiveProjectName = projectNameGenerator.determineProjectName(scanRequest);
        scanRequest.setProject(effectiveProjectName);
        List<VulnerabilityScanner> enabledScanners = getEnabledScanners(scanRequest);
        runScanRequest(scanRequest, enabledScanners);
    }

    private void runScanRequest(ScanRequest scanRequest, List<VulnerabilityScanner> scanners) {
        ScanResults combinedResults = new ScanResults();

        scanners.forEach(scanner -> {
            ScanResults scanResults = scanner.scan(scanRequest);
            combinedResults.mergeWith(scanResults);
        });
        resultsService.publishCombinedResults(scanRequest, combinedResults);
    }

    private List<VulnerabilityScanner> getEnabledScanners(ScanRequest scanRequest) {
        List<VulnerabilityScanner> enabledScanners = new ArrayList<>();

        List<VulnerabilityScanner> scanRequestVulnerabilityScanners = scanRequest.getVulnerabilityScanners();
        if (CollectionUtils.isNotEmpty(scanRequestVulnerabilityScanners)) {
            enabledScanners.addAll(scanRequestVulnerabilityScanners);
        } else {
            scanners.forEach(scanner -> {
                if (scanner.isEnabled()) {
                    enabledScanners.add(scanner);
                }
            });
        }

        return enabledScanners;
    }
}
