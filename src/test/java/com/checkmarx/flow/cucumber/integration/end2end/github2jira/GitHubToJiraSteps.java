package com.checkmarx.flow.cucumber.integration.end2end.github2jira;

import com.checkmarx.flow.cucumber.common.utils.TestUtils;
import io.cucumber.java.en.Given;

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
import com.checkmarx.flow.dto.github.Committer;
import com.checkmarx.flow.dto.github.Config;
import com.checkmarx.flow.dto.github.Hook;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import io.atlassian.util.concurrent.Promise;
import io.cucumber.java.After;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import static org.junit.jupiter.api.Assertions.*;

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
    public void init() throws IOException {
        Properties properties = getProperties();
        String namespace = Optional.ofNullable(System.getenv("HOOK_NAMESPACE")).orElse(properties.getProperty("namespace"));
        String repo = Optional.ofNullable(System.getenv("HOOK_REPO")).orElse(properties.getProperty("repo"));
        String filePath = Optional.ofNullable(properties.getProperty("fileCreatePath"))
            .orElse("{fileCreatePath}")
            .replace("{fileCreatePath}", "src\\main\\java\\sample\\encode.frm");
        hookTargetURL = Optional.ofNullable(System.getenv("HOOK_TARGET")).orElse(properties.getProperty("target"));
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
        TestUtils.runCxFlowAsService();
    }

    @And("webhook is configured for push event")
    public void generatePushWebHook() {
        final RestTemplate restTemplate = new RestTemplate();

        JSONArray hooks = getJSONArray(REPO_HOOKS_BASE_URL);

        if (!hooks.isEmpty()) {
            fail("repository alredy has hooks configured");
        }

        Hook data = null;
        try {
            data = generateHookData(hookTargetURL, gitHubProperties.getWebhookToken());
        } catch (Exception e) {
            fail("can not create web hook, check parameters");
        }

        final HttpHeaders headers = getHeaders();
        final HttpEntity<Hook> request = new HttpEntity<>(data, headers);
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
            Committer committer = new Committer();
            committer.setName("CxFlowTestUser");
            committer.setEmail("CxFlowTestUser@checkmarx.com");
            jo.putPOJO("committer", committer);
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

    private Hook generateHookData(String url, String secret) throws JsonProcessingException {
        Hook hook = new Hook();
        hook.setName("web");
        hook.setActive(true);
        hook.setEvents(Arrays.asList("push", "pull_request"));
        Config config = new Config();
        config.setUrl(url);
        config.setContentType("json");
        config.setInsecureSsl("0");
        config.setSecret(secret);
        hook.setConfig(config);
        return hook;
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
            Committer committer = new Committer();
            committer.setName("CxFlowTestUser");
            committer.setEmail("CxFlowTestUser@checkmarx.com");
            jo.putPOJO("committer", committer);
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
        String path = crumbsToPath(
            "cucumber",
            "data",
            "input-files-toscan",
            srcFile);
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            try (
                    InputStreamReader isr = new InputStreamReader(is , Charset.forName("UTF-8"));
                    BufferedReader reader = new BufferedReader(isr)
                ) {
                String content =  reader.lines().collect(Collectors.joining(System.lineSeparator()));
                String encodedString = Base64.getEncoder().encodeToString(content.getBytes());
                return encodedString;
            }
        }
    }

    private Properties getProperties() {
        Properties prop = new Properties();
        String path = crumbsToPath(
            "cucumber",
            "features",
            "e2eTests",
            "githubHookProperties.properties"
        );
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if(is != null) {
                prop.load(is);
            }
        } catch ( FileNotFoundException e) {
            fail("property file not found " + e.getMessage());
        } catch (IOException e) {
            fail("could not read properties file " + e.getMessage());
        }
        return prop;
    }

    
    private String crumbsToPath(Boolean includeFirst , String... crumbs) {
        StringJoiner joiner = new StringJoiner(
            File.separator,
            includeFirst ? File.separator : ""
            ,"");
        for (String crumb : crumbs) {
            joiner.add(crumb);
        }
        return joiner.toString();
    }

    private String crumbsToPath(String... crumbs) {
        return crumbsToPath(true , crumbs);
    }
}