package com.checkmarx.flow.service;

import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.sastscanning.ScanRequestConverter;
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
    private final ScanRequestConverter scanRequestConverter;

    @Async("webHook")
    public void initiateAutomation(ScanRequest scanRequest) {
        String projectName = null;
        projectName = determineProjectName(scanRequest, projectName);

        for (VulnerabilityScanner currentScanner : scanners) {
            currentScanner.scan(scanRequest, projectName);
        }
    }

    private String determineProjectName(ScanRequest scanRequest, String projectName) {
        try {
            projectName = scanRequestConverter.determineProjectName(scanRequest);
        } catch (MachinaException e) {
            log.error("Cannot initiate a new scan due to the following error: {}", e);
        }
        return projectName;
    }

}
