package com.checkmarx.flow.cucumber.integration.end2end.genericendtoend;

import java.util.Arrays;

import com.checkmarx.flow.dto.github.Committer;
import com.checkmarx.flow.dto.github.Config;
import com.checkmarx.flow.dto.github.Hook;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import io.cucumber.java.PendingException;

/**
 * This is a helper class to interact with GitHub's API<br>
 * This class has no buisness logic
 */
public class GitHubApiHandler {
    
    /**
     *
     */
    private static final String URL_PATERN_HOOKS = "%s/%s/%s/hooks";
    private static final String URL_PATERN_CONTENTS = "%s/%s/%s/contents/%s";

    /**
     * Push a file to a GitHub repository, Using GitHub's API <br>
     * Note! the file must be encrypted in base64.
     * @param content content of the file in base64
     * @param message the commit message
     * @param committer the Committer doing the commit
     * @param headers must include the Authentication and content type.
     * @param apiUrl the url of GitHub (can be a local server)
     * @param namespace usually the company or user who created the project
     * @param repo the key of the repository
     * @param filePath where to put place the file in the repo
     * @return the Json of the response
     * @throws JsonProcessingException indicates problem in creating the file for commit
     * @throws Exception indicates an error
     */
    public JSONObject pushFile(String content , String message, Committer committer, HttpHeaders headers,
            String apiUrl, String namespace, String repo, String filePath) throws JsonProcessingException, Exception {
        return pushFile(content, message, committer, headers, apiUrl, namespace, repo, filePath, "master");
    }

    /**
     * Push a file to a GitHub repository, Using GitHub's API <br>
     * Note! the file must be encrypted in base64.
     * @param content content of the file in base64
     * @param message the commit message
     * @param committer the Committer doing the commit
     * @param headers must include the Authentication and content type.
     * @param apiUrl the url of GitHub (can be a local server)
     * @param namespace usually the company or user who created the project
     * @param repo the key of the repository
     * @param filePath where to put place the file in the repo
     * @param branch the branch to push to
     * @return the Json of the response
     * @throws JsonProcessingException indicates problem in creating the file for commit
     * @throws Exception indicates an error
     */
    public JSONObject pushFile(String content , String message, Committer committer, HttpHeaders headers,
            String apiUrl, String namespace, String repo, String filePath, String branch) throws JsonProcessingException, Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jo = mapper.createObjectNode();
        jo.put("message", "GitHubToJira test message");
        jo.putPOJO("committer", committer);
        jo.put("content", content);
        jo.put("branch", branch);
        String data;
        data = mapper.writeValueAsString(jo);
        final HttpEntity<String> request = new HttpEntity<>(data, headers);
        RestTemplate restTemplate = new RestTemplate();
        String path = String.format(URL_PATERN_CONTENTS, apiUrl, namespace, repo, filePath);
        ResponseEntity<String> response = restTemplate.exchange(path, HttpMethod.PUT, request, String.class);
        return new JSONObject(response.getBody());
    }

    public String getWebhookURL(String apiUrl, String namespace, String repo) {
        return String.format(URL_PATERN_HOOKS, apiUrl, namespace, repo);
    }

    public JSONArray getHooks(HttpHeaders headers, String apiUrl, String namespace, String repo) {
        String repoHooksBaseUrl = getWebhookURL(apiUrl, namespace, repo);
        return getJSONArray(repoHooksBaseUrl, headers);
    }

    public HttpEntity<Hook> generateHookEntity(HttpHeaders headers, String target, String token, HookType hookType) throws Exception {
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
        config.setUrl(target);
        config.setContentType("json");
        config.setInsecureSsl("0");
        config.setSecret(token);
        hook.setConfig(config);
        return new HttpEntity<>(hook, headers);
    }

    private JSONArray getJSONArray(String uri, HttpHeaders headers) {
        ResponseEntity<String> response = doGet(uri, headers);
        String body = response.getBody();
        return (body == null)
        ? new JSONArray()
        : new JSONArray(body);
    }

    private final ResponseEntity<String> doGet(String url, HttpHeaders headers) {
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);
        return restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
    }

}