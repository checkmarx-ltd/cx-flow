package com.checkmarx.flow.service;

import com.checkmarx.flow.dto.ScanRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FlowService {

    private final List<VulnerabilityScanner> scanners;

    @Async("webHook")
    public void initiateAutomation(ScanRequest scanRequest) {
        for (VulnerabilityScanner currentScanner : scanners) {
            currentScanner.scan(scanRequest);
        }
    }
}
