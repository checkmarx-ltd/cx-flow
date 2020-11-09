package com.checkmarx.jira;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.BasicProject;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueType;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.internal.async.CustomAsynchronousJiraRestClientFactory;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.dto.Filter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;


@TestComponent
public class JiraTestUtils implements IJiraTestUtils {
    private static final Logger log = LoggerFactory.getLogger(JiraTestUtils.class);

    private static final String JIRA_DESCRIPTION_FINDING_LINE = "[Line #";

    private JiraRestClient client;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private JiraProperties jiraProperties;

    @PostConstruct
    public void initClient() {
        if (jiraProperties != null && !ScanUtils.empty(jiraProperties.getUrl())) {
            CustomAsynchronousJiraRestClientFactory factory = new CustomAsynchronousJiraRestClientFactory();
            URI jiraURI = null;
            try {
                jiraURI = new URI(jiraProperties.getUrl());
            } catch (URISyntaxException e) {
                log.error("Error constructing URI for JIRA", e);
            }
            this.client = factory.createWithBasicHttpAuthenticationCustom(jiraURI, jiraProperties.getUsername(), jiraProperties.getToken(), jiraProperties.getHttpTimeout());

        }
    }

    @Override
    public void deleteIssue(String issueKey) {
        client.getIssueClient().deleteIssue(issueKey, true).claim();
    }

    private SearchResult search(String jql) {
        return  client.getSearchClient().searchJql(jql).claim();
    }

    private SearchResult search(String jql, int startAtIndex) {
        return  client.getSearchClient().searchJql(jql, null, startAtIndex, null).claim();
    }

    @Override
    public void cleanProject(String projectKey) {
        Set<Issue> issues = geAllIssuesInProject(projectKey);
        for (Issue issue: issues) {
            deleteIssue(issue.getKey());
        }
    }

    @Override
    public int getNumberOfIssuesInProject(String projectKey) {
        return geAllIssuesInProject(projectKey).size();
    }

    private Set<Issue> geAllIssuesInProject(String projectKey) {
        List<Issue> issues = new ArrayList<>();
        SearchResult searchResult = search(getSearchAllProjectJql(projectKey), issues.size());
        searchResult.getIssues().forEach(issues::add);
        while (issues.size() < searchResult.getTotal()) {
            searchResult = search(getSearchAllProjectJql(projectKey), issues.size());
            searchResult.getIssues().forEach(issues::add);
        }
        Set<Issue> result = new HashSet<>(issues);
        log.info("found {} issues in project '{}'", issues.size(), projectKey);
        return result;
    }

    @Override
    public Map<Filter.Severity, Integer> getIssuesPerSeverity(String projectKey) {
        Map<Filter.Severity, Integer> result= new HashMap<>();
        SearchResult searchResults = searchForAllIssues(projectKey);
        for (Issue issue: searchResults.getIssues()) {
            String severity = getIssueSeverity(issue.getDescription());
            if (severity == null) {
                continue;
            }

            Filter.Severity filterSeverity = null;

            // iterate over enums using for loop 
            for (Filter.Severity s : Filter.Severity.values()) {
                log.debug("Comparing Filter Severity: '" + s.name() + "' to  '" + severity + "'\n");
                if(s.name().trim().equalsIgnoreCase(severity.trim())){
                    filterSeverity = s;
                }
            }

            //Filter.Severity filterSeverity = Filter.Severity.valueOf(severity.toUpperCase());
            if (filterSeverity!=null && result.containsKey(filterSeverity)) {
                result.put(filterSeverity,result.get(filterSeverity) + 1 );
            } else {
                result.put(filterSeverity, 1);
            }
        }
        return result;
    }

    private String getIssueSeverity(String issueDescription) {
        return getIssueBodyPart(issueDescription,"Severity:");
    }

    @Override
    public int getNumberOfVulnerabilites(String projectKey) {
        Set<Issue> isseus = geAllIssuesInProject(projectKey);
        int total = 0;
        for (Issue issue: isseus) {
            int vulNum =  getVulnerabilitesCount(issue);
            total += vulNum == 0 ? 1 : vulNum;
        }
        return total;
    }


    private String getIssueBodyPart(String issueDescription, String field) {
        // HEre's an example for Issue descriptionm for reference:
/*
Angular_Client_DOM_XSS issue exists @ jrecruiter/jrecruiter-flex/html-template/history/history.js in branch master
Namespace: compTest
Repository: repo
Branch: master
Repository Url: http://localhost/repo.git
Application: App1
Cx-Project: CodeInjection1
Cx-Team: CxServer
Severity: High
CWE: 79

Addition Info

Checkmarx
Mitre Details
Training
Guidance
Lines: 222

Line #222:
*/
        String[] lines = issueDescription.split(System.lineSeparator());
        for (String line: lines) {
            if (line.contains(field)) {
                return line.split(" ")[1];
            }
        }
        return null;
    }

