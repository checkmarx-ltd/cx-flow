package com.checkmarx.flow.custom;

import com.checkmarx.flow.config.ADOProperties;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.Issue;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.azure.CreateWorkItemAttr;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.dto.ScanResults;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Service("Azure")
public class ADOIssueTracker implements IssueTracker {

    private static final String STATE_FIELD = "System.State";
    private static final String TITLE_FIELD = "System.Title";
    private static final String TAGS_FIELD = "System.Tags";
    private static final String AREA_PATH_FIELD = "System.AreaPath";
    private static final String FIELD_PREFIX="System.";
    private static final String PROPOSED_STATE="Proposed";
    private static final String ISSUE_BODY = "<b>%s</b> issue exists @ <b>%s</b> in branch <b>%s</b>";
    public static final String CRLF = "<div><br></div>";
	private static final String ADO_PROJECT="alt-project";
    private static final String WORKITEMS="%s{p}/_apis/wit/wiql?api-version=%s";
    private static final String CREATEWORKITEMS="%s{p}/_apis/wit/workitems/$%s?api-version=%s";
    private static final String WORKITEMS_CLI="%s%s/{p}/_apis/wit/wiql?api-version=%s";
    private static final String CREATEWORKITEMS_CLI="%s%s/{p}/_apis/wit/workitems/$%s?api-version=%s";
    private static final String WIQ_REPO_BRANCH = "Select [System.Id], [System.Title], " +
            "[System.State], [System.State], [System.WorkItemType] From WorkItems Where " +
            "[System.TeamProject] = @project AND [Tags] Contains '%s' AND [Tags] Contains '%s:%s'" +
            "AND [Tags] Contains '%s:%s' AND [Tags] Contains '%s:%s'";
    private static final String WIQ_APP = "Select [System.Id], [System.Title], " +
            "[System.State], [System.State], [System.WorkItemType] From WorkItems Where " +
            "[System.TeamProject] = @project AND [Tags] Contains '%s' AND [Tags] Contains '%s:%s'";
    private static final Logger log = LoggerFactory.getLogger(ADOIssueTracker.class);

    private final RestTemplate restTemplate;
    private final ADOProperties properties;
    private final FlowProperties flowProperties;


    public ADOIssueTracker(@Qualifier("flowRestTemplate") RestTemplate restTemplate, ADOProperties properties, FlowProperties flowProperties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.flowProperties = flowProperties;
    }

    @Override
    public void init(ScanRequest request, ScanResults results) throws MachinaException {
        log.info("Initializing Azure processing");
        String issueType = request.getAdditionalMetadata(Constants.ADO_ISSUE_KEY);
        String issueBody = request.getAdditionalMetadata(Constants.ADO_ISSUE_BODY_KEY);
        String openedState = request.getAdditionalMetadata(Constants.ADO_OPENED_STATE_KEY);
        String closedState = request.getAdditionalMetadata(Constants.ADO_CLOSED_STATE_KEY);

        if(ScanUtils.empty(issueType)){
            issueType = properties.getIssueType();
            request.putAdditionalMetadata(Constants.ADO_ISSUE_KEY, issueType);
        }
        if(ScanUtils.empty(issueBody)){
            issueBody = properties.getIssueBody();
            request.putAdditionalMetadata(Constants.ADO_ISSUE_BODY_KEY, issueBody);
        }
        if(ScanUtils.empty(openedState)){
            openedState = properties.getOpenStatus();
            request.putAdditionalMetadata(Constants.ADO_OPENED_STATE_KEY, openedState);
        }
        if(ScanUtils.empty(closedState)){
            closedState = properties.getClosedStatus();
            request.putAdditionalMetadata(Constants.ADO_CLOSED_STATE_KEY, closedState);
        }
        if(ScanUtils.empty(request.getNamespace()) ||
                ScanUtils.empty(request.getRepoName()) ||
                ScanUtils.empty(request.getBranch())){
            throw new MachinaException("Namespace / RepoName / Branch are required");
        }

        if(ScanUtils.empty(request.getAdditionalMetadata(Constants.ADO_BASE_URL_KEY))){
            if(ScanUtils.empty(properties.getUrl())) {
                throw new MachinaException("Azure API Url must be provided in property config");
            }
            else{
                if(!properties.getUrl().endsWith("/")){
                    properties.setUrl(properties.getUrl().concat("/"));
                }
                request.putAdditionalMetadata(Constants.ADO_BASE_URL_KEY, properties.getUrl());
            }
        }
    }

