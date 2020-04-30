package com.checkmarx.flow.service;

import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.dto.ScanResults;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlowService {

    private final EmailService emailService;
    private final SastScannerService sastScannerService;

    @Async("webHook")
    public void initiateAutomation(ScanRequest request) {
        Map<String, Object>  emailCtx = new HashMap<>();
        try {
            if (request.getProduct().equals(ScanRequest.Product.CX)) {
                if(!ScanUtils.anyEmpty(request.getNamespace(), request.getRepoName(), request.getRepoUrl())) {
                    emailCtx.put("message", "Checkmarx Scan has been submitted for "
                            .concat(request.getNamespace()).concat("/").concat(request.getRepoName()).concat(" - ")
                            .concat(request.getRepoUrl()));
                    emailCtx.put("heading", "Scan Request Submitted");
                    emailService.sendmail(request.getEmail(), "Checkmarx Scan Submitted for ".concat(request.getNamespace()).concat("/").concat(request.getRepoName()), emailCtx, "message.html");
                }
                CompletableFuture<ScanResults> results = sastScannerService.executeCxScanFlow(request, null);
                if(results.isCompletedExceptionally()){
                    log.error("An error occurred while executing process");
                }
            } else {
                log.warn("Unknown Product type of {}, exiting", request.getProduct());
            }
        } catch (MachinaException e){
            log.error("Machina Exception has occurred.", e);
            emailCtx.put("message", "Error occurred during scan/bug tracking process for "
                    .concat(request.getNamespace()).concat("/").concat(request.getRepoName()).concat(" - ")
                    .concat(request.getRepoUrl()).concat("  Error: ").concat(e.getMessage()));
            emailCtx.put("heading","Error occurred during scan");
            emailService.sendmail(request.getEmail(), "Error occurred for ".concat(request.getNamespace()).concat("/").concat(request.getRepoName()), emailCtx, "message-error.html");
        }
    }
}
