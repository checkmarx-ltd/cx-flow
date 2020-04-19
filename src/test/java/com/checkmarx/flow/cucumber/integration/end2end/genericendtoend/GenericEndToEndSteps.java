package com.checkmarx.flow.cucumber.integration.end2end.genericendtoend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.Properties;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.cucumber.common.utils.TestUtils;
import com.checkmarx.flow.dto.github.Config;
import com.checkmarx.flow.dto.github.Hook;
import com.fasterxml.jackson.core.JsonProcessingException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import io.cucumber.java.After;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest(classes = { CxFlowApplication.class })
public class GenericEndToEndSteps {
    private enum Repository {
        GitHub {
            @Autowired
            private GitHubProperties gitHubProperties;
            private Integer hookId;

            @Override
            Boolean hasWebHook() {
                String repoHooksBaseUrl = String.format("%s/%s/%s/hooks", 
                    gitHubProperties.getApiUrl(), namespace, repo);
                JSONArray hooks = getJSONArray(repoHooksBaseUrl);
                assertNotNull(hooks, "could not create webhook configuration");

                if (!hooks.isEmpty()) {// NOSONAR
                    fail("repository alredy has hooks configured");
                }
                return null;
            }

            JSONArray getJSONArray(String uri) {
                RestTemplate restTemplate = new RestTemplate();
                HttpHeaders headers = getHeaders();
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
            void generateHook(String hookTargetURL) {
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
        },
        ADO {
            @Override
            Boolean hasWebHook() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            void generateHook(String hookTargetURL) {
                // TODO Auto-generated method stub
            }

            @Override
            void deleteHook() {
                // TODO Auto-generated method stub

            }
        };

        static Repository setTo(String toRepository) {
            log.info("setting repository to {}", toRepository);
            return valueOf(toRepository);
        }

        protected void generatePushWebHook(String hookTargetURL) {
            log.info("testing if repository alredy has hooks configured");
            assertTrue(!hasWebHook(), "repository alredy has hooks configured");
            log.info("creating the webhook");
            generateHook(hookTargetURL);
        }

        abstract Boolean hasWebHook();

        abstract void generateHook(String hookTargetURL);

        abstract void deleteHook();
        
        public void deleteFile() {
        }

        public void pushFile() {
        }

    }

    private enum BugTracker {
        JIRA;

        static BugTracker setTo(String bugTracker) {
            log.info("setting bug-tracker to {}", bugTracker);
            return valueOf(bugTracker);
        }

        public void deleteIssues() {
        }
    }

    @Autowired
    private FlowProperties flowProperties;

    /**************
     * bug-trackers
     **************/
    @Autowired
    private JiraProperties jiraProperties;

    private Repository repository;
    private BugTracker bugTracker;

    private static String namespace = null;
    private static String repo = null;
    private String hookTargetURL = null;
    private String filePath = "src/main/java/sample/encode.frm";
    
    @PostConstruct
    public void init() throws IOException {
        if (
            System.getenv("HOOK_NAMESPACE") == null || 
            System.getenv("HOOK_REPO") == null ||
            System.getenv("HOOK_TARGET") == null
        ) {
            log.info("running with property file");
            Properties properties = getProperties();
            namespace = properties.getProperty("namespace");
            repo = properties.getProperty("repo");
            hookTargetURL = properties.getProperty("target");
        } else {
            log.info("running with system variables");
            namespace = System.getenv("HOOK_NAMESPACE");
            repo = System.getenv("HOOK_REPO");
            hookTargetURL = System.getenv("HOOK_TARGET");
        }
    }

    @Given("repository is {string}")
    public void setRepository(String repository) {
        this.repository = Repository.setTo(repository);
    }

    @And("bug-tracker is {string}")
    public void setBugTracker(String bugTracker) {
        this.bugTracker = BugTracker.setTo(bugTracker);
        flowProperties.setBugTracker(bugTracker);
    }

    @And("CxFlow is running as a service")
    public void runAsService() {
        log.info("runnning cx-flow as a service");
        TestUtils.runCxFlowAsService();
    }

    @And("webhook is configured for push event")
    public void generatePushWebHook() {
        repository.generatePushWebHook(hookTargetURL);
    }

    @When("pushing a change")
    public void pushChange() {
        String content;
        try {
            content = getFileInBase64();
        } catch (IOException e) {
            fail("can not read source file");
        }
        repository.pushFile();
    }

    @Then("Then bug-tracker issues are updated")
    public void validateIssueOnBugTracker() {
    }

    @After
    public void cleanUp() {
        repository.deleteHook();
        repository.deleteFile();
        bugTracker.deleteIssues();
    }

    private Properties getProperties() {
        Properties prop = new Properties();
        String path = new StringJoiner(File.separator,File.separator,"")
            .add("cucumber")
            .add("features")
            .add("e2eTests")
            .add("hookProperties.properties")
            .toString();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            prop.load(is);
        } catch (FileNotFoundException e) {
            log.info("to run this test you need a file called {}", path);
            log.info("the file should have the following properties: \nnamespace\nrepo\ntarget");
            fail("property file not found (" + path + ") " + e.getMessage());
        } catch (IOException e) {
            log.error("please verify that the file {} is ok", path);
            fail("could not read properties file (" + path + ") " + e.getMessage());
        }
        return prop;
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
                    InputStreamReader isr = new InputStreamReader(is , Charset.forName("UTF-8"));
                    BufferedReader reader = new BufferedReader(isr)
                ) {
                String content =  reader.lines().collect(Collectors.joining(System.lineSeparator()));
                String encodedString = Base64.getEncoder().encodeToString(content.getBytes());
                return encodedString;
            }
        }
    }
}