    @Override
    public String getIssueFilename(String projectKey) {
        Issue issue = getFirstIssue(projectKey);
        String firstLine = issue.getDescription().split(System.lineSeparator())[0];
        String[] firstLineParts= firstLine.split(" ");
        return firstLineParts[4];
    }

    @Override
    public String getIssueVulnerability(String projectKey) {
        Issue issue = getFirstIssue(projectKey);
        String firstLine = issue.getDescription().split(System.lineSeparator())[0];
        String[] firstLineParts= firstLine.split(" ");
        return firstLineParts[0];
    }

    @Override
    public String getIssueVulnerabilityStatus(String projectKey) {
        Issue issue = getFirstIssue(projectKey);
        String statusLine = issue.getDescription().split(System.lineSeparator())[13];
        return statusLine;
    }

    @Override
    public String getIssueRecommendedFixLink(String projectKey) {
        Issue issue = getFirstIssue(projectKey);
        return  Objects.requireNonNull(issue.getDescription()).split(System.lineSeparator())[21];
    }

    @Override
    public int getFirstIssueNumOfFindings(String projectKey) {
        SearchResult result = searchForAllIssues(projectKey);
        if (result.getTotal() ==0) {
            return 0;
        }
        Issue issue = result.getIssues().iterator().next();
        return getVulnerabilitesCount(issue);
    }

    private int getVulnerabilitesCount(Issue issue) {
        int lastIndex = 0;
        int count = 0;
        while (lastIndex != -1) {
            lastIndex = issue.getDescription().indexOf(JIRA_DESCRIPTION_FINDING_LINE, lastIndex);
            if (lastIndex != -1) {
                count++;
                lastIndex += JIRA_DESCRIPTION_FINDING_LINE.length();
            }
        }
        return count;
    }

    @Override
    public void ensureProjectExists(String key) throws IOException {
        log.info("Making sure '{}' project exists in Jira.", key);
        ResourceCreationConfig config = getProjectCreationConfig(key);
        if (isProjectExits(key)) {
            log.info("Project {} already exists.", key);
            return;
        }
        ceateResource(config);
    }

    private boolean isIssueTypeExits(String issueType) {
        Iterable<IssueType> issueTypes =  client.getMetadataClient().getIssueTypes().claim();
        for (IssueType it: issueTypes) {
            if (it.getName().equals(issueType)) {
                return true;
            }
        }
        return false;
    }

    private boolean isProjectExits(String issueType) {
        Iterable<BasicProject> projects =  client.getProjectClient().getAllProjects().claim();
        for (BasicProject bp: projects) {
            if (bp.getKey().equals(issueType)) {
                return true;
            }
        }
        return false;
    }


    @Override
    public void ensureIssueTypeExists(String issueType) throws IOException {
        log.info("Making sure '{}' issue type exists in Jira.", issueType);
        if (isIssueTypeExits(issueType)) {
            log.info("Issue Type {} already exists", issueType);
            return;
        }
        ResourceCreationConfig config = getIssueCreationConfig(issueType);
        ceateResource(config);
    }

    @Override
    public String getIssuePriority(String projectKey) {
        Issue issue = getFirstIssue(projectKey);
        return issue.getPriority().getName();
    }

    @Override
    public Long getIssueUpdatedTime(String projectKey) {
        Issue issue = getFirstIssue(projectKey);
        return issue.getUpdateDate().getMillis();
    }

    @Override
    public String getIssueStatus(String projectKey) {
        Issue issue = getFirstIssue(projectKey);
        return issue.getStatus().getName();
    }

    @Override
    public Long getFirstIssueId(String projectKey) {
        Issue issue = getFirstIssue(projectKey);
        return issue.getId();
    }

    @Override
    public Map<String, Integer> getIssuesByStatus(String projectKey) {
        Map<String, Integer> result = new HashMap<>();
        SearchResult searchResults = searchForAllIssues(projectKey);
        for (Issue issue: searchResults.getIssues()) {
            if (result.containsKey(issue.getStatus().getName())) {
                result.put(issue.getStatus().getName(), result.get(issue.getStatus().getName()) + 1);
            } else {
                result.put(issue.getStatus().getName(), 1);
            }
        }
        return result;
    }

    private ResourceCreationConfig getIssueCreationConfig(String issueType) {
        ResourceCreationConfig config = new ResourceCreationConfig();
        config.body = getIssueTypeRequestBody(issueType);
        config.resourceName = "issuetype";
        config.expectedErrorStatus = HttpStatus.CONFLICT;
        config.errorFieldName = "name";
        config.errorFieldValue = "An issue type with this name already exists.";
        return config;
    }

