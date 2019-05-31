package com.checkmarx.flow.custom;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.Issue;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.ScanResults;
import com.checkmarx.flow.dto.azure.CreateWorkItemAttr;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.utils.ScanUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service("Azure")
public class AzureIssueTracker implements IssueTracker {

    private static final String TRANSITION_ACTIVE = "closed";
    private static final String TRANSITION_CLOSED = "open";
    private static final String STATE_FIELD = "System.State";
    private static final String TITLE_FIELD = "System.Title";
    private static final String TAGS_FIELD = "System.Tags";
    private static final String ISSUES_PER_PAGE = "100";
    private static final String WORKITEMS="%s/{o}/{p}/_apis/wit/wiql?api-version=%s";
    private static final String CREATEWORKITEMS="%s/{o}/{p}/_apis/wit/workitems/$%s?api-version=%s";
    private static final String WIQ_REPO_BRANCH = "Select [System.Id], [System.Title], " +
            "[System.State], [System.State], [System.WorkItemType] From WorkItems Where " +
            "[System.TeamProject] = @project AND [Tags] Contains '%s' AND [Tags] Contains '%s:%s'" +
            "AND [Tags] Contains '%s:%s' AND [Tags] Contains '%s:%s'";
    private static final String WIQ_APP = "Select [System.Id], [System.Title], " +
            "[System.State], [System.State], [System.WorkItemType] From WorkItems Where " +
            "[System.TeamProject] = @project AND [Tags] Contains '%s' AND [Tags] Contains '%s:%s'";
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AzureIssueTracker.class);

    private final RestTemplate restTemplate;
    private final AzureProperties properties;
    private final FlowProperties flowProperties;


    public AzureIssueTracker(RestTemplate restTemplate, AzureProperties properties, FlowProperties flowProperties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.flowProperties = flowProperties;
    }

    @Override
    public void init(ScanRequest request, ScanResults results) throws MachinaException {
        log.info("Initializing Azure processing");
        if(ScanUtils.empty(request.getNamespace()) ||
                ScanUtils.empty(request.getRepoName()) ||
                ScanUtils.empty(request.getBranch())){
            throw new MachinaException("Namespace / RepoName / Branch are required");
        }
        if(ScanUtils.empty(properties.getApiUrl())){
            throw new MachinaException("Azure API Url must be provided in property config");
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
        List<Issue> issues = new ArrayList<>();
        String endpoint = String.format(WORKITEMS, properties.getApiUrl(), properties.getApiVersion());

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
        HttpEntity httpEntity = new HttpEntity<>(wiq, createAuthHeaders());

        ResponseEntity<String> response = restTemplate.exchange(endpoint,
                HttpMethod.POST, httpEntity, String.class, request.getNamespace(), request.getRepoName());
        if(response.getBody() == null) return issues;

        JSONObject json = new JSONObject(response.getBody());
        JSONArray workItems = json.getJSONArray("workItems");

        if(workItems.length() < 1) return issues;

        for (int i = 0; i < workItems.length(); i++) {
            JSONObject workItem = workItems.getJSONObject(i);
            String workItemUri = workItem.getString("url");
            Issue wi = getIssue(workItemUri);
            if(wi != null){
                issues.add(wi);
            }

        }
        return issues;
    }

    private Issue getIssue(String uri){
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
        i.setBody(fields.getString(properties.getIssueBody()));
        i.setTitle(fields.getString(TITLE_FIELD));
        i.setId(String.valueOf(fields.getInt("id")));
        String[] tags = fields.getString(TAGS_FIELD).split(";");
        i.setLabels(Arrays.asList(tags));
        i.setUrl(uri);
        i.setState(fields.getString(STATE_FIELD));
        return i;
    }

    @Override
    public Issue createIssue(ScanResults.XIssue resultIssue, ScanRequest request) throws MachinaException {
        log.debug("Executing createIssue Azure API call");
        String endpoint = String.format(CREATEWORKITEMS, properties.getApiUrl(),
                request.getNamespace(),
                properties.getApiVersion());

        CreateWorkItemAttr title = new CreateWorkItemAttr();
        CreateWorkItemAttr description = new CreateWorkItemAttr();
        CreateWorkItemAttr tags = new CreateWorkItemAttr();
        title.setOp("add");
        title.setPath("fields/".concat(TITLE_FIELD));
        title.setValue("");
        description.setOp("add");
        description.setPath("fields/".concat(properties.getIssueBody()));
        description.setValue("");
        tags.setOp("add");
        tags.setPath("fields/".concat(TAGS_FIELD));
        tags.setValue("");
        List<CreateWorkItemAttr> body = new ArrayList<>(Arrays.asList(title, description, tags));
        HttpEntity<List<CreateWorkItemAttr>> httpEntity = new HttpEntity<>(body, createPatchAuthHeaders());

        ResponseEntity<String> response = restTemplate.exchange(endpoint,
                HttpMethod.POST, httpEntity, String.class, request.getNamespace(), request.getRepoName());

        return null;
    }

    @Override
    public void closeIssue(Issue issue, ScanRequest request) throws MachinaException {
        log.info("Executing closeIssue Azure API call");
        //HttpEntity httpEntity = new HttpEntity<>(getJSONCloseIssue().toString(), createAuthHeaders());
        //restTemplate.exchange(issue.getUrl(), HttpMethod.POST, httpEntity, Issue.class);
    }

    @Override
    public Issue updateIssue(Issue issue, ScanResults.XIssue resultIssue, ScanRequest request) throws MachinaException {
        log.info("Executing updateIssue Azure API call");
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
    public boolean isIssueClosed(Issue issue) {
        if(issue.getState() == null){
            return false;
        }
        return issue.getState().equals(properties.getClosedStatus());
    }

    @Override
    public boolean isIssueOpened(Issue issue) {
        if(issue.getState() == null){
            return true;
        }
        return issue.getState().equals(properties.getOpenStatus());
    }

    @Override
    public void complete(ScanRequest request, ScanResults results) throws MachinaException {
        log.info("Finalizing Azure Processing");
    }

    private HttpHeaders createAuthHeaders(){
        String encoding = Base64.getEncoder().encodeToString(properties.getToken().getBytes());
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