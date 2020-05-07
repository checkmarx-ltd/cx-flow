package com.checkmarx.flow.service;

import com.checkmarx.flow.dto.ScanRequest;
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

    @Async("webHook")
    public void initiateAutomation(ScanRequest scanRequest) {
        String effectiveProjectName = projectNameGenerator.determineProjectName(scanRequest);
        scanRequest.setProject(effectiveProjectName);

        for (VulnerabilityScanner currentScanner : scanners) {
            currentScanner.scan(scanRequest);
        }
    }
}
