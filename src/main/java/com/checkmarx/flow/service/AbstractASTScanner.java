package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.OperationResult;
import com.checkmarx.flow.dto.OperationStatus;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.report.AnalyticsReport;
import com.checkmarx.flow.dto.report.ScanReport;
import com.checkmarx.flow.exception.ExitThrowable;
import com.checkmarx.flow.exception.MachinaRuntimeException;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.ast.ASTResults;
import com.checkmarx.sdk.dto.ast.ASTResultsWrapper;
import com.checkmarx.sdk.dto.ast.SCAResults;
import com.checkmarx.sdk.dto.ast.ScanParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractASTScanner implements VulnerabilityScanner {
    private final com.checkmarx.sdk.service.AstClient client;
    private final FlowProperties flowProperties;
    private final String scanType;
    private final BugTrackerEventTrigger bugTrackerEventTrigger;

    @Override
    public ScanResults scan(ScanRequest scanRequest) {
        ScanResults result = null;
        log.info("--------------------- Initiating new {} scan ---------------------", scanType);
        ScanParams sdkScanParams = toSdkScanParams(scanRequest);
        ASTResultsWrapper internalResults = new ASTResultsWrapper(new SCAResults(), new ASTResults());
        try {
            internalResults = client.scan(sdkScanParams);
            logRequest(scanRequest, internalResults, OperationResult.successful());
            result = toScanResults(internalResults);
        } catch (Exception e) {
            treatError(scanRequest, internalResults, e);
        }
        
        return result;
    }

    @Override
    public ScanResults scanCli(ScanRequest request, String scanType, File... files) {
        ScanResults scanResults = null;
        try {
            switch (scanType) {
                case "Scan-git-clone":
                    scanResults = scan(request);
                    break;
                case "cxFullScan":
                    scanResults = scan(request, files[0].getPath());
                    break;
                case "cxParse":
                case "cxBatch":
                default:
                    log.warn("ScaScanner does not support scanType of {}, ignoring.", scanType);
                    break;
            }
        } catch (MachinaRuntimeException me) {
            log.error(me.getMessage());
            throw new MachinaRuntimeException(new ExitThrowable(10));
        } catch (ExitThrowable e) {
            throw new MachinaRuntimeException(e);
        }

        return scanResults;
    }

    @Override
    public ScanResults getLatestScanResults(ScanRequest request) {
        ScanParams sdkScanParams = ScanParams.builder()
                .projectName(request.getProject())
                .build();
        ASTResultsWrapper internalResults = client.getLatestScanResults(sdkScanParams);
        return toScanResults(internalResults);
    }

    private void treatError(ScanRequest scanRequest, ASTResultsWrapper internalResults, Exception e) {
        final String message = scanType + " scan failed.";
        log.error(message, e);
        OperationResult scanCreationFailure = new OperationResult(OperationStatus.FAILURE, e.getMessage());
        logRequest(scanRequest, internalResults, scanCreationFailure);
        throw new MachinaRuntimeException(message + "\n" + e.getMessage());
    }

    public ScanResults scan(ScanRequest scanRequest, String path) throws ExitThrowable {
        BugTracker.Type bugTrackerType = bugTrackerEventTrigger.triggerBugTrackerEvent(scanRequest);
        ScanResults result = null;
        if (bugTrackerType.equals(BugTracker.Type.NONE)) {
            log.info("Not waiting for scan completion as Bug Tracker type is NONE");
            CompletableFuture.supplyAsync(() -> actualScan(scanRequest, path));
        } else {
            result = actualScan(scanRequest, path);
        }
        return result;
    }

    private ScanResults actualScan(ScanRequest scanRequest, String path) {
        ScanResults result = null;
        log.info("--------------------- Initiating new {} scan ---------------------", scanType);
        ASTResultsWrapper internalResults = new ASTResultsWrapper(new SCAResults(), new ASTResults());            
        try {
            ScanParams sdkScanParams = toSdkScanParams(scanRequest, path);
            internalResults = client.scan(sdkScanParams);
            logRequest(scanRequest, internalResults, OperationResult.successful());
            result = toScanResults(internalResults);
        } catch (Exception e) {
            treatError(scanRequest, internalResults, e);
        }
        return result;
    }

    protected abstract String getScanId(ASTResultsWrapper internalResults);

    private ScanParams toSdkScanParams(ScanRequest scanRequest, String pathToScan) {
        return ScanParams.builder()
                .projectName(scanRequest.getProject())
                .sourceDir(pathToScan)
                .build();
    }

    protected abstract ScanResults toScanResults(ASTResultsWrapper internalResults);


    private ScanParams toSdkScanParams(ScanRequest scanRequest) {
        URL parsedUrl = getRepoUrl(scanRequest);

        return ScanParams.builder()
                .projectName(scanRequest.getProject())
                .remoteRepoUrl(parsedUrl)
                .scaConfig(scanRequest.getScaConfig())
                .filterConfiguration(scanRequest.getFilter())
                .build();
    }

    private URL getRepoUrl(ScanRequest scanRequest) {
        URL parsedUrl;
        try {
            parsedUrl = new URL(scanRequest.getRepoUrlWithAuth());
        } catch (MalformedURLException e) {
            log.error("Failed to parse repository URL: '{}'", scanRequest.getRepoUrl());
            throw new MachinaRuntimeException("Invalid repository URL.");
        }
        return parsedUrl;
    }

    @Override
    public boolean isEnabled() {
        List<String> enabledScanners = flowProperties.getEnabledVulnerabilityScanners();

        return enabledScanners != null
                && enabledScanners.stream().anyMatch(scanner -> scanner.equalsIgnoreCase(scanType));

    }

    private void logRequest(ScanRequest request, ASTResultsWrapper internalResults, OperationResult scanCreationResult) {
        String scanId = getScanId(internalResults);
        ScanReport report = new ScanReport(scanId, request, request.getRepoUrl(), scanCreationResult, AnalyticsReport.SCA);
        report.log();
    }
}

