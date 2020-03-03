package com.checkmarx.flow.cucumber.integration.azure.publishing;

import com.checkmarx.flow.config.ADOProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Nullable;
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

    public void ensureProjectExists(String projectName) {
        if (!projectExists(projectName)) {
            String operationId = queueProjectCreation(projectName);
            waitUntilProjectIsCreated(operationId);
        }
    }

    private boolean projectExists(String projectName) {
        HttpHeaders headers = getHeaders();
        HttpEntity<?> entity = new HttpEntity<>(headers);

        String url = getResourceUrl("projects", null);
        ResponseEntity<ObjectNode> response = restClient.exchange(url, HttpMethod.GET, entity, ObjectNode.class);
        return StreamSupport.stream(response.getBody().get("value").spliterator(), false)
                .anyMatch(withSame(projectName));
    }

    private String queueProjectCreation(String projectName) {
        ObjectNode body = null;
        try {
            body = (ObjectNode) objectMapper.readTree(PROJECT_CREATION_REQUEST_TEMPLATE);
            body.put("name", projectName);
        } catch (JsonProcessingException e) {
            log.error("JSON parse error.", e);
        }

        HttpHeaders headers = getHeaders();
        HttpEntity<?> entity = new HttpEntity<>(body, headers);

        String url = getResourceUrl("projects", null);
        ResponseEntity<ObjectNode> response = restClient.exchange(url, HttpMethod.POST, entity, ObjectNode.class);
        String operationId = response.getBody().get("id").textValue();
        log.info("Queued project creation: operation ID {}.", operationId);
        return operationId;
    }

    private void waitUntilProjectIsCreated(String operationId) {
        Awaitility.await()
                .atMost(WAITING_TIMEOUT)
                .pollDelay(POLL_INTERVAL)
                .until(() -> projectIsCreated(operationId));
    }

    private boolean projectIsCreated(String operationId) {
        String url = getResourceUrl("operations", operationId);
        HttpHeaders headers = getHeaders();
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<ObjectNode> response = restClient.exchange(url, HttpMethod.GET, entity, ObjectNode.class);

        String status = response.getBody().get("status").textValue();
        log.info("Status: {}.", status);
        return status.equals("succeeded");
    }

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();

        List<MediaType> mediaTypes = new ArrayList<>();
        mediaTypes.add(MediaType.APPLICATION_JSON);
        headers.setAccept(mediaTypes);

        headers.setContentType(MediaType.APPLICATION_JSON);

        headers.setBasicAuth("", adoProperties.getToken());
        return headers;
    }

    private String getResourceUrl(String resourceName, @Nullable String id) {
        return UriComponentsBuilder.fromHttpUrl(adoProperties.getUrl())
                .pathSegment("_apis", resourceName, id)
                .queryParam("api-version", adoProperties.getApiVersion())
                .toUriString();

    }

    private Predicate<JsonNode> withSame(String projectName) {
        return node -> node.get("name").textValue().equals(projectName);
    }
}
