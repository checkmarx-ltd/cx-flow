package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.*;
import com.checkmarx.flow.dto.report.ScanReport;
import com.checkmarx.flow.exception.ExitThrowable;
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
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import static com.checkmarx.flow.exception.ExitThrowable.exit;
import static com.checkmarx.sdk.config.Constants.UNKNOWN_INT;

@Service
@Slf4j
@RequiredArgsConstructor
public class SastScanner implements VulnerabilityScanner {

    private static final String SCAN_MESSAGE = "Scan submitted to Checkmarx";
    private static final String ERROR_BREAK_MSG = "Exiting with Error code 10 due to issues present";

    private final ResultsService resultsService;
    private final CxClient cxService;
    private final HelperService helperService;
    private final CxProperties cxProperties;
    private final FlowProperties flowProperties;
    private final GitHubService gitService;
    private final GitLabService gitLabService;
    private final BitBucketService bbService;
    private final ADOService adoService;
    private final CxOsaClient osaService;
    private final EmailService emailService;
    private final ScanRequestConverter scanRequestConverter;

    private ScanDetails scanDetails = null;
    private String sourcesPath = null;

    @Override
    public void scan(ScanRequest scanRequest) {
        if (isSastScanConfigured()) {
            try {
                if (!ScanUtils.anyEmpty(scanRequest.getNamespace(), scanRequest.getRepoName(), scanRequest.getRepoUrl())) {
                    sendSuccessScanEmail(scanRequest);
                }
                CompletableFuture<ScanResults> results = executeCxScanFlow(scanRequest, null);
                if (results.isCompletedExceptionally()) {
                    log.error("An error occurred while executing process");
                }
            } catch (MachinaException e) {
                sendErrorScanEmail(scanRequest, e);
            }
        }
    }

    public CompletableFuture<ScanResults> executeCxScanFlow(ScanRequest request, File cxFile) throws MachinaException {
        ScanDetails scanDetails = executeCxScan(request,cxFile);
        if(scanDetails.processResults()) {
            return resultsService.processScanResultsAsync(request, scanDetails.getProjectId(), scanDetails.getScanId(), scanDetails.getOsaScanId(), request.getFilters());
        }else{
            return scanDetails.getResults();
        }
    }

    public ScanDetails executeCxScan(ScanRequest request, File cxFile) throws MachinaException {

        String osaScanId = null;
        Integer scanId = null;
        Integer projectId = null;

        try {
            /*Check if team is provided*/
            String ownerId = scanRequestConverter.determineTeamAndOwnerID(request);

            /*Determine project name*/
            String projectName = scanRequestConverter.determineProjectName(request);

            log.debug("Auto profiling is enabled");
            projectId = scanRequestConverter.determinePresetAndProjectId(request, ownerId, projectName);

            request.setProject(projectName);

            CxScanParams params = scanRequestConverter.prepareScanParamsObject(request, cxFile, ownerId, projectName, projectId);

            BugTracker.Type bugTrackerType = triggerBugTrackerEvent(request);

            scanId = cxService.createScan(params,"CxFlow Automated Scan");

            if(bugTrackerType.equals(BugTracker.Type.NONE)){

                log.info("Not waiting for scan completion as Bug Tracker type is NONE");
                CompletableFuture<ScanResults> results = CompletableFuture.completedFuture(null);
                logRequest(request, scanId, cxFile, OperationResult.successful());
                return new ScanDetails(projectId, scanId, results, false);

            } else {

                cxService.waitForScanCompletion(scanId);
                if (projectId == UNKNOWN_INT) {
                    projectId = cxService.getProjectId(ownerId, projectName); //get the project id of the updated or created project
                }
                osaScanId = createOsaScan(request, projectId);
                if(osaScanId != null) {
                    logRequest(request, osaScanId, cxFile, OperationResult.successful());
                }
            }


        }catch (CheckmarxException | GitAPIException e){
            String extendedMessage = ExceptionUtils.getMessage(e);
            log.error(extendedMessage, e);
            Thread.currentThread().interrupt();
            OperationResult scanCreationFailure = new OperationResult(OperationStatus.FAILURE, e.getMessage());
            logRequest(request, scanId, cxFile, scanCreationFailure);
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
            if(flowProperties.isBreakBuild() && results !=null && results.getXIssues()!=null && !results.getXIssues().isEmpty()){
                log.error(ERROR_BREAK_MSG);
                exit(10);
            }
        } catch (IOException e) {
            log.error("Error occurred while attempting to zip path {}", path, e);
            exit(3);
        } catch (MachinaException e){
            log.error("Error occurred", e);
            exit(3);
        }
    }

    public void cxFullScan(ScanRequest request) throws ExitThrowable {

        try {
            CompletableFuture<ScanResults> future = executeCxScanFlow(request, null);
            log.debug("Waiting for scan to complete");
            ScanResults results = future.join();
            if(flowProperties.isBreakBuild() && results !=null && results.getXIssues()!=null && !results.getXIssues().isEmpty()){
                log.error(ERROR_BREAK_MSG);
                exit(10);
            }
        } catch (MachinaException e){
            log.error("Error occurred", e);
            exit(3);
        }
    }

