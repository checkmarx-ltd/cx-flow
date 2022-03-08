package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.*;
import com.checkmarx.flow.dto.report.AnalyticsReport;
import com.checkmarx.flow.dto.report.ScanReport;
import com.checkmarx.flow.exception.ExitThrowable;
import com.checkmarx.flow.exception.MachinaRuntimeException;
import com.checkmarx.sdk.dto.AstScaResults;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.ast.ASTResults;
import com.checkmarx.sdk.dto.ast.ScanParams;
import com.checkmarx.sdk.dto.sca.SCAResults;
import com.checkmarx.sdk.service.scanner.AbstractScanner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractASTScanner implements VulnerabilityScanner {
    private final AbstractScanner client;
    protected final FlowProperties flowProperties;
    private final String scanType;
    private final BugTrackerEventTrigger bugTrackerEventTrigger;

    protected final ResultsService resultsService;
    protected ScanDetails scanDetails = null;
    protected static final String ERROR_BREAK_MSG = "Exiting with Error code 10 due to issues present";


    @Override
    public ScanResults scan(ScanRequest scanRequest) {
        ScanResults result = null;
        log.info("--------------------- Initiating new {} scan ---------------------", scanType);
        ScanParams sdkScanParams = toSdkScanParams(scanRequest);
        AstScaResults internalResults = new AstScaResults(new SCAResults(), new ASTResults());
        try {
            bugTrackerEventTrigger.triggerScanStartedEvent(scanRequest);
            internalResults = client.scan(sdkScanParams);
            logRequest(scanRequest, internalResults, OperationResult.successful());
            result = toScanResults(internalResults);
        } catch (Exception e) {
            treatError(scanRequest, internalResults, e);
        }
        
        return result;
    }
    protected abstract void cxParseResults(ScanRequest request, File file) throws ExitThrowable;
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
                    cxParseResults(request, files[0]);
                    break;
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
                .scaConfig(request.getScaConfig())
                .filterConfiguration(request.getFilter())
                .build();
        setScannerSpecificProperties(request,sdkScanParams);
        AstScaResults internalResults = client.getLatestScanResults(sdkScanParams);
        return toScanResults(internalResults);
    }

    protected abstract void setScannerSpecificProperties(ScanRequest scanRequest, ScanParams scanParams);

    private void treatError(ScanRequest scanRequest, AstScaResults internalResults, Exception e) {
        final String message = scanType + " scan failed.";
        log.error(message, e);
        OperationResult scanCreationFailure = new OperationResult(OperationStatus.FAILURE, e.getMessage());
        logRequest(scanRequest, internalResults, scanCreationFailure);
        throw new MachinaRuntimeException(message + "\n" + e.getMessage());
    }

    public ScanResults scan(ScanRequest scanRequest, String path) throws ExitThrowable {
        BugTracker.Type bugTrackerType = bugTrackerEventTrigger.triggerScanStartedEvent(scanRequest);
        ScanResults result = null;
        if (bugTrackerType.equals(BugTracker.Type.NONE)) {
            log.info("Not waiting for scan completion as Bug Tracker type is NONE");
            ScanParams sdkScanParams = toSdkScanParams(scanRequest, path);
            client.scanWithNoWaitingToResults(sdkScanParams);
        } else {
            result = actualScan(scanRequest, path);
        }
        return result;
    }

    private ScanResults actualScan(ScanRequest scanRequest, String path) {
        ScanResults result = null;
        log.info("--------------------- Initiating new {} scan ---------------------", scanType);
        AstScaResults internalResults = new AstScaResults(new SCAResults(), new ASTResults());            
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

    protected abstract String getScanId(AstScaResults internalResults);

    private ScanParams toSdkScanParams(ScanRequest scanRequest, String pathToScan) {
        ScanParams scanParams = ScanParams.builder()
                .projectName(scanRequest.getProject())
                .sourceDir(pathToScan)
                .scaConfig(scanRequest.getScaConfig())
                .filterConfiguration(scanRequest.getFilter())
                .disableCertificateValidation(scanRequest.isDisableCertificateValidation())
                .build();
        setScannerSpecificProperties(scanRequest,scanParams);
        return scanParams;
    }

    protected abstract ScanResults toScanResults(AstScaResults internalResults);


    private ScanParams toSdkScanParams(ScanRequest scanRequest) {
        URL parsedUrl = getRepoUrl(scanRequest);

        ScanParams scanParams = ScanParams.builder()
                .branch(scanRequest.getBranch())
                .projectName(scanRequest.getProject())
                .remoteRepoUrl(parsedUrl)
                .scaConfig(scanRequest.getScaConfig())
                .filterConfiguration(scanRequest.getFilter())
                .build();

        setScannerSpecificProperties(scanRequest, scanParams);
        return scanParams;
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

    private void logRequest(ScanRequest request, AstScaResults internalResults, OperationResult scanCreationResult) {
        String scanId = getScanId(internalResults);
        ScanReport report = new ScanReport(scanId, request, request.getRepoUrl(), scanCreationResult, AnalyticsReport.SCA);
        report.log();
    }
}