    /**
     * Get all issues for a Azure repository
     *
     * @return List of Azure Issues
     * @ full name (owner/repo format)
     */
    @Override
    public List<Issue> getIssues(ScanRequest request) throws MachinaException {
        log.info("Executing getIssues Azure API call");
        String baseUrl = request.getAdditionalMetadata(Constants.ADO_BASE_URL_KEY);
        List<Issue> issues = new ArrayList<>();
        String adoProject = request.getAltProject();
        if (ScanUtils.empty(adoProject) && request.getCxFields() != null) {
            adoProject = request.getCxFields().get(ADO_PROJECT); // get from custom fields if available
        }
        String endpoint;
        if(!ScanUtils.empty(adoProject)) { //driven by command line
            endpoint = String.format(WORKITEMS_CLI, baseUrl, adoProject, properties.getApiVersion());
        }
        else { //driven by WebHook
            endpoint = String.format(WORKITEMS, baseUrl, properties.getApiVersion());
        }
        log.debug(endpoint);

        String issueBody = request.getAdditionalMetadata(Constants.ADO_ISSUE_BODY_KEY);
        String wiq;
        /*Namespace/Repo/Branch provided*/
        if(!flowProperties.isTrackApplicationOnly() &&
                !ScanUtils.empty(request.getNamespace()) &&
                !ScanUtils.empty(request.getRepoName()) &&
                !ScanUtils.empty(request.getBranch())) {
            wiq = String.format(WIQ_REPO_BRANCH,
                    request.getProduct().getProduct(),
                    properties.getOwnerTagPrefix(),
                    request.getNamespace(),
                    properties.getRepoTagPrefix(),
                    request.getRepoName(),
                    properties.getBranchLabelPrefix(),
                    request.getBranch()
            );
        }/*Only application provided*/
        else if(!ScanUtils.empty(request.getApplication())){
            wiq = String.format(WIQ_APP,
                    request.getProduct().getProduct(),
                    properties.getAppTagPrefix(),
                    request.getApplication()
            );
        }
        else {
            log.error("Application must be set at minimum");
            throw new MachinaException("Application must be set at minimum");
        }
        log.debug(wiq);
        JSONObject wiqJson = new JSONObject();
        wiqJson.put("query", wiq);
        HttpEntity httpEntity = new HttpEntity<>(wiqJson.toString(), createAuthHeaders());

        ResponseEntity<String> response = restTemplate.exchange(endpoint,
                HttpMethod.POST, httpEntity, String.class, request.getNamespace());
        if(response.getBody() == null) return issues;

        JSONObject json = new JSONObject(response.getBody());
        JSONArray workItems = json.getJSONArray("workItems");

        if(workItems.length() < 1) return issues;

        for (int i = 0; i < workItems.length(); i++) {
            JSONObject workItem = workItems.getJSONObject(i);
            String workItemUri = workItem.getString("url");
            Issue wi = getIssue(workItemUri, issueBody);
            if(wi != null){
                issues.add(wi);
            }
        }
        return issues;
    }

    private Issue getIssue(String uri, String issueBody){
        HttpEntity httpEntity = new HttpEntity<>(createAuthHeaders());
        log.debug("Getting issue at uri {}", uri);
        ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, httpEntity, String.class);
        String r = response.getBody();
        if( r == null){
            return null;
        }

        JSONObject o = new JSONObject(r);
        JSONObject fields = o.getJSONObject("fields");

