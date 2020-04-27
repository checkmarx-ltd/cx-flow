package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.*;
import com.checkmarx.flow.dto.report.ScanReport;
import com.checkmarx.flow.exception.ExitThrowable;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.flow.utils.ZipUtils;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.dto.ScanResults;
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
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.checkmarx.flow.exception.ExitThrowable.exit;
import static com.checkmarx.sdk.config.Constants.UNKNOWN;
import static com.checkmarx.sdk.config.Constants.UNKNOWN_INT;

@Service
@Slf4j
@RequiredArgsConstructor
public class SastScannerService {

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

    private ScanDetails scanDetails = null;
    private String sourcesPath = null;

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
            String ownerId = determineTeamAndOwnerID(request);

            /*Determine project name*/
            String projectName = determineProjectName(request);

            log.debug("Auto profiling is enabled");
            projectId = determinePresetAndProjectId(request, ownerId, projectName);

            request.setProject(projectName);

            CxScanParams params = prepareScanParamsObject(request, cxFile, ownerId, projectName, projectId);

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

    private String determineTeamAndOwnerID(ScanRequest request) throws CheckmarxException, MachinaException {

        String ownerId;
        String namespace = request.getNamespace();

        String team = helperService.getCxTeam(request);
        if(!ScanUtils.empty(team)){
            if(!team.startsWith(cxProperties.getTeamPathSeparator()))
                team = cxProperties.getTeamPathSeparator().concat(team);
            log.info("Overriding team with {}", team);
            ownerId = cxService.getTeamId(team);
        }
        else{
            team = cxProperties.getTeam();
            if(!team.startsWith(cxProperties.getTeamPathSeparator()))
                team = cxProperties.getTeamPathSeparator().concat(team);
            log.info("Using Checkmarx team: {}", team);
            ownerId = cxService.getTeamId(team);

            if(cxProperties.isMultiTenant() &&
                    !ScanUtils.empty(namespace)){
                String fullTeamName = cxProperties.getTeam().concat(cxProperties.getTeamPathSeparator()).concat(namespace);
                log.info("Using multi tenant team name: {}", fullTeamName);
                request.setTeam(fullTeamName);
                String tmpId = cxService.getTeamId(fullTeamName);
                if(tmpId.equals(UNKNOWN)){
                    ownerId = cxService.createTeam(ownerId, namespace);
                }
                else{
                    ownerId = tmpId;
                }
            }
            else{
                request.setTeam(team);
            }
        }

        //Kick out if the team is unknown
        if (ownerId.equals(UNKNOWN)) {
            throw new CheckmarxException(getTeamErrorMessage());
        }
        return ownerId;
    }

    private String determineProjectName(ScanRequest request) throws MachinaException {
        String projectName;
        String repoName = request.getRepoName();
        String branch = request.getBranch();
        String namespace = request.getNamespace();

        String project = helperService.getCxProject(request);
        if(!ScanUtils.empty(project)){
            projectName = project;
        }
        else if(cxProperties.isMultiTenant() && !ScanUtils.empty(repoName)){
            projectName = repoName;
            if(!ScanUtils.empty(branch)){
                projectName = projectName.concat("-").concat(branch);
            }
        }
        else{
            if(!ScanUtils.empty(namespace) && !ScanUtils.empty(repoName) && !ScanUtils.empty(branch)) {
                projectName = namespace.concat("-").concat(repoName).concat("-").concat(branch);
            }
            else if(!ScanUtils.empty(request.getApplication())) {
                projectName = request.getApplication();
            }
            else{
                log.error("Namespace (--namespace)/RepoName(--repo-name)/Branch(--branch) OR Application (--app) must be provided if the Project is not provided (--cx-project)");
                throw new MachinaException("Namespace (--namespace)/RepoName(--repo-name)/Branch(--branch) OR Application (--app) must be provided if the Project is not provided (--cx-project)") ;
            }
        }

        //only allow specific chars in project name in checkmarx
        projectName = projectName.replaceAll("[^a-zA-Z0-9-_.]+","-");
        log.info("Project Name being used {}", projectName);

        return projectName;
    }

    private Integer determinePresetAndProjectId(ScanRequest request, String ownerId, String projectName ) {

        Boolean projectExists = false;
        Integer projectId = UNKNOWN_INT;
        if(flowProperties.isAutoProfile() && !request.isScanPresetOverride()) {

            projectId = cxService.getProjectId(ownerId, projectName);
            if (projectId != UNKNOWN_INT) {
                int presetId = cxService.getProjectPresetId(projectId);
                if (presetId != UNKNOWN_INT) {
                    String preset = cxService.getPresetName(presetId);
                    request.setScanPreset(preset);
                    projectExists = true;
                }
            }
        }
        if(!projectExists || flowProperties.isAlwaysProfile()) {
            log.info("Project is new, profiling source...");
            Sources sources = new Sources();
            switch (request.getRepoType()) {
                case GITHUB:
                    sources = gitService.getRepoContent(request);
                    break;
                case GITLAB:
                    sources = gitLabService.getRepoContent(request);
                    break;
                case BITBUCKET:
                    log.warn("Profiling is not available for BitBucket Cloud");
                    break;
                case BITBUCKETSERVER:
                    log.warn("Profiling is not available for BitBucket Server");
                    break;
                case ADO:
                    log.warn("Profiling is not available for Azure DevOps");
                    break;
                default:
                    break;
            }
            String preset = helperService.getPresetFromSources(sources);
            if (!ScanUtils.empty(preset)) {
                request.setScanPreset(preset);
            }

        }
        return projectId;
    }

    private CxScanParams prepareScanParamsObject(ScanRequest request, File cxFile, String ownerId, String projectName, Integer projectId) {
        CxScanParams params = new CxScanParams()
                .teamId(ownerId)
                .withTeamName(request.getTeam())
                .projectId(projectId)
                .withProjectName(projectName)
                .withScanPreset(request.getScanPreset())
                .withGitUrl(request.getRepoUrlWithAuth())
                .withIncremental(request.isIncremental())
                .withForceScan(request.isForceScan())
                .withFileExclude(request.getExcludeFiles())
                .withFolderExclude(request.getExcludeFolders());
        if(!com.checkmarx.sdk.utils.ScanUtils.empty(request.getBranch())){
            params.withBranch(Constants.CX_BRANCH_PREFIX.concat(request.getBranch()));
        }
        if(cxFile != null){
            params.setSourceType(CxScanParams.Type.FILE);
            params.setFilePath(cxFile.getAbsolutePath());
        }
        return params;
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

    private String getTeamErrorMessage() {
        return "Parent team could not be established. Please ensure correct team is provided.\n" +
                "Some hints:\n" +
                "\t- team name is case-sensitive\n" +
                "\t- trailing slash is not allowed\n" +
                "\t- team name separator depends on Checkmarx product version specified in CxFlow configuration:\n" +
                String.format("\t\tCheckmarx version: %s%n", cxProperties.getVersion()) +
                String.format("\t\tSeparator that should be used: %s%n", cxProperties.getTeamPathSeparator());
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