package com.checkmarx.flow.service;

import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaRuntimeException;
import com.checkmarx.sdk.dto.ScanResults;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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
        ScanResults combinedResults = new ScanResults();
        boolean isAtLeastOneScannerIsEnabled = false;

        String effectiveProjectName = projectNameGenerator.determineProjectName(scanRequest);
        scanRequest.setProject(effectiveProjectName);

        for (VulnerabilityScanner currentScanner : scanners) {
            if (currentScanner.isEnabled()) {
                isAtLeastOneScannerIsEnabled = true;
                ScanResults scanResults = currentScanner.scan(scanRequest);
                combinedResults.mergeWith(scanResults);
            }
        }
        if (!isAtLeastOneScannerIsEnabled) {
            handleNoScannerIsEnabled();
        }
        resultsService.publishCombinedResults(scanRequest, combinedResults);
    }

    private void handleNoScannerIsEnabled() {
        String errorMessage = "The defined scanners are not supported.";
        throw new MachinaRuntimeException(errorMessage);
    }
}
