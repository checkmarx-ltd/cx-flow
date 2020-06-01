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

import com.checkmarx.flow.config.ADOProperties;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.dto.HookType;
import com.checkmarx.flow.dto.azure.ConsumerInputs;
import com.checkmarx.flow.dto.azure.PublisherInputs;
import com.checkmarx.flow.dto.azure.Subscription;
import com.checkmarx.flow.dto.github.Config;
import com.checkmarx.flow.dto.github.Hook;
import com.checkmarx.flow.dto.rally.Object;
import com.checkmarx.flow.utils.GitHubAPIHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.commons.lang3.mutable.MutableInt;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import cucumber.api.PendingException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
enum Repository {
    GITHUB {
        private GitHubAPIHandler handler;
        GitHubProperties gitHubProperties;
        private MutableInt hookId = new MutableInt(0);
        private String createdFileSha;
        private Integer prId;

        @Override
        public Boolean hasWebHook() {
            try {
                return handler.hasWebHook(hookId);
            }
            catch(IllegalStateException e){
                fail("could not create webhook configuration");
            }
            return false;
        }
        


        @Override
        void generateHook(HookType hookType) {
            Hook data = null;
            try {
                ResponseEntity<String> response = handler.generateHook(hookType, hookTargetURL, hookId);
                assertEquals(HttpStatus.CREATED, response.getStatusCode());
            } catch (Exception e) {
                fail(e.getMessage());
            }
        }
         
        @Override
        void deleteHook() {
            if(hookId.getValue() != 0) {
                handler.deleteHook(hookId);
            }
        }

        @Override
        HttpHeaders getHeaders() {
            return handler.getHeaders();
        }

        @Override
        void pushFile(String content) {

            try {
                createdFileSha = handler.pushFile(content, USER, EMAIL, MESSAGE);
            } catch (JsonProcessingException e) {
                fail(e.getMessage());
            }catch (Exception e) {
                fail("faild to push a file: " + e.getMessage());
            }
        }

        @Override
        void deleteFile() {
            
            try {
                handler.deleteFile(createdFileSha, USER, EMAIL, MESSAGE);
            } catch (JsonProcessingException e) {
                fail(e.getMessage());
            }catch (Exception e) {
                fail("faild to push a file: " + e.getMessage());
            }
       }
       

        @Override
        protected void init(GenericEndToEndSteps genericEndToEndSteps) {
            gitHubProperties = genericEndToEndSteps.gitHubProperties;
            super.init(genericEndToEndSteps);
            this.handler = new GitHubAPIHandler(gitHubProperties, namespace, repo, filePath);
        }

        @Override
        void createPR() {
            try {
                final ResponseEntity<String> response = handler.createPR(TITLE, BODY, BASE, prId);
                assertEquals(HttpStatus.CREATED, response.getStatusCode());
                prId = new JSONObject(response.getBody()).getInt("id");
            }catch (PendingException p){
                throw p;
            }
            catch (Exception e) {
                fail("failed to create PR " + e.getMessage());
            }
        }



        @Override
        void deletePR() {
            try {
                final ResponseEntity<String> response = this.handler.deletePR(TITLE, BODY, BASE, prId);
                assertEquals(HttpStatus.OK, response.getStatusCode());
            }
            catch (Exception e) {
                fail("failed to delete PR " + e.getMessage());
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

        private ResponseEntity<String> getResponseEntity(String requestUrl) {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = getHeaders();
            HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);
            return restTemplate.exchange(requestUrl, HttpMethod.GET, requestEntity, String.class);
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

    };

    private static final String MESSAGE = "GitHubToJira test message";
    private static final String EMAIL = "CxFlowTestUser@checkmarx.com";
    private static final String USER = "CxFlowTestUser";
    private static final String BASE = "master";
    private static final String BODY = "This is an automated test";
    private static final String TITLE = "cxflow GitHub e2e test";
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
            Properties properties = getProperties();
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
        log.info("testing if repository alredy has hooks configured");
        //assertTrue(!hasWebHook(), "repository alredy has hooks configured");
        this.hookType = hookType;
        log.info("creating the webhook ({})", hookType);
        try {
            if (hasWebHook()) {
                deleteHook();
            }
            //deleteFileByParam();
        } catch (Exception e) {/*do nothing */}
        
        generateHook(hookType);
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

    abstract void generateHook(HookType hookType);
    abstract void deleteHook();

    abstract HttpHeaders getHeaders();

    abstract void pushFile(String content);
    abstract void deleteFile();
    
    abstract void createPR();
    abstract void deletePR();

    protected String namespace = null;
    protected String repo = null;
    protected String hookTargetURL = null;

    public void cleanup() {
        switch (hookType) {
            case PUSH:
                deleteHook();
                deleteFile();        
                break;
            case PULL_REQUEST:
                deleteHook();
                deletePR();
                break;
        }
    }

}