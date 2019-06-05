package com.checkmarx.flow.custom;

import com.checkmarx.flow.config.ADOProperties;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.Issue;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.ScanResults;
import com.checkmarx.flow.dto.azure.CreateWorkItemAttr;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.utils.Constants;
import com.checkmarx.flow.utils.ScanUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service("Azure")
public class ADOIssueTracker implements IssueTracker {

    private static final String STATE_FIELD = "System.State";
    private static final String TITLE_FIELD = "System.Title";
    private static final String TAGS_FIELD = "System.Tags";
    private static final String ISSUE_BODY = "<b>%s</b> issue exists @ <b>%s</b> in branch <b>%s</b>";
    public static final String CRLF = "<div><br></div>";
    private static final String ISSUES_PER_PAGE = "100";
    private static final String FIELD_PREFIX="System.";
    private static final String WORKITEMS="%s{p}/_apis/wit/wiql?api-version=%s";
    private static final String CREATEWORKITEMS="%s{p}/_apis/wit/workitems/$%s?api-version=%s";
    private static final String WIQ_REPO_BRANCH = "Select [System.Id], [System.Title], " +
            "[System.State], [System.State], [System.WorkItemType] From WorkItems Where " +
            "[System.TeamProject] = @project AND [Tags] Contains '%s' AND [Tags] Contains '%s:%s'" +
            "AND [Tags] Contains '%s:%s' AND [Tags] Contains '%s:%s'";
    private static final String WIQ_APP = "Select [System.Id], [System.Title], " +
            "[System.State], [System.State], [System.WorkItemType] From WorkItems Where " +
            "[System.TeamProject] = @project AND [Tags] Contains '%s' AND [Tags] Contains '%s:%s'";
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ADOIssueTracker.class);

    private final RestTemplate restTemplate;
    private final ADOProperties properties;
    private final FlowProperties flowProperties;


    public ADOIssueTracker(RestTemplate restTemplate, ADOProperties properties, FlowProperties flowProperties) {
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

        if(ScanUtils.empty(request.getAdditionalMetadata(Constants.ADO_BASE_URL_KEY))){
            if(ScanUtils.empty(properties.getUrl())) {
                throw new MachinaException("Azure API Url must be provided in property config");
            }
            else{
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
        String endpoint = String.format(WORKITEMS, baseUrl, properties.getApiVersion());

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
        i.setBody(fields.getString(FIELD_PREFIX.concat(properties.getIssueBody())));
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
        String baseUrl = request.getAdditionalMetadata(Constants.ADO_BASE_URL_KEY);
        String endpoint = String.format(CREATEWORKITEMS, baseUrl,
                properties.getIssueType(),
                properties.getApiVersion());
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
        description.setPath(Constants.ADO_FIELD.concat(FIELD_PREFIX.concat(properties.getIssueBody())));
        description.setValue(getMDBody(resultIssue, request.getBranch()));

        CreateWorkItemAttr tagsBlock = new CreateWorkItemAttr();
        tagsBlock.setOp("add");
        tagsBlock.setPath(Constants.ADO_FIELD.concat(TAGS_FIELD));
        tagsBlock.setValue(tags.toString());

        List<CreateWorkItemAttr> body = new ArrayList<>(Arrays.asList(title, description, tagsBlock));
        log.debug(body.toString());
        HttpEntity<List<CreateWorkItemAttr>> httpEntity = new HttpEntity<>(body, createPatchAuthHeaders());

        ResponseEntity<String> response = restTemplate.exchange(endpoint,
                HttpMethod.POST, httpEntity, String.class, request.getNamespace());
        try {
            String url = new JSONObject(response.getBody()).getJSONObject("_links").getString("href");
            return getIssue(url);
        }catch (NullPointerException e){
            log.warn("Error occurred while retrieving new WorkItem url.  Returning null");
            return null;
        }
    }

    @Override
    public void closeIssue(Issue issue, ScanRequest request) {
        log.debug("Executing closeIssue Azure API call");
        String endpoint = issue.getUrl().concat("?").concat(properties.getApiVersion());

        CreateWorkItemAttr state = new CreateWorkItemAttr();
        state.setOp("add");
        state.setPath(Constants.ADO_FIELD.concat(STATE_FIELD));
        state.setValue(properties.getClosedStatus());

        List<CreateWorkItemAttr> body = new ArrayList<>(Collections.singletonList(state));

        HttpEntity<List<CreateWorkItemAttr>> httpEntity = new HttpEntity<>(body, createPatchAuthHeaders());

        restTemplate.exchange(endpoint, HttpMethod.PATCH, httpEntity, String.class);
    }

    @Override
    public Issue updateIssue(Issue issue, ScanResults.XIssue resultIssue, ScanRequest request)  {
        log.debug("Executing update Azure API call");
        String endpoint = issue.getUrl().concat("?api-version=").concat(properties.getApiVersion());

        CreateWorkItemAttr state = new CreateWorkItemAttr();
        state.setOp("add");
        state.setPath(Constants.ADO_FIELD.concat(STATE_FIELD));
        state.setValue(properties.getOpenStatus());

        CreateWorkItemAttr description = new CreateWorkItemAttr();
        description.setOp("add");
        description.setPath(Constants.ADO_FIELD.concat(FIELD_PREFIX.concat(properties.getIssueBody())));
        description.setValue(getMDBody(resultIssue, request.getBranch()));

        List<CreateWorkItemAttr> body = new ArrayList<>(Arrays.asList(state, description));

        HttpEntity<List<CreateWorkItemAttr>> httpEntity = new HttpEntity<>(body, createPatchAuthHeaders());

        restTemplate.exchange(endpoint, HttpMethod.PATCH, httpEntity, String.class);
        return getIssue(issue.getUrl());
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

    private String getMDBody(ScanResults.XIssue issue, String branch) {
        StringBuilder body = new StringBuilder();
        body.append("<div>");
        body.append(String.format(ISSUE_BODY, issue.getVulnerability(), issue.getFilename(), branch)).append(CRLF);
        if(!ScanUtils.empty(issue.getDescription())) {
            body.append("<div><i>").append(issue.getDescription().trim()).append("</i></div>");
        }
        body.append(CRLF);
        
        if(!ScanUtils.empty(issue.getSeverity())) {
            body.append("<div><b>Severity:</b> ").append(issue.getSeverity()).append("</div>");
        }
        if(!ScanUtils.empty(issue.getCwe())) {
            body.append("<div><b>CWE:</b>").append(issue.getCwe()).append("</div>");
            if(!ScanUtils.empty(flowProperties.getMitreUrl())) {
                body.append("<div><a href=\'").append(
                        String.format(
                                flowProperties.getMitreUrl(),
                                issue.getCwe()
                        )
                ).append("\'>Vulnerability details and guidance</a></div>");
            }
        }
        if(!ScanUtils.empty(flowProperties.getWikiUrl())) {
            body.append("<div><a href=\'").append(flowProperties.getWikiUrl()).append("\'>Internal Guidance</a></div>");
        }
        if(!ScanUtils.empty(issue.getLink())){
            body.append("<div><a href=\'").append(issue.getLink()).append("\'>Checkmarx</a></div>");
        }
        if(issue.getDetails() != null && !issue.getDetails().isEmpty()) {
            body.append("<div><b>Lines: </b>");
            for (Map.Entry<Integer, String> entry : issue.getDetails().entrySet()) {
                if (entry.getKey() != null) {  //[<line>](<url>)
                        body.append(entry.getKey()).append(" ");
                }
            }
            body.append("</div>");

            for (Map.Entry<Integer, String> entry : issue.getDetails().entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    body.append("<hr/>");
                    body.append("<b>Line #").append(entry.getKey()).append("</b>");
                    body.append("<pre><code><div>");
                    body.append(entry.getValue());
                    body.append("</div></code></pre><div>");
                }
            }
            body.append("<hr/>");
        }
        if(issue.getOsaDetails()!=null){
            for(ScanResults.OsaDetails o: issue.getOsaDetails()){
                body.append(CRLF);
                if(!ScanUtils.empty(o.getCve())) {
                    body.append("<b>").append(o.getCve()).append("</b>").append(CRLF);
                }
                body.append("<pre><code><div>");
                if(!ScanUtils.empty(o.getSeverity())) {
                    body.append("Severity: ").append(o.getSeverity()).append(CRLF);
                }
                if(!ScanUtils.empty(o.getVersion())) {
                    body.append("Version: ").append(o.getVersion()).append(CRLF);
                }
                if(!ScanUtils.empty(o.getDescription())) {
                    body.append("Description: ").append(o.getDescription()).append(CRLF);
                }
                if(!ScanUtils.empty(o.getRecommendation())){
                    body.append("Recommendation: ").append(o.getRecommendation()).append(CRLF);
                }
                if(!ScanUtils.empty(o.getUrl())) {
                    body.append("URL: ").append(o.getUrl());
                }
                body.append("</div></code></pre><div>");
                body.append(CRLF);
            }
        }
        body.append("</div>");
        return body.toString();
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