        Issue i = new Issue();
        i.setBody(fields.getString(FIELD_PREFIX.concat(issueBody)));
        i.setTitle(fields.getString(TITLE_FIELD));
        i.setId(String.valueOf(o.getInt("id")));
        String[] tags = fields.getString(TAGS_FIELD).split(";");
        i.setLabels(Arrays.asList(tags));
        i.setUrl(uri);
        i.setState(fields.getString(STATE_FIELD));
        return i;
    }

    @Override
    public Issue createIssue(ScanResults.XIssue resultIssue, ScanRequest request) throws MachinaException {
        log.debug("Executing createIssue Azure API call");
		String ADOProject;
        String baseUrl = request.getAdditionalMetadata(Constants.ADO_BASE_URL_KEY);
        String issueType = request.getAdditionalMetadata(Constants.ADO_ISSUE_KEY);
        String issueBody = request.getAdditionalMetadata(Constants.ADO_ISSUE_BODY_KEY);

        ADOProject = request.getAltProject();
        if (ScanUtils.empty(ADOProject) && request.getCxFields() != null) {
            ADOProject = request.getCxFields().get(ADO_PROJECT); // get from custom fields if available
        }
        String endpoint;
        if(!ScanUtils.empty(ADOProject)) { //driven by command line
            endpoint = String.format(CREATEWORKITEMS_CLI, baseUrl,
                    ADOProject,
                    issueType,
                    properties.getApiVersion());
        }
        else { //driven by WebHook
            endpoint = String.format(CREATEWORKITEMS, baseUrl,
                    issueType,
                    properties.getApiVersion());
        }
		log.debug(endpoint);
        /*Namespace/Repo/Branch provided*/
        StringBuilder tags = new StringBuilder();
        tags.append(request.getProduct().getProduct()).append("; ");
        if(!flowProperties.isTrackApplicationOnly() &&
                !ScanUtils.empty(request.getNamespace()) &&
                !ScanUtils.empty(request.getRepoName()) &&
                !ScanUtils.empty(request.getBranch())) {
                    tags.append(properties.getOwnerTagPrefix()).append(":").append(request.getNamespace()).append("; ");
                    tags.append(properties.getRepoTagPrefix()).append(":").append(request.getRepoName()).append("; ");
                    tags.append(properties.getBranchLabelPrefix()).append(":").append(request.getBranch());
        }/*Only application provided*/
        else if(!ScanUtils.empty(request.getApplication())){
            tags.append(properties.getAppTagPrefix()).append(":").append(request.getApplication());
        }


        log.debug("tags: {}", tags.toString());
        CreateWorkItemAttr title = new CreateWorkItemAttr();
        title.setOp("add");
        title.setPath(Constants.ADO_FIELD.concat(TITLE_FIELD));
        title.setValue(getXIssueKey(resultIssue, request));

        CreateWorkItemAttr description = new CreateWorkItemAttr();
        description.setOp("add");
        description.setPath(Constants.ADO_FIELD.concat(FIELD_PREFIX.concat(issueBody)));
        description.setValue(ScanUtils.getHTMLBody(resultIssue, request, flowProperties));
        CreateWorkItemAttr tagsBlock = new CreateWorkItemAttr();
        tagsBlock.setOp("add");
        tagsBlock.setPath(Constants.ADO_FIELD.concat(TAGS_FIELD));
        tagsBlock.setValue(tags.toString());

        List<CreateWorkItemAttr> body = new ArrayList<>(Arrays.asList(title, description, tagsBlock));

        for (Map.Entry<String, String> map : request.getAltFields().entrySet()) {
            log.debug("Custom field: {},  value: {}", map.getKey(), map.getValue());
            CreateWorkItemAttr fieldBlock = new CreateWorkItemAttr();
            fieldBlock.setOp("add");
            fieldBlock.setPath(Constants.ADO_FIELD.concat(map.getKey()));
            fieldBlock.setValue(map.getValue());
            body.add(fieldBlock);
        }

        log.debug(body.toString());
        HttpEntity<List<CreateWorkItemAttr>> httpEntity = new HttpEntity<>(body, createPatchAuthHeaders());

        ResponseEntity<String> response = restTemplate.exchange(endpoint,
                HttpMethod.POST, httpEntity, String.class, request.getNamespace());
        try {
            String url = new JSONObject(response.getBody()).getJSONObject("_links").getJSONObject("self").getString("href");
            return getIssue(url, issueBody);
        }catch (NullPointerException e){
            log.warn("Error occurred while retrieving new WorkItem url.  Returning null");
            return null;
        }
    }

    @Override
    public void closeIssue(Issue issue, ScanRequest request) {
        log.debug("Executing closeIssue Azure API call");
        String endpoint = issue.getUrl().concat("?api-version=").concat(properties.getApiVersion());
        String adoClosedState = request.getAdditionalMetadata(Constants.ADO_CLOSED_STATE_KEY);

        CreateWorkItemAttr state = new CreateWorkItemAttr();
        state.setOp("add");
        state.setPath(Constants.ADO_FIELD.concat(STATE_FIELD));
        state.setValue(adoClosedState);

        List<CreateWorkItemAttr> body = new ArrayList<>(Collections.singletonList(state));

        HttpEntity<List<CreateWorkItemAttr>> httpEntity = new HttpEntity<>(body, createPatchAuthHeaders());

        restTemplate.exchange(endpoint, HttpMethod.PATCH, httpEntity, String.class);
    }

    @Override
    public Issue updateIssue(Issue issue, ScanResults.XIssue resultIssue, ScanRequest request)  {
        log.debug("Executing update Azure API call");
        String endpoint = issue.getUrl().concat("?api-version=").concat(properties.getApiVersion());
        String issueBody = request.getAdditionalMetadata(Constants.ADO_ISSUE_BODY_KEY);
        String adoOpenedState = request.getAdditionalMetadata(Constants.ADO_OPENED_STATE_KEY);

        CreateWorkItemAttr state = new CreateWorkItemAttr();
        state.setOp("add");
        state.setPath(Constants.ADO_FIELD.concat(STATE_FIELD));
        state.setValue(adoOpenedState);

        CreateWorkItemAttr description = new CreateWorkItemAttr();
        description.setOp("add");
        description.setPath(Constants.ADO_FIELD.concat(FIELD_PREFIX.concat(issueBody)));
        description.setValue(ScanUtils.getHTMLBody(resultIssue, request, flowProperties));

        List<CreateWorkItemAttr> body = new ArrayList<>(Arrays.asList(state, description));

        HttpEntity<List<CreateWorkItemAttr>> httpEntity = new HttpEntity<>(body, createPatchAuthHeaders());

        restTemplate.exchange(endpoint, HttpMethod.PATCH, httpEntity, String.class);
        return getIssue(issue.getUrl(), issueBody);
    }

    @Override
    public String getFalsePositiveLabel() {
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
        String adoClosedState = request.getAdditionalMetadata(Constants.ADO_CLOSED_STATE_KEY);
        if(issue.getState() == null){
            return false;
        }
        return issue.getState().equals(adoClosedState);
    }

    @Override
    public boolean isIssueOpened(Issue issue, ScanRequest request) {
        String adoOpenedState = request.getAdditionalMetadata(Constants.ADO_OPENED_STATE_KEY);
        String state = issue.getState();
        if(state == null){
            return true;
        }
        return (state.equals(adoOpenedState) || state.equals(PROPOSED_STATE));
    }

    @Override
    public void complete(ScanRequest request, ScanResults results) throws MachinaException {
        log.info("Finalizing Azure Processing");
    }

    private HttpHeaders createAuthHeaders(){
        String encoding = Base64.getEncoder().encodeToString(":".concat(properties.getToken()).getBytes());
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("Content-Type", "application/json");
        httpHeaders.set("Authorization", "Basic ".concat(encoding));
        httpHeaders.set("Accept", "application/json");
        return httpHeaders;
    }

    private HttpHeaders createPatchAuthHeaders(){
        HttpHeaders httpHeaders = createAuthHeaders();
        httpHeaders.set("Content-Type", "application/json-patch+json");
        return httpHeaders;
    }
}