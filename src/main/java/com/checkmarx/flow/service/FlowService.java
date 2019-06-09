package com.checkmarx.flow.service;

import com.checkmarx.flow.config.CxProperties;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.*;
import com.checkmarx.flow.dto.cx.CxProject;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.utils.ScanUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import javax.annotation.Nullable;
import java.beans.ConstructorProperties;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import static com.checkmarx.flow.service.CxService.UNKNOWN;
import static com.checkmarx.flow.service.CxService.UNKNOWN_INT;
import static java.lang.System.exit;

@Service
public class FlowService {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(FlowService.class);

    private static final String SCAN_MESSAGE = "Scan submitted to Checkmarx";

    private final CxService cxService;
    private final GitHubService gitService;
    private final GitLabService gitLabService;
    private final BitBucketService bbService;
    private final ADOService adoService;
    private final EmailService emailService;
    private final CxProperties cxProperties;
    private final FlowProperties flowProperties;
    private final ResultsService resultsService;
    private final HelperService helperService;
    private static final Long SLEEP = 20000L;

    @ConstructorProperties({"cxService", "resultService", "gitService", "gitLabService", "bbService",
            "adoService", "emailService", "helperService", "cxProperties", "flowProperties"})
    public FlowService(CxService cxService, ResultsService resultsService, GitHubService gitService,
                       GitLabService gitLabService, BitBucketService bbService, ADOService adoService,
                       EmailService emailService, HelperService helperService, CxProperties cxProperties,
                       FlowProperties flowProperties) {
        this.cxService = cxService;
        this.resultsService = resultsService;
        this.gitService = gitService;
        this.gitLabService = gitLabService;
        this.bbService = bbService;
        this.adoService = adoService;
        this.emailService = emailService;
        this.helperService = helperService;
        this.cxProperties = cxProperties;
        this.flowProperties = flowProperties;
    }

    @Async("webHook")
    public void initiateAutomation(ScanRequest request) {
        Map<String, Object>  emailCtx = new HashMap<>();
        try {
            if (request.getProduct().equals(ScanRequest.Product.CX)) {
                emailCtx.put("message", "Checkmarx Scan has been submitted for "
                        .concat(request.getNamespace()).concat("/").concat(request.getRepoName()).concat(" - ")
                        .concat(request.getRepoUrl()));
                emailCtx.put("heading","Scan Request Submitted");
                emailService.sendmail(request.getEmail(), "Checkmarx Scan Submitted for ".concat(request.getNamespace()).concat("/").concat(request.getRepoName()), emailCtx, "message.html");
                CompletableFuture<ScanResults> results = executeCxScanFlow(request, null);
                if(results.isCompletedExceptionally()){
                    log.error("An error occurred while executing process");
                }
            } else {
                log.warn("Unknown Product type of {}, exiting", request.getProduct());
            }
        } catch (MachinaException e){
            log.error("Machina Exception has occurred.  {}", ExceptionUtils.getStackTrace(e));
            emailCtx.put("message", "Error occurred during scan/bug tracking process for "
                    .concat(request.getNamespace()).concat("/").concat(request.getRepoName()).concat(" - ")
                    .concat(request.getRepoUrl()).concat("  Error: ").concat(e.getMessage()));
            emailCtx.put("heading","Error occurred during scan");
            emailService.sendmail(request.getEmail(), "Error occurred for ".concat(request.getNamespace()).concat("/").concat(request.getRepoName()), emailCtx, "message-error.html");
        }
    }

