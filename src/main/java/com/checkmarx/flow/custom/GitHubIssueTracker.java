package com.checkmarx.flow.custom;

import com.checkmarx.flow.config.FindingSeverity;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.config.ScmConfigOverrider;
import com.checkmarx.flow.constants.FlowConstants;
import com.checkmarx.flow.dto.Issue;
import com.checkmarx.flow.dto.LabelField;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.github.LabelsItem;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.exception.MachinaRuntimeException;
import com.checkmarx.flow.service.GitHubService;
import com.checkmarx.flow.utils.HTMLHelper;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.dto.ScanResults;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.ThreadUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service("GitHub")
public class GitHubIssueTracker implements IssueTracker {

    private static final String TRANSITION_CLOSE = "closed";
    private static final String TRANSITION_OPEN = "open";
    private static final String ISSUES_PER_PAGE = "100";
    private static final Logger log = LoggerFactory.getLogger(GitHubIssueTracker.class);

    private final RestTemplate restTemplate;
    private final GitHubProperties properties;
    private final FlowProperties flowProperties;
    private final ScmConfigOverrider scmConfigOverrider;
    private final GitHubService gitHubService;




    public GitHubIssueTracker(@Qualifier("flowRestTemplate") RestTemplate restTemplate, GitHubProperties properties, FlowProperties flowProperties,
                              ScmConfigOverrider scmConfigOverrider, GitHubService gitHubService) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.flowProperties = flowProperties;
        this.scmConfigOverrider = scmConfigOverrider;
        this.gitHubService = gitHubService;
    }

    @Override
    public void init(ScanRequest request, ScanResults results) throws MachinaException {
        log.info("======== Initializing GitHub processing ========");
        if(ScanUtils.empty(request.getNamespace()) ||
                ScanUtils.empty(request.getRepoName()) ||
                ScanUtils.empty(request.getBranch())){
            throw new MachinaException("Namespace / RepoName / Branch are required");
        }

        if (ScanUtils.empty(scmConfigOverrider.determineConfigApiUrl(properties, request))) {
            throw new MachinaException("GitHub API Url must be provided in property config");
        }
    }

    /**
     * Get all issues for a GitHub repository
     *
     * @return List of GitHub Issues
     * @ full name (owner/repo format)
     */
    @Override
    public List<Issue> getIssues(ScanRequest request) {
        String apiUrl = String.format("%s/%s/%s/issues?state=all&per_page=%s",
                scmConfigOverrider.determineConfigApiUrl(properties, request),
                request.getNamespace(),
                request.getRepoName(),
                ISSUES_PER_PAGE);

        log.info("Executing getIssues GitHub API call: {}", apiUrl);
        List<Issue> issues = new ArrayList<>();
        HttpEntity<?> httpEntity = new HttpEntity<>(gitHubService.createAuthHeaders(request));
        ResponseEntity<com.checkmarx.flow.dto.github.Issue[]> response = null;
        try {
            response = restTemplate.exchange(apiUrl,
                    HttpMethod.GET, httpEntity, com.checkmarx.flow.dto.github.Issue[].class);
        } catch (HttpClientErrorException e) {
            log.error("Error occurred while getting Github issue. http error {} ", e.getStatusCode(), e);
            log.debug(ExceptionUtils.getStackTrace(e));
        }
        if (response.getBody() == null) {
            log.info("No issues found.");
            return new ArrayList<>();
        }

        for (com.checkmarx.flow.dto.github.Issue issue : response.getBody()) {
            Issue i = mapToIssue(issue);
            if (i != null && i.getTitle().startsWith(request.getProduct().getProduct())) {
                issues.add(i);
            }
        }

        String next = getNextURIFromHeaders(response.getHeaders(), "link", "next");
        while (next != null) {
            log.debug("Getting issue from {}", next);
            ResponseEntity<com.checkmarx.flow.dto.github.Issue[]> responsePage = null;
            try {
                responsePage = restTemplate.exchange(next, HttpMethod.GET,
                        httpEntity, com.checkmarx.flow.dto.github.Issue[].class);
            } catch (HttpClientErrorException e) {
                log.error("Error occurred while getting Github issue. http error {} ", e.getStatusCode(), e);
                log.debug(ExceptionUtils.getStackTrace(e));
            }
                    mapIssues(request, issues, responsePage);
            next = getNextURIFromHeaders(responsePage.getHeaders(), "link", "next");
        }
        return issues;
    }

    private void mapIssues(ScanRequest request, List<Issue> issues, ResponseEntity<com.checkmarx.flow.dto.github.Issue[]> responsePage) {
        if (responsePage.getBody() != null) {
            for (com.checkmarx.flow.dto.github.Issue issue : responsePage.getBody()) {
                Issue i = mapToIssue(issue);
                if (i != null && i.getTitle().startsWith(request.getProduct().getProduct())) {
                    issues.add(i);
                }
            }
        }
    }


    private Issue mapToIssue(com.checkmarx.flow.dto.github.Issue issue){
        if(issue == null){
            return null;
        }
        Issue i = new Issue();
        i.setBody(issue.getBody());
        i.setTitle(issue.getTitle());
        i.setId(String.valueOf(issue.getId()));
        List<String> labels = new ArrayList<>();
        for(LabelsItem l: issue.getLabels()){
            labels.add(l.getName());
        }
        i.setLabels(labels);
        i.setUrl(issue.getUrl());
        i.setState(issue.getState());
        return i;
    }

    /**
     * Retrieve DTO representation of GitHub Issue
     *
     * @param issueUrl URL for specific GitHub Issue
     * @return GitHub Issue
     */
    private Issue getIssue(String issueUrl, ScanRequest scanRequest) {
        log.info("Executing getIssue GitHub API call");
        HttpEntity<Object> httpEntity = new HttpEntity<>(gitHubService.createAuthHeaders(scanRequest));
        ResponseEntity<com.checkmarx.flow.dto.github.Issue> response = null;
        try {
            response = restTemplate.exchange(issueUrl, HttpMethod.GET, httpEntity, com.checkmarx.flow.dto.github.Issue.class);
        } catch (HttpClientErrorException e) {
            log.error("Error occurred while getting Github issue. http error {} ", e.getStatusCode(), e);
            log.debug(ExceptionUtils.getStackTrace(e));
        }
        return mapToIssue(response.getBody());
    }

    /**
     * Add a comment to an existing GitHub Issue
     *
     * @param issueUrl URL for specific GitHub Issue
     * @param comment  Comment to append to the GitHub Issue
     */
    private void addComment(String issueUrl, String comment, ScanRequest scanRequest) {
        log.debug("Executing add comment GitHub API call with following comment {}", comment);
        HttpEntity<String> httpEntity = new HttpEntity<>(getJSONComment(comment).toString(), gitHubService.createAuthHeaders(scanRequest));
        try {
            restTemplate.exchange(issueUrl.concat("/comments"), HttpMethod.POST, httpEntity, String.class);
        } catch (HttpClientErrorException e) {
            log.error("Error occurred while adding comment. http error {} ", e.getStatusCode(), e);
            log.debug(ExceptionUtils.getStackTrace(e));
        }
    }

    @Override
    public Issue createIssue(ScanResults.XIssue resultIssue, ScanRequest request) {
        log.debug("Executing createIssue GitHub API call");
        String apiUrl = scmConfigOverrider.determineConfigApiUrl(properties, request)
                .concat("/").concat(request.getNamespace()
                .concat("/").concat(request.getRepoName()))
                .concat("/issues");
        ResponseEntity<com.checkmarx.flow.dto.github.Issue> response;
        try {
            HttpEntity<String> httpEntity = new HttpEntity<>(getJSONCreateIssue(resultIssue, request).toString(),
                    gitHubService.createAuthHeaders(request));
            if(properties.getMaxDelay()>=3)
            {
                sleepToPreventRateLimitError();
            }
            else
            {
                log.debug("Please provide max delay of minimum 3 sec");
            }
            response = restTemplate.exchange(apiUrl, HttpMethod.POST, httpEntity, com.checkmarx.flow.dto.github.Issue.class);
        } catch (HttpClientErrorException e) {
            log.error("Error occurred while creating GitHub Issue", e);
            log.debug(ExceptionUtils.getStackTrace(e));
            if (e.getStatusCode().equals(HttpStatus.GONE)) {
                log.error("Issues are not enabled for this repository");
            }
            throw new MachinaRuntimeException(e);
        }
        return mapToIssue(response.getBody());
    }

    @Override
    public void closeIssue(Issue issue, ScanRequest request) throws MachinaException {
        log.info("Executing closeIssue GitHub API call");
        HttpEntity<String> httpEntity = new HttpEntity<>(getJSONCloseIssue().toString(), gitHubService.createAuthHeaders(request));
        try {
            restTemplate.exchange(issue.getUrl(), HttpMethod.POST, httpEntity, Issue.class);
        } catch (HttpClientErrorException e) {
            log.error("Error occurred while closing Github issue. http error {} ", e.getStatusCode(), e);
            log.debug(ExceptionUtils.getStackTrace(e));
        }
    }

    @Override
    public Issue updateIssue(Issue issue, ScanResults.XIssue resultIssue, ScanRequest request) throws MachinaException {
        log.info("Executing updateIssue GitHub API call");
        HttpEntity<String> httpEntity = new HttpEntity<>(getJSONUpdateIssue(resultIssue, request).toString(),
                gitHubService.createAuthHeaders(request));
        ResponseEntity<com.checkmarx.flow.dto.github.Issue> response;
        try {
            if(properties.getMaxDelay()>=3)
            {
                sleepToPreventRateLimitError();
            }
            else
            {
                log.debug("Please provide max delay of minimum 3 sec");
            }
            response = restTemplate.exchange(issue.getUrl(), HttpMethod.POST, httpEntity, com.checkmarx.flow.dto.github.Issue.class);
            this.addComment(Objects.requireNonNull(response.getBody()).getUrl(),"Issue still exists.", request);
            return mapToIssue(response.getBody());
        } catch (HttpClientErrorException e) {
            handleIssueUpdateError(e);
            this.addComment(issue.getUrl(), "This issue still exists.  Please add label 'false-positive' to remove from scope of SAST results", request);
            log.debug(ExceptionUtils.getStackTrace(e));
        }
        return this.getIssue(issue.getUrl(), request);
    }

    private void handleIssueUpdateError(HttpClientErrorException e) {
        log.error("Error updating issue. This is likely due to the fact that another user has closed this issue. Adding comment", e);
        boolean isForbidden = e.getStatusCode().equals(HttpStatus.FORBIDDEN);
        if (isForbidden) {
            log.error("Please check the scopes of your personal access token and make sure you have the necessary permissions for the repo.");
        }
        if (e.getStatusCode().equals(HttpStatus.GONE) || isForbidden) {
            throw new MachinaRuntimeException(e);
        }
    }
    private void sleepToPreventRateLimitError() {
        try {
            log.debug("delay for {} sec",properties.getMaxDelay());
            ThreadUtils.sleep(Duration.ofSeconds(properties.getMaxDelay()));
        } catch (InterruptedException e) {
            log.error("Interrupted between issue creation requests", e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Create JSON http request body for an create/update Issue POST request to GitHub
     *
     */
    private JSONObject getJSONUpdateIssue(ScanResults.XIssue resultIssue, ScanRequest request) {
        JSONObject requestBody = new JSONObject();
        log.debug("max description length {}",properties.getMaxDescriptionLength());
        String fileUrl = ScanUtils.getFileUrl(request, resultIssue.getFilename());
        String body = HTMLHelper.getMDBody(resultIssue, request.getBranch(), fileUrl, flowProperties,properties.getMaxDescriptionLength());
        String title = getXIssueKey(resultIssue, request);
        String[] label  = getString(request,resultIssue);


        try {
            requestBody.put("title", title);
            requestBody.put("body", body);
            requestBody.put("state", TRANSITION_OPEN);
            if(label.length>0) {
                requestBody.put("labels", label);
            }
        } catch (JSONException e) {
            log.error("Error creating JSON Update Object - JSON object will be empty", e);
        }
        return requestBody;
    }

    /**
     * Create JSON http request body for an create/update Issue POST request to GitHub
     *
     * @return JSON Object of create issue request
     */
    private JSONObject getJSONCreateIssue(ScanResults.XIssue resultIssue, ScanRequest request) {
        JSONObject requestBody = new JSONObject();
        log.debug("max description length {}",properties.getMaxDescriptionLength());
        String fileUrl = ScanUtils.getFileUrl(request, resultIssue.getFilename());
        String body = HTMLHelper.getMDBody(resultIssue, request.getBranch(), fileUrl, flowProperties,properties.getMaxDescriptionLength());
        String title = HTMLHelper.getScanRequestIssueKeyWithDefaultProductValue(request, this, resultIssue);
        String[] label = getString(request,resultIssue);

        try {
            requestBody.put("title", title);
            requestBody.put("body", body);
            if(label.length>0) {
                requestBody.put("labels", label);
            }

        } catch (JSONException e) {
            log.error("Error creating JSON Create Issue Object - JSON Object will be empty", e);
        }
        return requestBody;
    }

    private String[] getString(ScanRequest request, ScanResults.XIssue issue) {
        List<String> value = new ArrayList<>();
        String[] strArray = new String[]{};
        try {
            if(properties.getIssueslabel()!=null){
                Map<FindingSeverity, String> findingsPerSeverity = properties.getIssueslabel();
                for (Map.Entry<FindingSeverity, String> entry : findingsPerSeverity.entrySet()) {
                    if (issue.getSeverity().equalsIgnoreCase(entry.getKey().toString()) || issue.getSeverity().toLowerCase(Locale.ROOT).contains(entry.getKey().toString().toLowerCase(Locale.ROOT))) {
//converting using String.split() method with whitespace as a delimiter
                        value=Arrays.stream(entry.getValue().split(",")).collect(Collectors.toList());
                        break;
                    }
                }
            }

                //adding fields in labels
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
                strArray = value.toArray(new String[0]);
            return strArray;
        } catch (Exception e) {
            return strArray;
        }
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
        return ScanUtils.isSAST(issue)
                ? getSastIssueKey(issue, request)
                : ScanUtils.getScaSummaryIssueKey(request, issue);
    }

    private String getSastIssueKey(ScanResults.XIssue issue, ScanRequest request) {
        return flowProperties.isTrackApplicationOnly() || ScanUtils.empty(request.getBranch())
                ? String.format(ScanUtils.ISSUE_TITLE_KEY, request.getProduct().getProduct(), issue.getVulnerability(), issue.getFilename())
                : String.format(ScanUtils.ISSUE_TITLE_KEY_WITH_BRANCH, request.getProduct().getProduct(), issue.getVulnerability(), issue.getFilename(), request.getBranch());
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

    @Override
    public void complete(ScanRequest request, ScanResults results) throws MachinaException {
        log.info("======== Finalizing GitHub Processing ========");
    }

    /**
     * Create JSON http request body for adding a comment to an Issue in GitHub
     *
     * @param comment Comment to append to an issue
     * @return JSON Object for comment request
     */
    private JSONObject getJSONComment(String comment) {
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("body", comment);
        } catch (JSONException e) {
            log.error("Error creating JSON Comment Object - JSON object will be empty", e);
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
            log.error("Error creating JSON Close Issue Object - JSON object will be empty", e);
        }
        return requestBody;
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
            linkRelation = link.substring(positionOfSeparator + 1, link.length()).trim();
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
