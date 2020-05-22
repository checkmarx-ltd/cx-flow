package com.checkmarx.flow.service;

import com.atlassian.jira.rest.client.api.RestClientException;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.Field;
import com.checkmarx.flow.dto.ScanDetails;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.report.ScanResultsReport;
import com.checkmarx.flow.exception.*;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.dto.Filter;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.cx.CxProject;
import com.checkmarx.sdk.exception.CheckmarxException;
import com.checkmarx.sdk.service.CxClient;
import com.checkmarx.sdk.service.CxOsaClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.checkmarx.sdk.config.Constants.UNKNOWN_INT;

@Service
@Slf4j
@RequiredArgsConstructor
public class ResultsService {

    public static final String COMPLETED_PROCESSING = "Successfully completed processing for ";
    public static final String MESSAGE_KEY = "message";

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

    @Async("scanRequest")
    public CompletableFuture<ScanResults> processScanResultsAsync(ScanRequest request, Integer projectId,
                                                                  Integer scanId, String osaScanId, List<Filter> filters) throws MachinaException {
        try {
            CompletableFuture<ScanResults> future = new CompletableFuture<>();
            //TODO async these, and join and merge after
            ScanResults results = cxService.getReportContentByScanId(scanId, filters);
            new ScanResultsReport(scanId, request, results).log();
            results = isOSAScanEnable(request, projectId, osaScanId, filters, results);

            sendEmailNotification(request, results);
            processResults(request, results, new ScanDetails(projectId, scanId, osaScanId));
            logScanDetails(request, projectId, results);
            future.complete(results);

            return future;
        } catch (Exception e) {

            log.error("Error occurred while processing results.", e);
            CompletableFuture<ScanResults> x = new CompletableFuture<>();
            x.completeExceptionally(e);
            return x;
        }
    }

    @Async("scanRequest")
    public CompletableFuture<ScanResults> publishCombinedResults(ScanRequest scanRequest, ScanResults scanResults) {
        try {
            CompletableFuture<ScanResults> future = new CompletableFuture<>();
            Integer projectId = Integer.parseInt(scanResults.getProjectId());

            if(projectId != UNKNOWN_INT) {
                new ScanResultsReport(scanResults.getSastScanId(), scanRequest, scanResults).log();
                sendEmailNotification(scanRequest, scanResults);
                processResults(scanRequest, scanResults, new ScanDetails(projectId, scanResults.getSastScanId(), null));
                logScanDetails(scanRequest, projectId, scanResults);
            }

            future.complete(scanResults);
            
            log.info("Finished processing the request");
            
            return future;

        } catch (Exception e) {
            log.error("Error occurred while processing results.", e);
            CompletableFuture<ScanResults> x = new CompletableFuture<>();
            x.completeExceptionally(e);
            return x;
        }
    }