    private CompletableFuture<ScanResults> executeCxScanFlow(ScanRequest request, File cxFile) throws MachinaException {
        try {
            String ownerId;
            Integer presetId = cxService.getPresetId(request.getScanPreset());
            Integer engineId = cxService.getScanConfiguration(cxProperties.getConfiguration());
            String projectName;
            Integer projectId;
            String repoName = request.getRepoName();
            String branch = request.getBranch();
            String namespace = request.getNamespace();

            /*Check if team is provided*/
            String team = helperService.getCxTeam(request);
            if(!ScanUtils.empty(team)){
                log.info("Overriding team with {}", team);
                ownerId = cxService.getTeamId(team);
            }
            else{
                ownerId = cxService.getTeamId(cxProperties.getTeam());

                if(cxProperties.isMultiTenant() &&
                        !ScanUtils.empty(namespace)){
                    String fullTeamName = cxProperties.getTeam().concat("\\").concat(namespace);
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
                    request.setTeam(cxProperties.getTeam());
                }
            }

            /*Determine project name*/
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
            projectId = cxService.getProjectId(ownerId, projectName);
            if (projectId.equals(UNKNOWN_INT)) {
                log.info("Project does not exist.  Creating new project now for {}", projectName);
                projectId = cxService.createProject(ownerId, projectName);
            }
            request.setProject(projectName);
            if(cxService.scanExists(projectId)){
                throw new MachinaException("Active Scan already exists for Project:"+projectId);
            }
            cxService.createScanSetting(projectId, presetId, engineId);
            //If a file is provided, it will be uploaded as source
            if(cxFile != null){
                cxService.uploadProjectSource(projectId, cxFile);
            }
            else {
                cxService.setProjectRepositoryDetails(projectId, request.getRepoUrlWithAuth(), request.getRefs());
            }
            /*
            If incremental scan support is enabled, determine if the last full finished scan (within configurable number of scans) is under
            a configurable number of days old, if so, an incremental scan is completed - otherwise, full scan is completed
             */
            if(request.isIncremental()){
                LocalDateTime scanDate = cxService.getLastScanDate(projectId);
                if(scanDate == null || LocalDateTime.now().isAfter(scanDate.plusDays(cxProperties.getIncrementalThreshold()))){
                    log.debug("Last scanDate: {}", scanDate);
                    log.info("Last scanDate does not meet the threshold for an incremental scan.");
                    request.setIncremental(false);
                }
                else{
                    log.info("Scan will be incremental");
                }
            }
            cxService.setProjectExcludeDetails(projectId, request.getExcludeFolders(), request.getExcludeFiles());
            Integer scanId = cxService.createScan(projectId, request.isIncremental(), true, false, "Automated scan");

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

            Integer status = cxService.getScanStatus(scanId);
            if(bugTrackerType.equals(BugTracker.Type.NONE)){
                log.info("Not waiting for scan completion as Bug Tracker type is NONE");
                return CompletableFuture.completedFuture(null);
            }
            long timer = 0;
            while (!status.equals(CxService.SCAN_STATUS_FINISHED) && !status.equals(CxService.SCAN_STATUS_CANCELED) &&
                    !status.equals(CxService.SCAN_STATUS_FAILED)) {
                Thread.sleep(SLEEP);
                status = cxService.getScanStatus(scanId);
                timer += SLEEP;
                if(timer >= (cxProperties.getScanTimeout()*60000)){
                    log.error("Scan timeout exceeded.  {} minutes", cxProperties.getScanTimeout());
                    throw new MachinaException("Timeout exceeded during scan");
                }
            }
            if(status.equals(CxService.SCAN_STATUS_FAILED)){
                throw new MachinaException("Scan failed");
            }
             return resultsService.processScanResultsAsync(request, scanId, request.getFilters());
        }catch (InterruptedException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            Thread.currentThread().interrupt();
            throw new MachinaException("Interrupted Exception Occurred");
        }
    }

    public void cxFullScan(ScanRequest request, String path){

        try {
            String cxZipFile = FileSystems.getDefault().getPath("cx.".concat(UUID.randomUUID().toString()).concat(".zip")).toAbsolutePath().toString();
            ScanUtils.zipDirectory(path, cxZipFile);
            File f = new File(cxZipFile);
            log.debug(f.getPath());
            log.debug("free space "+ f.getFreeSpace());
            log.debug("total space "+ f.getTotalSpace());
            log.debug(f.getAbsolutePath());
            CompletableFuture<ScanResults> future = executeCxScanFlow(request, f);
            log.debug("Waiting for scan to complete");
            ScanResults results = future.join();
            if(flowProperties.isBreakBuild() && results !=null && results.getXIssues()!=null && !results.getXIssues().isEmpty()){
                log.error("Exiting with Error code 10 due to issues present");
                exit(10);
            }
        } catch (IOException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            log.error("Error occurred while attempting to zip path {}", path);
            exit(3);
        } catch (MachinaException e){
            log.error(ExceptionUtils.getStackTrace(e));
            exit(3);
        }
    }

    public void cxParseResults(ScanRequest request, File file){
        try {
            ScanResults results = cxService.getReportContent(file, request.getFilters());
            resultsService.processResults(request, results);
            if(flowProperties.isBreakBuild() && results !=null && results.getXIssues()!=null && !results.getXIssues().isEmpty()){
                log.error("Exiting with Error code 10 due to issues present");
                exit(10);
            }
        } catch (MachinaException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            log.error("Error occurred while processing results file");
            exit(3);
        }
    }

