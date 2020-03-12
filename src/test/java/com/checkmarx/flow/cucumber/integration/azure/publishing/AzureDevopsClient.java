package com.checkmarx.flow.cucumber.integration.azure.publishing;

import com.checkmarx.flow.config.ADOProperties;
import com.checkmarx.flow.dto.Issue;
import com.checkmarx.flow.dto.azure.CreateWorkItemAttr;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.awaitility.Awaitility;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
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

    static final String PROJECT_NAME_KEY = "projectName";
    private static final String ID_KEY = "id";
    static final String DEFAULT_BRANCH = "master";

    private static final Duration WAITING_TIMEOUT = Duration.ofMinutes(1);
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(1);


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

        HttpComponentsClientHttpRequestFactory factorySupportingPatch = new HttpComponentsClientHttpRequestFactory();
        restClient.setRequestFactory(factorySupportingPatch);
    }

    public void ensureProjectExists(String projectName) throws IOException {
        if (!projectExists(projectName)) {
            String operationId = queueProjectCreation(projectName);
            waitUntilProjectIsCreated(operationId);
        }
    }

    public void deleteProjectIssues(String projectName) throws IOException {
        List<String> issueIds = getProjectIssueIds(projectName);
        for (String issueId : issueIds) {
            deleteIssue(projectName, issueId);
        }
    }

    public String createIssue(Issue issue) throws IOException {
        log.info("Creating ADO issue: {}.", objectMapper.writeValueAsString(issue));
        String projectName = issue.getMetadata().get(AzureDevopsClient.PROJECT_NAME_KEY);
        String url = getIssueCreationUrl(projectName);

        IssueRequestBuilder requestBuilder = new IssueRequestBuilder();
        List<CreateWorkItemAttr> body = requestBuilder.getEntityBodyForCreation(issue);

        HttpHeaders headers = getIssueHeaders();
        HttpEntity<?> request = new HttpEntity<>(body, headers);

        ResponseEntity<ObjectNode> response = restClient.exchange(url, HttpMethod.POST, request, ObjectNode.class);
        return getNewIssueId(response);
    }

    public void updateIssueState(Issue issue, String newState) {
        log.info("Setting state to {} for issue {}.", newState, issue.getId());
        String url = getIssueUpdateUrl(issue);

        IssueRequestBuilder requestBuilder = new IssueRequestBuilder();
        List<CreateWorkItemAttr> body = requestBuilder.getEntityBodyForUpdate(newState);

        HttpHeaders headers = getIssueHeaders();
        HttpEntity<?> request = new HttpEntity<>(body, headers);

        restClient.exchange(url, HttpMethod.PATCH, request, String.class);
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
        log.info("Getting project issue IDs.");

        ObjectNode requestBody = objectMapper.createObjectNode();

        // WIQL language is read-only, so potential parameter injection shouldn't do any harm.
        String wiqlQuery = String.format("Select System.Id From WorkItems Where System.TeamProject = '%s'", projectName);

        requestBody.put("query", wiqlQuery);
        HttpEntity<ObjectNode> request = getRequestEntity(requestBody);

        String url = getResourceUrl("wit/wiql", null);

        ResponseEntity<ObjectNode> response = restClient.exchange(url, HttpMethod.POST, request, ObjectNode.class);

        ObjectNode responseBody = extractBody(response);

        return StreamSupport.stream(responseBody.get("workItems").spliterator(), false)
                .map(issue -> issue.get(ID_KEY).asText())
                .collect(Collectors.toList());
    }

    private String getNewIssueId(ResponseEntity<ObjectNode> response) throws IOException {
        if (response.getBody() == null) {
            throw new IOException("Response body is missing.");
        }
        return response.getBody().get(ID_KEY).asText();
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

        boolean result = StreamSupport.stream(body.get("value").spliterator(), false)
                .anyMatch(withSame(projectName));

        log.info(result ? "Project {} already exists" : "Project {} doesn't exist.", projectName);
        return result;
    }

    private String queueProjectCreation(String projectName) throws IOException {
        log.info("Queueing project creation: {}", projectName);
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
        String operationId = extractBody(response).get(ID_KEY).textValue();
        log.info("Queued project creation: operation ID {}.", operationId);
        return operationId;
    }

    private List<Issue> getProjectIssuesByIds(List<String> issueIds, String projectName) throws IOException {
        if (issueIds.isEmpty()) {
            log.info("No issues found.");
            return new ArrayList<>();
        }

        log.info("Getting issues by IDs: {}", objectMapper.writeValueAsString(issueIds));
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
        log.info("Project creation status: {}.", status);

        return status.equals("succeeded");
    }

    private Function<JsonNode, Issue> toTypedIssue() {
        return rawIssue -> {
            Issue result = new Issue();
            result.setId(rawIssue.get(ID_KEY).asText());
            result.setBody(rawIssue.at("/fields/System.Description").textValue());
            result.setTitle(rawIssue.at("/fields/System.Title").textValue());
            result.setState(rawIssue.at("/fields/System.State").textValue());
            return result;
        };
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

    private HttpHeaders getIssueHeaders() {
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

    private String getIssueUpdateUrl(Issue issue) {
        String projectName = issue.getMetadata().get(AzureDevopsClient.PROJECT_NAME_KEY);
        return UriComponentsBuilder.fromHttpUrl(adoProperties.getUrl())
                .path("/{project}/_apis/wit/workitems/{id}")
                .query(apiVersionParam)
                .buildAndExpand(projectName, issue.getId())
                .toUriString();
    }

    private String getIssueByIdsUrl(List<String> issueIds, String projectName) {
        String joinedIds = StringUtils.join(issueIds, ',');
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
