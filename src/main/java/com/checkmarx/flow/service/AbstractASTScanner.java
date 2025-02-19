package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.*;
import com.checkmarx.flow.dto.report.AnalyticsReport;
import com.checkmarx.flow.dto.report.ScanReport;
import com.checkmarx.flow.exception.ExitThrowable;
import com.checkmarx.flow.exception.MachinaRuntimeException;
import com.checkmarx.sdk.config.ScaProperties;
import com.checkmarx.sdk.dto.AstScaResults;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.ast.ASTResults;
import com.checkmarx.sdk.dto.ast.ScanParams;
import com.checkmarx.sdk.dto.sca.SCAResults;
import com.checkmarx.sdk.exception.CheckmarxException;
import com.checkmarx.sdk.service.scanner.AbstractScanner;
import com.checkmarx.sdk.service.scanner.AstScanner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractASTScanner implements VulnerabilityScanner {
    private final AbstractScanner client;
    protected final FlowProperties flowProperties;
    private final ScaProperties scaProperties;
    private final String scanType;
    private final BugTrackerEventTrigger bugTrackerEventTrigger;
    protected final ProjectNameGenerator projectNameGenerator;
    protected final ResultsService resultsService;
    protected ScanDetails scanDetails = null;
    protected static final String ERROR_BREAK_MSG = "Exiting with Error code 10 due to issues present";


    @Override
    public ScanResults scan(ScanRequest scanRequest) {
        ScanResults result = null;
        log.info("--------------------- Initiating new {} scan ---------------------", scanType);
        String effectiveProjectName;
        if(scaProperties.getProjectName()!=null)
        {
            effectiveProjectName = normalize(scaProperties.getProjectName(),flowProperties.isPreserveProjectName());
        }
        else {
            effectiveProjectName = normalize(scanRequest.getProject(),flowProperties.isPreserveProjectName());
        }
        scanRequest.setProject(effectiveProjectName);
        ScanParams sdkScanParams = toSdkScanParams(scanRequest);
        AstScaResults internalResults = new AstScaResults(new SCAResults(), new ASTResults());
        try {
            bugTrackerEventTrigger.triggerScanStartedEvent(scanRequest);
            internalResults = client.scan(sdkScanParams);
            logRequest(scanRequest, internalResults, OperationResult.successful());
            result = toScanResults(internalResults);
        } catch (Exception e) {
            if(!flowProperties.isDisablePRFeedBack()) {
                bugTrackerEventTrigger.triggerOffScanStartedEvent(scanRequest);
            }
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
            log.error("Exception in scanCli method. ", me.getMessage(), me);
            throw new MachinaRuntimeException(new ExitThrowable(10));
        } catch (ExitThrowable e) {
            throw new MachinaRuntimeException(e);
        }

        return scanResults;
    }

    @Override
    public ScanResults getLatestScanResults(ScanRequest request) {
        ScanParams sdkScanParams;
        if(scaProperties.getProjectName()!=null)
        {
            sdkScanParams = ScanParams.builder()
                    .projectName(scaProperties.getProjectName())
                    .scaConfig(request.getScaConfig())
                    .filterConfiguration(request.getFilter())
                    .build();
        }
        else {
            sdkScanParams = ScanParams.builder()
                    .projectName(request.getProject())
                    .scaConfig(request.getScaConfig())
                    .filterConfiguration(request.getFilter())
                    .build();
        }
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
        String effectiveProjectName;
        if(scaProperties.getProjectName()!=null)
        {
            effectiveProjectName = normalize(scaProperties.getProjectName(),flowProperties.isPreserveProjectName());
        }
        else {
            effectiveProjectName = normalize(scanRequest.getProject(),flowProperties.isPreserveProjectName());
        }
        scanRequest.setProject(effectiveProjectName);
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
            log.error("Failed to parse repository URL: '{}'", scanRequest.getRepoUrl(), e);
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

    private static String normalize(String rawProjectName, boolean preserveProjectName) {
        String result = null;
        if (rawProjectName != null) {
            if (!preserveProjectName) {
                if(!rawProjectName.contains("#")) {
                    result = rawProjectName.replaceAll("[^a-zA-Z0-9-_.]+", "-");
                }
                else {
                    result = rawProjectName;
                }
                if (!result.equals(rawProjectName)) {
                    log.debug("Project name ({}) has been normalized to allow only valid characters.", rawProjectName);
                }
            } else {
                result = rawProjectName;
                log.info("Project name ({}) has not been normalized.", rawProjectName);
            }
            log.info("Project name being used: {}", result);
        } else {
            log.warn("Project name returned NULL");
        }
        return result;
    }

    @Override
    public void deleteProject(ScanRequest request) {
        String effectiveProjectName = projectNameGenerator.determineScaProjectName(request);;
        if(scaProperties.getProjectName()!=null)
        {
            effectiveProjectName = normalize(scaProperties.getProjectName(),flowProperties.isPreserveProjectName());
        }
        request.setProject(effectiveProjectName);
        ScanParams sdkScanParams = ScanParams.builder()
                .branch(request.getBranch())
                .projectName(request.getProject())
                .remoteRepoUrl(null)
                .scaConfig(request.getScaConfig())
                .filterConfiguration(request.getFilter())
                .build();
        log.info("Going to delete Project Name: {}", effectiveProjectName);

        if (canDeleteProject(request)) {
            client.deleteProject(sdkScanParams);
        }
    }


    private boolean isBranchProtected(String branchToCheck, List<String> protectedBranchPatterns, ScanRequest request) {
        boolean result;
        if (protectedBranchPatterns.isEmpty() && branchToCheck.equalsIgnoreCase(request.getDefaultBranch())) {
            result = true;
            log.info("Scanning default branch - {}", request.getDefaultBranch());
        } else {
            result= protectedBranchPatterns.stream().anyMatch(aBranch -> Pattern.matches(aBranch, branchToCheck));

        }
        return result;
    }

    private boolean canDeleteProject( ScanRequest request) {
        boolean result = false;
            boolean branchIsProtected = isBranchProtected(request.getBranch(),
                    flowProperties.getBranches(),
                    request);

            if (branchIsProtected) {
                log.info("Unable to delete SCA project, because the corresponding repo branch is protected.");
            } else {
                result = true;
            }
        return result;
    }

    @Override
    public ScanResults scanCliToGeneratePDF(ScanRequest scanRequest, String scanType, File... files) {
        return null;
    }

}