    public void cxOsaParseResults(ScanRequest request, File file, File libs){
        try {
            ScanResults results = cxService.getOsaReportContent(file, libs, request.getFilters());
            resultsService.processResults(request, results);
            if(flowProperties.isBreakBuild() && results !=null && results.getXIssues()!=null && !results.getXIssues().isEmpty()){
                log.error("Exiting with Error code 10 due to issues present");
                exit(10);
            }
        } catch (MachinaException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            log.error("Error occurred while processing results file(s)");
            exit(3);
        }
    }


    public CompletableFuture<ScanResults> cxGetResults(ScanRequest request, @Nullable CxProject cxProject){
        try {
            CxProject project;

            if(cxProject == null) {
                String team = request.getTeam();
                if(ScanUtils.empty(team)){
                    //if the team is not provided, use the default
                    team = cxProperties.getTeam();
                    request.setTeam(team);
                }
                if (!team.startsWith("\\")) {
                    team = "\\".concat(team);
                }
                String teamId = cxService.getTeamId(team);
                Integer projectId = cxService.getProjectId(teamId, request.getProject());
                if(projectId.equals(UNKNOWN_INT)){
                    log.warn("No project found for {}", request.getProject());
                    CompletableFuture<ScanResults> x = new CompletableFuture<>();
                    x.complete(null);
                    return x;
                }
                project = cxService.getProject(projectId);

            }
            else {
                project = cxProject;
            }
            Integer scanId = cxService.getLastScanId(project.getId());
            if(scanId.equals(UNKNOWN_INT)){
                log.warn("No Scan Results to process for project {}", project.getName());
                CompletableFuture<ScanResults> x = new CompletableFuture<>();
                x.complete(null);
                return x;
            }
            else {
                getCxFields(project, request);
                return resultsService.processScanResultsAsync(request, scanId, request.getFilters());
            }

        } catch (MachinaException e) {
            log.debug(ExceptionUtils.getStackTrace(e));
            log.error("Error occurred while processing results for {}{}", request.getTeam(), request.getProject());
            CompletableFuture<ScanResults> x = new CompletableFuture<>();
            x.completeExceptionally(e);
            return x;
        }
    }

    private void getCxFields(CxProject project, ScanRequest request) {
        if(project == null) { return; }

        Map<String, String> fields = new HashMap<>();
        for(CxProject.CustomField field : project.getCustomFields()){
            String name = field.getName();
            String value = field.getValue();
            if(!ScanUtils.empty(name) && !ScanUtils.empty(value)) {
                fields.put(name, value);
            }
        }
        if(!ScanUtils.empty(cxProperties.getJiraProjectField())){
            String jiraProject = fields.get(cxProperties.getJiraProjectField());
            if(!ScanUtils.empty(jiraProject)) {
                request.getBugTracker().setProjectKey(jiraProject);
            }
        }
        if(!ScanUtils.empty(cxProperties.getJiraIssuetypeField())) {
            String jiraIssuetype = fields.get(cxProperties.getJiraIssuetypeField());
            if (!ScanUtils.empty(jiraIssuetype)) {
                request.getBugTracker().setIssueType(jiraIssuetype);
            }
        }
        if(!ScanUtils.empty(cxProperties.getJiraCustomField()) &&
                (fields.get(cxProperties.getJiraCustomField()) != null) && !fields.get(cxProperties.getJiraCustomField()).isEmpty()){
            request.getBugTracker().setFields(ScanUtils.getCustomFieldsFromCx(fields.get(cxProperties.getJiraCustomField())));
        }

        if(!ScanUtils.empty(cxProperties.getJiraAssigneeField())){
            String assignee = fields.get(cxProperties.getJiraAssigneeField());
            if(!ScanUtils.empty(assignee)) {
                request.getBugTracker().setAssignee(assignee);
            }
        }

        request.setCxFields(fields);
    }


    /**
     * Process Projects in batch mode - JIRA ONLY
     *
     * @param originalRequest
     */
    public void cxBatch(ScanRequest originalRequest) {
        try {
            List<CxProject> projects;
            List<CompletableFuture<ScanResults>> processes = new ArrayList<>();
            //Get all projects
            if(ScanUtils.empty(originalRequest.getTeam())){
                projects = Arrays.asList(cxService.getProjects());
            }
            else{ //Get projects for the provided team
                String team = originalRequest.getTeam();
                if(!team.startsWith("\\")){
                    team = "\\".concat(team);
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
                processes.add(cxGetResults(request, project));
            }
            log.info("Waiting for processing to complete");
            processes.forEach(CompletableFuture::join);

        } catch (MachinaException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            log.error("Error occurred while processing projects in batch mode");
            exit(3);
        }
    }

}
