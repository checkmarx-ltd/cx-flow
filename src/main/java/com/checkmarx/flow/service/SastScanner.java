package com.checkmarx.flow.service;

import com.checkmarx.flow.bug_tracker_trigger.BugTrackerTriggerEvent;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.*;
import com.checkmarx.flow.dto.report.ScanReport;
import com.checkmarx.flow.exception.ExitThrowable;
import com.checkmarx.flow.exception.GitHubRepoUnavailableException;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.sastscanning.ScanRequestConverter;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.flow.utils.ZipUtils;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.cx.CxProject;
import com.checkmarx.sdk.dto.cx.CxScanParams;
import com.checkmarx.sdk.exception.CheckmarxException;
import com.checkmarx.sdk.service.CxClient;
import com.checkmarx.sdk.service.CxOsaClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.checkmarx.flow.exception.ExitThrowable.exit;
import static com.checkmarx.sdk.config.Constants.UNKNOWN;
import static com.checkmarx.sdk.config.Constants.UNKNOWN_INT;

@Service
@Slf4j
@RequiredArgsConstructor
public class SastScanner implements VulnerabilityScanner {
    private static final String SCAN_TYPE = CxProperties.CONFIG_PREFIX;
    private static final String ERROR_BREAK_MSG = "Exiting with Error code 10 due to issues present";
    private static final String CXFLOW_SCAN_MSG = "CxFlow Automated Scan";

    private final ResultsService resultsService;
    private final CxClient cxService;
    private final HelperService helperService;
    private final CxProperties cxProperties;
    private final FlowProperties flowProperties;
    private final CxOsaClient osaService;
    private final EmailService emailService;
    private final ScanRequestConverter scanRequestConverter;
    private final BugTrackerTriggerEvent bugTrackerTriggerEvent;
    private final ProjectNameGenerator projectNameGenerator;

    private ScanDetails scanDetails = null;
    private String sourcesPath = null;

    @Override
    public ScanResults scan(ScanRequest scanRequest) {
        ScanResults scanResults = null;
        log.info("--------------------- Initiating new {} scan ---------------------", SCAN_TYPE);
        checkScanSubmitEmailDelivery(scanRequest);

        try {
            Integer scanId;
            CxScanParams cxScanParams = scanRequestConverter.toScanParams(scanRequest);
            Integer projectId = cxScanParams.getProjectId();

            log.info("Checking if there is any existing scan for Project: {}", projectId);
            Integer existingScanId = cxService.getScanIdOfExistingScanIfExists(projectId);

            if (existingScanId != UNKNOWN_INT) {
                if (flowProperties.getScanResubmit()) {
                    log.info("Existing ongoing scan with id {} found for Project : {}", existingScanId, projectId);
                    log.info("Aborting the ongoing scan with id {} for Project: {}", existingScanId, projectId);
                    cxService.cancelScan(existingScanId);
                    log.info("Resubmitting the scan for Project: {}", projectId);
                    scanId = cxService.createScan(cxScanParams, CXFLOW_SCAN_MSG);
                } else {
                    log.warn("Property scan-resubmit set to {} : New scan not submitted, due to existing ongoing scan for the same Project id {}", flowProperties.getScanResubmit(), projectId);
                    throw new CheckmarxException(String.format("Active Scan with Id %d already exists for Project: %d , ", existingScanId, projectId));
                }
            } else {
                scanId = cxService.createScan(cxScanParams, CXFLOW_SCAN_MSG);
            }

            BugTracker.Type bugTrackerType = bugTrackerTriggerEvent.triggerBugTrackerEvent(scanRequest);
            if (bugTrackerType.equals(BugTracker.Type.NONE)) {
                scanDetails = handleNoneBugTrackerCase(scanRequest, null, scanId, projectId);
            } else {
                cxService.waitForScanCompletion(scanId);
                projectId = handleUnKnownProjectId(cxScanParams.getProjectId(), cxScanParams.getTeamId(), cxScanParams.getProjectName());
                scanDetails = new ScanDetails(projectId, scanId, null);
            }
            logRequest(scanRequest, scanId, null, OperationResult.successful());

            scanResults = cxService.getReportContentByScanId(scanId, scanRequest.getFilter());
            scanResults.setSastScanId(scanId);
            return scanResults;

        } catch (GitHubRepoUnavailableException e) {
            return getEmptyScanResults();

        } catch (Exception e) {
            log.error("SAST scan failed", e);
            OperationResult scanCreationFailure = new OperationResult(OperationStatus.FAILURE, e.getMessage());
            ScanReport report = new ScanReport(-1, scanRequest, scanRequest.getRepoUrl(), scanCreationFailure);
            report.log();
            return getEmptyScanResults();
        }
    }