    public void cxParseResults(ScanRequest request, File file) throws ExitThrowable {
        try {
            ScanResults results = cxService.getReportContent(file, request.getFilters());
            resultsService.processResults(request, results, scanDetails);
            if(flowProperties.isBreakBuild() && results !=null && results.getXIssues()!=null && !results.getXIssues().isEmpty()){
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
            if(ScanUtils.empty(originalRequest.getTeam())){
                projects = cxService.getProjects();
            }
            else{ //Get projects for the provided team
                String team = originalRequest.getTeam();
                if(!team.startsWith(cxProperties.getTeamPathSeparator())){
                    team = cxProperties.getTeamPathSeparator().concat(team);
                }
                String teamId = cxService.getTeamId(team);
                projects = cxService.getProjects(teamId);
            }
            for(CxProject project: projects){
                ScanRequest request = new ScanRequest(originalRequest);
                String name = project.getName().replaceAll("[^a-zA-Z0-9-_]+","_");
                //TODO set team when entire instance batch mode
                helperService.getShortUid(request); //update new request object with a unique id for thread log monitoring
                request.setProject(name);
                request.setApplication(name);
                processes.add(resultsService.cxGetResults(request, project));
            }
            log.info("Waiting for processing to complete");
            processes.forEach(CompletableFuture::join);

        } catch ( CheckmarxException e) {
            log.error("Error occurred while processing projects in batch mode", e);
            exit(3);
        }
    }

    private BugTracker.Type triggerBugTrackerEvent(ScanRequest request) {
        BugTracker.Type bugTrackerType = request.getBugTracker().getType();
        if(bugTrackerType.equals(BugTracker.Type.GITLABMERGE)){
            gitLabService.sendMergeComment(request, SCAN_MESSAGE);
            gitLabService.startBlockMerge(request);
        }
        else if(bugTrackerType.equals(BugTracker.Type.GITLABCOMMIT)){
            gitLabService.sendCommitComment(request, SCAN_MESSAGE);
        }
        else if(bugTrackerType.equals(BugTracker.Type.GITHUBPULL)){
            gitService.sendMergeComment(request, SCAN_MESSAGE);
            gitService.startBlockMerge(request, cxProperties.getUrl());
        }
        else if(bugTrackerType.equals(BugTracker.Type.BITBUCKETPULL)){
            bbService.sendMergeComment(request, SCAN_MESSAGE);
        }
        else if(bugTrackerType.equals(BugTracker.Type.BITBUCKETSERVERPULL)){
            bbService.sendServerMergeComment(request, SCAN_MESSAGE);
        }
        else if(bugTrackerType.equals(BugTracker.Type.ADOPULL)){
            adoService.sendMergeComment(request, SCAN_MESSAGE);
            adoService.startBlockMerge(request);
        }
        return bugTrackerType;
    }

    private void logRequest(ScanRequest request, Integer scanId, File  cxFile, OperationResult scanCreationResult)  {
        ScanReport report = new ScanReport(scanId, request, getRepoUrl(request, cxFile), scanCreationResult);
        report.log();
    }

    private void logRequest(ScanRequest request, String scanId, File  cxFile,  OperationResult scanCreationResult)  {
        ScanReport report = new ScanReport(scanId, request, getRepoUrl(request, cxFile), scanCreationResult);
        report.log();
    }

    private String getRepoUrl(ScanRequest request, File cxFile) {
        String repoUrl = null;

        if(sourcesPath != null){
            //the folder to scan is supplied via -f flag in command line and it is located in the filesystem
            repoUrl = sourcesPath;
        }else if(cxFile != null){
            //in general cxFile is a zip created by cxFlow using the folder supplied y -f
            //the use case when sourcePath is empty but cxFile is set is only for the test flow
            repoUrl = cxFile.getAbsolutePath();
        }else{
            //sources to scan are in the remote repository (GitHib, TFS ... etc)
            repoUrl = request.getRepoUrl();
        }
        return repoUrl;
    }

    private boolean isSastScanConfigured() {
        return flowProperties.getEnabledVulnerabilityScanners().contains(ScannerType.SAST.getScanner());
    }

    private void sendSuccessScanEmail(ScanRequest request) {
        Map<String, Object> emailCtx = new HashMap<>();

        emailCtx.put("message", "Checkmarx Scan has been submitted for "
                .concat(request.getNamespace()).concat("/").concat(request.getRepoName()).concat(" - ")
                .concat(request.getRepoUrl()));
        emailCtx.put("heading", "Scan Request Submitted");
        emailService.sendmail(request.getEmail(), "Checkmarx Scan Submitted for ".concat(request.getNamespace()).concat("/").concat(request.getRepoName()), emailCtx, "message.html");
    }

    private void sendErrorScanEmail(ScanRequest request, MachinaException e) {
        Map<String, Object>  emailCtx = new HashMap<>();

        log.error("Machina Exception has occurred.", e);
        emailCtx.put("message", "Error occurred during scan/bug tracking process for "
                .concat(request.getNamespace()).concat("/").concat(request.getRepoName()).concat(" - ")
                .concat(request.getRepoUrl()).concat("  Error: ").concat(e.getMessage()));
        emailCtx.put("heading", "Error occurred during scan");
        emailService.sendmail(request.getEmail(), "Error occurred for ".concat(request.getNamespace()).concat("/").concat(request.getRepoName()), emailCtx, "message-error.html");
    }

    private String createOsaScan(ScanRequest request, Integer projectId) throws GitAPIException, CheckmarxException {
        String osaScanId = null;
        if(cxProperties.getEnableOsa()){
            String path = cxProperties.getGitClonePath().concat("/").concat(UUID.randomUUID().toString());
            File pathFile = new File(path);

            Git git = Git.cloneRepository()
                    .setURI(request.getRepoUrlWithAuth())
                    .setBranch(request.getBranch())
                    .setBranchesToClone(Collections.singleton(Constants.CX_BRANCH_PREFIX.concat(request.getBranch()) ))
                    .setDirectory(pathFile)
                    .call();
            osaScanId = osaService.createScan(projectId, path);
        }
        return osaScanId;
    }
}