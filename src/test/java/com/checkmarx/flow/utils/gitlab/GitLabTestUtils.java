package com.checkmarx.flow.utils.gitlab;

import com.checkmarx.flow.config.GitLabProperties;
import com.checkmarx.flow.dto.Issue;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@TestComponent
public class GitLabTestUtils {

    private GitLabProperties gitLabProperties;
    private static final String GET_PROJECT_URL = "/projects?search={name}";
    private static final String GET_ISSUES_URL = "/projects/{id}/issues/";
    private static final String DELETE_ISSUE_URL = "/projects/{id}/issues/{issueId}";
    private final RestTemplate restTemplate = new RestTemplate();
    protected final String PRIVATE_TOKEN_HEADER = "PRIVATE-TOKEN";

    public GitLabTestUtils(GitLabProperties properties){
        gitLabProperties = properties;
    }
    public List<Issue> getAllProjectIssues(String projectName){
        int projectId = getProjectId(projectName);
        return getAllProjectIssues(projectId);
    }

    public void deleteAllProjectIssues(String projectName) {
        int projectId = getProjectId(projectName);

        List<Issue> issues = getAllProjectIssues(projectId);
        log.info("going to delete {} issues from gitlab project", issues.size());

        for (int i=0; i < issues.size(); i++){
            String issueIid = issues.get(i).getId();
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

    private int getProjectId(String projectName)
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

    private List<Issue> getAllProjectIssues(int projectId){
        List<Issue> issues = new ArrayList<>();
        String getProjectsUrl = String.format("%s%s", gitLabProperties.getApiUrl(), GET_ISSUES_URL);
        HttpEntity<String> httpEntity = new HttpEntity<>(getHeaders());

        ResponseEntity<com.checkmarx.flow.dto.gitlab.Issue[]> response = restTemplate.exchange(getProjectsUrl, HttpMethod.GET, httpEntity, com.checkmarx.flow.dto.gitlab.Issue[].class, projectId);
        log.info("Found {} issues in project" , issues.size());

        for(com.checkmarx.flow.dto.gitlab.Issue issue: response.getBody()){
            Issue i = mapToIssue(issue);
            if (i != null) {
                issues.add(i);
            }
        }

        return issues;
    }

    private Issue mapToIssue(com.checkmarx.flow.dto.gitlab.Issue issue){
        if(issue == null){
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
}
