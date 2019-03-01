package com.custodela.machina.service;

import checkmarx.wsdl.portal.Scan;
import com.custodela.machina.config.CxProperties;
import com.custodela.machina.config.MachinaProperties;
import com.custodela.machina.dto.*;
import com.custodela.machina.dto.cx.CxProject;
import com.custodela.machina.exception.MachinaException;
import com.custodela.machina.utils.ScanUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import javax.annotation.Nullable;
import java.beans.ConstructorProperties;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import static com.custodela.machina.service.CxService.UNKNOWN;
import static com.custodela.machina.service.CxService.UNKNOWN_INT;
import static java.lang.System.exit;

@Service
public class MachinaService {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(MachinaService.class);
    private final CxService cxService;
    private final GitHubService gitService;
    private final GitLabService gitLabService;
    private final BitBucketService bbService;
    private final EmailService emailService;
    private final CxProperties cxProperties;
    private final MachinaProperties machinaProperties;
    private final ResutlsService resutlsService;
    private static final Long SLEEP = 20000L;

    @ConstructorProperties({"cxService", "resultService", "gitService", "gitLabService", "bbService", "emailService", "cxProperties", "machinaProperties"})
    public MachinaService(CxService cxService, ResutlsService resutlsService, GitHubService gitService,
                          GitLabService gitLabService, BitBucketService bbService, EmailService emailService,
                          CxProperties cxProperties, MachinaProperties machinaProperties) {
        this.cxService = cxService;
        this.resutlsService = resutlsService;
        this.gitService = gitService;
        this.gitLabService = gitLabService;
        this.bbService = bbService;
        this.emailService = emailService;
        this.cxProperties = cxProperties;
        this.machinaProperties = machinaProperties;
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
                executeCxScanFlow(request, null);
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

    private void executeCxScanFlow(ScanRequest request, File cxFile) throws MachinaException {
        try {
            String ownerId = cxService.getTeamId(cxProperties.getTeam());
            Integer presetId = cxService.getPresetId(request.getScanPreset());
            Integer engineId = cxService.getScanConfiguration(cxProperties.getConfiguration());
            String projectName;
            Integer projectId;
            if(cxProperties.isMultiTenant()){
                String fullTeamName = cxProperties.getTeam().concat("\\").concat(request.getNamespace());

                String tmpId = cxService.getTeamId(fullTeamName);
                if(tmpId.equals(UNKNOWN)){
                    ownerId = cxService.createTeam(ownerId, request.getNamespace());
                }
                else{
                    ownerId = tmpId;
                }
                projectName = request.getRepoName().concat("-").concat(request.getBranch());
            }
            else {
                projectName = request.getNamespace().concat("-").concat(request.getRepoName()).concat("-").concat(request.getBranch());
            }
            projectId = cxService.getProjectId(ownerId, projectName);
            if (projectId.equals(UNKNOWN_INT)) {
                projectId = cxService.createProject(ownerId, projectName);
            }
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
            cxService.setProjectExcludeDetails(projectId, request.getExcludeFolders(), request.getExcludeFiles());
            Integer scanId = cxService.createScan(projectId, request.isIncremental(), false, false, "Automated scan");

            String SCAN_MESSAGE = "Scan submitted to Checkmarx";
            if(request.getBugTracker().getType().equals(BugTracker.Type.GITLABMERGE)){
                gitLabService.sendMergeComment(request, SCAN_MESSAGE);
            }
            else if(request.getBugTracker().getType().equals(BugTracker.Type.GITLABCOMMIT)){
                gitLabService.sendCommitComment(request, SCAN_MESSAGE);
            }
            else if(request.getBugTracker().getType().equals(BugTracker.Type.GITHUBPULL)){
                gitService.sendMergeComment(request, SCAN_MESSAGE);
            }
            else if(request.getBugTracker().getType().equals(BugTracker.Type.BITBUCKETPULL)){
                bbService.sendMergeComment(request, SCAN_MESSAGE);
            }

            Integer status = cxService.getScanStatus(scanId);
            if(request.getBugTracker().getType().equals(BugTracker.Type.NONE)){
                log.info("Not waiting for scan completion as Bug Tracker type is NONE");
                return;
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
             resutlsService.processScanResultsAsync(request, scanId, request.getFilters());
        }catch (InterruptedException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            throw new MachinaException("Interrupted Exception Occurred");
        }
    }

    public void cxFullScan(ScanRequest request, String path){

        try {
            String cxZipFile = FileSystems.getDefault().getPath("cx.".concat(UUID.randomUUID().toString()).concat(".zip")).toAbsolutePath().toString();
            ScanUtils.zipDirectory(path, cxZipFile);
            File f = new File(cxZipFile);
            executeCxScanFlow(request, f);
            log.info("Processing results with JIRA issue tracking");
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
            resutlsService.processResults(request, results);
            if(machinaProperties.isBreakBuild() && !results.getXIssues().isEmpty()){
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
            resutlsService.processResults(request, results);
            if(machinaProperties.isBreakBuild() && !results.getXIssues().isEmpty()){
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
                if (!team.startsWith("\\")) {
                    team = "\\".concat(team);
                }
                String teamId = cxService.getTeamId(team);
                Integer projectId = cxService.getProjectId(teamId, request.getProject());
                project = cxService.getProject(projectId);

            }
            else {
                project = cxProject;
            }
            Integer scanId = cxService.getLastScanId(project.getId());
            if(scanId.equals(UNKNOWN_INT)){
                log.info("No Scan Results to process for project {}", project.getName());
                CompletableFuture<ScanResults> x = new CompletableFuture<>();
                x.complete(null);
                return x;
            }
            else {
                getCxFields(project, request);
                CompletableFuture<ScanResults> results = resutlsService.processScanResultsAsync(request, scanId, request.getFilters());
                /*If cxProject is null, it is a single project request*/
                if(cxProject == null) {
                    results.join();
                }
                return results;
            }

        } catch (MachinaException e) {
            log.debug(ExceptionUtils.getStackTrace(e));
            log.error("Error occurred while processing results for {}{}", request.getTeam(), request.getProject());
            CompletableFuture x = new CompletableFuture();
            x.completeExceptionally(e);
            return x;
        }
    }

    private void getCxFields(CxProject project, ScanRequest request) {
        if(project == null) { return; }

        Map<String, String> fields = new HashMap<>();
        for(CxProject.CustomField field : project.getCustomFields()){
            if(!ScanUtils.empty(field.getName()) && !ScanUtils.empty(field.getValue())) {
                fields.put(field.getName(), field.getValue());
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
                request.setProject(project.getName());
                request.setApplication(project.getName());
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