    public CompletableFuture<ScanResults> cxGetResults(ScanRequest request, CxProject cxProject){
        try {
            CxProject project;

            if(cxProject == null) {
                String team = request.getTeam();
                if(ScanUtils.empty(team)){
                    //if the team is not provided, use the default
                    team = cxProperties.getTeam();
                    request.setTeam(team);
                }
                if (!team.startsWith(cxProperties.getTeamPathSeparator())) {
                    team = cxProperties.getTeamPathSeparator().concat(team);
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
                //null is passed for osaScanId as it is not applicable here and will be ignored
                return processScanResultsAsync(request, project.getId(), scanId, null, request.getFilters());
            }

        } catch (MachinaException | CheckmarxException e) {
            log.error("Error occurred while processing results for {}{}", request.getTeam(), request.getProject(), e);
            CompletableFuture<ScanResults> x = new CompletableFuture<>();
            x.completeExceptionally(e);
            return x;
        }
    }

    void processResults(ScanRequest request, ScanResults results, ScanDetails scanDetails) throws MachinaException {

        if(scanDetails == null){
            scanDetails = new ScanDetails();
        }
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
                handleJiraCase(request, results, scanDetails);
                log.info("Results Service case JIRA : request =:  {}  results = {}  scanDetails= {}", request.toString(),results.toString(),scanDetails.toString());
                break;
            case GITHUBPULL:
                gitService.processPull(request, results);
                gitService.endBlockMerge(request, results, scanDetails);
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
                adoService.endBlockMerge(request, results, scanDetails);
                break;
            case EMAIL:
                if(!flowProperties.getMail().isEnabled()) {
                    Map<String, Object> emailCtx = new HashMap<>();
                    String namespace = request.getNamespace();
                    String repoName = request.getRepoName();
                    emailCtx.put(MESSAGE_KEY, "Checkmarx Scan Results "
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
                    emailService.sendmail(request.getEmail(), COMPLETED_PROCESSING.concat(namespace).concat("/").concat(repoName), emailCtx, "template-demo.html");
                }
                break;
            case CUSTOM:
                handleCustomIssueTracker(request, results);
                break;
            default:
                log.warn("No valid bug type was provided");
        }
        if(results != null && results.getScanSummary() != null) {
            log.info("####Checkmarx Scan Results Summary####");
            log.info("Team: {}, Project: {}, Scan-Id: {}", request.getTeam(), request.getProject(), results.getAdditionalDetails().get("scanId"));
            log.info(String.format("The vulnerabilities found for the scan are: %s", String.valueOf(results.getScanSummary())));
            log.info("To view results use following link: {}", results.getLink());
            log.info("######################################");
        }
    }

    void logScanDetails(ScanRequest request, Integer projectId, ScanResults results) {
        if (log.isInfoEnabled()) {
            log.info(String.format("request : %s", String.valueOf(request)));
            log.info(String.format("results : %s", String.valueOf(results)));
            log.info(String.format("projectId : %s", String.valueOf(projectId)));
            log.info("Process completed Succesfully");
        }
    }

    void sendEmailNotification(ScanRequest request, ScanResults results) {
        Map<String, Object> emailCtx = new HashMap<>();
        BugTracker.Type bugTrackerType = request.getBugTracker().getType();
        log.info("bugTrackerType : {}", bugTrackerType);
        //Send email (if EMAIL was enabled and EMAIL was not main feedback option
        if (flowProperties.getMail() != null && flowProperties.getMail().isEnabled() &&
                !bugTrackerType.equals(BugTracker.Type.NONE) &&
                !bugTrackerType.equals(BugTracker.Type.EMAIL)) {
            String namespace = request.getNamespace();
            String repoName = request.getRepoName();
            String concat = COMPLETED_PROCESSING
                    .concat(namespace).concat("/").concat(repoName);
            if (!ScanUtils.empty(namespace) && !ScanUtils.empty(request.getBranch())) {
                emailCtx.put(MESSAGE_KEY, concat.concat(" - ")
                        .concat(request.getRepoUrl()));
            } else if (!ScanUtils.empty(request.getApplication())) {
                emailCtx.put(MESSAGE_KEY, COMPLETED_PROCESSING
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
    }

    ScanResults isOSAScanEnable(ScanRequest request, Integer projectId, String osaScanId, List<Filter> filters, ScanResults results) throws CheckmarxException {
        if(cxProperties.getEnableOsa() && !ScanUtils.empty(osaScanId)){
            log.info("Waiting for OSA Scan results for scan id {}", osaScanId);
            results = osaService.waitForOsaScan(osaScanId, projectId, results, filters);

            new ScanResultsReport(osaScanId,request, results).log();
        }
        return results;
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

    private void handleCustomIssueTracker(ScanRequest request, ScanResults results) throws MachinaException {
        try {
            log.info("Issue tracking is custom bean implementation");
            issueService.process(results, request);
        } catch (HttpClientErrorException e) {
            if (e.getRawStatusCode() == HttpStatus.UNAUTHORIZED.value()) {
                throw new MachinaRuntimeException("Token is invalid. Please make sure your custom tokens are correct.\n" + e.getMessage());
            } else {
                throw e;
            }
        }
    }

    private void handleJiraCase(ScanRequest request, ScanResults results, ScanDetails scanDetails) throws JiraClientException {
        try {
            log.info("======== Processing results with JIRA issue tracking ========");
            jiraService.process(results, request, scanDetails);
        } catch (RestClientException e) {
            handleJiraRestClientException(e);
        } catch (JiraClientException e) {
            handleJiraClientException(e);
        }
    }

    private void handleJiraClientException(JiraClientException e) throws JiraClientException {
        Map<String, ScanResults.XIssue> nonPublishedScanResultsMap = jiraService.getNonPublishedScanResults();
        if (nonPublishedScanResultsMap.size() > 0) {
            throwExceptionWhenPublishingErrorOccurred(e, nonPublishedScanResultsMap);
        } else {
            throw e;
        }
    }

    private void handleJiraRestClientException(RestClientException e) {
        if (e.getStatusCode().isPresent() && e.getStatusCode().get() == HttpStatus.NOT_FOUND.value()) {
            throw new JiraClientRunTimeException("Jira service is not accessible for URL: " + jiraService.getJiraURI() + "\n", e);
        } else if (e.getStatusCode().isPresent() && e.getStatusCode().get() == HttpStatus.FORBIDDEN.value()) {
            throw new JiraClientRunTimeException("Access is forbidden. Please check your basic auth Token \n", e);
        } else {
            Map<String, ScanResults.XIssue> nonPublishedScanResultsMap = jiraService.getNonPublishedScanResults();
            if (e.getStatusCode().isPresent() &&
                    e.getStatusCode().get() == HttpStatus.BAD_REQUEST.value() &&
                    nonPublishedScanResultsMap.size() > 0) {
                throwExceptionWhenPublishingErrorOccurred(e, nonPublishedScanResultsMap);

            } else {
                throw e;
            }
        }
    }

    private void throwExceptionWhenPublishingErrorOccurred(Exception cause, Map<String, ScanResults.XIssue> nonPublishedScanResultsMap) {
        String errorMessage = "Wasn't able to publish the following issues into JIRA:\n" +
                printNonPublishedScanResults(nonPublishedScanResultsMap) +
                "\ndue to the following reason: " + cause.getMessage();

        throw new JiraClientRunTimeException(errorMessage, cause);
    }

    private String printNonPublishedScanResults(Map<String, ScanResults.XIssue> nonPublishedScanResultsMap) {
        StringBuilder sb = new StringBuilder();

        if (nonPublishedScanResultsMap.size() > 0) {
            sb.append("=====================================================\n");
            int count = 0;

            for (Map.Entry<String, ScanResults.XIssue> currentEntry : nonPublishedScanResultsMap.entrySet()) {
                count++;
                sb.append(count).append(". ").append(currentEntry.getKey()).append("\n");
                sb.append("Severity: ").append(currentEntry.getValue().getSeverity()).append("\n");
                sb.append("Similarity id: ").append(currentEntry.getValue().getSimilarityId()).append("\n");
                sb.append("Cwe: ").append(currentEntry.getValue().getCwe()).append("\n");
                sb.append("Link: ").append(currentEntry.getValue().getLink()).append("\n").append("==============\n");
            }
        }
        sb.append("=====================================================");
        return sb.toString();
    }

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

