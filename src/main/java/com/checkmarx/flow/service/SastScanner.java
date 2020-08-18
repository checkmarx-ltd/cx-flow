package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.*;
import com.checkmarx.flow.dto.report.ScanReport;
import com.checkmarx.flow.exception.ExitThrowable;
import com.checkmarx.flow.exception.GitHubRepoUnavailableException;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.exception.MachinaRuntimeException;
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
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
    private final BugTrackerEventTrigger bugTrackerEventTrigger;
    private final ProjectNameGenerator projectNameGenerator;

    private ScanDetails scanDetails = null;
    private String sourcesPath = null;

    @Override
    public ScanResults scan(ScanRequest scanRequest) {
        ScanResults scanResults;
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
                    bugTrackerEventTrigger.triggerScanNotSubmittedBugTrackerEvent(scanRequest, getEmptyScanResults());
                    throw new CheckmarxException(String.format("Active Scan with Id %d already exists for Project: %d", existingScanId, projectId));
                }
            } else {
                scanId = cxService.createScan(cxScanParams, CXFLOW_SCAN_MSG);
            }

            BugTracker.Type bugTrackerType = bugTrackerEventTrigger.triggerBugTrackerEvent(scanRequest);
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
            //the repository is unavailable - can happen for a push event of a deleted branch - nothing to do

            //the error message is printed when the exception is thrown
            //usually should occur during push event occurring on delete branch
            //therefore need to eliminate the scan process but do not want to create
            //an error stack trace in the log
            return getEmptyScanResults();

        } catch (Exception e) {
            log.error("SAST scan failed", e);
            OperationResult scanCreationFailure = new OperationResult(OperationStatus.FAILURE, e.getMessage());
            ScanReport report = new ScanReport(-1, scanRequest, scanRequest.getRepoUrl(), scanCreationFailure);
            report.log();
            return getEmptyScanResults();
        }
    } 

    @Override
    public ScanResults scanCli(ScanRequest request, String scanType, File... files) {
        ScanResults scanResults = null;
        try {
            switch (scanType) {
                case "Scan-git-clone":
                    scanResults = cxFullScan(request);
                    break;
                case "cxFullScan":
                    scanResults = cxFullScan(request, files[0].getPath());
                    break;
                case "cxParse":
                    cxParseResults(request, files[0]);
                    break;
                case "cxBatch":
                    cxBatch(request);
                    break;
                default:
                    log.warn("SastScanner does not support scanType of {}, ignoring.", scanType);
                    break;
            }
        } catch (ExitThrowable e) {
            throw new MachinaRuntimeException(e);
        }
        return scanResults;
    }

    @Override
    public ScanResults getLatestScanResults(ScanRequest request) {
        return cxGetResults(request, null).join();
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

            BugTracker.Type bugTrackerType = bugTrackerEventTrigger.triggerBugTrackerEvent(request);
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
            // The error message is printed when the exception is thrown.
            // Usually should occur during push event occurring on delete branch.
            // Therefore need to eliminate the scan process but do not want to create
            // an error stack trace in the log.
            return new ScanDetails(UNKNOWN_INT, UNKNOWN_INT, new CompletableFuture<>(), false);
        } catch (CheckmarxException | GitAPIException e) {
            String extendedMessage = treatFailure(request, cxFile, scanId, e);
            throw new MachinaException("Checkmarx Error Occurred: " + extendedMessage);
        }

        logRequest(request, scanId, cxFile, OperationResult.successful());
        
        this.scanDetails = new ScanDetails(projectId, scanId, osaScanId);
        return scanDetails;
    }

    public ScanResults cxFullScan(ScanRequest request, String path) throws ExitThrowable {
        ScanResults results = null;
        try {
            String effectiveProjectName = projectNameGenerator.determineProjectName(request);
            request.setProject(effectiveProjectName);

            String cxZipFile = FileSystems.getDefault().getPath("cx.".concat(UUID.randomUUID().toString()).concat(".zip")).toAbsolutePath().toString();
            ZipUtils.zipFile(path, cxZipFile, flowProperties.getZipExclude());
            File f = new File(cxZipFile);
            log.debug("Creating temp file {}", f.getPath());
            log.debug("free space {}", f.getFreeSpace());
            log.debug("total space {}", f.getTotalSpace());
            log.debug(f.getAbsolutePath());
            ScanDetails details = executeCxScan(request, f);
            results = cxService.getReportContentByScanId(details.getScanId(), request.getFilter());
            log.debug("Deleting temp file {}", f.getPath());
            Files.deleteIfExists(Paths.get(cxZipFile));
        } catch (IOException e) {
            log.error("Error occurred while attempting to zip path {}", path, e);
            exit(3);
        } catch (MachinaException | CheckmarxException e) {
            log.error("Error occurred", e);
            exit(3);
        }
        return results;
    }

    public ScanResults cxFullScan(ScanRequest request) throws ExitThrowable {
        ScanResults results = null;
        try {
            String effectiveProjectName = projectNameGenerator.determineProjectName(request);
            request.setProject(effectiveProjectName);
            ScanDetails details = executeCxScan(request, null);
            results = cxService.getReportContentByScanId(details.getScanId(), request.getFilter());
        } catch (MachinaException | CheckmarxException e) {
            log.error("Error occurred", e);
            exit(3);
        }
        return results;
    }

    public void cxParseResults(ScanRequest request, File file) throws ExitThrowable {
        try {
            ScanResults results = cxService.getReportContent(file, request.getFilter());
            resultsService.processResults(request, results, scanDetails);
            if (flowProperties.isBreakBuild() && results != null && results.getXIssues() != null && !results.getXIssues().isEmpty()) {
                log.error(ERROR_BREAK_MSG);
                exit(ExitCode.BUILD_INTERRUPTED);
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
                processes.add(cxGetResults(request, project));
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

            if (canDeleteProject(projectId, request)) {
                cxService.deleteProject(projectId);
            }
        } catch (CheckmarxException e) {
            log.error("Error delete branch " + e.getMessage());
        }
    }

    private boolean canDeleteProject(Integer projectId, ScanRequest request) {
        boolean result = false;
        if (projectId == null || projectId == UNKNOWN_INT) {
            log.warn("{} project with the provided name is not found, nothing to delete.", SCAN_TYPE);
        } else {
            boolean branchIsProtected = helperService.isBranchProtected(request.getBranch(),
                    flowProperties.getBranches(),
                    request);

            if (branchIsProtected) {
                log.info("Unable to delete {} project, because the corresponding repo branch is protected.", SCAN_TYPE);
            } else {
                result = true;
            }
        }
        return result;
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

    private void checkScanSubmitEmailDelivery(ScanRequest scanRequest) {
        if (StringUtils.isNoneEmpty(scanRequest.getNamespace(), scanRequest.getRepoName(), scanRequest.getRepoUrl())) {
            emailService.sendScanSubmittedEmail(scanRequest);
        }
    }

    private String createOsaScan(ScanRequest request, Integer projectId) throws GitAPIException, CheckmarxException {
        String osaScanId = null;
        if (Boolean.TRUE.equals(cxProperties.getEnableOsa())) {
            String path = cxProperties.getGitClonePath().concat("/").concat(UUID.randomUUID().toString());
            File pathFile = new File(path);

            Git.cloneRepository()
                    .setURI(request.getRepoUrlWithAuth())
                    .setBranch(request.getBranch())
                    .setBranchesToClone(Collections.singleton(Constants.CX_BRANCH_PREFIX.concat(request.getBranch())))
                    .setDirectory(pathFile)
                    .call();
            osaScanId = osaService.createScan(projectId, path);
        }
        return osaScanId;
    }

    public CompletableFuture<ScanResults> cxGetResults(ScanRequest request, CxProject cxProject) {
        try {
            CxProject project;

            if (cxProject == null) {
                String team = request.getTeam();
                if (ScanUtils.empty(team)) {
                    //if the team is not provided, use the default
                    team = cxProperties.getTeam();
                    request.setTeam(team);
                }
                if (!team.startsWith(cxProperties.getTeamPathSeparator())) {
                    team = cxProperties.getTeamPathSeparator().concat(team);
                }
                String teamId = cxService.getTeamId(team);
                Integer projectId = cxService.getProjectId(teamId, request.getProject());
                if (projectId.equals(UNKNOWN_INT)) {
                    log.warn("No project found for {}", request.getProject());
                    CompletableFuture<ScanResults> x = new CompletableFuture<>();
                    x.complete(null);
                    return x;
                }
                project = cxService.getProject(projectId);

            } else {
                project = cxProject;
            }
            Integer scanId = cxService.getLastScanId(project.getId());
            if (scanId.equals(UNKNOWN_INT)) {
                log.warn("No Scan Results to process for project {}", project.getName());
                CompletableFuture<ScanResults> x = new CompletableFuture<>();
                x.complete(null);
                return x;
            } else {
                getCxFields(project, request);
                //null is passed for osaScanId as it is not applicable here and will be ignored
                return resultsService.processScanResultsAsync(request, project.getId(), scanId, null, request.getFilter());
            }

        } catch (MachinaException | CheckmarxException e) {
            log.error("Error occurred while processing results for {}{}", request.getTeam(), request.getProject(), e);
            CompletableFuture<ScanResults> x = new CompletableFuture<>();
            x.completeExceptionally(e);
            return x;
        }
    }

    private void getCxFields(CxProject project, ScanRequest request) {
        if (project == null) {
            return;
        }

        Map<String, String> fields = new HashMap<>();
        for (CxProject.CustomField field : project.getCustomFields()) {
            String name = field.getName();
            String value = field.getValue();
            if (!ScanUtils.empty(name) && !ScanUtils.empty(value)) {
                fields.put(name, value);
            }
        }
        if (!ScanUtils.empty(cxProperties.getJiraProjectField())) {
            String jiraProject = fields.get(cxProperties.getJiraProjectField());
            if (!ScanUtils.empty(jiraProject)) {
                request.getBugTracker().setProjectKey(jiraProject);
            }
        }
        if (!ScanUtils.empty(cxProperties.getJiraIssuetypeField())) {
            String jiraIssuetype = fields.get(cxProperties.getJiraIssuetypeField());
            if (!ScanUtils.empty(jiraIssuetype)) {
                request.getBugTracker().setIssueType(jiraIssuetype);
            }
        }
        if (!ScanUtils.empty(cxProperties.getJiraCustomField()) &&
                (fields.get(cxProperties.getJiraCustomField()) != null) && !fields.get(cxProperties.getJiraCustomField()).isEmpty()) {
            request.getBugTracker().setFields(ScanUtils.getCustomFieldsFromCx(fields.get(cxProperties.getJiraCustomField())));
        }

        if (!ScanUtils.empty(cxProperties.getJiraAssigneeField())) {
            String assignee = fields.get(cxProperties.getJiraAssigneeField());
            if (!ScanUtils.empty(assignee)) {
                request.getBugTracker().setAssignee(assignee);
            }
        }

        request.setCxFields(fields);
    }
}