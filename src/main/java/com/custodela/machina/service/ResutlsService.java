package com.custodela.machina.service;

import com.custodela.machina.config.CxProperties;
import com.custodela.machina.config.MachinaProperties;
import com.custodela.machina.dto.*;
import com.custodela.machina.dto.cx.CxProject;
import com.custodela.machina.exception.InvalidCredentialsException;
import com.custodela.machina.exception.MachinaException;
import com.custodela.machina.utils.ScanUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.beans.ConstructorProperties;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import static com.custodela.machina.service.CxService.UNKNOWN;
import static com.custodela.machina.service.CxService.UNKNOWN_INT;

@Service
public class ResutlsService {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ResutlsService.class);
    private final CxService cxService;
    private final JiraService jiraService;
    private final GitHubService gitService;
    private final GitLabService gitLabService;
    private final BitBucketService bbService;
    private final EmailService emailService;
    private final CxProperties cxProperties;
    private final MachinaProperties machinaProperties;
    private static final Long SLEEP = 20000L;
    private static final Long TIMEOUT = 300000L;

    @ConstructorProperties({"cxService", "jiraService", "gitService", "gitLabService", "bbService","emailService", "cxProperties", "machinaProperties"})
    public ResutlsService(CxService cxService, JiraService jiraService, GitHubService gitService,
                          GitLabService gitLabService, BitBucketService bbService, EmailService emailService,
                          CxProperties cxProperties, MachinaProperties machinaProperties) {
        this.cxService = cxService;
        this.jiraService = jiraService;
        this.gitService = gitService;
        this.gitLabService = gitLabService;
        this.bbService = bbService;
        this.emailService = emailService;
        this.cxProperties = cxProperties;
        this.machinaProperties = machinaProperties;
    }

    @Async("scanRequest")
    public CompletableFuture<ScanResults> processScanResultsAsync(ScanRequest request, Integer scanId, List<Filter> filters) throws MachinaException {

        CompletableFuture<ScanResults> future = new CompletableFuture<>();
        ScanResults results = getScanResults(scanId, filters);
        Map<String, Object>  emailCtx = new HashMap<>();
        //Send email (if EMAIL was enabled and EMAL was not main feedback option
        if(machinaProperties.getMail().isEnabled() &&
                !request.getBugTracker().getType().equals(BugTracker.Type.NONE) &&
                !request.getBugTracker().getType().equals(BugTracker.Type.EMAIL)) {

            emailCtx.put("message", "Successfully completed processing for "
                    .concat(request.getNamespace()).concat("/").concat(request.getRepoName()).concat(" - ")
                    .concat(request.getRepoUrl()));
            emailCtx.put("heading", "Scan Successfully Completed");

            if (results != null) {
                emailCtx.put("issues", results.getXIssues());
            }
            if (results != null && !ScanUtils.empty(results.getLink())) {
                emailCtx.put("link", results.getLink());
            }
            emailCtx.put("repo", request.getRepoUrl());
            emailCtx.put("repo_fullname", request.getNamespace().concat("/").concat(request.getRepoName()));
            emailService.sendmail(request.getEmail(), "Successfully completed processing for ".concat(request.getNamespace()).concat("/").concat(request.getRepoName()), emailCtx, "template-demo.html");
            log.info("Successfully completed automation for repository {} under namespace {}", request.getRepoName(), request.getNamespace());
        }
        processResults(request, results);
        log.info("Process completed Succesfully");
        future.complete(results);
        return future;
    }

    private ScanResults getScanResults(Integer scanId, List<Filter> filters) throws MachinaException {
        try {
            Integer reportId = cxService.createScanReport(scanId);
            Thread.sleep(SLEEP);
            int timer = 0;
            while (cxService.getReportStatus(reportId).equals(CxService.REPORT_STATUS_FINISHED)) {
                Thread.sleep(SLEEP);
                timer += SLEEP;
                if (timer >= TIMEOUT) {
                    log.error("Report Generation timeout.  {}", TIMEOUT);
                    throw new MachinaException("Timeout exceeded during report generation");
                }
            }
            Thread.sleep(SLEEP);
            return cxService.getReportContent(reportId, filters);
        } catch (InterruptedException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            throw new MachinaException("Interrupted Exception Occurred");
        }
    }

    void processResults(ScanRequest request, ScanResults results) throws MachinaException {
        switch (request.getBugTracker().getType()) {
            case JIRA:
                log.info("Processing results with JIRA issue tracking");
                if(!cxProperties.getOffline()) {
                    getCxFields(request, results);
                }
                jiraService.process(results, request);
                break;
            case GITHUB:
                log.info("Processing results with Github issue tracking");
                gitService.process(results, request);
                break;
            case GITHUBPULL:
                gitService.processPull(request, results);
                break;
            case GITLAB:
                log.info("Processing results with Gitlab issue tracking");
                if(ScanUtils.empty(request.getNamespace()) || ScanUtils.empty(request.getRepoName())){
                    throw new MachinaException("namespace and repo-name must be provided");
                }
                Integer projectId = gitLabService.getProjectDetails(request.getNamespace(), request.getRepoName());
                if(projectId.equals(UNKNOWN_INT)){
                    throw new MachinaException("Project not found in Gitlab");
                }
                request.setId(projectId);
                gitLabService.process(results, request);
                break;
            case GITLABCOMMIT:
                gitLabService.processCommit(request, results);
                break;
            case GITLABMERGE:
                gitLabService.processMerge(request, results);
                break;
            case BITBUCKETCOMMIT:
                bbService.processCommit(request, results);
                break;
            case BITBUCKETPULL:
                bbService.processMerge(request, results);
                break;
            case BITBUCKETSERVERPULL:
                bbService.processServerMerge(request, results);
                break;
            case EMAIL:
                if(!machinaProperties.getMail().isEnabled()) {
                    Map<String, Object> emailCtx = new HashMap<>();
                    emailCtx.put("message", "Checkmarx Scan Results "
                            .concat(request.getNamespace()).concat("/").concat(request.getRepoName()).concat(" - ")
                            .concat(request.getRepoUrl()));
                    emailCtx.put("heading", "Scan Successfully Completed");

                    if (results != null) {
                        emailCtx.put("issues", results.getXIssues());
                    }
                    if (results != null && !ScanUtils.empty(results.getLink())) {
                        emailCtx.put("link", results.getLink());
                    }
                    emailCtx.put("repo", request.getRepoUrl());
                    emailCtx.put("repo_fullname", request.getNamespace().concat("/").concat(request.getRepoName()));
                    emailService.sendmail(request.getEmail(), "Successfully completed processing for ".concat(request.getNamespace()).concat("/").concat(request.getRepoName()), emailCtx, "template-demo.html");
                }
                break;
            case NONE:
                log.info("Issue tracking is turned off");
                break;
            default:
                log.warn("No valid bug type was provided");
        }
    }

    /**
     *
     * @param request
     * @param results
     */
    private void getCxFields(ScanRequest request, ScanResults results) throws MachinaException{
        try{
            /*Are cx fields required?*/
            if(!requiresCxCustomFields(request.getBugTracker().getFields())){
                return;
            }
            /*if so, then get them and add them to the request object*/
            if(!ScanUtils.empty(results.getProjectId()) && !results.getProjectId().equals(UNKNOWN)){
                CxProject project = cxService.getProject(Integer.parseInt(results.getProjectId()));
                Map<String, String> fields = new HashMap<>();
                for(CxProject.CustomField field : project.getCustomFields()){
                    if(!ScanUtils.empty(field.getName()) && !ScanUtils.empty(field.getValue())) {
                        fields.put(field.getName(), field.getValue());
                    }
                }
                if(!fields.isEmpty()){
                    request.setCxFields(fields);
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
                }
            }
        }catch (InvalidCredentialsException e){
            log.warn("Error retrieving Checkmarx Project details for {}, no custom fields will be available", results.getProjectId());
            throw new MachinaException("Error logging into Checkmarx");
        }
    }

    /**
     * Check if even 1 cx field is required, which means Checkmarx project needs to be retrieved
     *
     * @param fields
     * @return
     */
    private boolean requiresCxCustomFields(List<Field> fields){
        if(fields == null){
            return false;
        }
        for(Field f: fields){
            if(f.getType().equals("cx")){
                return true;
            }
        }
        return false;
    }

}

