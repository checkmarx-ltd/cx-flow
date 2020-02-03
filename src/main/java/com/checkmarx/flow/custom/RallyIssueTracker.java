/** Code Review Notes (remove when review complete)
 * - I force the issue to state 'Open' when its created, is that OK? The default is 'Submitted'
 * - Rally support a state 'Fixed' but I'm only checking for 'Closed', is that OK?
 * - I figured out I didn't need a separate Update and Create issue for generating
 * - JSON objects
 */
package com.checkmarx.flow.custom;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.RallyProperties;
import com.checkmarx.flow.dto.Issue;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.rally.CreateResultAction;
import com.checkmarx.flow.dto.rally.QueryResult;
import com.checkmarx.flow.dto.rally.Result;
import com.checkmarx.flow.dto.rally.RallyQuery;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.exception.MachinaRuntimeException;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.dto.ScanResults;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service("Rally")
public class RallyIssueTracker implements IssueTracker {

    private static final String TRANSITION_CLOSE = "Closed";
    private static final String TRANSITION_OPEN = "Open";
    private static final String ISSUES_PER_PAGE = "100";
    private static final Logger log = LoggerFactory.getLogger(RallyIssueTracker.class);

    private final RestTemplate restTemplate;
    private final RallyProperties properties;
    private final FlowProperties flowProperties;

    //
    /// RestAPI Endpoints
    //
    private static final String GET_ISSUES = "/defect?query=&fetch=true&start={page_index}&pagesize={issues_per_page}";
    private static final String CREATE_ISSUE = "/defect/create";

    public RallyIssueTracker(@Qualifier("flowRestTemplate") RestTemplate restTemplate, RallyProperties properties, FlowProperties flowProperties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.flowProperties = flowProperties;
    }

    @Override
    public void init(ScanRequest request, ScanResults results) throws MachinaException {
        log.info("Initializing Rally processing");
        if(ScanUtils.empty(request.getNamespace()) ||
                ScanUtils.empty(request.getRepoName()) ||
                ScanUtils.empty(request.getBranch())){
            throw new MachinaException("Namespace / RepoName / Branch are required");
        }
        if(ScanUtils.empty(properties.getApiUrl())){
            throw new MachinaException("Rally API Url must be provided in property config");
        }
    }

    /**
     * Get all issues for a Rally repository
     *
     * @return List of Rally Issues
     * @ full name (owner/repo format)
     */
    @Override
    public List<Issue> getIssues(ScanRequest request) {
        log.info("Executing getIssues Rally API call");
        List<Issue> issues = new ArrayList<>();
        HttpEntity httpEntity = new HttpEntity(createAuthHeaders());
        try {
            int pageIndex = 0;
            QueryResult rallyQuery;
            ResponseEntity<QueryResult> response;
            //
            /// Read the first list of defects from Rally, it will contain the totalResultCount we can use
            /// to figure out how many more pages of data needs to be pulled.
            //
            response = restTemplate.exchange(
                    properties.getApiUrl().concat(GET_ISSUES),
                    HttpMethod.GET,
                    httpEntity,
                    QueryResult.class,
                    pageIndex,
                    ISSUES_PER_PAGE
            );
            rallyQuery = response.getBody();
            //
            /// Now decode the CxFlow defects and continue reading lists of defects until we've found the
            // totalResultCount
            //
            int resultsFound = 0;
            while(resultsFound < rallyQuery.getQueryResult().getTotalResultCount()) {
                resultsFound += rallyQuery.getQueryResult().getPageSize();
                readCxFlowIssues(rallyQuery, request, issues);
                if(resultsFound < rallyQuery.getQueryResult().getTotalResultCount()) {
                    pageIndex++;
                    response = restTemplate.exchange(
                            properties.getApiUrl().concat(GET_ISSUES),
                            HttpMethod.GET,
                            httpEntity,
                            QueryResult.class,
                            pageIndex,
                            ISSUES_PER_PAGE
                    );
                    rallyQuery = response.getBody();
                }
            }
            return issues;
        } catch(RestClientException e) {
            return new ArrayList<>();
        }
    }

    /**
     * Examin Rally issue and add CxFlow issues to to issues list.
     *
     * @param rallyQuery contains RallyQuery with list of defects
     * @param request CxFlow ScanRequest object with required scan info
     * @param issues will contain list of found issues
     */
    private void readCxFlowIssues(QueryResult rallyQuery,
                                ScanRequest request,
                                List<Issue> issues) {
        for(Result issue: rallyQuery.getQueryResult().getResults()){
            Issue i = mapToIssue(issue);
            if(i != null && i.getTitle().startsWith(request.getProduct().getProduct())){
                issues.add(i);
            }
        }
    }

