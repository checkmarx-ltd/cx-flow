/** Code Review Notes (remove when review complete)
 * See why markdown isn't showing up correctly in Rally??
 * Follow Azure and Jira for tag examples, you won't need to put CX title
 *     - Map the labels to the tags in Rally
 *
 * - try this command line tests
 *
 * - I force the issue to state 'Open' when its created, is that OK? The default is 'Submitted'
 *     - Give them an option to decide if submitted or open on create (see azure)
 * - Rally support a state 'Fixed' but I'm only checking for 'Closed', is that OK?
 * - I figured out I didn't need a separate Update and Create issue for generating
 * - JSON objects
 * - The closest thing to comments are dicussions, so that where I put the defect comments?
 * - I have update return the Issue passed into it, not sure why. The Rally API doesn't return anything other that
 *   a 200 when the update is successful.
 * - getIssue() is called when an update fail, why? Is it kludge?
 */
package com.checkmarx.flow.custom;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.RallyProperties;
import com.checkmarx.flow.dto.Issue;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.rally.CreateResult;
import com.checkmarx.flow.dto.rally.CreateResultAction;
import com.checkmarx.flow.dto.rally.DefectQuery;
import com.checkmarx.flow.dto.rally.QueryResult;
import com.checkmarx.flow.dto.rally.Result;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.exception.MachinaRuntimeException;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.dto.ScanResults;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONArray;
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
    private static final String CREATE_DISCUSSION = "/conversationpost/create";
    private static final String CREATE_TAG = "/tag/create";

    //
    /// Tracks Required Rally Tag ID's
    //
    private String rallyAppTag = "";

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
                ScanUtils.empty(request.getBranch())) {
            throw new MachinaException("Namespace / RepoName / Branch are required");
        }
        if(ScanUtils.empty(properties.getApiUrl())) {
            throw new MachinaException("Rally API Url must be provided in property config");
        }
        createRallyTags();
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

            // TODO: remove this after review, I was just testing getIssue() here.
            // TODO: replace this with code to read the label information
            getIssue(i.getUrl());
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
        // TODO: decide if I need use tags
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
        List<String> labels = new ArrayList<>();
        //rallyDefect.get
        // TODO: decide if I need use tags
        /*
        for(LabelsItem l: issue.getLabels()){
            labels.add(l.getName());
        }
        */
        i.setLabels(labels);
        return i;
    }

    /**
     * Retrieve DTO representation of Rally defect
     *
     * @param issueUrl URL for specific Rally defect
     * @return Rally Issue
     */
    private Issue getIssue(String issueUrl) {
        log.info("Executing getIssue Rally API call");
        HttpEntity httpEntity = new HttpEntity<>(createAuthHeaders());
        ResponseEntity<DefectQuery> response = restTemplate.exchange(
                issueUrl,
                HttpMethod.GET,
                httpEntity,
                DefectQuery.class);
        //return mapToIssue(response.getBody());
        return null;
    }

    /**
     * Add a comment to an existing Rally Issue (technically adding a 'discussion')
     *
     * @param issueUrl URL for specific Rally Issue
     * @param comment  Comment to append to the Rally Issue
     */
    private void addComment(String issueUrl, String comment) {
        log.debug("Executing add comment Rally API call");
        String defID = issueUrl.substring(issueUrl.lastIndexOf("/") + 1);
        String json = getJSONComment(comment, defID);
        HttpEntity httpEntity = new HttpEntity(json, createAuthHeaders());
        restTemplate.exchange(
                properties.getApiUrl().concat(CREATE_DISCUSSION),
                HttpMethod.POST,
                httpEntity,
                String.class);
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
            // TODO: Map Rally tag to this new issue
            //addRallyTags(m);
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
        JSONObject cxFlowTag  = new JSONObject();
        //cxFlowTag.put("_ref", "/tag/367406838020");
        cxFlowTag.put("_ref", this.rallyAppTag);
        JSONArray tagsList = new JSONArray();
        tagsList.put(cxFlowTag);
        String fileUrl = ScanUtils.getFileUrl(request, resultIssue.getFilename());
        String body = ScanUtils.getMDBody(resultIssue, request.getBranch(), fileUrl, flowProperties);
        String title = getXIssueKey(resultIssue, request);
        try {
            requestBody.put("Name", title);
            requestBody.put("Workspace", properties.getRallyWorkspaceId());
            requestBody.put("Project", properties.getRallyProjectId());
            requestBody.put("State", TRANSITION_OPEN);
            requestBody.put("Description", body);
            requestBody.put("Tags", tagsList);
            createBody.put("Defect", requestBody);
        } catch (JSONException e) {
            log.error("Error creating JSON Issue Object - JSON Object will be empty");
        }
        return createBody.toString();
    }

    private void createRallyTags() {
        log.info("Creating required Rally tags");
        this.rallyAppTag = createRallyTag("New Rally Tag From CxFlow");
    }

    /**
     * Creates or locates existing Rally tag and return it's reference.
     *
     * @param name The name of the Rally Tag to create or locate
     * @return String containing the reference, this should never be null!
     */
    private String createRallyTag(String name) {
        log.info("Creating Rally Tag: ".concat(name));
        HttpEntity httpEntity = new HttpEntity(getJSONCreateTag(name), createAuthHeaders());
        CreateResultAction cra;
        ResponseEntity<CreateResultAction> response;
        response = restTemplate.exchange(
                properties.getApiUrl().concat(CREATE_TAG),
                HttpMethod.POST,
                httpEntity,
                CreateResultAction.class);
        cra = response.getBody();
        Map<String, Object> m = (Map<String, Object>)cra.getAdditionalProperties().get("CreateResult");
        m = (Map<String, Object>)m.get("Object");
        return (String)m.get("_ref");
    }

    /**
     * Creates JSON object to create new Rally Tag.
     *
     * @return JSON Object to create a new tag
     */
    private String getJSONCreateTag(String name) {
        JSONObject requestBody = new JSONObject();
        JSONObject createBody = new JSONObject();
        try {
            requestBody.put("Name", name);
            createBody.put("Tag", requestBody);
        } catch (JSONException e) {
            log.error("Error creating JSON Create Tag object - JSON object will be empty");
        }
        return createBody.toString();
    }

    /**
     * Add required Rally tags to the current issue.
     *
     * @param rallyDefect contains the Rally defect object
     */
    private void addRallyTags(Map<String, Object> rallyDefect){
        if(rallyDefect == null){
            return;
        }
        String ref = (String)rallyDefect.get("Description");
        log.info("Mapping Rally tags to new issue");
        HttpEntity httpEntity = new HttpEntity(getJSONAddTags(), createAuthHeaders());
        CreateResult cra;
        ResponseEntity<CreateResult> response;
        response = restTemplate.exchange(
                ref,
                HttpMethod.POST,
                httpEntity,
                CreateResult.class);
        cra = response.getBody();
        Map<String, Object> m = (Map<String, Object>)cra.getAdditionalProperties().get("CreateResult");
    }

    /**
     * Creates JSON object to add tags to Rally Issue
     *
     * @return JSON Object to add the tags
     */
    private String getJSONAddTags() {
        JSONObject requestBody = new JSONObject();
        JSONObject createBody = new JSONObject();
        try {
            //requestBody.put("Name", name);
            createBody.put("Tag", requestBody);
        } catch (JSONException e) {
            log.error("Error creating JSON Create Tag object - JSON object will be empty");
        }
        return createBody.toString();
    }

    /**
     * Closes an open issue.
     *
     * @param issue
     * @param request
     * @throws MachinaException
     */
    @Override
    public void closeIssue(Issue issue, ScanRequest request) throws MachinaException {
        log.info("Executing closeIssue Rally API call");
        String json = getJSONCloseIssue();
        HttpEntity httpEntity = new HttpEntity(json, createAuthHeaders());
        restTemplate.exchange(
                issue.getUrl(),
                HttpMethod.POST,
                httpEntity,
                Issue.class);
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
            this.addComment(issue.getUrl(),"Issue still exists. ");
            return issue;
        } catch (HttpClientErrorException e) {
            log.error("Error updating issue.  This is likely due to the fact that another user has closed this issue. Adding comment");
            if(e.getStatusCode().equals(HttpStatus.GONE)) {
                throw new MachinaRuntimeException();
            }
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
     * Create JSON http request body for adding a comment/discussion to an Issue in Rally
     *
     * @param comment Comment to append to an issue
     * @return String representation of the add comment request
     */
    private String getJSONComment(String comment, String issueID) {
        JSONObject requestBody = new JSONObject();
        JSONObject createBody = new JSONObject();
        try {
            requestBody.put("Artifact", "/defect/".concat(issueID));
            requestBody.put("Text", comment);
            createBody.put("ConversationPost", requestBody);
        } catch (JSONException e) {
            log.error("Error creating JSON Comment Object - JSON object will be empty");
        }
        return createBody.toString();
    }

    /**
     * @return JSON Object for close issue request
     */
    private String getJSONCloseIssue() {
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("state", TRANSITION_CLOSE);
        } catch (JSONException e) {
            log.error("Error creating JSON Close Issue Object - JSON object will be empty");
        }
        return requestBody.toString();
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