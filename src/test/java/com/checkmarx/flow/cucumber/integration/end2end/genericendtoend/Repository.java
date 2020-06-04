package com.checkmarx.flow.cucumber.integration.end2end.genericendtoend;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

import com.checkmarx.flow.config.ADOProperties;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.dto.azure.ConsumerInputs;
import com.checkmarx.flow.dto.azure.PublisherInputs;
import com.checkmarx.flow.dto.azure.Subscription;
import com.checkmarx.flow.dto.github.Committer;
import com.checkmarx.flow.dto.github.Config;
import com.checkmarx.flow.dto.github.Hook;
import com.checkmarx.flow.dto.rally.Object;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.cucumber.java.PendingException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.platform.commons.PreconditionViolationException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;


import lombok.extern.slf4j.Slf4j;

@Slf4j
enum Repository {
    GITHUB {
        GitHubProperties gitHubProperties;
        private Integer hookId;
        private String createdFileSha;
        private Integer prId;
        private GitHubApiHandler api;

        @Override
        Boolean hasWebHook() {
            String repoHooksBaseUrl = getHooksFormat();
            JSONArray hooks = getJSONArray(repoHooksBaseUrl);
            assertNotNull(hooks, "could not create webhook configuration");
            return !hooks.isEmpty();
        }

        private String getHooksFormat() {
            return String.format("%s/%s/%s/hooks",
                    gitHubProperties.getApiUrl(), namespace, repo);
        }

        JSONArray getJSONArray(String uri) {
            ResponseEntity<String> response = getResponseEntity(uri);
            String body = response.getBody();
            if (body == null) {
                return null;
            }
            return new JSONArray(body);
        }

        JSONObject getJSONObject(String uri) {
            ResponseEntity<String> response = getResponseEntity(uri);
            String body = response.getBody();
            if (body == null) {
                return null;
            }
            return new JSONObject(body);
        }


        @Override
        void generateHook(HookType hookType) {
            Hook data = null;
            try {
                data = generateHookData(hookTargetURL, gitHubProperties.getWebhookToken(), hookType);
            } catch (Exception e) {
                fail("can not create web hook, check parameters");
            }
            final HttpHeaders headers = getHeaders();
            final HttpEntity<Hook> request = new HttpEntity<>(data, headers);
            try {
                RestTemplate restTemplate = new RestTemplate();
                String url = getHooksFormat();
                final ResponseEntity<String> response = restTemplate.postForEntity(url, request,
                        String.class);
                assertEquals(HttpStatus.CREATED, response.getStatusCode());
                hookId = new JSONObject(response.getBody()).getInt("id");
            } catch (Exception e) {
                fail("failed to create hook " + e.getMessage());
            }
        }

        private Hook generateHookData(String url, String secret, HookType hookType) {
            Hook hook = new Hook();
            hook.setName("web");
            hook.setActive(true);
            switch (hookType) {
                case PUSH:
                    hook.setEvents(Arrays.asList("push"));
                    break;
                case PULL_REQUEST:
                    hook.setEvents(Arrays.asList("pull_request"));    
                    break;
                default:
                    throw new PendingException();
            }
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
                String url = getHooksFormat();
                restTemplate.exchange(url + "/" + hookId, HttpMethod.DELETE, requestEntity, Object.class);
            });
        }

        @Override
        HttpHeaders getHeaders() {
            final HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "token " + gitHubProperties.getToken());
            return headers;
        }

        @Override
        void pushFile(String content) {
            Committer committer = new Committer();
            committer.setName("CxFlowTestUser");
            committer.setEmail("CxFlowTestUser@checkmarx.com");
            try {
                JSONObject response = api.pushFile(content, "GitHubToJira test message", committer, getHeaders(), gitHubProperties.getApiUrl(), namespace, repo, filePath);
                createdFileSha = response.getJSONObject("content").getString("sha");
            } catch (JsonProcessingException e) {
                String msg = "faild to create file for push";
                log.error(msg);
                fail(msg);
            } catch (HttpClientErrorException e) {
                if (e.getRawStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY.value()) {
                    String message = "There is already a file with the specified name (" + filePath + "). please delete the file before running the test";
                    log.error(message);
                    throw new PreconditionViolationException(message);
                } else {
                    throw e;
                }
            } catch (Exception e) {
                fail("faild to push a file: " + e.getMessage());
            }
        }

        private String getContentsFormat() {
            return String.format("%s/%s/%s/contents/%s",
                    gitHubProperties.getApiUrl(), namespace, repo, filePath);
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
            String path = getContentsFormat();
            restTemplate.exchange(path, HttpMethod.DELETE, requestEntity, String.class);
        }

        @Override
        protected void init(GenericEndToEndSteps genericEndToEndSteps) {
            gitHubProperties = genericEndToEndSteps.gitHubProperties;
            api = new GitHubApiHandler();
            super.init(genericEndToEndSteps);
        }

        @Override
        void createPR() {
            String data = createPRData(false);
            final HttpHeaders headers = getHeaders();
            final HttpEntity<String> request = new HttpEntity<>(data, headers);
            try {
                RestTemplate restTemplate = new RestTemplate();
                String url = String.format("%s/%s/%s/pulls", 
                    gitHubProperties.getApiUrl(), namespace, repo);
//                if (log.isInfoEnabled()) {
//                    throw new PendingException("createPR is waiting on parameters");
//                }
                final ResponseEntity<String> response = restTemplate.postForEntity(url, request,
                    String.class);
                assertEquals(HttpStatus.CREATED, response.getStatusCode());
                prId = new JSONObject(response.getBody()).getInt("number");
            } catch (Exception e) {
                fail("failed to create PR " + e.getMessage());
            }
        }

        private String createPRData(boolean isDelete) {
            Properties prProperties = getProperties("PullRequestProperties");
            //TO DO: replace parameters 
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode pr = mapper.createObjectNode();
            if (isDelete) {
                pr.put("state", "closed");
            } else {
                pr.put("title", prProperties.getProperty("title"))
                        .put("body", prProperties.getProperty("body"))
                        .put("base", prProperties.getProperty("base"))
                        .put("head", prProperties.getProperty("head"));
            }
            String data = null;
            try {
                data = mapper.writeValueAsString(pr);
            } catch (JsonProcessingException e) {
                String msg = "faild to create GitHub PR data";
                log.error(msg);
                fail(msg);
            }
            return data;
        }

        @Override
        void deletePR() {
            String data = createPRData(true);
            final HttpHeaders headers = getHeaders();
            final HttpEntity<String> request = new HttpEntity<>(data, headers);
            try {
                RestTemplate restTemplate = new RestTemplate();
                String url = getPRURL();
                final ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST,
                    request, String.class);
                assertEquals(HttpStatus.OK, response.getStatusCode());
            } catch (Exception e) {
                fail("failed to delete PR " + e.getMessage());
            }
        }

        private String getPRURL() {
            return String.format("%s/%s/%s/pulls/%d",
                            gitHubProperties.getApiUrl(), namespace, repo, prId);
        }

        private String getPRCommentsUrl() {
            return String.format("%s/%s/%s/issues/%d/comments", gitHubProperties.getApiUrl(), namespace, repo, prId);
        }

        @Override
        void verifyPRUpdated() {
            String url = getPRCommentsUrl();
            boolean isFound = false;
            for (int retries = 0 ; retries < 20 && !isFound ; retries++) {
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (Exception e) {
                    log.info("starting attempt {}", retries + 1);
                }
                try {
                    JSONArray comments = getJSONArray(url);
                    for (java.lang.Object c : Objects.requireNonNull(comments)) {
                        if (((JSONObject) c).getString("body").startsWith("### Checkmarx Dependency (CxSCA)")) {
                            log.info("Relevant PR comment was found");
                            isFound = true;
                            break;
                        }
                    }
                } catch (Exception e) {
                    log.info("failed attempt {}", retries + 1);
                }
            }
            if (!isFound) {
                String msg = "failed to find update in PR comments";
                log.error(msg);
                fail(msg);
            }
        }
    },
    ADO {
        private ADOProperties adoProperties;
        private String hookId = null;
        private String apiVersion = "api-version=5.0";
        private String projectId = null;
        
        @Override
        Boolean hasWebHook() {
            String uri = getHookServiceURI();
            ResponseEntity<String> response = getResponseEntity(uri);
            String body = response.getBody();
            if (body == null) {
                return null;
            }
            JSONObject jBody = new JSONObject(body);
            return jBody.getInt("count") > 3;
        }

        @Override
        void generateHook(HookType hookType) {
            String eventType = null;
            switch (hookType) {
                case PUSH:
                    hookTargetURL = hookTargetURL + "/ado/push";
                    eventType = "git.push";
                    break;
                case PULL_REQUEST:
                    hookTargetURL = hookTargetURL + "/ado/pull";
                    eventType = "git.pullrequest.created";
                    break;
                default:
                    throw new PendingException();
            }
            String url = getHookServiceURI();
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = getHeaders();
            Subscription data = null;
            try {
                data = generateHookData(hookTargetURL , eventType);

            } catch (IOException e) {
                fail("can not create web hook, check parameters");
            }
            final HttpEntity<Subscription> request = new HttpEntity<>(data, headers);
            try {
                final ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
                assertEquals(HttpStatus.OK, response.getStatusCode());
                Map<?,?> responseMap = new ObjectMapper().readValue(response.getBody().replace("<", ""), Map.class);
                hookId = responseMap.get("id").toString();
                log.info("generated hookId={}", hookId);

            } catch (Exception e) {
                fail("failed to create hook " + e.getMessage());
            }
        }


        private Subscription generateHookData(String url, String eventType) throws IOException {
            String auth = Base64.getEncoder().encodeToString(adoProperties.getWebhookToken().getBytes());
            projectId = getProjectId();
            ConsumerInputs consumerInputs = ConsumerInputs.builder()
                    .httpHeaders("Authorization: Basic ".concat(auth))
                    .url(url)
                    .build();
            PublisherInputs publisherInput = PublisherInputs.builder()
                    .projectId(projectId)
                    .build();

            return Subscription.builder()
                    .consumerActionId("httpRequest")
                    .consumerId("webHooks")
                    .consumerInputs(consumerInputs)
                    .eventType(eventType)
                    .publisherId("tfs")
                    .publisherInputs(publisherInput)
                    .resourceVersion("1.0")
                    .scope(1)
                    .build();
        }

        @Override
        void deleteHook() {
            Optional.ofNullable(hookId).ifPresent(h -> {
                String url = getDeleteHooksFormat();
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
        HttpHeaders getHeaders() {
            String encoding = Base64.getEncoder().encodeToString(":".concat(adoProperties.getToken()).getBytes());
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.set("Content-Type", "application/json");
            httpHeaders.set("Authorization", "Basic ".concat(encoding));
            httpHeaders.set("Accept", "application/json");

            return httpHeaders;
        }

        @Override
        void pushFile(String content) {
            String data = null;
            String commitFilePath = getPushesFormat();
            String Path = "/encode.frm";
            final RestTemplate restTemplate = new RestTemplate();

            ObjectMapper mapper = new ObjectMapper();
            String oldObject = getLastOldObject();

            ObjectNode commit = createCommit(Path, content, oldObject);

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

                ResponseEntity<String> response = restTemplate.exchange(commitFilePath, HttpMethod.POST, request,
                        String.class);
                log.info("Pushed response body={}", response.getBody());
            } catch (Exception e) {
                String msg = "faild to push a file:";
                log.error(msg);
                fail(msg + e.getMessage());

            }

        }

        private String getLastOldObject() {
            String OldObjectUrl = getRefsFormat();
            String LastOldObject = null;
            ResponseEntity<String> response = getResponseEntity(OldObjectUrl);
            String[] OldObjectResponse = Objects.requireNonNull(response.getBody()).split(",");
            String[] OldObject = OldObjectResponse[1].split(":");
            LastOldObject = OldObject[1];

            return LastOldObject;
        }

        private String getDeleteHooksFormat() {
            return format("%s/%s/_apis/hooks/subscriptions/%s?%s",
                    adoProperties.getUrl(), namespace, hookId, apiVersion);

        }

        private String getPushesFormat() {
            return format("%s/%s/%s/_apis/git/repositories/%s/pushes?%s",
                    adoProperties.getUrl(), namespace, repo, repo, apiVersion);
        }

        private String getRefsFormat() {
            return format("%s/%s/%s/_apis/git/repositories/%s/refs?%s",
                    adoProperties.getUrl(), namespace, repo, repo, apiVersion);
        }

        private String getHookServiceURI() {
            return String.format("%s/%s/_apis/hooks/subscriptions?%s",
                    adoProperties.getUrl(), namespace, apiVersion);
        }

        private String getProjectId() throws IOException {
            String url = getProjectsFormat();
            String response = getResponseEntity(url).getBody();
            Map<?,?> responseMap = new ObjectMapper().readValue(response, Map.class);

            projectId = responseMap.get("id").toString();

            return projectId;
        }

        private String getDeleteFileFormat() {
            return format("%s/%s/%s/_apis/git/repositories/%s/pushes?%s",
                    adoProperties.getUrl(), namespace, repo, repo, apiVersion);
        }

        private String getProjectsFormat() {

            return format("%s/%s/_apis/projects/%s?%s",
                    adoProperties.getUrl(), namespace, repo, apiVersion);
        }

        @Override
        void deleteFile() {
            String Path = "/encode.frm";
            String createdFileSha = getLastOldObject();
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
            String deleteFileUrl = getDeleteFileFormat();
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
            changes.put("changeType", "add")
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
            log.info("Commit object Created = {}", object);

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

        @Override
        void createPR() {
            throw new PendingException(); 
            // TODO Auto-generated method stub

            // POST https://dev.azure.com/fabrikam/_apis/git/repositories/3411ebc1-d5aa-464f-9615-0b527bc66719/pullrequests?api-version=5.1
            
            // {
            //     "sourceRefName": "refs/heads/npaulk/my_work",
            //     "targetRefName": "refs/heads/new_feature",
            //     "title": "A new feature",
            //     "description": "Adding a new feature",
            //     "reviewers": [
            //       {
            //         "id": "d6245f20-2af8-44f4-9451-8107cb2767db"
            //       }
            //     ]
            //   }

        }

        @Override
        void deletePR() {
            throw new PendingException();
            // TODO Auto-generated method stub

        }

        @Override
        void verifyPRUpdated() {
            throw new PendingException();
        }

    };

    private HookType hookType;
    /* where to push the file */
    static final String filePath = "src/main/java/sample/encode.frm";

    static Repository setTo(String toRepository, GenericEndToEndSteps genericEndToEndSteps) {
        log.info("setting repository to {}", toRepository);
        Repository repo = valueOf(toRepository.toUpperCase());
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
            Properties properties = getProperties("HookProperties");
            namespace = properties.getProperty(upperCaseName + "_namespace");
            repo = properties.getProperty(upperCaseName + "_repo");
            hookTargetURL = properties.getProperty(upperCaseName + "_target");
        } else {
            log.info("running with system variables");
            namespace = System.getenv(upperCaseName + "_HOOK_NAMESPACE");
            repo = System.getenv(upperCaseName + "_HOOK_REPO");
            hookTargetURL = System.getenv(upperCaseName + "_HOOK_TARGET");
        }
    }

    protected void generateWebHook(HookType hookType) {
        log.info("testing if repository already has hooks configured");
        assertTrue(!hasWebHook(), "repository already has hooks configured");
        this.hookType = hookType;
        log.info("creating the webhook ({})", hookType);
        generateHook(hookType);
    }

    protected Properties getProperties(String propertiesName) {
        Properties prop = new Properties();
        String path = new StringJoiner(File.separator, File.separator, "")
                .add("cucumber")
                .add("features")
                .add("e2eTests")
                .add(String.format("%s_%s.properties", propertiesName, name()))
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

    abstract void generateHook(HookType hookType);
    abstract void deleteHook();

    abstract HttpHeaders getHeaders();

    abstract void pushFile(String content);

    protected ResponseEntity<String> getResponseEntity(String requestUrl) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHeaders();
        HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);
        return restTemplate.exchange(requestUrl, HttpMethod.GET, requestEntity, String.class);
    }

    abstract void deleteFile();
    
    abstract void createPR();
    abstract void deletePR();
    abstract void verifyPRUpdated();

    protected String namespace = null;
    protected String repo = null;
    protected String hookTargetURL = null;

    public void cleanup() {
        Optional.ofNullable(hookType).ifPresent(h -> {
            switch (h) {
                case PUSH:
                    deleteHook();
                    deleteFile();
                    break;
                case PULL_REQUEST:
                    deleteHook();
                    deletePR();
                    break;
            }
        });
    }

}