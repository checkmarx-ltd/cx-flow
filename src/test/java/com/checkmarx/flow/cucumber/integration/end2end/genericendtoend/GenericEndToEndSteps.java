package com.checkmarx.flow.cucumber.integration.end2end.genericendtoend;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.SearchRestClient;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.internal.async.CustomAsynchronousJiraRestClientFactory;
import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.ADOProperties;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.cucumber.common.utils.TestUtils;
import com.checkmarx.flow.dto.azure.ConsumerInputs;
import com.checkmarx.flow.dto.azure.PublisherInputs;
import com.checkmarx.flow.dto.azure.Subscription;
import com.checkmarx.flow.dto.github.Committer;
import com.checkmarx.flow.dto.github.Config;
import com.checkmarx.flow.dto.github.Hook;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.atlassian.util.concurrent.Promise;
import io.cucumber.java.After;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest(classes = {CxFlowApplication.class})
public class GenericEndToEndSteps {
    private enum Repository {
        GitHub {
            private GitHubProperties gitHubProperties;
            private Integer hookId;
            private String createdFileSha;

            @Override
            Boolean hasWebHook() {
                String repoHooksBaseUrl = String.format("%s/%s/%s/hooks",
                        gitHubProperties.getApiUrl(), namespace, repo);
                JSONArray hooks = getJSONArray(repoHooksBaseUrl);
                assertNotNull(hooks, "could not create webhook configuration");
                return !hooks.isEmpty();
            }

            JSONArray getJSONArray(String uri) {
                RestTemplate restTemplate = new RestTemplate();
                HttpHeaders headers = getHeaders();
                log.info("GET array headers: {}", headers.toString());
                HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);
                ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, requestEntity, String.class);
                String body = response.getBody();
                if (body == null) {
                    return null;
                }
                return new JSONArray(body);
            }

            HttpHeaders getHeaders() {
                final HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Authorization", "token " + gitHubProperties.getToken());
                return headers;
            }

            @Override
            void generateHook() {
                Hook data = null;
                try {
                    data = generateHookData(hookTargetURL, gitHubProperties.getWebhookToken());
                } catch (Exception e) {
                    fail("can not create web hook, check parameters");
                }
                final HttpHeaders headers = getHeaders();
                final HttpEntity<Hook> request = new HttpEntity<>(data, headers);
                try {
                    RestTemplate restTemplate = new RestTemplate();
                    String url = String.format("%s/%s/%s/hooks", gitHubProperties.getApiUrl(), namespace, repo);
                    final ResponseEntity<String> response = restTemplate.postForEntity(url, request,
                            String.class);
                    assertEquals(HttpStatus.CREATED, response.getStatusCode());
                    hookId = new JSONObject(response.getBody()).getInt("id");
                } catch (Exception e) {
                    fail("failed to create hook " + e.getMessage());
                }
            }

            private Hook generateHookData(String url, String secret) {
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

            @Override
            void deleteHook() {
                Optional.ofNullable(hookId).ifPresent(id -> {
                    RestTemplate restTemplate = new RestTemplate();
                    final HttpHeaders headers = getHeaders();
                    HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);
                    String url = String.format("%s/%s/%s/hooks", gitHubProperties.getApiUrl(), namespace, repo);
                    restTemplate.exchange(url + "/" + hookId, HttpMethod.DELETE, requestEntity, Object.class);
                });
            }

            @Override
            void pushFile(String content) {
                ObjectMapper mapper = new ObjectMapper();
                ObjectNode jo = mapper.createObjectNode();
                jo.put("message", "GitHubToJira test message");
                Committer committer = new Committer();
                committer.setName("CxFlowTestUser");
                committer.setEmail("CxFlowTestUser@checkmarx.com");
                jo.putPOJO("committer", committer);
                jo.put("content", content);
                String data;
                try {
                    data = mapper.writeValueAsString(jo);
                } catch (JsonProcessingException e) {
                    String msg = "faild to create file for push";
                    log.error(msg);
                    fail(msg);
                    data = null;
                }
                final HttpHeaders headers = getHeaders();
                final HttpEntity<String> request = new HttpEntity<>(data, headers);
                RestTemplate restTemplate = new RestTemplate();
                try {
                    String path = String.format("%s/%s/%s/contents/%s", gitHubProperties.getApiUrl(), namespace, repo, filePath);
                    ResponseEntity<String> response = restTemplate.exchange(path, HttpMethod.PUT, request,
                            String.class);
                    createdFileSha = new JSONObject(response.getBody()).getJSONObject("content").getString("sha");
                } catch (Exception e) {
                    fail("faild to push a file: " + e.getMessage());
                }

            }