    /**
     * Converts a Rally defect result to a CxFlow issue.
     *
     * @param rallyDefect contains the Rally defect object
     * @return CxFlow issue with rally defect data encoded into it
     */
    private Issue mapToIssue(Result rallyDefect){
        if(rallyDefect == null){
            return null;
        }
        Issue i = new Issue();
        i.setBody(rallyDefect.getDescription());
        i.setTitle(rallyDefect.getRefObjectName());
        i.setId(String.valueOf(rallyDefect.getRefObjectUUID()));
        i.setUrl(rallyDefect.getRef());
        i.setState(rallyDefect.getState());
        List<String> labels = new ArrayList<>();
        // TODO: decide if I can just use tags to track the issues
        /*
        for(LabelsItem l: issue.getLabels()){
            labels.add(l.getName());
        }
        */
        i.setLabels(labels);
        return i;
    }

    /**
     * Converts a Rally defect represented as a HashMap to a CxFlow issue.
     *
     * @param rallyDefect contains the Rally defect object
     * @return CxFlow issue with rally defect data encoded into it
     */
    private Issue mapHashManToIssue(Map<String, Object> rallyDefect){
        if(rallyDefect == null){
            return null;
        }
        Issue i = new Issue();
        i.setBody((String)rallyDefect.get("Description"));
        i.setTitle((String)rallyDefect.get("_refObjectName"));
        i.setId((String)rallyDefect.get("_refObjectUUID"));
        i.setUrl((String)rallyDefect.get("_ref"));
        i.setState((String)rallyDefect.get("State"));
        //List<String> labels = new ArrayList<>();
        // TODO: decide if I can just use tags to track the issues
        /*
        for(LabelsItem l: issue.getLabels()){
            labels.add(l.getName());
        }
        */
        //i.setLabels(labels);
        return i;
    }

    /**
     * Retrieve DTO representation of Rally defect
     *
     * @param issueUrl URL for specific Rally defect
     * @return Rally Issue
     */
    /* TODO: Update for Rally */
    private Issue getIssue(String issueUrl) {
        log.info("Executing getIssue Rally API call");
        HttpEntity httpEntity = new HttpEntity<>(createAuthHeaders());
        ResponseEntity<com.checkmarx.flow.dto.rally.Issue> response =
                restTemplate.exchange(issueUrl, HttpMethod.GET, httpEntity, com.checkmarx.flow.dto.rally.Issue.class);

        // TODO fix this for Rally Support
        return null; //mapToIssue(response.getBody());
    }

    /**
     * Add a comment to an existing Rally Issue
     *
     * @param issueUrl URL for specific Rally Issue
     * @param comment  Comment to append to the Rally Issue
     */
    /* TODO: Update for Rally */
    private void addComment(String issueUrl, String comment) {
        log.debug("Executing add comment GitHub API call");
        HttpEntity<String> httpEntity = new HttpEntity<>(getJSONComment(comment).toString(), createAuthHeaders());
        restTemplate.exchange(issueUrl.concat("/comments"), HttpMethod.POST, httpEntity, String.class);
    }

    /**
     * Creates new Rally defect.
     *
     * @param resultIssue
     * @param request
     * @return
     * @throws MachinaException
     */
    @Override
    public Issue createIssue(ScanResults.XIssue resultIssue, ScanRequest request) throws MachinaException {
        log.debug("Executing createIssue Rally API call");
        try {
            String json = getJSONCreateIssue(resultIssue, request);
            HttpEntity httpEntity = new HttpEntity(json, createAuthHeaders());
            CreateResultAction cra;
            ResponseEntity<CreateResultAction> response;
            response = restTemplate.exchange(
                    properties.getApiUrl().concat(CREATE_ISSUE),
                    HttpMethod.POST,
                    httpEntity,
                    CreateResultAction.class);
            cra = response.getBody();
            Map<String, Object> m = (Map<String, Object>)cra.getAdditionalProperties().get("CreateResult");
            m = (Map<String, Object>)m.get("Object");
            return mapHashManToIssue(m);
        } catch (HttpClientErrorException e) {
            log.error("Error occurred while creating Rally Issue");
            log.error(ExceptionUtils.getStackTrace(e));
            throw new MachinaRuntimeException();
        }
    }

    /**
     * Create JSON http request body for an create/update Issue POST request to GitHub
     *
     * @return JSON Object of create issue request
     */
    private String getJSONCreateIssue(ScanResults.XIssue resultIssue, ScanRequest request) {
        JSONObject requestBody = new JSONObject();
        JSONObject createBody = new JSONObject();
        String fileUrl = ScanUtils.getFileUrl(request, resultIssue.getFilename());
        String body = ScanUtils.getMDBody(resultIssue, request.getBranch(), fileUrl, flowProperties);
        String title = getXIssueKey(resultIssue, request);
        try {
            requestBody.put("Name", title);
            requestBody.put("Workspace", properties.getRallyWorkspaceId());
            requestBody.put("Project", properties.getRallyProjectId());
            requestBody.put("State", TRANSITION_OPEN);
            requestBody.put("Description", body);
            createBody.put("Defect", requestBody);
        } catch (JSONException e) {
            log.error("Error creating JSON Issue Object - JSON Object will be empty");
        }
        return createBody.toString();
    }

