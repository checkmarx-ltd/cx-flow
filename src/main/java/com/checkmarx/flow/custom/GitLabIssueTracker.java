package com.checkmarx.flow.custom;

import com.checkmarx.flow.config.FindingSeverity;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitLabProperties;
import com.checkmarx.flow.config.ScmConfigOverrider;
import com.checkmarx.flow.dto.Issue;
import com.checkmarx.flow.dto.LabelField;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.gitlab.Note;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.utils.HTMLHelper;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.dto.ScanResults;
import org.apache.commons.lang3.StringUtils;
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
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service("GitLab")
public class GitLabIssueTracker implements IssueTracker {

    private static final String TRANSITION_CLOSE = "close";
    private static final String TRANSITION_OPEN = "reopen";
    private static final String OPEN_STATE = "opened";
    private static final String ISSUES_PER_PAGE = "100";

    //To Fix GitLab Pagination Issue
    private static final String PROJECT = "/projects?search={repo}&pagination=keyset&per_page=100&order_by=id&sort=asc&id_after={id}";

    private static final String ISSUES_PATH = "/projects/{id}/issues?per_page=".concat(ISSUES_PER_PAGE);
    private static final String NEW_ISSUE_PATH = "/projects/{id}/issues";
    private static final String ISSUE_PATH = "/projects/{id}/issues/{iid}";
    private static final String COMMENT_PATH = "/projects/{id}/issues/{iid}/notes";
    private static final Logger log = LoggerFactory.getLogger(GitLabIssueTracker.class);
    private static final int UNKNOWN_INT = -1;
    private final RestTemplate restTemplate;
    private final GitLabProperties properties;
    private final FlowProperties flowProperties;
    private final ScmConfigOverrider scmConfigOverrider;

    private final int max_desc_length = 0;

