package com.checkmarx.flow.cucumber.integration.azure.publishing;

import com.checkmarx.flow.config.ADOProperties;
import com.checkmarx.flow.dto.azure.CreateWorkItemAttr;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
import java.util.Spliterator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
@Slf4j
public class AzureDevopsClient {
    // Affects the list of valid issue states. CxFlow assumes this template by default.
    private static final String AGILE_PROJECT_TEMPLATE_ID = "adcc42ab-9882-485e-a3ed-7678f01f66bc";

    private static final Duration WAITING_TIMEOUT = Duration.ofMinutes(1);
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(1);

    static final String DEFAULT_BRANCH = "master";

    private static final String PROJECT_CREATION_REQUEST_TEMPLATE =
            ("{" +
                    "  'name': '',                      " +
                    "  'description': '',               " +
                    "  'capabilities': {                " +
                    "    'versioncontrol': {            " +
                    "      'sourceControlType': 'Git'   " +
                    "    },                             " +
                    "    'processTemplate': {           " +
                    "      'templateTypeId': '" + AGILE_PROJECT_TEMPLATE_ID + "'" +
                    "    }" +
                    "  }" +
                    "}").replace("'", "\"");

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String apiVersionParam;

    private final RestTemplate restClient = new RestTemplate();

    private final ADOProperties adoProperties;

    public AzureDevopsClient(ADOProperties adoProperties) {
        this.adoProperties = adoProperties;
        apiVersionParam = "api-version=" + adoProperties.getApiVersion();
    }

    public void ensureProjectExists(String projectName) throws IOException {
        if (!projectExists(projectName)) {
            String operationId = queueProjectCreation(projectName);
            waitUntilProjectIsCreated(operationId);
        }
    }

    public int getIssueCount(String projectName) throws IOException {
        return getProjectIssueIds(projectName).size();
    }

    public void deleteProjectIssues(String projectName) throws IOException {
        List<String> issueIds = getProjectIssueIds(projectName);
        for (String issueId : issueIds) {
            deleteIssue(projectName, issueId);
        }
    }

    public void createIssue(Issue issue) {
        log.info("Creating ADO issue: {}.", issue);
        String url = getIssueCreationUrl(issue.getProjectName());

        IssueCreationRequestBuilder bodyBuilder = new IssueCreationRequestBuilder();
        List<CreateWorkItemAttr> body = bodyBuilder.getHttpEntityBody(issue);

        HttpHeaders headers = getNewIssueHeaders();
        HttpEntity<?> request = new HttpEntity<>(body, headers);

        restClient.exchange(url, HttpMethod.POST, request, String.class);
    }

    public List<Issue> getIssues(String projectName) throws IOException {
        List<String> issueIds = getProjectIssueIds(projectName);
        return getProjectIssuesByIds(issueIds, projectName);
    }

    private void deleteIssue(String projectName, String issueId) {
        log.info("Deleting ADO issue, ID: {}", issueId);
        String url = getIssueDeletionUrl(projectName, issueId);
        HttpEntity<?> request = getRequestEntity(null);
        restClient.exchange(url, HttpMethod.DELETE, request, String.class);
    }

    private List<String> getProjectIssueIds(String projectName) throws IOException {
        ObjectNode requestBody = objectMapper.createObjectNode();

        // WIQL language is read-only, so potential parameter injection shouldn't do any harm.
        String wiqlQuery = String.format("Select System.Id From WorkItems Where System.TeamProject = '%s'", projectName);

        requestBody.put("query", wiqlQuery);
        HttpEntity<ObjectNode> request = getRequestEntity(requestBody);

        String url = getResourceUrl("wit/wiql", null);

        ResponseEntity<ObjectNode> response = restClient.exchange(url, HttpMethod.POST, request, ObjectNode.class);

        ObjectNode responseBody = extractBody(response);

        return StreamSupport.stream(responseBody.get("workItems").spliterator(), false)
                .map(issue -> issue.get("id").asText())
                .collect(Collectors.toList());
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

    private List<Issue> getProjectIssuesByIds(List<String> issueIds, String projectName) throws IOException {
        String url = getIssueByIdsUrl(issueIds, projectName);
        HttpEntity<?> request = getRequestEntity(null);

        ResponseEntity<ObjectNode> response = restClient.exchange(url, HttpMethod.GET, request, ObjectNode.class);

        Spliterator<JsonNode> spliterator = extractBody(response).get("value").spliterator();
        return StreamSupport.stream(spliterator, false)
                .map(toTypedIssue())
                .collect(Collectors.toList());
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

    private Function<JsonNode, Issue> toTypedIssue() {
        return rawIssue -> Issue.builder()
                .id(rawIssue.get("id").asText())
                .description(rawIssue.at("/fields/System.Description").textValue())
                .title(rawIssue.at("/fields/System.Title").textValue())
                .state(rawIssue.at("/fields/System.State").textValue())
                .build();
    }

    private <T> HttpEntity<T> getRequestEntity(@Nullable T body) {
        HttpHeaders headers = new HttpHeaders();

        List<MediaType> mediaTypes = new ArrayList<>();
        mediaTypes.add(MediaType.APPLICATION_JSON);
        headers.setAccept(mediaTypes);

        headers.setContentType(MediaType.APPLICATION_JSON);

        setAuthentication(headers);

        return new HttpEntity<>(body, headers);
    }

    private HttpHeaders getNewIssueHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, "application/json-patch+json");
        setAuthentication(headers);
        return headers;
    }

    private void setAuthentication(HttpHeaders headers) {
        headers.setBasicAuth("", adoProperties.getToken());
    }

    private String getResourceUrl(String resourceName, @Nullable String id) {
        return UriComponentsBuilder.fromHttpUrl(adoProperties.getUrl())
                .path("/_apis/{resource}/{id}")
                .query(apiVersionParam)
                .buildAndExpand(resourceName, id)
                .toUriString();
    }

    private String getIssueDeletionUrl(String projectName, String issueId) {
        return UriComponentsBuilder.fromHttpUrl(adoProperties.getUrl())
                .path("/{project}/_apis/wit/workitems/{id}")
                .query("{version}&destroy=true")
                .buildAndExpand(projectName, issueId, apiVersionParam)
                .toUriString();
    }

    private String getIssueCreationUrl(String projectName) {
        return UriComponentsBuilder.fromHttpUrl(adoProperties.getUrl())
                .path("/{project}/_apis/wit/workitems/${issue-type}")
                .query(apiVersionParam)
                .buildAndExpand(projectName, adoProperties.getIssueType())
                .toUriString();
    }

    private String getIssueByIdsUrl(List<String> issueIds, String projectName) {
        String joinedIds = StringUtils.join(issueIds,',');
        return UriComponentsBuilder.fromHttpUrl(adoProperties.getUrl())
                .path("/{project}/_apis/wit/workitems")
                .query("{version}&ids={ids}&fields=System.Description,System.Title,System.State")
                .buildAndExpand(projectName, apiVersionParam, joinedIds)
                .toUriString();
    }

    private Predicate<JsonNode> withSame(String projectName) {
        return node -> node.get("name").textValue().equals(projectName);
    }
}