    private ScanResults getEmptyScanResults() {
        ScanResults scanResults;
        scanResults = new ScanResults();
        scanResults.setProjectId(UNKNOWN);
        scanResults.setProject(UNKNOWN);
        scanResults.setScanType(SCAN_TYPE);
        return scanResults;
    }

    @Override
    public boolean isEnabled() {
        boolean result = false;
        List<String> enabledScanners = flowProperties.getEnabledVulnerabilityScanners();
        if (enabledScanners == null || enabledScanners.isEmpty()) {
            log.info("None of the vulnerability scanners is enabled in the configuration. Using CxSAST scanner by default.");
            result = true;
        } else if (StringUtils.containsIgnoreCase(enabledScanners.toString(), CxProperties.CONFIG_PREFIX)) {
            result = true;
        }
        return result;
    }

    public CompletableFuture<ScanResults> executeCxScanFlow(ScanRequest request, File cxFile) throws MachinaException {
        ScanDetails scanDetails = executeCxScan(request, cxFile);
        if (scanDetails.processResults()) {
            return resultsService.processScanResultsAsync(request, scanDetails.getProjectId(), scanDetails.getScanId(), scanDetails.getOsaScanId(), request.getFilter());
        } else {
            return scanDetails.getResults();
        }
    }

    public ScanDetails executeCxScan(ScanRequest request, File cxFile) throws MachinaException {

        String osaScanId;
        Integer scanId = null;
        Integer projectId;

        try {
            /*Check if team is provided*/
            String ownerId = scanRequestConverter.determineTeamAndOwnerID(request);

            log.debug("Auto profiling is enabled");
            projectId = scanRequestConverter.determinePresetAndProjectId(request, ownerId);

            CxScanParams params = scanRequestConverter.prepareScanParamsObject(request, cxFile, ownerId, projectId);

            scanId = cxService.createScan(params, CXFLOW_SCAN_MSG);

            BugTracker.Type bugTrackerType = bugTrackerTriggerEvent.triggerBugTrackerEvent(request);
            if (bugTrackerType.equals(BugTracker.Type.NONE)) {
                return handleNoneBugTrackerCase(request, cxFile, scanId, projectId);
            } else {
                cxService.waitForScanCompletion(scanId);
                projectId = handleUnKnownProjectId(projectId, ownerId, request.getProject());
                osaScanId = createOsaScan(request, projectId);

                if (osaScanId != null) {
                    logRequest(request, osaScanId, cxFile, OperationResult.successful());
                }
            }
        } catch (GitHubRepoUnavailableException e) {
            //the error message is printed when the exception is thrown
            //usually should occur during push event occuring on delete branch
            //therefore need to eliminate the scan process but do not want to create
            //an error stuck trace in the log
            return new ScanDetails(UNKNOWN_INT, UNKNOWN_INT, new CompletableFuture<>(), false);
        } catch (CheckmarxException | GitAPIException e) {
            String extendedMessage = treatFailure(request, cxFile, scanId, e);
            throw new MachinaException("Checkmarx Error Occurred: " + extendedMessage);
        }

        logRequest(request, scanId, cxFile, OperationResult.successful());

        this.scanDetails = new ScanDetails(projectId, scanId, osaScanId);
        return scanDetails;
    }

    public void cxFullScan(ScanRequest request, String path) throws ExitThrowable {

        try {
            String cxZipFile = FileSystems.getDefault().getPath("cx.".concat(UUID.randomUUID().toString()).concat(".zip")).toAbsolutePath().toString();
            ZipUtils.zipFile(path, cxZipFile, flowProperties.getZipExclude());
            File f = new File(cxZipFile);
            log.debug(f.getPath());
            log.debug("free space {}", f.getFreeSpace());
            log.debug("total space {}", f.getTotalSpace());
            log.debug(f.getAbsolutePath());
            CompletableFuture<ScanResults> future = executeCxScanFlow(request, f);
            log.debug("Waiting for scan to complete");
            ScanResults results = future.join();
            if (flowProperties.isBreakBuild() && results != null && results.getXIssues() != null && !results.getXIssues().isEmpty()) {
                log.error(ERROR_BREAK_MSG);
                exit(10);
            }
        } catch (IOException e) {
            log.error("Error occurred while attempting to zip path {}", path, e);
            exit(3);
        } catch (MachinaException e) {
            log.error("Error occurred", e);
            exit(3);
        }
    }

