package com.checkmarx.flow.cucumber.integration.end2end.github2jira;

import io.cucumber.java.en.Given;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.SearchRestClient;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.internal.async.CustomAsynchronousJiraRestClientFactory;
import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.dto.BugTracker.Type;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.RestTemplate;

import io.atlassian.util.concurrent.Promise;
import io.cucumber.java.After;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

/**
 * Check cxflow end-2-end SAST flow between GitHub webhook and JIRA
 */
@SpringBootTest(classes = { CxFlowApplication.class })
public class GitHubToJiraSteps {

    private static final String srcFile = "GitHubToJiraSteps.src";

    private String COMMIT_FILE_PATH;
    private String REPO_HOOKS_BASE_URL;

    @Autowired
    private FlowProperties flowProperties;
    @Autowired
    private GitHubProperties gitHubProperties;
    @Autowired
    private JiraProperties jiraProperties;

    private URI jiraURI;
    private JiraRestClient client;
    private SearchRestClient searchClient;
    private List<String> issueCreatedKeys = new ArrayList<>();

    private Integer hookId = null;
    private String createdFileSha = null;
    private String Authorization = null;
    private String hookTargetURL = null;

    @PostConstruct
    public void init() {
        Properties properties = getProperties();
        String namespace = properties.getProperty("namespace");
        String repo = properties.getProperty("repo");
        String filePath = properties.getProperty("fileCreatePath");
        hookTargetURL = properties.getProperty("target");
        COMMIT_FILE_PATH = String.format("%s/%s/%s/contents/%s", gitHubProperties.getApiUrl(), namespace, repo,
                filePath);
        REPO_HOOKS_BASE_URL = String.format("%s/%s/%s/hooks", gitHubProperties.getApiUrl(), namespace, repo);

        CustomAsynchronousJiraRestClientFactory factory = new CustomAsynchronousJiraRestClientFactory();
        try {
            this.jiraURI = new URI(jiraProperties.getUrl());
        } catch (URISyntaxException e) {
            fail("Error constructing URI for JIRA");
        }
        client = factory.createWithBasicHttpAuthenticationCustom(jiraURI, jiraProperties.getUsername(),
                jiraProperties.getToken(), jiraProperties.getHttpTimeout());
        this.searchClient = client.getSearchClient();

    }

    @After
    public void cleanUp() {
        Optional.ofNullable(hookId).ifPresent(hookId -> {
            Authorization = "token " + gitHubProperties.getToken();
            deleteHook(hookId);
        });
        if (getStatus(COMMIT_FILE_PATH).equals(HttpStatus.OK)) {
            deleteFile(COMMIT_FILE_PATH);
        }
        for (String issueCreatedKey : issueCreatedKeys) {
            deleteIssue(issueCreatedKey);
        }
    }

    @Given("source is GitHub")
    public void setSourceToGithb() {
        Authorization = "token " + gitHubProperties.getToken();
    }

    @And("target is Jira")
    public void setTargetToJira() {
        flowProperties.setBugTracker(Type.JIRA.name());
    }

    @And("CxFlow is running as a service")
    public void runAsService() {
        SpringApplication.run(CxFlowApplication.class, new String[] { "--web" });
    }

    @And("webhook is configured for push event")
    public void generatePushWebHook() {
        final RestTemplate restTemplate = new RestTemplate();

        JSONArray hooks = getJSONArray(REPO_HOOKS_BASE_URL);

        if (!hooks.isEmpty()) {
            fail("repository alredy has hooks configured");
        }

        String data = null;
        try {
            data = generateHookData(hookTargetURL, gitHubProperties.getWebhookToken());
        } catch (Exception e) {
            fail("can not create web hook, check parameters");
        }

        final HttpHeaders headers = getHeaders();
        final HttpEntity<String> request = new HttpEntity<>(data, headers);
        try {
            final ResponseEntity<String> response = restTemplate.postForEntity(REPO_HOOKS_BASE_URL, request,
                    String.class);
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            hookId = new JSONObject(response.getBody()).getInt("id");
        } catch (Exception e) {
            fail("failed to create hook " + e.getMessage());
        }
    }

    @When("pushing a change")
    public void pushChange() {
        final RestTemplate restTemplate = new RestTemplate();

        String data = null;
        try {
            String content = getFileInBase64();
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode jo = mapper.createObjectNode();
            jo.put("message", "GitHubToJira test message");
            ObjectNode committer = mapper.createObjectNode();
            committer.put("name", "cxflowtestuser");
            committer.put("email", "cxflowtestuser@checkmarx.com");
            jo.set("committer", committer);
            jo.put("content", content);

            data = mapper.writeValueAsString(jo);
        } catch (Exception e) {
            fail("faild to create file for push");
        }
        final HttpHeaders headers = getHeaders();
        final HttpEntity<String> request = new HttpEntity<>(data, headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(COMMIT_FILE_PATH, HttpMethod.PUT, request,
                    String.class);
            createdFileSha = new JSONObject(response.getBody()).getJSONObject("content").getString("sha");
        } catch (Exception e) {
            fail("faild to push a file: " + e.getMessage());
        }
    }