            @Override
            void deleteFile() {
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
                    String msg = "faild to delete file of push";
                    log.error(msg);
                    fail(msg);
                }

                HttpEntity<String> requestEntity = new HttpEntity<>(data, headers);
                String path = String.format("%s/%s/%s/contents/%s", gitHubProperties.getApiUrl(), namespace, repo, filePath);
                restTemplate.exchange(path, HttpMethod.DELETE, requestEntity, String.class);
            }

            @Override
            protected void init(GenericEndToEndSteps genericEndToEndSteps) {
                gitHubProperties = genericEndToEndSteps.gitHubProperties;
                super.init(genericEndToEndSteps);
            }
        }, ADO {
            private ADOProperties adoProperties;
            private final String hooksFormat = "%s/%s/_apis/hooks/subscriptions?api-version=5.0";
            private final String deleteHooksFormat = "%s/%s/_apis/hooks/subscriptions/%s?api-version=5.0";
            private final String deleteFileFormat = "%s/%s/_apis/git/repositories/%s/pushes?api-version=5.0";
            private String COMMIT_FILE_PATH = null;
            private String hookId = null;
            private String OldObject = null;
            private String createdFileSha = null;
            private String projectId = "72b33c9c-84a3-48e7-b262-39da686d5bee";
            private String repositoryId = "0a1f3cf1-4e37-4733-873e-e95aad7515c0";


            @Override
            Boolean hasWebHook() {
                String uri = getHookServiceURI();
                RestTemplate restTemplate = new RestTemplate();
                HttpHeaders headers = getHeaders();
                log.info("GET json headers: {}", headers.toString());
                HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);
                ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, requestEntity, String.class);
                String body = response.getBody();
                if (body == null) {
                    return null;
                }
                JSONObject jBody = new JSONObject(body);
                return jBody.getInt("count") > 3;
            }

            private HttpHeaders getHeaders() {
                String encoding = Base64.getEncoder().encodeToString(":".concat(adoProperties.getToken()).getBytes());
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.set("Content-Type", "application/json");
                httpHeaders.set("Authorization", "Basic ".concat(encoding));
                httpHeaders.set("Accept", "application/json");

                return httpHeaders;
            }

            @Override
            void generateHook() {
                String url = getHookServiceURI();
                RestTemplate restTemplate = new RestTemplate();
                HttpHeaders headers = getHeaders();

                Subscription data = generateHookData(projectId, hookTargetURL);
                final HttpEntity<Subscription> request = new HttpEntity<>(data, headers);
                try {
                    final ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    hookId = new JSONObject(response.getBody()).getString("id");
                } catch (Exception e) {
                    fail("failed to create hook " + e.getMessage());
                }
            }

            private Subscription generateHookData(String projectId, String url) {
                String auth = Base64.getEncoder().encodeToString(adoProperties.getWebhookToken().getBytes());
                ConsumerInputs consumerInputs = ConsumerInputs.builder()
                        .httpHeaders("Authorization: Basic ".concat(auth))
                        .basicAuthUsername("cxflow")
                        .basicAuthPassword("1234")
                        .url(url)
                        .build();
                PublisherInputs publisherInput = PublisherInputs.builder()
                        .projectId(projectId)
                        .build();

                return Subscription.builder()
                        .consumerActionId("httpRequest")
                        .consumerId("webHooks")
                        .consumerInputs(consumerInputs)
                        .eventType("git.push")
                        .publisherId("tfs")
                        .publisherInputs(publisherInput)
                        .resourceVersion("1.0")
                        .scope(1)
                        .build();
            }

            @Override
            void deleteHook() {
                Optional.ofNullable(hookId).ifPresent(hookId -> {
                    String url = String.format(deleteHooksFormat, adoProperties.getUrl(), namespace, hookId);
                    RestTemplate restTemplate = new RestTemplate();
                    HttpHeaders headers = getHeaders();
                    final HttpEntity<Subscription> request = new HttpEntity<>(null, headers);
                    try {
                        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.DELETE, request, String.class);
                        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
                    } catch (Exception e) {
                        String msg = "failed deleting Azure web-hook subscription. " + e.getMessage();
                        log.error(msg);
                        fail(msg);
                    }
                });
            }

            @Override
            void pushFile(String content) {
                String data = null;
                COMMIT_FILE_PATH = format("%s/%s/%s/_apis/git/repositories/%s/pushes?api-version=5.0",
                        adoProperties.getUrl(), namespace, repo, repositoryId);
                String Path = "/encode.frm";
                final RestTemplate restTemplate = new RestTemplate();

                ObjectMapper mapper = new ObjectMapper();
                OldObject = getLastOldObject();

                ObjectNode commit = createCommit(Path, content, OldObject);

                try {

                    data = mapper.writeValueAsString(commit);
                    data = data.replace("\\\"", "");


                } catch (Exception e) {
                    String msg = "faild to create file for push";
                    log.error(msg);
                    fail(msg);
                    data = null;
                }
                final HttpHeaders headers = getHeaders();
                final HttpEntity<String> request = new HttpEntity<>(data, headers);

                try {

                    ResponseEntity<String> response = restTemplate.exchange(COMMIT_FILE_PATH, HttpMethod.POST, request,
                            String.class);
                    log.info("Push response body=" + response.getBody());
                } catch (Exception e) {
                    String msg = "faild to push a file:";
                    log.error(msg);
                    fail(msg + e.getMessage());

                }

            }

            private String getLastOldObject() {
                String OldObjectUrl = format("%s/%s/_apis/git/repositories/%s/refs?api-version=5.0", adoProperties.getUrl(), namespace, repositoryId);
                String LastOldObject = null;
                RestTemplate restTemplate = new RestTemplate();
                HttpHeaders headers = getHeaders();
                HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);
                ResponseEntity<String> response = restTemplate.exchange(OldObjectUrl, HttpMethod.GET, requestEntity, String.class);
                String[] OldObjectResponse = Objects.requireNonNull(response.getBody()).split(",");
                String[] OldObject = OldObjectResponse[1].split(":");
                LastOldObject = OldObject[1];

                return LastOldObject;
            }

            @Override
            void deleteFile() {
                String Path = "/encode.frm";
                createdFileSha = getLastOldObject();
                ObjectMapper mapper = new ObjectMapper();
                String data = null;
                ObjectNode deleteCommit = deleteCommit(Path, createdFileSha);

                try {

                    data = mapper.writeValueAsString(deleteCommit);
                    data = data.replace("\\\"", "");

                } catch (Exception e) {
                    String msg = "faild to create  json data for Delete file";
                    log.error(msg);
                    fail(msg);
                    data = null;
                }
                String deleteFileUrl = format(deleteFileFormat, adoProperties.getUrl(), namespace, repositoryId);
                RestTemplate restTemplate = new RestTemplate();
                HttpHeaders headers = getHeaders();
                final HttpEntity<String> request = new HttpEntity<>(data, headers);
                try {
                    ResponseEntity<String> response = restTemplate.exchange(deleteFileUrl, HttpMethod.POST, request, String.class);
                    assertEquals(HttpStatus.valueOf(201), response.getStatusCode());
                    log.info("File Deleted");

                } catch (Exception e) {
                    String msg = "failed deleting Pushed flie . " + e.getMessage();
                    log.error(msg);
                    fail(msg);
                }

            }

            private ObjectNode createCommit(String path, String content, String ObjectId) {
                ObjectMapper mapper = new ObjectMapper();
                ObjectNode object = mapper.createObjectNode();
                ObjectNode Commits = mapper.createObjectNode();
                ObjectNode changes = mapper.createObjectNode();
                ObjectNode item = mapper.createObjectNode();
                ObjectNode newContent = mapper.createObjectNode();
                ObjectNode refUpdates = mapper.createObjectNode();

                //newContent
                newContent.put("content", content)
                        .put("contentType", "base64Encoded");

                item.put("path", path);
                //changes
                changes.put("changeType", 1)
                        .putPOJO("item", item)
                        .putPOJO("newContent", newContent);
                Commits.putArray("changes").addPOJO(changes);
                //Commits
                object.putArray("commits").addPOJO(Commits);

                Commits.put("comment", "adding the encode.frm  file via API");


                //   refUpdates
                object.putArray("refUpdates").addPOJO(refUpdates);
                refUpdates.put("name", "refs/heads/master")
                        .put("oldObjectId", ObjectId);
                log.info("Commit object Created = " + object);

                return object;

            }

            private ObjectNode deleteCommit(String path, String ObjectId) {
                ObjectMapper mapper = new ObjectMapper();
                ObjectNode object = mapper.createObjectNode();
                ObjectNode Commits = mapper.createObjectNode();
                ObjectNode changes = mapper.createObjectNode();
                ObjectNode item = mapper.createObjectNode();
                ObjectNode refUpdates = mapper.createObjectNode();


                item.put("path", path);
                //changes
                changes.put("changeType", "delete")
                        .putPOJO("item", item);

                Commits.putArray("changes").addPOJO(changes);
                //Commits
                object.putArray("commits").addPOJO(Commits);

                Commits.put("comment", "Deleting the encode.frm  file via API");
                // Commits.putPOJO("changes", changes)

                //   refUpdates
                object.putArray("refUpdates").addPOJO(refUpdates);
                refUpdates.put("name", "refs/heads/master")
                        .put("oldObjectId", ObjectId);
                log.info("Delete commit  Created " + object);
                return object;

            }

            @Override
            protected void init(GenericEndToEndSteps genericEndToEndSteps) {
                adoProperties = genericEndToEndSteps.adoProperties;
                super.init(genericEndToEndSteps);
            }

            private String getHookServiceURI() {

                return String.format(hooksFormat, adoProperties.getUrl(), namespace);
            }
        };

        /* where to push the file */
        static final String filePath = "src/main/java/sample/encode.frm";

        static Repository setTo(String toRepository, GenericEndToEndSteps genericEndToEndSteps) {
            log.info("setting repository to {}", toRepository);
            Repository repo = valueOf(toRepository);
            repo.init(genericEndToEndSteps);
            return repo;
        }

        protected void init(GenericEndToEndSteps genericEndToEndSteps) {
            String upperCaseName = name().toUpperCase();
            if (

                    System.getenv(upperCaseName + "_HOOK_NAMESPACE") == null ||
                            System.getenv(upperCaseName + "_HOOK_REPO") == null ||
                            System.getenv(upperCaseName + "_HOOK_TARGET") == null
            ) {
                log.info("running with property file");
                Properties properties = getProperties();
                namespace = properties.getProperty("namespace");
                repo = properties.getProperty("repo");
                hookTargetURL = properties.getProperty("target");
            } else {
                log.info("running with system variables");
                namespace = System.getenv(upperCaseName + "_HOOK_NAMESPACE");
                repo = System.getenv(upperCaseName + "_HOOK_REPO");
                hookTargetURL = System.getenv(upperCaseName + "_HOOK_TARGET");
            }
        }

        protected void generatePushWebHook() {
            log.info("testing if repository alredy has hooks configured");
            assertTrue(!hasWebHook(), "repository alredy has hooks configured");
            log.info("creating the webhook");
            generateHook();
        }

        protected Properties getProperties() {
            Properties prop = new Properties();
            String path = new StringJoiner(File.separator, File.separator, "")
                    .add("cucumber")
                    .add("features")
                    .add("e2eTests")
                    .add(String.format("HookProperties_%s.properties", name()))
                    .toString();
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
                prop.load(is);
            } catch (NullPointerException | FileNotFoundException e) {
                log.info("to run this test you need a file called {}", path);
                log.info("the file should have the following properties: \nnamespace\nrepo\ntarget");
                fail("property file not found (" + path + ") " + e.getMessage());
            } catch (IOException e) {
                log.error("please verify that the file {} is ok", path);
                fail("could not read properties file (" + path + ") " + e.getMessage());
            }
            return prop;
        }

        abstract Boolean hasWebHook();

        abstract void generateHook();

        abstract void deleteHook();

        abstract void pushFile(String content);

        abstract void deleteFile();

        protected String namespace = null;
        protected String repo = null;
        protected String hookTargetURL = null;

    }

    private enum BugTracker {
        JIRA {
            private JiraProperties jiraProperties;
            private List<String> issueCreatedKeys = new ArrayList<>();
            private JiraRestClient client;
            private SearchRestClient searchClient;


            @Override
            void init(GenericEndToEndSteps genericEndToEndSteps) {
                jiraProperties = genericEndToEndSteps.jiraProperties;
                CustomAsynchronousJiraRestClientFactory factory = new CustomAsynchronousJiraRestClientFactory();
                URI jiraURI;
                try {
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
            void verifyIssueCreated(String severities) {
                String jql = String.format("project = %s and priority  in %s", jiraProperties.getProject(), severities);
                log.info("filtering issue with jql: {}", jql);
                Set<String> fields = new HashSet<String>();
                fields.addAll(
                        Arrays.asList("key", "project", "issuetype", "summary", "labels", "created", "updated", "status"));
                SearchResult result = null;
                Boolean found = false;
                for (int retries = 0; retries < 20; retries++) {
                    Promise<SearchResult> temp = searchClient.searchJql(jql, 10, 0, fields);
                    try {
                        TimeUnit.SECONDS.sleep(5);
                    } catch (InterruptedException e) {
                        log.info("starting attempt {}", retries + 1);
                    }
                    try {
                        result = temp.get(500, TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        log.info("failed attempt {}", retries + 1);
                    }

                    if (result != null && result.getTotal() > 0) {
                        found = true;
                        Iterator<Issue> itr = result.getIssues().iterator();
                        assertTrue(itr.hasNext(), "Jira is missing the issues");
                        while (itr.hasNext()) {
                            this.issueCreatedKeys.add(itr.next().getKey());
                        }
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
                for (String issueKey : issueCreatedKeys) {
                    client.getIssueClient().deleteIssue(issueKey, false);
                }
            }
        };

        static BugTracker setTo(String bugTracker, GenericEndToEndSteps genericEndToEndSteps) {
            log.info("setting bug-tracker to {}", bugTracker);
            BugTracker bt = valueOf(bugTracker);
            bt.init(genericEndToEndSteps);
            return bt;
        }

        abstract void verifyIssueCreated(String severities);

        abstract void deleteIssues();

        abstract void init(GenericEndToEndSteps genericEndToEndSteps);
    }

    @Autowired
    private FlowProperties flowProperties;
    /*
     * repositories
     */
    @Autowired
    private GitHubProperties gitHubProperties;
    @Autowired
    private ADOProperties adoProperties;

    /*
     * bug-trackers
     */
    @Autowired
    private JiraProperties jiraProperties;

    private Repository repository;
    private BugTracker bugTracker;
    private ConfigurableApplicationContext appContext;

    @Given("repository is GitHub")
    public void setRepositoryGitHub() {
        setRepository("GitHub");
    }

    @Given("repository is ADO")
    public void setRepositoryADO() {
        setRepository("ADO");
    }

    // @Given("repository is {string}")
    public void setRepository(String repository) {
        this.repository = Repository.setTo(repository, this);
    }

    @And("bug-tracker is JIRA")
    public void setBugTrackerJira() {
        setBugTracker("JIRA");
    }

    @And("bug-tracker is {string}")
    public void setBugTracker(String bugTracker) {
        this.bugTracker = BugTracker.setTo(bugTracker, this);
        flowProperties.setBugTracker(bugTracker);
    }

    @And("CxFlow is running as a service")
    public void runAsService() {
        log.info("runnning cx-flow as a service");
        appContext = TestUtils.runCxFlowAsService();
    }

    @And("webhook is configured for push event")
    public void generatePushWebHook() {
        repository.generatePushWebHook();
    }

    @When("pushing a change")
    public void pushChange() {
        String content = null;
        try {
            content = getFileInBase64();
        } catch (IOException e) {
            fail("can not read source file");
        }
        repository.pushFile(content);
    }

    @Then("bug-tracker issues are updated")
    public void validateIssueOnBugTracker() {
        String severities = "(" + flowProperties.getFilterSeverity().stream().collect(Collectors.joining(",")) + ")";
        bugTracker.verifyIssueCreated(severities);
    }

    @After
    public void cleanUp() {
        repository.deleteHook();
        repository.deleteFile();
        bugTracker.deleteIssues();
        SpringApplication.exit(appContext);
    }

    private String getFileInBase64() throws IOException {
        String path = new StringJoiner(File.separator)
                .add("cucumber")
                .add("data")
                .add("input-files-toscan")
                .add("e2e.src")
                .toString();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            try (
                    InputStreamReader isr = new InputStreamReader(is, Charset.forName("UTF-8"));
                    BufferedReader reader = new BufferedReader(isr)
            ) {
                String content = reader.lines().collect(Collectors.joining(System.lineSeparator()));
                String encodedString = Base64.getEncoder().encodeToString(content.getBytes());
                return encodedString;
            }
        }
    }
}