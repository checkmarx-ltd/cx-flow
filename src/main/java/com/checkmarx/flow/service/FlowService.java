package com.checkmarx.flow.service;

import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.sdk.dto.ScanResults;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlowService {

    private final List<VulnerabilityScanner> scanners;
    private final ProjectNameGenerator projectNameGenerator;
    private final ResultsService resultsService;

    @Async("webHook")
    public void initiateAutomation(ScanRequest scanRequest) {
        String effectiveProjectName = projectNameGenerator.determineProjectName(scanRequest);
        scanRequest.setProject(effectiveProjectName);
        List<VulnerabilityScanner> enabledScanners = getEnabledScanners();

        if (enabledScanners.isEmpty()) {
            log.error("The defined scanners are not supported.");
            return;
        }
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

    private List<VulnerabilityScanner> getEnabledScanners() {
        List<VulnerabilityScanner> enabledScanners = new ArrayList<>();

        scanners.forEach(scanner -> {
            if (scanner.isEnabled()) {
                enabledScanners.add(scanner);
            }
        });
        return enabledScanners;
    }
}
