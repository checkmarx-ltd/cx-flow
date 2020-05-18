package com.checkmarx.flow.service;

import com.checkmarx.flow.dto.ScanRequest;
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

        String effectiveProjectName = projectNameGenerator.determineProjectName(scanRequest);
        scanRequest.setProject(effectiveProjectName);

        for (VulnerabilityScanner currentScanner : scanners) {
            if (currentScanner.isThisScannedEnabled()) {
                ScanResults scanResults = currentScanner.scan(scanRequest);
                combinedResults.mergeResultsWith(scanResults);
            }
        }

        // TODO: 5/18/2020 temporary check for a situation that only SCA is enabled. Need to remove this validation after implementing SCA results
        if (combinedResults.getScanSummary() != null) {
            resultsService.publishCombinedResults(scanRequest, combinedResults);
        }
    }
}