    @Then("target issues are updated")
    public void validateScanStarted() {
        String severities = "(" + flowProperties.getFilterSeverity().stream().collect(Collectors.joining(",")) + ")";
        String jql = String.format("project = %s and priority  in %s", jiraProperties.getProject(), severities);
        HashSet<String> fields = new HashSet<String>();
        fields.addAll(
                Arrays.asList("key", "project", "issuetype", "summary", "labels", "created", "updated", "status"));

        SearchResult result = null;
        int retries = 0;
        do {
            if (++retries >= 20) {
                fail("failed to find update in Jira after expected time");
            }
            Promise<SearchResult> temp = searchClient.searchJql(jql, 10, 0, fields);
            try {
                result = temp.get(500, TimeUnit.MILLISECONDS);
                TimeUnit.SECONDS.sleep(5);
            } catch (Exception e) {
                result = null;
                continue;
            }

        } while (result == null || result.getTotal() == 0);

        Iterator<Issue> itr = result.getIssues().iterator();
        assertTrue(itr.hasNext(), "Jira is missing the issues");
        while (itr.hasNext()) {
            this.issueCreatedKeys.add(itr.next().getKey());
        }

    }

    private HttpHeaders getHeaders() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", this.Authorization);
        return headers;
    }

    private JSONArray getJSONArray(String uri) {
        String body = getRaw(uri);
        if (body == null) {
            return null;
        }
        return new JSONArray(body);
    }

    private String getRaw(String uri) {
        ResponseEntity<String> response = doExchange(uri);
        return response.getBody();
    }

    private ResponseEntity<String> doExchange(String uri) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHeaders();
        HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);
        ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, requestEntity, String.class);
        return response;
    }

    private HttpStatus getStatus(String uri) {
        ResponseEntity<String> response = doExchange(uri);
        return response.getStatusCode();
    }

    private String generateHookData(String url, String secret) throws JsonProcessingException {
        String data;
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jo = mapper.createObjectNode();
        jo.put("name", "web");
        jo.put("active", true);
        ArrayNode arrayNode = mapper.createArrayNode();
        arrayNode.add("push");
        arrayNode.add("pull_request");
        jo.set("events", arrayNode);
        ObjectNode configJo = mapper.createObjectNode();
        configJo.put("url", url);
        configJo.put("content_type", "json");
        configJo.put("insecure_ssl", "0");
        configJo.put("secret", secret);
        jo.set("config", configJo);

        data = mapper.writeValueAsString(jo);
        return data;
    }

    private void deleteHook(Integer hookId) {
        RestTemplate restTemplate = new RestTemplate();
        final HttpHeaders headers = getHeaders();
        HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);
        restTemplate.exchange(REPO_HOOKS_BASE_URL + "/" + hookId, HttpMethod.DELETE, requestEntity, Object.class);
    }

    private void deleteFile(String url) {
        RestTemplate restTemplate = new RestTemplate();
        final HttpHeaders headers = getHeaders();
        String data = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode jo = mapper.createObjectNode();
            jo.put("message", "deleting test commited file");
            ObjectNode committer = mapper.createObjectNode();
            committer.put("name", "NimrodGolan");
            committer.put("email", "nimrod.golan@checkmarx.com");
            jo.set("committer", committer);
            jo.put("sha", createdFileSha);

            data = mapper.writeValueAsString(jo);
        } catch (Exception e) {
            fail("faild to delete file of push");
        }

        HttpEntity<String> requestEntity = new HttpEntity<>(data, headers);
        restTemplate.exchange(COMMIT_FILE_PATH, HttpMethod.DELETE, requestEntity, String.class);

    }

    private void deleteIssue(String issueKey) {
        client.getIssueClient().deleteIssue(issueKey, false);
    }

    private String getFileInBase64() throws IOException {
        File file = ResourceUtils.getFile("classpath:\\cucumber\\data\\input-files-toscan\\" + srcFile);
        String content = new String(Files.readAllBytes(file.toPath()), Charset.forName("UTF-8"));
        String encodedString = Base64.getEncoder().encodeToString(content.getBytes());
        return encodedString;
    }

    private Properties getProperties() {
        Properties prop = new Properties();
        try {
            File file = ResourceUtils.getFile("classpath:\\cucumber\\features\\e2eTests\\githubHookProperties.properties");
            prop.load(Files.newInputStream(file.toPath()));
        } catch ( FileNotFoundException e) {
            fail("property file not found " + e.getMessage());
        } catch (IOException e) {
            fail("could not read properties file " + e.getMessage());
        }
        return prop;
    }
}