    public GitLabIssueTracker(@Qualifier("flowRestTemplate") RestTemplate restTemplate, GitLabProperties properties, FlowProperties flowProperties,
                              ScmConfigOverrider scmConfigOverrider) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.flowProperties = flowProperties;
        this.scmConfigOverrider = scmConfigOverrider;
    }

    @Override
    public void init(ScanRequest request, ScanResults results) throws MachinaException {
        log.info("Initializing GitLab processing");

        if (ScanUtils.empty(request.getNamespace()) ||
                ScanUtils.empty(request.getRepoName()) ||
                ScanUtils.empty(request.getBranch())) {
            throw new MachinaException("Namespace / RepoName / Branch are required");
        }

        if (ScanUtils.empty(scmConfigOverrider.determineConfigApiUrl(properties, request))) {
            throw new MachinaException("GitLab API Url must be provided in property config");
        }
        if (request.getRepoProjectId() == null) {
            Integer projectId = getProjectId(request);
            if (projectId.equals(UNKNOWN_INT)) {
                log.error("Could not obtain GitLab Project Id for {}/{}/{}", request.getNamespace(), request.getRepoName(), request.getBranch());
                throw new MachinaException("Could not obtain GitLab Project Id");
            }
            request.setRepoProjectId(projectId);
        }
    }

    private Integer getProjectId(ScanRequest request) {
        try {
            int projectId = 0;
            String targetRepoName = request.getRepoName();

            String lastProjectId = "0";
            while (true) {
                JSONArray candidateProjects = getProjectSearchResults(request, lastProjectId);

                if (candidateProjects == null) return projectId;

                int length = candidateProjects.length();
                if (length >= 100)
                    lastProjectId = String.valueOf((((JSONObject) candidateProjects.get(99)).getInt("id")));

                log.debug("Projects found: {}. Looking for exact match.", candidateProjects.length());

                // The search is fuzzy, so we need to additionally filter search results here for strict match.
                for (Object project : candidateProjects) {
                    JSONObject projectJson = (JSONObject) project;
                    if (isTargetProject(projectJson, request.getNamespace(), targetRepoName)) {
                        projectId = projectJson.getInt("id");
                        log.debug("Using GitLab project ID: {}", projectId);
                        break;
                    }
                }
                if (length < 100) break;

            }


            return projectId;


        } catch (HttpClientErrorException e) {
            log.error("Error calling gitlab project api {}", e.getResponseBodyAsString(), e);
        } catch (JSONException e) {
            log.error("Error parsing gitlab project response.", e);
        } catch (URISyntaxException e) {
            log.error("Incorrect URI", e);
        }
        return UNKNOWN_INT;
    }

    private static boolean isTargetProject(JSONObject projectJson, String targetNamespace, String targetRepo) {
        // Cannot use the 'name' property here, because it's for display only and may be different from 'path'.
        String repoPath = projectJson.getString("path");

        // Cannot use the 'name' or 'path' properties here.
        // 'name' is for display only. 'path' only includes the last segment.
        // E.g. "path": "my-good-old-namespace", "full_path": "dir1/dir2/my-good-old-namespace"
        String namespacePath = projectJson.getJSONObject("namespace")
                .getString("full_path");


        boolean result = repoPath.equals(targetRepo) && namespacePath.equals(targetNamespace);
        log.debug("Checking {}/{}... {}", namespacePath, repoPath, result ? "match!" : "no match.");
        return result;
    }

    private JSONArray getProjectSearchResults(ScanRequest scanRequest, String currentProjectID) throws URISyntaxException {
        String targetRepoName = scanRequest.getRepoName();
        log.debug("Searching repo by query: {}", targetRepoName);

        //Change Added :: Modifying URL
        String url = scmConfigOverrider.determineConfigApiUrl(properties, scanRequest)
                .concat(PROJECT)
                .replace("{repo}", targetRepoName).replace("{id}", currentProjectID);
        URI uri = new URI(url);
        HttpEntity<Void> httpEntity = new HttpEntity<>(createAuthHeaders(scanRequest));
        ResponseEntity<String> response = null;
        try {
            response = restTemplate.exchange(uri, HttpMethod.GET, httpEntity, String.class);
        } catch (HttpClientErrorException e) {
            log.error("Error occurred while getting Project Search Result. http error {} ", e.getStatusCode(), e);
            log.debug(ExceptionUtils.getStackTrace(e));
        }
        return new JSONArray(response.getBody());
    }

    /**
     * Get list of issues associated with the project in GitLab
     */
    @Override
    public List<Issue> getIssues(ScanRequest request) {
        log.info("Executing getIssues GitLab API call");
        List<Issue> issues = new ArrayList<>();
        HttpEntity<Void> httpEntity = new HttpEntity<>(createAuthHeaders(request));
        String endpoint = scmConfigOverrider.determineConfigApiUrl(properties, request).concat(ISSUES_PATH);
        ResponseEntity<com.checkmarx.flow.dto.gitlab.Issue[]> response = null;
        try {
            response = restTemplate.exchange(endpoint,
                    HttpMethod.GET, httpEntity, com.checkmarx.flow.dto.gitlab.Issue[].class, request.getRepoProjectId());
        } catch (HttpClientErrorException e) {
            log.error("Error occurred while getting Gitlab issue. http error {} ", e.getStatusCode(), e);
            log.debug(ExceptionUtils.getStackTrace(e));
        }
        if (response.getBody() == null) {
            return issues;
        }
        for (com.checkmarx.flow.dto.gitlab.Issue issue : response.getBody()) {
            Issue i = mapToIssue(issue);
            if (i != null && i.getTitle().startsWith(request.getProduct().getProduct())) {
                issues.add(i);
            }
        }
        String next = getNextURIFromHeaders(response.getHeaders(), "link", "next");
        while (next != null) {
            ResponseEntity<com.checkmarx.flow.dto.gitlab.Issue[]> responsePage = null;
            try {
                responsePage = restTemplate.exchange(next, HttpMethod.GET, httpEntity, com.checkmarx.flow.dto.gitlab.Issue[].class);
            } catch (HttpClientErrorException e) {
                log.error("Error occurred while getting issue. http error {} ", e.getStatusCode(), e);
                log.debug(ExceptionUtils.getStackTrace(e));
            }
            if (responsePage.getBody() != null) {
                for (com.checkmarx.flow.dto.gitlab.Issue issue : responsePage.getBody()) {
                    Issue i = mapToIssue(issue);
                    if (i != null && i.getTitle().startsWith(request.getProduct().getProduct())) {
                        issues.add(i);
                    }
                }
            }
            next = getNextURIFromHeaders(responsePage.getHeaders(), "link", "next");
        }
        return issues;
    }

    private Issue mapToIssue(com.checkmarx.flow.dto.gitlab.Issue issue) {
        if (issue == null) {
            return null;
        }
        Issue i = new Issue();
        i.setBody(issue.getDescription());
        i.setTitle(issue.getTitle());
        i.setId(issue.getIid().toString());
        i.setLabels(issue.getLabels());
        i.setUrl(issue.getWebUrl());
        i.setState(issue.getState());
        return i;
    }

    /**
     * Retrieve DTO representation of GitLab Issue
     *
     * @return GitLab Issue
     */
    private Issue getIssue(ScanRequest scanRequest, Integer projectId, Integer iid) {
        log.debug("Executing getIssue GitLab API call");
        String endpoint = scmConfigOverrider.determineConfigApiUrl(properties, scanRequest).concat(ISSUE_PATH);
        HttpEntity<Void> httpEntity = new HttpEntity<>(createAuthHeaders(scanRequest));
        ResponseEntity<com.checkmarx.flow.dto.gitlab.Issue> response = null;
        try {
            response = restTemplate.exchange(endpoint, HttpMethod.GET, httpEntity, com.checkmarx.flow.dto.gitlab.Issue.class, projectId, iid);
        } catch (HttpClientErrorException e) {
            log.error("Error occurred while getting Gitlab issue. http error {} ", e.getStatusCode(), e);
            log.debug(ExceptionUtils.getStackTrace(e));
        }
        return mapToIssue(response.getBody());
    }


    /**
     * Adds a comment (Note) to an issue
     */
    private void addComment(ScanRequest scanRequest, Integer projectId, Integer iid, String comment) {
        log.debug("Executing add comment GitLab API call");
        String endpoint = scmConfigOverrider.determineConfigApiUrl(properties, scanRequest).concat(COMMENT_PATH);
        Note note = Note.builder()
                .body(comment)
                .build();
        HttpEntity<Note> httpEntity = new HttpEntity<>(note, createAuthHeaders(scanRequest));
        try {
            restTemplate.exchange(endpoint, HttpMethod.POST, httpEntity, String.class, projectId, iid);
        } catch (HttpClientErrorException e) {
            log.error("Error occurred while adding Gitlab comment. http error {} ", e.getStatusCode(), e);
            log.debug(ExceptionUtils.getStackTrace(e));
        }
    }

    @Override
    public Issue createIssue(ScanResults.XIssue resultIssue, ScanRequest request) throws MachinaException {
        log.debug("Executing createIssue GitLab API call");

        String endpoint = scmConfigOverrider.determineConfigApiUrl(properties, request).concat(NEW_ISSUE_PATH);
        ResponseEntity<com.checkmarx.flow.dto.gitlab.Issue> response;

        try {
            HttpEntity<String> httpEntity = new HttpEntity<>(getJSONCreateIssue(resultIssue, request).toString(), createAuthHeaders(request));
            response = restTemplate.exchange(endpoint, HttpMethod.POST, httpEntity, com.checkmarx.flow.dto.gitlab.Issue.class, request.getRepoProjectId());
        } catch (HttpClientErrorException e) {
            log.error("Error occurred while creating GitLab Issue", e);
            log.debug(ExceptionUtils.getStackTrace(e));
            if (e.getStatusCode().equals(HttpStatus.GONE)) {
                throw new MachinaException("Issues are not enabled for this repository");
            } else {
                throw new MachinaException("Error occurred while creating GitLab Issue");
            }

        }
        return mapToIssue(response.getBody());
    }

    @Override
    public void closeIssue(Issue issue, ScanRequest request) throws MachinaException {
        closeIssue(request, Integer.parseInt(issue.getId()));
    }

    private void closeIssue(ScanRequest request, Integer iid) {
        log.debug("Executing closeIssue GitLab API call");
        String endpoint = scmConfigOverrider.determineConfigApiUrl(properties, request).concat(ISSUE_PATH);
        HttpEntity<String> httpEntity = new HttpEntity<>(getJSONCloseIssue().toString(), createAuthHeaders(request));
        try {
            restTemplate.exchange(endpoint, HttpMethod.PUT, httpEntity,
                    com.checkmarx.flow.dto.gitlab.Issue.class, request.getRepoProjectId(), iid);
        } catch (HttpClientErrorException e) {
            log.error("Error occurred while closing Gitlab issue. http error {} ", e.getStatusCode(), e);
            log.debug(ExceptionUtils.getStackTrace(e));
        }
    }

    @Override
    public Issue updateIssue(Issue issue, ScanResults.XIssue resultIssue, ScanRequest request) throws MachinaException {
        return updateIssue(request, getJSONUpdateIssue(resultIssue, request), request.getRepoProjectId(), Integer.parseInt(issue.getId()));
    }

    /**
     * Update existing issue in GitLab
     */
    private Issue updateIssue(ScanRequest scanRequest, JSONObject issue, Integer projectId, Integer iid) {
        log.debug("Executing updateIssue GitLab API call");
        String endpoint = scmConfigOverrider.determineConfigApiUrl(properties, scanRequest).concat(ISSUE_PATH);

        HttpEntity<String> httpEntity = new HttpEntity<>(issue.toString(), createAuthHeaders(scanRequest));
        ResponseEntity<com.checkmarx.flow.dto.gitlab.Issue> response;
        try {
            response = restTemplate.exchange(endpoint, HttpMethod.PUT, httpEntity, com.checkmarx.flow.dto.gitlab.Issue.class, projectId, iid);
            this.addComment(scanRequest, projectId, iid, "Issue still exists. ");
            return mapToIssue(response.getBody());
        } catch (HttpClientErrorException e) {
            this.addComment(scanRequest, projectId, iid, "This issue still exists.  Please add label 'false-positive' to remove from scope of SAST results");
            log.debug(ExceptionUtils.getStackTrace(e));
        }
        return this.getIssue(scanRequest, projectId, iid);
    }

    private String getFileUrl(ScanRequest request, String filename) {
        if (ScanUtils.empty(request.getRepoUrl())) {
            return null;
        }
        String repoUrl = request.getRepoUrl().replace(".git", "/");
        if ( !ScanUtils.empty(repoUrl) && repoUrl.contains("gitlab-ci-token") && repoUrl.contains("@")) {
            repoUrl = repoUrl.substring(0, 8) + repoUrl.substring(repoUrl.indexOf('@') + 1);
        }
        return (Optional.ofNullable(filename).isPresent())
                ? String.format(String.format("%s/blob/%%s/%%s", repoUrl), request.getBranch(), filename)
                : null;
    }


    /**
     * @return JSON Object for close issue request
     */
    private JSONObject getJSONCloseIssue() {
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("state_event", TRANSITION_CLOSE);
        } catch (JSONException e) {
            log.error("Error creating JSON Close Issue Object - JSON object will be empty", e);
        }
        return requestBody;
    }

    /**
     * Create JSON http request body for an create/update Issue POST request to GitLab
     */
    private JSONObject getJSONUpdateIssue(ScanResults.XIssue resultIssue, ScanRequest request) {
        JSONObject requestBody = new JSONObject();
        String fileUrl = getFileUrl(request, resultIssue.getFilename());
        String body = HTMLHelper.getMDBody(resultIssue, request.getBranch(), fileUrl, flowProperties, max_desc_length);
        String title = getXIssueKey(resultIssue, request);
        String label = getString(request,resultIssue);

        try {
            requestBody.put("title", title);
            requestBody.put("description", body);
            requestBody.put("state_event", TRANSITION_OPEN);
            if (!label.equalsIgnoreCase("NA")) {
                requestBody.put("labels", label);
            }
        } catch (JSONException e) {
            log.error("Error creating JSON Update Object - JSON object will be empty", e);
        }
        return requestBody;
    }

    /**
     * Create JSON http request body for an create/update Issue POST request to GitLab
     *
     * @return JSON Object of create issue request
     */
    private JSONObject getJSONCreateIssue(ScanResults.XIssue resultIssue, ScanRequest request) {
        JSONObject requestBody = new JSONObject();
        String fileUrl = getFileUrl(request, resultIssue.getFilename());
        String body = HTMLHelper.getMDBody(resultIssue, request.getBranch(), fileUrl, flowProperties, max_desc_length);
        String title = HTMLHelper.getScanRequestIssueKeyWithDefaultProductValue(request, this, resultIssue);
        String label = getString(request,resultIssue);

        try {
            requestBody.put("title", title);
            requestBody.put("description", body);
            if (!label.equalsIgnoreCase("NA")) {
                requestBody.put("labels", label);
            }
        } catch (JSONException e) {
            log.error("Error creating JSON Create Issue Object - JSON Object will be empty", e);
        }
        return requestBody;
    }

    private String getString(ScanRequest request,ScanResults.XIssue issue) {
        String label = "NA";
        List<String> value = new ArrayList<>();
        try {
            if(properties.getIssueslabel()!=null)
            {
                Map<FindingSeverity, String> findingsPerSeverity = properties.getIssueslabel();
                for (Map.Entry<FindingSeverity, String> entry : findingsPerSeverity.entrySet()) {
                    if (issue.getSeverity().equalsIgnoreCase(entry.getKey().toString()) || issue.getSeverity().toLowerCase(Locale.ROOT).contains(entry.getKey().toString().toLowerCase(Locale.ROOT))) {
                        label = entry.getValue();
                        break;
                    }
                }
            }

            for (LabelField f : properties.getFields()) {
                String fieldType = f.getType();
                String fieldName;
                if (ScanUtils.empty(fieldType)) {
                    log.warn("Field type not supplied. Using 'result' by default.");
                    // use default = result
                    fieldType = "result";
                }
                Map<String, Object> addDetails = null;
                Map<String, String> scanCustomFields = null;
                String scanScaTags = null;
                String scanCustomFieldsValue = null;
                if (Objects.nonNull(issue.getAdditionalDetails()) && Objects.nonNull((Map<String, String>) issue.getAdditionalDetails().get("scanCustomFields"))) {
                    addDetails = issue.getAdditionalDetails();
                    scanCustomFields = (Map<String, String>) addDetails.get("scanCustomFields");
                    scanCustomFieldsValue = scanCustomFields.get(f.getName());
                }
                if (Objects.nonNull(issue.getScaDetails()) && Objects.nonNull(issue.getScaDetails().get(0).getScanTags())) {
                    scanScaTags = (String) issue.getScaDetails().get(0).getScanTags().get(f.getName());
                }

                switch (fieldType) {
                    case "cx-scan"://cx-scan, cx-sca, sca-results, static, result
                        log.debug("Checkmarx scan custom field {}", f.getName());
                        if (scanCustomFieldsValue != null) {
                            log.debug("Checkmarx scan custom field");
                            value.add(f.getName() + ":" + scanCustomFieldsValue);
                            log.debug("Cx Scan Field value: {}", value);
                            if (ScanUtils.empty(value) && !ScanUtils.empty(f.getDefaultValue())) {
                                value.add(f.getName() + ":" + f.getDefaultValue());
                                log.debug("default Value is {}", value);
                            }
                        } else {
                            log.debug("No value found for {}", f.getName());
                            value.add("");
                        }
                        break;
                    case "cx-sca":
                        log.debug("SCA scan Tags Key name {}", f.getName());
                        if (scanScaTags != null) {
                            value.add(f.getName() + ":" + scanScaTags);
                            log.debug("SCA scan Field value: {}", value);
                            if (ScanUtils.empty(value) && !ScanUtils.empty(f.getDefaultValue())) {
                                value.add(f.getName() + ":" + f.getDefaultValue());
                                log.debug(" default Value is {}", value);
                            }
                        } else {
                            log.debug("No value found for {}", f.getName());
                            value.add("");
                        }
                        break;
                    case "sca-results":
                        if (issue.getScaDetails() == null) {
                            log.debug("Sca details not available");
                            break;
                        }
                        fieldName = f.getName();
                        switch (fieldName) {
                            case "package-name":
                                log.debug("package-name: {}", issue.getScaDetails().get(0).getVulnerabilityPackage().getId());
                                value.add(f.getName() + ":" + issue.getScaDetails().get(0).getVulnerabilityPackage().getId());
                                break;
                            case "current-version":
                                log.debug("current-version: {}", issue.getScaDetails().get(0).getVulnerabilityPackage().getVersion());
                                value.add(f.getName() + ":" + issue.getScaDetails().get(0).getVulnerabilityPackage().getVersion());
                                break;
                            case "fixed-version":
                                log.debug("fixed-version: {}", issue.getScaDetails().get(0).getFinding().getFixResolutionText());
                                value.add(f.getName() + ":" + issue.getScaDetails().get(0).getFinding().getFixResolutionText());
                                break;
                            case "newest-version":
                                log.debug(issue.getScaDetails().get(0).getVulnerabilityPackage().getNewestVersion());
                                value.add(f.getName() + ":" + issue.getScaDetails().get(0).getVulnerabilityPackage().getNewestVersion());
                                break;
                            case "locations":
                                List<String> locations = issue.getScaDetails().get(0).getVulnerabilityPackage().getLocations();
                                String location = null;
                                for (String l : locations
                                ) {
                                    location = l + ",";
                                }
                                log.debug("locations: {}", location);
                                assert location != null;
                                value.add(f.getName() + ":" + location.substring(0, location.length() - 1));
                                break;
                            case "dev-dependency":
                                log.debug("dev-dependency: {}", issue.getScaDetails().get(0).getVulnerabilityPackage().isIsDevelopmentDependency());
                                value.add(f.getName() + ":" + String.valueOf(issue.getScaDetails().get(0).getVulnerabilityPackage().isIsDevelopmentDependency()).toUpperCase());
                                break;
                            case "direct-dependency":
                                log.debug("direct-dependency: {}", issue.getScaDetails().get(0).getVulnerabilityPackage().isIsDirectDependency());
                                value.add(f.getName() + ":" + String.valueOf(issue.getScaDetails().get(0).getVulnerabilityPackage().isIsDirectDependency()).toUpperCase());
                                break;
                            case "risk-score":
                                log.debug("risk score: {}", issue.getScaDetails().get(0).getVulnerabilityPackage().getRiskScore());
                                value.add(f.getName() + ":" + String.valueOf(issue.getScaDetails().get(0).getVulnerabilityPackage().getRiskScore()));
                                break;
                            case "outdated":
                                log.debug("outdated: {}", issue.getScaDetails().get(0).getVulnerabilityPackage().isOutdated());
                                value.add(f.getName() + ":" + String.valueOf(issue.getScaDetails().get(0).getVulnerabilityPackage().isOutdated()).toUpperCase());
                                break;
                            case "violates-policy":
                                log.debug("Violates-Policy: {}", issue.getScaDetails().get(0).getFinding().isViolatingPolicy());
                                value.add(f.getName() + ":" + String.valueOf(issue.getScaDetails().get(0).getFinding().isViolatingPolicy()).toUpperCase());

                        }
                        break;
                    case "static":
                        log.debug("Static value {} - {}", f.getName(), f.getDefaultValue());
                        value.add(f.getDefaultValue());
                        break;
                    default: //result
                        fieldName = f.getName();
                        if (fieldName == null) {
                            log.warn("Field name not supplied. Skipping.");
                            /* there is no default, move on to the next field */
                            continue;
                        }
                        /*known values we can use*/
                        switch (fieldName) {
                            case "application":
                                log.debug("application: {}", request.getApplication());
                                if(request.getApplication()!=null){
                                    value.add(f.getName() + ":" + request.getApplication());
                                }else{
                                    value.add(f.getName() + ":" +"NA");
                                }
                                break;
                            case "project":
                                log.debug("project: {}", request.getProject());
                                value.add(f.getName() + ":" + request.getProject());
                                break;
                            case "namespace":
                                log.debug("namespace: {}", request.getNamespace());
                                value.add(f.getName() + ":" + request.getNamespace());
                                break;
                            case "repo-name":
                                log.debug("repo-name: {}", request.getRepoName());
                                value.add(f.getName() + ":" + request.getRepoName());
                                break;
                            case "repo-url":
                                log.debug("repo-url: {}", request.getRepoUrl());
                                value.add(f.getName() + ":" + request.getRepoUrl());
                                break;
                            case "branch":
                                log.debug("branch: {}", request.getBranch());
                                value.add(f.getName() + ":" + request.getBranch());
                                break;
                            case "severity":
                                if (issue.getScaDetails() != null) {
                                    log.debug("severity: {}", issue.getScaDetails().get(0).getFinding().getSeverity());
                                    value.add(f.getName() + ":" + ScanUtils.toProperCase(String.valueOf(issue.getScaDetails().get(0).getFinding().getSeverity())));
                                } else {
                                    log.debug("severity: {}", issue.getSeverity());
                                    value.add(f.getName() + ":" + ScanUtils.toProperCase(issue.getSeverity()));
                                }
                                break;
                            case "category":
                                log.debug("category: {}", issue.getVulnerability());
                                value.add(f.getName() + ":" + issue.getVulnerability());
                                break;
                            case "cwe":
                                log.debug("cwe: {}", issue.getCwe());
                                value.add(f.getName() + ":" + issue.getCwe());
                                break;
                            case "cve":
                                if (issue.getScaDetails() != null) {
                                    log.debug("cve: {}", issue.getScaDetails().get(0).getFinding().getId());
                                    value.add(f.getName() + ":" + issue.getScaDetails().get(0).getFinding().getId());
                                } else {
                                    log.debug("cve: {}", issue.getCve());
                                    value.add(f.getName() + ":" + issue.getCve());
                                }
                                break;
                            case "system-date":
                                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                                LocalDateTime now = LocalDateTime.now();
                                value.add(f.getName() + ":" + dtf.format(now));
                                log.debug("system date: {}", value);
                                break;
                            case "recommendation":
                                StringBuilder recommendation = new StringBuilder();
                                if (issue.getLink() != null && !issue.getLink().isEmpty()) {
                                    recommendation.append("Checkmarx Link: ").append(issue.getLink()).append(HTMLHelper.CRLF);
                                }
                                if (!ScanUtils.anyEmpty(flowProperties.getMitreUrl(), issue.getCwe())) {
                                    recommendation.append("Mitre Details: ").append(String.format(flowProperties.getMitreUrl(), issue.getCwe())).append(HTMLHelper.CRLF);
                                }
                                if (!ScanUtils.empty(flowProperties.getWikiUrl())) {
                                    recommendation.append("Guidance: ").append(flowProperties.getWikiUrl()).append(HTMLHelper.CRLF);
                                }
                                value.add(f.getName() + ":" + recommendation.toString());
                                break;
                            case "loc":
                                if (issue.getDetails() != null) {
                                    List<Integer> lines = issue.getDetails().entrySet()
                                            .stream()
                                            .filter(x -> x.getKey() != null && x.getValue() != null && !x.getValue().isFalsePositive())
                                            .map(Map.Entry::getKey)
                                            .collect(Collectors.toList());
                                    if (!lines.isEmpty()) {
                                        Collections.sort(lines);
                                        value.add(f.getName() + ":" + StringUtils.join(lines, ","));
                                        log.debug("loc: {}", value);
                                    }
                                }
                                break;
                            case "not-exploitable":
                                List<Integer> fpLines;
                                if (issue.getDetails() != null) {
                                    fpLines = issue.getDetails().entrySet()
                                            .stream()
                                            .filter(x -> x.getKey() != null && x.getValue() != null && x.getValue().isFalsePositive())
                                            .map(Map.Entry::getKey)
                                            .collect(Collectors.toList());
                                    if (!fpLines.isEmpty()) {
                                        Collections.sort(fpLines);
                                        value.add(f.getName() + ":" + StringUtils.join(fpLines, ","));
                                        log.debug("loc: {}", value);
                                    }
                                }
                                break;
                            case "site":
                                log.debug("site: {}", request.getSite());
                                value.add(f.getName() + ":" + request.getSite());
                                break;
                            case "issue-link":
                                if (issue.getScaDetails() != null) {
                                    log.debug("issue-link: {}", issue.getScaDetails().get(0).getVulnerabilityLink());
                                    value.add(f.getName() + ":" + issue.getScaDetails().get(0).getVulnerabilityLink());
                                } else {
                                    log.debug("issue-link: {}", issue.getLink());
                                    value.add(f.getName() + ":" + issue.getLink());
                                }
                                break;
                            case "filename":
                                log.debug("filename: {}", issue.getFilename());
                                value.add(f.getName() + ":" + issue.getFilename());
                                break;
                            case "language":
                                log.debug("language: {}", issue.getLanguage());
                                value.add(f.getName() + ":" + issue.getLanguage());
                                break;
                            case "similarity-id":
                                log.debug("similarity-id: {}", issue.getSimilarityId());
                                value.add(f.getName() + ":" + issue.getSimilarityId());
                                break;
                            case "comment":
                                StringBuilder comments = new StringBuilder();
                                String commentFmt = "[Line %s]: [%s]".concat(HTMLHelper.CRLF);
                                if (issue.getDetails() != null) {
                                    issue.getDetails().entrySet()
                                            .stream()
                                            .filter(x -> x.getKey() != null && x.getValue() != null && x.getValue().getComment() != null && !x.getValue().getComment().isEmpty())
                                            .forEach(c -> comments.append(String.format(commentFmt, c.getKey(), c.getValue().getComment())));
                                    value.add(f.getName() + ":" + comments.toString());
                                }
                                break;
                            default:
                                log.warn("field value for {} not found", f.getName());
                                value.add("");
                        }
                        /*If the value is missing, check if a default value was specified*/
                        if (ScanUtils.empty(value)) {
                            log.debug("Value is empty, defaulting to configured default (if applicable)");
                            if (!ScanUtils.empty(f.getDefaultValue())) {
                                value.add(f.getName() + ":" + f.getDefaultValue());
                                log.debug("Default value is {}", value);
                            }
                        }
                        break;
                }
            }

            
            if(properties.getIssueslabel()==null && !value.isEmpty())
            {
                label = String.join(",", value);
            }else{
                label = label + "," + String.join(",", value);
            }
        } catch (Exception e) {
            return label;
        }
        return label;
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
        if (flowProperties.isTrackApplicationOnly() || ScanUtils.empty(request.getBranch())) {
            return String.format(ScanUtils.ISSUE_TITLE_KEY, request.getProduct().getProduct(), issue.getVulnerability(), issue.getFilename());
        } else {
            return ScanUtils.isSAST(issue)
                    ? String.format(ScanUtils.ISSUE_TITLE_KEY_WITH_BRANCH, request.getProduct().getProduct(), issue.getVulnerability(), issue.getFilename(), request.getBranch())
                    : ScanUtils.getScaSummaryIssueKey(request, issue);
        }
    }

    @Override
    public boolean isIssueClosed(Issue issue, ScanRequest request) {
        if (issue.getState() == null) {
            return false;
        }
        return issue.getState().equals(TRANSITION_CLOSE);
    }

    @Override
    public boolean isIssueOpened(Issue issue, ScanRequest request) {
        if (issue.getState() == null) {
            return true;
        }
        return issue.getState().equals(OPEN_STATE);
    }

    @Override
    public void complete(ScanRequest request, ScanResults results) throws MachinaException {
        log.info("Finalizing GitLab Processing");
    }

    /**
     * Creates authentication header for GitLab API Access
     * TODO swap out for Portal based customer storage and possibly OAuth
     * https://docs.gitlab.com/ee/api/README.html#oauth2-tokens
     * https://docs.gitlab.com/ee/api/README.html#personal-access-tokens
     * https://gitlab.msu.edu/help/integration/oauth_provider.md
     *
     * @return HttpHeaders for authentication
     */
    private HttpHeaders createAuthHeaders(ScanRequest scanRequest) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        httpHeaders.set("PRIVATE-TOKEN", scmConfigOverrider.determineConfigToken(properties, scanRequest.getScmInstance()));
        httpHeaders.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        return httpHeaders;
    }

    private static String getNextURIFromHeaders(HttpHeaders headers, final String headerName, final String rel) {
        if (headerName == null) {
            return null;
        }
        if (headers == null || headers.get(headerName) == null) {
            return null;
        }
        String linkHeader = Objects.requireNonNull(headers.get(headerName)).get(0);
        String uriWithSpecifiedRel = null;
        final String[] links = linkHeader.split(", ");
        String linkRelation;
        for (final String link : links) {
            final int positionOfSeparator = link.indexOf(';');
            linkRelation = link.substring(positionOfSeparator + 1).trim();
            if (extractTypeOfRelation(linkRelation).equals(rel)) {
                uriWithSpecifiedRel = link.substring(1, positionOfSeparator - 1);
                break;
            }
        }

        return uriWithSpecifiedRel;
    }

    private static String extractTypeOfRelation(final String linkRelation) {
        int positionOfEquals = linkRelation.indexOf('=');
        return linkRelation.substring(positionOfEquals + 2, linkRelation.length() - 1).trim();
    }

}
