package com.checkmarx.flow.utils;

import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.dto.HookType;
import com.checkmarx.flow.dto.github.Committer;
import com.checkmarx.flow.dto.github.Config;
import com.checkmarx.flow.dto.github.Hook;
import com.checkmarx.flow.dto.rally.Object;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.mutable.MutableInt;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import sun.security.util.PendingException;

import java.util.Arrays;
import java.util.Optional;

@Slf4j
public class GitHubAPIHandler {

    private GitHubProperties gitHubProperties;
    private String namespace, repo, filePath;

    public GitHubAPIHandler(GitHubProperties gitHubProperties, String namespace, String repo, String filePath) {
        this.gitHubProperties = gitHubProperties;
        this.namespace = namespace;
        this.repo = repo;
        this.filePath = filePath;
    }

    private ObjectMapper mapper = new ObjectMapper();
    
            
    public HttpHeaders getHeaders() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "token " + gitHubProperties.getToken());
        return headers;
    }


    private String getContentsFormat() {
        return String.format("%s/%s/%s/contents/%s",
                gitHubProperties.getApiUrl(), namespace, repo, filePath);
    }
    
    private ObjectNode createJavaObject(String name, String email, String message){
        ObjectNode jo = mapper.createObjectNode();
        jo.put("message", message);
        Committer committer = new Committer();
        committer.setName(name);
        committer.setEmail(email);
        jo.putPOJO("committer", committer);
        return jo;
    }
    public String pushFile(String content, String name, String email, String message) throws JsonProcessingException {

        ObjectNode jo = createJavaObject(name, email, message);
        jo.put("content", content);
        String data;
        try {
            data = mapper.writeValueAsString(jo);
        } catch (JsonProcessingException e) {
            String msg = "Failed to create file for push";
            log.error(msg);
            throw e;
        }
        try {
            final HttpHeaders headers = getHeaders();
            final HttpEntity<String> request = new HttpEntity<>(data, headers);
            RestTemplate restTemplate = new RestTemplate();

            String path = getContentsFormat();
            ResponseEntity<String> response = restTemplate.exchange(path, HttpMethod.PUT, request,
                    String.class);
            return new JSONObject(response.getBody()).getJSONObject("content").getString("sha");
        } catch (Exception e) {
            data = null;
            log.error(e.getMessage());
            throw e;

        }
    }

    public void deleteFile(String createdFileSha, String name, String email, String message) throws JsonProcessingException {
        RestTemplate restTemplate = new RestTemplate();
        final HttpHeaders headers = getHeaders();
        String data = null;
        try {

            ObjectNode jo = createJavaObject(name, email, message);
            jo.put("sha", createdFileSha);

            data = mapper.writeValueAsString(jo);
        } catch (Exception e) {
            String msg = "Failed to delete file of push";
            log.error(msg);
            throw e;
        }

        HttpEntity<String> requestEntity = new HttpEntity<>(data, headers);
        String path = getContentsFormat();
        restTemplate.exchange(path, HttpMethod.DELETE, requestEntity, String.class);
    }

    public ResponseEntity<String> createPR(String TITLE, String BODY, String BASE) throws JsonProcessingException {
        String data = createPRData(false, TITLE, BODY, BASE);
        final HttpHeaders headers = getHeaders();
        final HttpEntity<String> request = new HttpEntity<>(data, headers);

        RestTemplate restTemplate = new RestTemplate();
        String url = String.format("%s/%s/%s/pulls",
                gitHubProperties.getApiUrl(), namespace, repo);
        if (log.isInfoEnabled()) {
            throw new PendingException("createPR is waiting on deletePR, and parameters");
        }
        final ResponseEntity<String> response = restTemplate.postForEntity(url, request,
                String.class);

        return response;
        
    }

    private String createPRData(boolean isDelete, String title, String body, String base) throws JsonProcessingException {
        //TO DO: replace parameters 
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode pr = mapper.createObjectNode();
        pr.put("title", title)
                .put("body", body)
                .put("base", base);
        if (isDelete) {
            pr.put("state", "close");
        } else {
            pr.put("head", "feature");
        }
        String data = null;
        try {
            data = mapper.writeValueAsString(pr);
        } catch (JsonProcessingException e) {
            String msg = "faild to create GitHub PR data";
            log.error(msg);
            throw e;
        }
        return data;
    }


    public ResponseEntity<String> deletePR(String  TITLE, String BODY, String BASE, Integer prId) throws JsonProcessingException {
        String data = createPRData(true, TITLE, BODY, BASE);
        final HttpHeaders headers = getHeaders();
        final HttpEntity<String> request = new HttpEntity<>(data, headers);

        RestTemplate restTemplate = new RestTemplate();
        String url = String.format("%s/%s/%s/pulls/%d",
                gitHubProperties.getApiUrl(), namespace, repo, prId);
        final ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PATCH,
                request, String.class);
        return response;
    }
    
    public Boolean hasWebHook(MutableInt hookId) {
        String repoHooksBaseUrl = getHooksFormat();
        JSONArray hooks = getJSONArray(repoHooksBaseUrl);
        
        if(hooks == null){
            throw new IllegalStateException("could not create webhook configuration");
        }
        boolean hasHook = !hooks.isEmpty();
        if(hasHook){
            hookId.setValue(((JSONObject)hooks.get(0)).getInt("id"));
        }
        return hasHook;
    }

    private String getHooksFormat() {
        return String.format("%s/%s/%s/hooks",
                gitHubProperties.getApiUrl(), namespace, repo);
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

    
    public ResponseEntity<String> generateHook(HookType hookType, String hookTargetURL, MutableInt hookId) {
        Hook data = null;
        try {
            data = generateHookData(hookTargetURL, gitHubProperties.getWebhookToken(), hookType);
        } catch (Exception e) {
            throw new IllegalArgumentException("can not create web hook, check parameters");
        }
        final HttpHeaders headers = getHeaders();
        final HttpEntity<Hook> request = new HttpEntity<>(data, headers);
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = getHooksFormat();
            final ResponseEntity<String> response = restTemplate.postForEntity(url, request,
                    String.class);
            hookId.setValue(new JSONObject(response.getBody()).getInt("id"));
            return response;
        } catch (Exception e) {
            throw new IllegalStateException("failed to create hook " + e.getMessage());
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


    public void deleteHook(MutableInt hookId) {
        Optional.ofNullable(hookId).ifPresent(id -> {
            RestTemplate restTemplate = new RestTemplate();
            final HttpHeaders headers = getHeaders();
            HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);
            String url = getHooksFormat();
            restTemplate.exchange(url + "/" + hookId, HttpMethod.DELETE, requestEntity, Object.class);
        });
    }
}