    private ResourceCreationConfig getProjectCreationConfig(String key) throws JsonProcessingException {
        ResourceCreationConfig config = new ResourceCreationConfig();
        config.body = getProjectRequestBody(key);
        config.resourceName = "project";
        config.expectedErrorStatus = HttpStatus.BAD_REQUEST;
        config.errorFieldName = "projectName";
        config.errorFieldValue = "A project with that name already exists.";
        return config;
    }

    private void ceateResource(ResourceCreationConfig config)
            throws IOException {
        boolean createdSuccessfully = false;
        ResponseEntity<JsonNode> response = sendCreationRequest(config);
        createdSuccessfully = isResourceCreatedSuccessfully(response);
        if (createdSuccessfully) {
            log.info("{} created successfully.", config.resourceName);
        } else  {
            throw new JiraUtilsException("Unable to create " + config.resourceName);
        }
    }

    private ObjectNode getIssueTypeRequestBody(String issueType) {
        return objectMapper.createObjectNode()
                .put("name", issueType)
                .put("type", "standard");
    }

    private boolean isResourceCreatedSuccessfully(ResponseEntity<JsonNode> response) {
        return response != null && response.getStatusCode() == HttpStatus.CREATED;
    }

    private boolean doesResourceAlreadyExist(Exception exception, ResourceCreationConfig config) {
        boolean result = false;
        if (exception instanceof HttpClientErrorException) {
            HttpClientErrorException clientException = (HttpClientErrorException) exception;
            result = clientException.getStatusCode() == config.expectedErrorStatus &&
                    containsExpectedErrorMessage(clientException, config);
        }
        return result;
    }

    private boolean containsExpectedErrorMessage(HttpClientErrorException exception, ResourceCreationConfig config) {
        boolean result = false;
        try {
            JsonNode response = objectMapper.readTree(exception.getResponseBodyAsByteArray());
            result = response.at("/errors/" + config.errorFieldName)
                    .asText()
                    // Checking text: less reliable, but saves us an additional API call.
                    .equals(config.errorFieldValue);

        } catch (IOException ex) { /* Ignored */ }
        return result;
    }

    private ResponseEntity<JsonNode> sendCreationRequest(ResourceCreationConfig config) {
        URI fullUri = null;
        try {
            fullUri = new URI(jiraProperties.getUrl())
                    .resolve("/rest/api/2/" + config.resourceName);
        } catch (URISyntaxException e) {
            throw new JiraUtilsException(String.format("Could not create resource: %s", config.resourceName), e);
        }

        HttpEntity<ObjectNode> request = new HttpEntity<>(config.body, config.headers);

        RestTemplate client = new RestTemplate();
        return client.exchange(fullUri, HttpMethod.POST, request, JsonNode.class);
    }

    private ObjectNode getProjectRequestBody(String key) throws JsonProcessingException {
        final String REQUEST_TEMPLATE = ("{" +
                " 'projectTypeKey': 'software'," +
                " 'projectTemplateKey': 'com.pyxis.greenhopper.jira:gh-scrum-template'," +
                " 'description': 'Automation'," +
                " 'leadAccountId': '5e3815a9e697e80e5b414719'," +
                " 'assigneeType': 'PROJECT_LEAD'," +
                " 'avatarId': 10200" +
                " }")
                .replace("'", "\"");

        ObjectNode body = (ObjectNode) objectMapper.readTree(REQUEST_TEMPLATE);

        body.put("key", key)
                .put("name", key);
        return body;
    }

    private HttpHeaders getHeaders() {
        String credentials = String.format("%s:%s", jiraProperties.getUsername(), jiraProperties.getToken());
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + encodedCredentials);
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        return headers;
    }

    // Added to avoid passing too many method args.
    private class ResourceCreationConfig {
        public final HttpHeaders headers = getHeaders();
        private ObjectNode body;
        private String resourceName;
        private String errorFieldName;
        private String errorFieldValue;
        private HttpStatus expectedErrorStatus;
    }

    private Issue getFirstIssue(String projectKey) {
        SearchResult result = searchForAllIssues(projectKey);
        if (result.getTotal() == 0) {
             throw new JiraUtilsException("No issues found in JIRA. At least one issue is expected");
        }
        return result.getIssues().iterator().next();
    }

    public SearchResult searchForAllIssues(String projectKey) {
        return search(getSearchAllProjectJql(projectKey));
    }

    private String getSearchAllProjectJql(String projectKey) {
        return String.format("project = \"%s\"", projectKey);
    }
}
