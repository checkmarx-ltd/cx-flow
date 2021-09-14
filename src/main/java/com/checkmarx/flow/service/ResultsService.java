package com.checkmarx.flow.service;

import com.atlassian.jira.rest.client.api.RestClientException;
import com.checkmarx.flow.constants.FlowConstants;
import com.checkmarx.flow.dto.Field;
import com.checkmarx.flow.dto.ScanDetails;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.report.AnalyticsReport;
import com.checkmarx.flow.dto.report.ScanResultsReport;
import com.checkmarx.flow.exception.InvalidCredentialsException;
import com.checkmarx.flow.exception.JiraClientException;
import com.checkmarx.flow.exception.JiraClientRunTimeException;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.exception.MachinaRuntimeException;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.dto.sast.Filter;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.cx.CxProject;
import com.checkmarx.sdk.dto.filtering.EngineFilterConfiguration;
import com.checkmarx.sdk.dto.filtering.FilterConfiguration;
import com.checkmarx.sdk.exception.CheckmarxException;
import com.checkmarx.sdk.service.scanner.CxOsaClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.checkmarx.sdk.config.Constants.UNKNOWN_INT;

@Service
@Slf4j
@RequiredArgsConstructor
public class ResultsService {

    private final CxScannerService cxScannerService;
    private final CxOsaClient osaService;
    private final JiraService jiraService;
    private final IssueService issueService;
    private final GitHubService gitService;
    private final GitLabService gitLabService;
    private final BitBucketService bbService;
    private final ADOService adoService;
    private final EmailService emailService;