    public void cxFullScan(ScanRequest request) throws ExitThrowable {

        try {
            CompletableFuture<ScanResults> future = executeCxScanFlow(request, null);

            if (future.isCompletedExceptionally()) {
                log.error("An error occurred while executing process");
            } else {
                if (log.isInfoEnabled()) {
                    log.info("Finished processing the request");
                }
            }

            log.debug("Waiting for scan to complete");
            ScanResults results = future.join();
            if (flowProperties.isBreakBuild() && results != null && results.getXIssues() != null && !results.getXIssues().isEmpty()) {
                log.error(ERROR_BREAK_MSG);
                exit(10);
            }
        } catch (MachinaException e) {
            log.error("Error occurred", e);
            exit(3);
        }
    }

    public void cxParseResults(ScanRequest request, File file) throws ExitThrowable {
        try {
            ScanResults results = cxService.getReportContent(file, request.getFilter());
            resultsService.processResults(request, results, scanDetails);
            if (flowProperties.isBreakBuild() && results != null && results.getXIssues() != null && !results.getXIssues().isEmpty()) {
                log.error(ERROR_BREAK_MSG);
                exit(10);
            }
        } catch (MachinaException | CheckmarxException e) {
            log.error("Error occurred while processing results file", e);
            exit(3);
        }
    }

    /**
     * Process Projects in batch mode - JIRA ONLY
     */
    public void cxBatch(ScanRequest originalRequest) throws ExitThrowable {
        try {
            List<CxProject> projects;
            List<CompletableFuture<ScanResults>> processes = new ArrayList<>();
            //Get all projects
            if (ScanUtils.empty(originalRequest.getTeam())) {
                projects = cxService.getProjects();
            } else { //Get projects for the provided team
                String team = originalRequest.getTeam();
                if (!team.startsWith(cxProperties.getTeamPathSeparator())) {
                    team = cxProperties.getTeamPathSeparator().concat(team);
                }
                String teamId = cxService.getTeamId(team);
                projects = cxService.getProjects(teamId);
            }
            for (CxProject project : projects) {
                ScanRequest request = new ScanRequest(originalRequest);
                String name = project.getName().replaceAll("[^a-zA-Z0-9-_]+", "_");
                //TODO set team when entire instance batch mode
                helperService.getShortUid(request); //update new request object with a unique id for thread log monitoring
                request.setProject(name);
                request.setApplication(name);
                processes.add(resultsService.cxGetResults(request, project));
            }
            log.info("Waiting for processing to complete");
            processes.forEach(CompletableFuture::join);

        } catch (CheckmarxException e) {
            log.error("Error occurred while processing projects in batch mode", e);
            exit(3);
        }
    }

    public void deleteProject(ScanRequest request) {

        try {

            String ownerId = scanRequestConverter.determineTeamAndOwnerID(request);

            String projectName = projectNameGenerator.determineProjectName(request);
            request.setProject(projectName);

            Integer projectId = scanRequestConverter.determinePresetAndProjectId(request, ownerId);

            if (projectId != UNKNOWN_INT) {
                cxService.deleteProject(projectId);
            }

        } catch (CheckmarxException e) {
            log.error("Error delete branch " + e.getMessage());
        }
    }

    private String treatFailure(ScanRequest request, File cxFile, Integer scanId, Exception e) {
        String extendedMessage = ExceptionUtils.getMessage(e);
        log.error(extendedMessage, e);
        Thread.currentThread().interrupt();
        OperationResult scanCreationFailure = new OperationResult(OperationStatus.FAILURE, e.getMessage());
        logRequest(request, scanId, cxFile, scanCreationFailure);
        return extendedMessage;
    }

    private void logRequest(ScanRequest request, Integer scanId, File cxFile, OperationResult scanCreationResult) {
        ScanReport report = new ScanReport(scanId, request, getRepoUrl(request, cxFile), scanCreationResult);
        report.log();
    }

