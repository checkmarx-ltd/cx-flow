package com.checkmarx.flow.service;

import com.checkmarx.flow.config.CxProperties;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.*;
import com.checkmarx.flow.dto.cx.CxProject;
import com.checkmarx.flow.exception.InvalidCredentialsException;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.utils.ScanUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.beans.ConstructorProperties;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import static com.checkmarx.flow.service.CxService.UNKNOWN;

@Service
public class ResultsService {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ResultsService.class);
    private final CxService cxService;
    private final JiraService jiraService;
    private final IssueService issueService;
    private final GitHubService gitService;
    private final GitLabService gitLabService;
    private final BitBucketService bbService;
    private final ADOService adoService;
    private final EmailService emailService;
    private final CxProperties cxProperties;
    private final FlowProperties flowProperties;
    private static final Long SLEEP = 20000L;
    private static final Long TIMEOUT = 300000L;

    @ConstructorProperties({"cxService", "jiraService", "issueService", "gitService", "gitLabService", "bbService",
            "adoService","emailService", "cxProperties", "flowProperties"})
    public ResultsService(CxService cxService, JiraService jiraService, IssueService issueService, GitHubService gitService,
                          GitLabService gitLabService, BitBucketService bbService, ADOService adoService,
                          EmailService emailService, CxProperties cxProperties, FlowProperties flowProperties) {
        this.cxService = cxService;
        this.jiraService = jiraService;
        this.issueService = issueService;
        this.gitService = gitService;
        this.gitLabService = gitLabService;
        this.bbService = bbService;
        this.adoService = adoService;
        this.emailService = emailService;
        this.cxProperties = cxProperties;
        this.flowProperties = flowProperties;
    }

    @Async("scanRequest")
    public CompletableFuture<ScanResults> processScanResultsAsync(ScanRequest request, Integer scanId, List<Filter> filters) throws MachinaException {
        try {
            CompletableFuture<ScanResults> future = new CompletableFuture<>();
            ScanResults results = getScanResults(scanId, filters);
            Map<String, Object> emailCtx = new HashMap<>();
            BugTracker.Type bugTrackerType = request.getBugTracker().getType();
            //Send email (if EMAIL was enabled and EMAL was not main feedback option
            if (flowProperties.getMail() != null && flowProperties.getMail().isEnabled() &&
                    !bugTrackerType.equals(BugTracker.Type.NONE) &&
                    !bugTrackerType.equals(BugTracker.Type.EMAIL)) {
                String namespace = request.getNamespace();
                String repoName = request.getRepoName();
                if (!ScanUtils.empty(namespace) && !ScanUtils.empty(request.getBranch())) {
                    emailCtx.put("message", "Successfully completed processing for "
                            .concat(namespace).concat("/").concat(repoName).concat(" - ")
                            .concat(request.getRepoUrl()));
                } else if (!ScanUtils.empty(request.getApplication())) {
                    emailCtx.put("message", "Successfully completed processing for "
                            .concat(request.getApplication()));
                }
                emailCtx.put("heading", "Scan Successfully Completed");

                if (results != null) {
                    emailCtx.put("issues", results.getXIssues());
                }
                if (results != null && !ScanUtils.empty(results.getLink())) {
                    emailCtx.put("link", results.getLink());
                }
                emailCtx.put("repo", request.getRepoUrl());
                emailCtx.put("repo_fullname", namespace.concat("/").concat(repoName));
                emailService.sendmail(request.getEmail(), "Successfully completed processing for ".concat(namespace).concat("/").concat(repoName), emailCtx, "template-demo.html");
                log.info("Successfully completed automation for repository {} under namespace {}", repoName, namespace);
            }
            processResults(request, results);
            log.info("Process completed Succesfully");
            future.complete(results);
            return future;
        }catch (Exception e){
            log.error("Error occurred while processing results {}", ExceptionUtils.getMessage(e));
            CompletableFuture<ScanResults> x = new CompletableFuture<>();
            x.completeExceptionally(e);
            return x;
        }

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
            Thread.currentThread().interrupt();
            throw new MachinaException("Interrupted Exception Occurred");
        }
    }

    void processResults(ScanRequest request, ScanResults results) throws MachinaException {
        switch (request.getBugTracker().getType()) {
            case NONE:
            case wait:
            case WAIT:
                log.info("Issue tracking is turned off");
                break;
            case JIRA:
                log.info("Processing results with JIRA issue tracking");
                if(!cxProperties.getOffline()) {
                    getCxFields(request, results);
                }
                jiraService.process(results, request);
                break;
            case GITHUBPULL:
                gitService.processPull(request, results);
                gitService.endBlockMerge(request, results.getLink());
                break;
            case GITLABCOMMIT:
                gitLabService.processCommit(request, results);
                break;
            case GITLABMERGE:
                gitLabService.processMerge(request, results);
                gitLabService.endBlockMerge(request);
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
            case ADOPULL:
                adoService.processPull(request, results);
                adoService.endBlockMerge(request);
                break;
            case EMAIL:
                if(!flowProperties.getMail().isEnabled()) {
                    Map<String, Object> emailCtx = new HashMap<>();
                    String namespace = request.getNamespace();
                    String repoName = request.getRepoName();
                    emailCtx.put("message", "Checkmarx Scan Results "
                            .concat(namespace).concat("/").concat(repoName).concat(" - ")
                            .concat(request.getRepoUrl()));
                    emailCtx.put("heading", "Scan Successfully Completed");

                    if (results != null) {
                        emailCtx.put("issues", results.getXIssues());
                    }
                    if (results != null && !ScanUtils.empty(results.getLink())) {
                        emailCtx.put("link", results.getLink());
                    }
                    emailCtx.put("repo", request.getRepoUrl());
                    emailCtx.put("repo_fullname", namespace.concat("/").concat(repoName));
                    emailService.sendmail(request.getEmail(), "Successfully completed processing for ".concat(namespace).concat("/").concat(repoName), emailCtx, "template-demo.html");
                }
                break;
            case CUSTOM:
                log.info("Issue tracking is custom bean implementation");
                issueService.process(results, request);
                break;
            default:
                log.warn("No valid bug type was provided");
        }
        log.info("####Checkmarx Scan Results Summary####");
        log.info(results.getScanSummary().toString());
        log.info("To veiw results: {}", results.getLink());
        log.info("######################################");
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