    @Async("scanRequest")
    public CompletableFuture<ScanResults> processScanResultsAsync(ScanRequest request, Integer projectId,
                                                                  Integer scanId, String osaScanId, FilterConfiguration filterConfiguration) throws MachinaException {
        try {

            CompletableFuture<ScanResults> future = new CompletableFuture<>();
            //TODO async these, and join and merge after
            ScanResults results = cxScannerService.getScannerClient().getReportContentByScanId(scanId, filterConfiguration);
            logGetResultsJsonLogger(request, scanId, results);
            results = getOSAScan(request, projectId, osaScanId, filterConfiguration, results);

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

    private void logGetResultsJsonLogger(ScanRequest request, Integer scanId, ScanResults results) {
        //SAST results
        if (results.getScanSummary() != null) {
            new ScanResultsReport(scanId, request, results).log();
        }
        if (results.getScaResults() != null) {
            new ScanResultsReport(results.getScaResults().getScanId(), request, results, AnalyticsReport.SCA).log();
        }
    }

    @Async("scanRequest")
    public CompletableFuture<ScanResults> publishCombinedResults(ScanRequest scanRequest, ScanResults scanResults) {
        try {
            CompletableFuture<ScanResults> future = new CompletableFuture<>();

            if (scanResults.getProjectId() != null) {
                Integer projectId = Integer.parseInt(scanResults.getProjectId());

                if (projectId != UNKNOWN_INT) {
                    logGetResultsJsonLogger(scanRequest, scanResults.getSastScanId(), scanResults);
                    sendEmailNotification(scanRequest, scanResults);
                    processResults(scanRequest, scanResults, new ScanDetails(projectId, scanResults.getSastScanId(), null));
                    logScanDetails(scanRequest, projectId, scanResults);
                }
                else{
                    processResults(scanRequest, scanResults, new ScanDetails(null, scanResults.getSastScanId(), null));
                }
            } else {
                processResults(scanRequest, scanResults, new ScanDetails(null, scanResults.getSastScanId(), null));
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



    public void processResults(ScanRequest request, ScanResults results, ScanDetails scanDetails) throws MachinaException {

        scanDetails = Optional.ofNullable(scanDetails).orElseGet(ScanDetails::new);
        if (Boolean.FALSE.equals(cxScannerService.getProperties().getOffline())) {
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
                log.info("Results Service case JIRA : request =:  {}  results = {}  scanDetails= {}", request.toString(), results.toString(), scanDetails.toString());
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
                bbService.processServerMerge(request, results, scanDetails);
                bbService.setBuildEndStatus(request, results, scanDetails);
                break;
            case ADOPULL:
                adoService.processPull(request, results);
                adoService.endBlockMerge(request, results, scanDetails);
                break;
            case EMAIL:
                emailService.handleEmailBugTracker(request, results);
                break;
            case CUSTOM:
                handleCustomIssueTracker(request, results);
                break;
            default:
                log.warn("No valid bug type was provided");
        }
        if (results != null && results.getScanSummary() != null) {
            log.info("####Checkmarx Scan Results Summary####");
            log.info("Team: {}, Project: {}, Scan-Id: {}", request.getTeam(), request.getProject(), results.getAdditionalDetails().get("scanId"));
            log.info(String.format("The vulnerabilities found for the scan are: %s", results.getScanSummary()));
            log.info("To view results use following link: {}", results.getLink());
            log.info("######################################");
        }
    }

    void logScanDetails(ScanRequest request, Integer projectId, ScanResults results) {
        if (log.isInfoEnabled()) {
            log.info(String.format("request : %s", request));
            log.info(String.format("results : %s", results));
            log.info(String.format("projectId : %s", projectId));
            log.info("Process completed Successfully");
        }
    }

    void sendEmailNotification(ScanRequest request, ScanResults results) {
        emailService.sendScanCompletedEmail(request, results);
    }

    ScanResults getOSAScan(ScanRequest request, Integer projectId, String osaScanId, FilterConfiguration filter, ScanResults results) throws CheckmarxException {
        if (Boolean.TRUE.equals(cxScannerService.getProperties().getEnableOsa()) && !ScanUtils.empty(osaScanId)) {
            log.info("Waiting for OSA Scan results for scan id {}", osaScanId);

            List<Filter> filters = Optional.ofNullable(filter.getScaFilters())
                    .map(EngineFilterConfiguration::getSimpleFilters)
                    .orElse(null);

            results = osaService.waitForOsaScan(osaScanId, projectId, results, filters);

            new ScanResultsReport(osaScanId, request, results).log();
        }
        return results;
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

    private void getCxFields(ScanRequest request, ScanResults results) throws MachinaException {
        try {
            /*Are cx fields required?*/
            if (!requiresCxCustomFields(request.getBugTracker().getFields())) {
                return;
            }
            /*if so, then get them and add them to the request object*/
            if (!ScanUtils.empty(results.getProjectId()) && !results.getProjectId().equals(Constants.UNKNOWN)) {
                CxProject project = cxScannerService.getScannerClient().getProject(Integer.parseInt(results.getProjectId()));
                Map<String, String> fields = new HashMap<>();
                for (CxProject.CustomField field : project.getCustomFields()) {
                    if (!ScanUtils.empty(field.getName()) && !ScanUtils.empty(field.getValue())) {
                        fields.put(field.getName(), field.getValue());
                    }
                }
                if (!fields.isEmpty()) {
                    request.setCxFields(fields);
                    if (!ScanUtils.empty(cxScannerService.getProperties().getJiraProjectField())) {
                        String jiraProject = fields.get(cxScannerService.getProperties().getJiraProjectField());
                        if (!ScanUtils.empty(jiraProject)) {
                            request.getBugTracker().setProjectKey(jiraProject);
                        }
                    }
                    if (!ScanUtils.empty(cxScannerService.getProperties().getJiraIssuetypeField())) {
                        String jiraIssuetype = fields.get(cxScannerService.getProperties().getJiraIssuetypeField());
                        if (!ScanUtils.empty(jiraIssuetype)) {
                            request.getBugTracker().setIssueType(jiraIssuetype);
                        }
                    }
                }
            }
        } catch (InvalidCredentialsException e) {
            log.warn("Error retrieving Checkmarx Project details for {}, no custom fields will be available", results.getProjectId(), e);
            throw new MachinaException("Error logging into Checkmarx");
        }
    }

    /**
     * Check if even 1 cx field is required, which means Checkmarx project needs to be retrieved
     */
    private boolean requiresCxCustomFields(List<Field> fields) {
        if (fields == null) {
            return false;
        }
        for (Field f : fields) {
            if (f.getType().equals(FlowConstants.MAIN_MDC_ENTRY)) {
                return true;
            }
        }
        return false;
    }

    public boolean filteredSastIssuesPresent(ScanResults results) {
        if(results == null || results.getAdditionalDetails()==null) {
            // assuming no bugTracker or SCA results only
            return false;
        }

        Map<String, Integer> findingCountPerSeverity = (Map<String, Integer>) results.getAdditionalDetails().get(Constants.SUMMARY_KEY);
        return findingCountPerSeverity == null || !findingCountPerSeverity.isEmpty();
    }

    /**
     * Join any number of results together
     * @param results combined results object
     */
    public ScanResults joinResults(ScanResults... results){
        ScanResults scanResults = null;
        for(ScanResults r: results){
            if(r != null){
                if(scanResults != null){
                    scanResults.mergeWith(r);
                }
                else{
                    scanResults = r;
                }
            }
        }
        return scanResults;
    }

}