    /**
     *
     * @param issue
     * @param request
     * @throws MachinaException
     */
    /* TODO: Update for Rally */
    @Override
    public void closeIssue(Issue issue, ScanRequest request) throws MachinaException {
        log.info("Executing closeIssue GitHub API call");
        HttpEntity httpEntity = new HttpEntity<>(getJSONCloseIssue().toString(), createAuthHeaders());
        restTemplate.exchange(issue.getUrl(), HttpMethod.POST, httpEntity, Issue.class);
    }

    /**
     *
     * @param issue
     * @param resultIssue
     * @param request
     * @return
     * @throws MachinaException
     */
    @Override
    public Issue updateIssue(Issue issue, ScanResults.XIssue resultIssue, ScanRequest request) throws MachinaException {
        log.info("Executing updateIssue Rally API call");
        String json = getJSONCreateIssue(resultIssue, request);
        HttpEntity httpEntity = new HttpEntity<>(json, createAuthHeaders());
        ResponseEntity<com.checkmarx.flow.dto.rally.Issue> response;
        try {
            response = restTemplate.exchange(
                    issue.getUrl(),
                    HttpMethod.POST,
                    httpEntity,
                    com.checkmarx.flow.dto.rally.Issue.class);
            // TODO: fix getUrl() so addComment works.
            //this.addComment(issue.getUrl(),"Issue still exists. ");
            // TODO: fix this for Rally support
            //rallyQuery.getQueryResult().getResults()
            return null; //mapToIssue(response.getBody());
        } catch (HttpClientErrorException e) {
            log.error("Error updating issue.  This is likely due to the fact that another user has closed this issue. Adding comment");
            // throw new MachinaRuntimeException();
            // TODO: do des this work correctly?
            this.addComment(issue.getUrl(), "This issue still exists.  Please add label 'false-positive' to remove from scope of SAST results");
        }
        return this.getIssue(issue.getUrl());
    }

    @Override
    public String getFalsePositiveLabel() throws MachinaException {
        return properties.getFalsePositiveLabel();
    }

    @Override
    public String getIssueKey(Issue issue, ScanRequest request) {
        return issue.getTitle();
    }

    @Override
    public String getXIssueKey(ScanResults.XIssue issue, ScanRequest request) {
        if(flowProperties.isTrackApplicationOnly() || ScanUtils.empty(request.getBranch())){
            return String.format(ScanUtils.ISSUE_KEY_2, request.getProduct().getProduct(), issue.getVulnerability(), issue.getFilename());
        }
        else {
            return String.format(ScanUtils.ISSUE_KEY, request.getProduct().getProduct(), issue.getVulnerability(), issue.getFilename(), request.getBranch());
        }
    }

    @Override
    public boolean isIssueClosed(Issue issue, ScanRequest request) {
        if(issue.getState() == null){
            return false;
        }
        return issue.getState().equals(TRANSITION_CLOSE);
    }

    @Override
    public boolean isIssueOpened(Issue issue, ScanRequest request) {
        if(issue.getState() == null){
            return true;
        }
        return issue.getState().equals(TRANSITION_OPEN);
    }

    /**
     * Called after all defects have been processed.
     *
     * @param request
     * @param results
     * @throws MachinaException
     */
    @Override
    public void complete(ScanRequest request, ScanResults results) throws MachinaException {
        log.info("Finalizing Rally Defect Processing");
    }

    /**
     * Create JSON http request body for adding a comment to an Issue in GitHub
     *
     * @param comment Comment to append to an issue
     * @return JSON Object for comment request
     */
    // TODO: finish Rally comment
    private JSONObject getJSONComment(String comment) {
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("body", comment);
        } catch (JSONException e) {
            log.error("Error creating JSON Comment Object - JSON object will be empty");
        }
        return requestBody;
    }

    /**
     * @return JSON Object for close issue request
     */
    private JSONObject getJSONCloseIssue() {
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("state", TRANSITION_CLOSE);
        } catch (JSONException e) {
            log.error("Error creating JSON Close Issue Object - JSON object will be empty");
        }
        return requestBody;
    }

    /**
     * @return Header consisting of API token used for authentication
     */
    private HttpHeaders createAuthHeaders() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("ZSESSIONID", properties.getToken());
        return httpHeaders;
    }
}