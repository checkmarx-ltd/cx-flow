package com.checkmarx.flow.cucumber.integration.end2end.genericendtoend;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.SearchRestClient;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.internal.async.CustomAsynchronousJiraRestClientFactory;
import com.checkmarx.flow.config.properties.GitLabProperties;
import com.checkmarx.flow.config.properties.JiraProperties;

import com.checkmarx.sdk.config.CxGoProperties;
import com.checkmarx.sdk.config.CxProperties;
import io.atlassian.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

@Slf4j
enum BugTracker {
    JIRA {
        private JiraProperties jiraProperties;
        private JiraRestClient client;
        private SearchRestClient searchClient;
        private String jqlQuery;


        @Override
        void init(GenericEndToEndSteps genericEndToEndSteps) {
            jiraProperties = genericEndToEndSteps.jiraProperties;
            CustomAsynchronousJiraRestClientFactory factory = new CustomAsynchronousJiraRestClientFactory();
            URI jiraURI;
            try {
                log.info(jiraProperties.getUrl());
                jiraURI = new URI(jiraProperties.getUrl());
            } catch (URISyntaxException e) {
                fail("Error constructing URI for JIRA");
                jiraURI = null;
            }
            client = factory.createWithBasicHttpAuthenticationCustom(jiraURI, jiraProperties.getUsername(),
                    jiraProperties.getToken(), jiraProperties.getHttpTimeout());
            searchClient = client.getSearchClient();
        }

        @Override
        void verifyIssueCreated(String severities, String engine) {
            log.info("JIRA Project: {}",jiraProperties.getProject());
            jqlQuery =  String.format("project = %s", jiraProperties.getProject());
            log.info("JQL Query before: {}",jqlQuery);
            jqlQuery =  (CxProperties.CONFIG_PREFIX.equalsIgnoreCase(engine) || CxGoProperties.CONFIG_PREFIX.equalsIgnoreCase(engine))
                    ? String.format("project = %s and priority  in %s", jiraProperties.getProject(), severities)
                    : String.format("project = %s and summary ~\"CVE-?\"", jiraProperties.getProject());
            log.info("JQL Query finished: {}",jqlQuery);
            log.info("filtering issue with jql: {}", jqlQuery);
            Set<String> fields = new HashSet<>();
            fields.addAll(
                    Arrays.asList("key", "project", "issuetype", "summary", "labels", "created", "updated", "status"));
            SearchResult result = null;
            boolean found = false;
            for (int retries = 0; retries < NUMBER_OF_RETRIES; retries++) {
                Promise<SearchResult> temp = searchClient.searchJql(jqlQuery, 10, 0, fields);
                try {
                    TimeUnit.SECONDS.sleep(RETRY_TIMEOUT_IN_SECONDS);
                    log.info("checking for issues in jira project '{}' starting attempt {}", jiraProperties.getProject(), retries + 1);
                } catch (Exception e) {
                    log.warn("error in jira verifyIssueCreated loop: {}", e.getMessage());
                }
                try {
                    result = temp.get(500, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    log.info("failed attempt {}", retries + 1);
                }

                if (result != null && result.getTotal() > 0) {
                    log.info("Found {} issues in jira project", result.getTotal());
                    found = true;
                    break;
                }
            }

            if (!found) {
                String msg = "failed to find update in Jira after expected time";
                log.error(msg);
                fail(msg);
            }
        }

        @Override
        void deleteIssues() {
            try {
                Set<String> fields = new HashSet<>();
                fields.addAll(
                        Arrays.asList("key", "project", "issuetype", "summary", "labels", "created", "updated", "status"));
                Promise<SearchResult> temp = searchClient.searchJql(jqlQuery, 10, 0, fields);
                SearchResult result = temp.get(500, TimeUnit.MILLISECONDS);

                boolean isfound = false;
                for (Issue currentIssue : result.getIssues()) {
                    isfound = true;
                    client.getIssueClient().deleteIssue(currentIssue.getKey(), false);
                }
                if (isfound) {
                    try {
                        TimeUnit.SECONDS.sleep(5);
                    } catch (Exception e) {
                        Thread.currentThread().interrupt();
                    }
                    deleteIssues();
                }
            } catch (Exception e) {
                log.error("Failed to clean tickets from Jira {}", e.getMessage());
            }
        }
    },
    GitLab{
        private static final String GET_PROJECT_URL = "/projects?search={name}";
        private static final String GET_ISSUES_URL = "/projects/{id}/issues/";
        private static final String DELETE_ISSUE_URL = "/projects/{id}/issues/{issueId}";

        private final RestTemplate restTemplate = new RestTemplate();
        private String projectName;
        private  Integer projectId;
        private  GitLabProperties gitLabProperties;
        @Override
        void init(GenericEndToEndSteps genericEndToEndSteps) {
            gitLabProperties = genericEndToEndSteps.gitLabProperties;
            projectName = genericEndToEndSteps.getRepository().getRepoName();
            projectId = getProjectId();
        }

        @Override
        void verifyIssueCreated(String severities, String engine) {
            boolean foundIssueInBugTracker = false;
            for (int retries = 0; retries < NUMBER_OF_RETRIES; retries++) {
                try {
                    TimeUnit.SECONDS.sleep(RETRY_TIMEOUT_IN_SECONDS);
                    log.info("checking for issues in gitlab project {} starting attempt {}", projectId, retries + 1);
                } catch (Exception e) {
                    log.error("error in timeout while verifying created issues: {}", e.getMessage());
                }
                JSONArray issuesList = getAllProjectIssues();
                if (issuesList.length() > 0){
                    log.info("successfully verified created issues!");
                    foundIssueInBugTracker = true;
                    break;
                }
            }
            assertTrue(foundIssueInBugTracker, "didn't find any security issues in gitlab project %d after %d retries");
        }

        @Override
        void deleteIssues() {
            JSONArray issuesArray = getAllProjectIssues();
            log.info("going to delete {} issues from gitlab project", issuesArray.length());

            for (int i=0; i < issuesArray.length(); i++){
                JSONObject issue = issuesArray.getJSONObject(i);
                int issueIid = issue.getInt("iid");
                String getProjectsUrl = String.format("%s%s", gitLabProperties.getApiUrl(), DELETE_ISSUE_URL);
                HttpEntity<String> httpEntity = new HttpEntity<>(getHeaders());
                ResponseEntity<String> response = restTemplate.exchange(getProjectsUrl, HttpMethod.DELETE, httpEntity, String.class, projectId, issueIid);

                if (response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
                    log.info("deleted issue iid {} successfully", issueIid);
                }
                else{
                    log.warn("failed to delete issue {}. response status: {}", issueIid, response.getStatusCode());
                }
            }
        }

        private int getProjectId()
        {
            String getProjectsUrl = String.format("%s%s", gitLabProperties.getApiUrl(), GET_PROJECT_URL);
            HttpEntity<String> httpEntity = new HttpEntity<>(getHeaders());
            ResponseEntity<String> response = restTemplate.exchange(getProjectsUrl, HttpMethod.GET, httpEntity, String.class, projectName);

            JSONArray projects = new JSONArray(response.getBody());
            JSONObject gitlabProject = projects.getJSONObject(0);

            return  gitlabProject.getInt("id");
        }

        HttpHeaders getHeaders() {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            httpHeaders.set(PRIVATE_TOKEN_HEADER, gitLabProperties.getToken());
            httpHeaders.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
            return httpHeaders;
        }

        private JSONArray getAllProjectIssues(){
            String getProjectsUrl = String.format("%s%s", gitLabProperties.getApiUrl(), GET_ISSUES_URL);
            HttpEntity<String> httpEntity = new HttpEntity<>(getHeaders());

            ResponseEntity<String> response = restTemplate.exchange(getProjectsUrl, HttpMethod.GET, httpEntity, String.class, projectId);
            JSONArray issues = new JSONArray(response.getBody());
            log.info("Found {} issues in project" , issues.length());
            return issues;
        }
    };

    protected static Integer NUMBER_OF_RETRIES = 24;
    protected static Integer RETRY_TIMEOUT_IN_SECONDS = 5;
    protected final String PRIVATE_TOKEN_HEADER = "PRIVATE-TOKEN";
    static BugTracker setTo(String bugTracker, GenericEndToEndSteps genericEndToEndSteps) {
        log.info("setting bug-tracker to {}", bugTracker);
        BugTracker bt = valueOf(bugTracker);
        bt.init(genericEndToEndSteps);
        return bt;
    }

    abstract void verifyIssueCreated(String severities, String engine);

    abstract void deleteIssues();

    abstract void init(GenericEndToEndSteps genericEndToEndSteps);
}