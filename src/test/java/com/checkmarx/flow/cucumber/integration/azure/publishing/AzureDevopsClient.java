package com.checkmarx.flow.cucumber.integration.azure.publishing;

import com.checkmarx.flow.config.ADOProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Nullable;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

@Component
@Slf4j
public class AzureDevopsClient {
    private static final String BASIC_PROJECT_TEMPLATE_ID = "b8a3a935-7e91-48b8-a94c-606d37c3e9f2";
    private static final Duration WAITING_TIMEOUT = Duration.ofMinutes(1);
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(1);

    public static final String API_SEGMENT = "_apis";
    public static final String API_VERSION_PARAM = "api-version";

    private static final String PROJECT_CREATION_REQUEST_TEMPLATE =
            ("{" +
                    "  'name': '',                      " +
                    "  'description': '',               " +
                    "  'capabilities': {                " +
                    "    'versioncontrol': {            " +
                    "      'sourceControlType': 'Git'   " +
                    "    },                             " +
                    "    'processTemplate': {           " +
                    "      'templateTypeId': '" + BASIC_PROJECT_TEMPLATE_ID + "'" +
                    "    }" +
                    "  }" +
                    "}").replace("'", "\"");

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final RestTemplate restClient = new RestTemplate();

    private final ADOProperties adoProperties;

    public AzureDevopsClient(ADOProperties adoProperties) {
        this.adoProperties = adoProperties;
    }

    public void ensureProjectExists(String projectName) throws IOException {
        if (!projectExists(projectName)) {
            String operationId = queueProjectCreation(projectName);
            waitUntilProjectIsCreated(operationId);
        }
    }

    public int getIssueCount(String projectName) throws IOException {
        return getProjectIssues(projectName).size();
    }

    public void deleteProjectIssues(String projectName) throws IOException {
        ArrayNode issues = getProjectIssues(projectName);
        for (JsonNode issue : issues) {
            String issueId = issue.get("id").asText();
            deleteIssue(projectName, issueId);
        }
    }

    private void deleteIssue(String projectName, String issueId) {
        log.info("Deleting ADO issue, ID: {}", issueId);
        String url = getIssueDeletionUrl(projectName, issueId);
        HttpEntity<?> request = getRequestEntity(null);
        restClient.exchange(url, HttpMethod.DELETE, request, String.class);
    }

    private ArrayNode getProjectIssues(String projectName) throws IOException {
        ObjectNode requestBody = objectMapper.createObjectNode();

        // WIQL language is read-only, so potential parameter injection shouldn't do any harm.
        String wiqlQuery = String.format("Select System.Id From WorkItems Where System.TeamProject = '%s'", projectName);

        requestBody.put("query", wiqlQuery);
        HttpEntity<ObjectNode> request = getRequestEntity(requestBody);

        String url = getResourceUrl("wit/wiql", null);

        ResponseEntity<ObjectNode> response = restClient.exchange(url, HttpMethod.POST, request, ObjectNode.class);

        ObjectNode responseBody = extractBody(response);
        return (ArrayNode) responseBody.get("workItems");
    }

    private ObjectNode extractBody(HttpEntity<ObjectNode> response) throws IOException {
        ObjectNode body = response.getBody();
        if (body != null) {
            return body;
        } else {
            throw new IOException("Response body is null.");
        }
    }

    private boolean projectExists(String projectName) throws IOException {
        HttpEntity<?> request = getRequestEntity(null);

        String url = getResourceUrl("projects", null);
        ResponseEntity<ObjectNode> response = restClient.exchange(url, HttpMethod.GET, request, ObjectNode.class);
        ObjectNode body = extractBody(response);

        return StreamSupport.stream(body.get("value").spliterator(), false)
                .anyMatch(withSame(projectName));
    }

    private String queueProjectCreation(String projectName) throws IOException {
        ObjectNode body;
        try {
            body = (ObjectNode) objectMapper.readTree(PROJECT_CREATION_REQUEST_TEMPLATE);
            body.put("name", projectName);
        } catch (JsonProcessingException e) {
            log.error("JSON parse error.", e);
            throw new IOException(e);
        }

        String url = getResourceUrl("projects", null);
        HttpEntity<?> entity = getRequestEntity(body);
        ResponseEntity<ObjectNode> response = restClient.exchange(url, HttpMethod.POST, entity, ObjectNode.class);
        String operationId = extractBody(response).get("id").textValue();
        log.info("Queued project creation: operation ID {}.", operationId);
        return operationId;
    }

    private void waitUntilProjectIsCreated(String operationId) {
        Awaitility.await()
                .atMost(WAITING_TIMEOUT)
                .pollDelay(POLL_INTERVAL)
                .until(() -> projectIsCreated(operationId));
    }

    private boolean projectIsCreated(String operationId) throws IOException {
        String url = getResourceUrl("operations", operationId);
        HttpEntity<Object> request = getRequestEntity(null);

        ResponseEntity<ObjectNode> response = restClient.exchange(url, HttpMethod.GET, request, ObjectNode.class);

        String status = extractBody(response).get("status").textValue();
        log.info("Status: {}.", status);

        return status.equals("succeeded");
    }

    private <T> HttpEntity<T> getRequestEntity(@Nullable T body) {
        HttpHeaders headers = new HttpHeaders();

        List<MediaType> mediaTypes = new ArrayList<>();
        mediaTypes.add(MediaType.APPLICATION_JSON);
        headers.setAccept(mediaTypes);

        headers.setContentType(MediaType.APPLICATION_JSON);

        headers.setBasicAuth("", adoProperties.getToken());

        return new HttpEntity<>(body, headers);
    }

    private String getResourceUrl(String resourceName, @Nullable String id) {
        return UriComponentsBuilder.fromHttpUrl(adoProperties.getUrl())
                .pathSegment(API_SEGMENT)
                .path(resourceName)
                .pathSegment(id)
                .queryParam(API_VERSION_PARAM, adoProperties.getApiVersion())
                .toUriString();
    }

    private String getIssueDeletionUrl(String projectName, String issueId) {
        return UriComponentsBuilder.fromHttpUrl(adoProperties.getUrl())
                .path("/{project}/{api}/wit/workitems/{id}")
                .query("{version-param}={version}&destroy=true")
                .buildAndExpand(projectName, API_SEGMENT, issueId, API_VERSION_PARAM, adoProperties.getApiVersion())
                .toUriString();
    }

    private Predicate<JsonNode> withSame(String projectName) {
        return node -> node.get("name").textValue().equals(projectName);
    }
}
