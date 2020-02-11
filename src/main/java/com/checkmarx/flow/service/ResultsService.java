package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.Field;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.InvalidCredentialsException;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.dto.Filter;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.cx.CxProject;
import com.checkmarx.sdk.service.CxClient;
import com.checkmarx.sdk.service.CxOsaClient;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class ResultsService {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ResultsService.class);
    private final CxClient cxService;
    private final CxOsaClient osaService;
    private final JiraService jiraService;
    private final IssueService issueService;
    private final GitHubService gitService;
    private final GitLabService gitLabService;
    private final BitBucketService bbService;
    private final ADOService adoService;
    private final EmailService emailService;
    private final CxProperties cxProperties;
    private final FlowProperties flowProperties;

    public ResultsService(CxClient cxService, CxOsaClient osaService, JiraService jiraService, IssueService issueService, GitHubService gitService,
                          GitLabService gitLabService, BitBucketService bbService, ADOService adoService,
                          EmailService emailService, CxProperties cxProperties, FlowProperties flowProperties) {
        this.cxService = cxService;
        this.osaService = osaService;
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
    public CompletableFuture<ScanResults> processScanResultsAsync(ScanRequest request, Integer projectId,
                                                                  Integer scanId, String osaScanId, List<Filter> filters) throws MachinaException {
        try {
            CompletableFuture<ScanResults> future = new CompletableFuture<>();
            //TODO async these, and join and merge after
            ScanResults results = cxService.getReportContentByScanId(scanId, filters);
            if(cxProperties.getEnableOsa() && !ScanUtils.empty(osaScanId)){
                log.info("Waiting for OSA Scan results for scan id {}", osaScanId);
                results = osaService.waitForOsaScan(osaScanId, projectId, results, filters);
            }
            Map<String, Object> emailCtx = new HashMap<>();
            BugTracker.Type bugTrackerType = request.getBugTracker().getType();
            //Send email (if EMAIL was enabled and EMAIL was not main feedback option
            if (flowProperties.getMail() != null && flowProperties.getMail().isEnabled() &&
                    !bugTrackerType.equals(BugTracker.Type.NONE) &&
                    !bugTrackerType.equals(BugTracker.Type.EMAIL)) {
                String namespace = request.getNamespace();
                String repoName = request.getRepoName();
                String concat = "Successfully completed processing for "
                        .concat(namespace).concat("/").concat(repoName);
                if (!ScanUtils.empty(namespace) && !ScanUtils.empty(request.getBranch())) {
                    emailCtx.put("message", concat.concat(" - ")
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
                emailService.sendmail(request.getEmail(), concat, emailCtx, "template-demo.html");
                log.info("Successfully completed automation for repository {} under namespace {}", repoName, namespace);
            }
            processResults(request, results);
            log.info("Process completed Succesfully");
            future.complete(results);
            return future;
        }catch (Exception e){
            log.error("Error occurred while processing results.", e);
            CompletableFuture<ScanResults> x = new CompletableFuture<>();
            x.completeExceptionally(e);
            return x;
        }

    }

    void processResults(ScanRequest request, ScanResults results) throws MachinaException {
        if(!cxProperties.getOffline()) {
            getCxFields(request, results);
        }
        switch (request.getBugTracker().getType()) {
            case NONE:
            case wait:
            case WAIT:
                log.info("Issue tracking is turned off");
                break;
            case JIRA:
                log.info("Processing results with JIRA issue tracking");
                jiraService.process(results, request);
                break;
            case GITHUBPULL:
                gitService.processPull(request, results);
                gitService.endBlockMerge(request, results.getLink(), !results.getXIssues().isEmpty());
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
                adoService.endBlockMerge(request, results.getLink(), !results.getXIssues().isEmpty());
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
        if(results != null && results.getScanSummary() != null) {
            log.info("####Checkmarx Scan Results Summary####");
            log.info("Team: {}, Project: {}", request.getTeam(), request.getProject());
            log.info(results.getScanSummary().toString());
            log.info("To veiw results: {}", results.getLink());
            log.info("######################################");
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
            if(!ScanUtils.empty(results.getProjectId()) && !results.getProjectId().equals(Constants.UNKNOWN)){
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
            log.warn("Error retrieving Checkmarx Project details for {}, no custom fields will be available", results.getProjectId(), e);
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