    private void logRequest(ScanRequest request, String scanId, File cxFile, OperationResult scanCreationResult) {
        ScanReport report = new ScanReport(scanId, request, getRepoUrl(request, cxFile), scanCreationResult);
        report.log();
    }

    private String getRepoUrl(ScanRequest request, File cxFile) {
        String repoUrl;

        if (sourcesPath != null) {
            //the folder to scan is supplied via -f flag in command line and it is located in the filesystem
            repoUrl = sourcesPath;
        } else if (cxFile != null) {
            //in general cxFile is a zip created by cxFlow using the folder supplied y -f
            //the use case when sourcePath is empty but cxFile is set is only for the test flow
            repoUrl = cxFile.getAbsolutePath();
        } else {
            //sources to scan are in the remote repository (GitHib, TFS ... etc)
            repoUrl = request.getRepoUrl();
        }
        return repoUrl;
    }

    private void sendSubmittedScanEmail(ScanRequest request) {
        Map<String, Object> emailCtx = new HashMap<>();

        emailCtx.put("message", "Checkmarx Scan has been submitted for "
                .concat(request.getNamespace()).concat("/").concat(request.getRepoName()).concat(" - ")
                .concat(request.getRepoUrl()));
        emailCtx.put("heading", "Scan Request Submitted");
        emailService.sendmail(request.getEmail(), "Checkmarx Scan Submitted for ".concat(request.getNamespace()).concat("/").concat(request.getRepoName()), emailCtx, "message.html");
    }

    private void sendErrorScanEmail(ScanRequest request, MachinaException e) {
        Map<String, Object> emailCtx = new HashMap<>();

        log.error("Machina Exception has occurred.", e);
        emailCtx.put("message", "Error occurred during scan/bug tracking process for "
                .concat(request.getNamespace()).concat("/").concat(request.getRepoName()).concat(" - ")
                .concat(request.getRepoUrl()).concat("  Error: ").concat(e.getMessage()));
        emailCtx.put("heading", "Error occurred during scan");
        emailService.sendmail(request.getEmail(), "Error occurred for ".concat(request.getNamespace()).concat("/").concat(request.getRepoName()), emailCtx, "message-error.html");
    }

    private ScanDetails handleNoneBugTrackerCase(ScanRequest request, File cxFile, Integer scanId, Integer projectId) {
        log.info("Not waiting for scan completion as Bug Tracker type is NONE");
        CompletableFuture<ScanResults> results = CompletableFuture.completedFuture(null);
        logRequest(request, scanId, cxFile, OperationResult.successful());
        return new ScanDetails(projectId, scanId, results, false);
    }

    private Integer handleUnKnownProjectId(Integer projectId, String ownerId, String projectName) {
        if (projectId == UNKNOWN_INT) {
            projectId = cxService.getProjectId(ownerId, projectName); //get the project id of the updated or created project
        }
        return projectId;
    }

    private ScanResults getResults(ScanResults scanResults, CompletableFuture<ScanResults> futureResults) {
        try {
            scanResults = futureResults.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Thread was interrupted while trying to get scan results", e);
            Thread.currentThread().interrupt();
        }
        return scanResults;
    }

    private void checkScanSubmitEmailDelivery(ScanRequest scanRequest) {
        if (!ScanUtils.anyEmpty(scanRequest.getNamespace(), scanRequest.getRepoName(), scanRequest.getRepoUrl())) {
            sendSubmittedScanEmail(scanRequest);
        }
    }

    private String createOsaScan(ScanRequest request, Integer projectId) throws GitAPIException, CheckmarxException {
        String osaScanId = null;
        if (cxProperties.getEnableOsa()) {
            String path = cxProperties.getGitClonePath().concat("/").concat(UUID.randomUUID().toString());
            File pathFile = new File(path);

            Git git = Git.cloneRepository()
                    .setURI(request.getRepoUrlWithAuth())
                    .setBranch(request.getBranch())
                    .setBranchesToClone(Collections.singleton(Constants.CX_BRANCH_PREFIX.concat(request.getBranch())))
                    .setDirectory(pathFile)
                    .call();
            osaScanId = osaService.createScan(projectId, path);
        }
        return osaScanId;
    }
}