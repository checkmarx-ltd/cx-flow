package com.checkmarx.flow.utils;

import com.atlassian.jira.rest.client.api.StatusCategory;
import com.atlassian.jira.rest.client.api.domain.*;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.dto.jira.Fields;
import com.checkmarx.flow.dto.jira.JiraIssue;
import com.checkmarx.flow.dto.jira.JiraSearchRequest;
import com.checkmarx.flow.dto.jira.JiraSearchResponse;
import com.checkmarx.flow.exception.JiraClientRunTimeException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * This class provides utility methods to perform Jira searches using JQL for JIRA Cloud.
 * It supports both GET and POST requests based on the complexity of the JQL query.
 * It handles pagination to retrieve all matching issues and maps the results to
 * the appropriate domain objects.
 */

@Slf4j
@Component
public class JiraSearchUtils {
    private static final String ENHANCED_SEARCH_JQL = "%s/rest/api/3/search/jql";
    private final JiraProperties jiraProperties;
    private final RestTemplate restTemplate;

    public JiraSearchUtils(JiraProperties jiraProperties,@Qualifier("flowRestTemplate")RestTemplate restTemplate) {
        this.jiraProperties = jiraProperties;
        this.restTemplate = restTemplate;
    }

    /**
     * creates HttpHeaders with authentication details
     * @return HttpHeaders with auth information
     */
    private HttpHeaders createAuthHeaders() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        httpHeaders.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        if(jiraProperties.getTokenType()==null || jiraProperties.getTokenType().name().equalsIgnoreCase("API") || jiraProperties.getTokenType().name().equalsIgnoreCase("PASSWORD")){
            String credentials = String.format("%s:%s", jiraProperties.getUsername(), jiraProperties.getToken());
            String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
            httpHeaders.setBasicAuth(encodedCredentials);
        }
        else {
            httpHeaders.setBearerAuth(jiraProperties.getToken());
        }
        return httpHeaders;
    }

    /**
     * Main function to perform a Jira search using JQL.
     * It handles pagination and returns a list of all matching JiraIssue objects.
     * Chooses between GET and POST requests based on JQL complexity and length.
     * @param jql The JQL query string.
     * @param fields List of fields to retrieve for each issue.
     * @return List of JiraIssue objects matching the JQL query.
     */

    public List<JiraIssue> performJiraSearch( String jql, List<String> fields) {
        log.info("Perform EnhancedJQL Search...");
        List<JiraIssue> allIssues = new ArrayList<>();
        String nextPageToken = null;
        // Use pagination to retrieve all issues
        //nextPageToken is a string token provided by Jira to fetch the next set of results
        do {
            JiraSearchResponse response;

            // If JQL is large or contains complex characters, use POST
            if (shouldUsePostRequest(jql, fields)) {
                response = performPostSearch(jiraProperties.getUrl(), jql, fields, nextPageToken,createAuthHeaders());
            } else {
                response = performGetSearch( jiraProperties.getUrl(), jql, fields, nextPageToken,createAuthHeaders());
            }

            if (response != null && response.getIssues() != null) {
                allIssues.addAll(response.getIssues());
            }

            nextPageToken = response != null ? response.getNextPageToken() : null;

        } while (StringUtils.isNotEmpty(nextPageToken));

        return allIssues;
    }


    /**
     * Performs a Jira search and returns a SearchResult.
     * Uses pagination and maps the final result to SearchResult.
     * Chooses between GET and POST requests based on JQL complexity and length.
     * @param jql The JQL query string.
     * @param fields List of fields to retrieve for each issue.
     * @return SearchResult containing all matching issues.
     */
    public SearchResult performJiraSearchResult(String jql, List<String> fields) {
        log.info("Perform EnhancedJQL Search for SearchResult...");
        String nextPageToken = null;
        List<JiraIssue> allIssues = new ArrayList<>();

        do {
            JiraSearchResponse response;
            if (shouldUsePostRequest(jql, fields)) {
                response = performPostSearch(jiraProperties.getUrl(), jql, fields, nextPageToken, createAuthHeaders());
            } else {
                response = performGetSearch(jiraProperties.getUrl(), jql, fields, nextPageToken, createAuthHeaders());
            }

            if (response != null && response.getIssues() != null) {
                allIssues.addAll(response.getIssues());
            }
            //lastResponse = response;
            nextPageToken = response != null ? response.getNextPageToken() : null;
        } while (StringUtils.isNotEmpty(nextPageToken));

        // Build a SearchResult using the last response's paging info and all collected issues
        JiraSearchResponse resultDto = new JiraSearchResponse();
            resultDto.setStartAt(0);
            resultDto.setMaxResults(allIssues.size());
            resultDto.setTotal(allIssues.size());
        resultDto.setIssues(allIssues);

        return mapJiraSearchResponseToSearchResult(resultDto);
    }

    /**
     * Perform a check to determine if a POST request should be used based on JQL complexity and length.
     * @param jql The JQL query string.
     * @param fields List of fields to retrieve for each issue.
     * @return true if POST should be used, false for GET.
     */
    private boolean shouldUsePostRequest(String jql, List<String> fields) {
        // Use POST if:
        // 1. JQL is longer than 1500 characters (safe margin for URL limits)
        // 2. JQL contains special characters that might cause URL encoding issues
        // 3. Total URL would exceed safe length

        String encodedJql = URLEncoder.encode(jql, StandardCharsets.UTF_8);
        int estimatedUrlLength;
        if(fields!=null){
            String fieldsParam = String.join(",", fields);
            estimatedUrlLength= jiraProperties.getUrl().length() +
                    ENHANCED_SEARCH_JQL.length() +
                    "?jql=".length() + encodedJql.length() +
                    "&maxResults=".length() + String.valueOf(jiraProperties.getMaxJqlResults()).length() +
                    "&fields=".length() + fieldsParam.length();
        }else{
            estimatedUrlLength = jiraProperties.getUrl().length() +
                    ENHANCED_SEARCH_JQL.length() +
                    "?jql=".length() + encodedJql.length() +
                    "&maxResults=".length() + String.valueOf(jiraProperties.getMaxJqlResults()).length();
        }

        // Estimate URL length
        return jql.length() > 1500 || estimatedUrlLength > 1800 ||
                jql.contains("\"") || jql.contains("'") || jql.contains("&");
    }

    /**
     *
     * Performs a GET request to the Jira search API with the provided JQL and fields.
     * @param baseUrl Base URL of the Jira instance.
     * @param jql The JQL query string.
     * @param fields List of fields to retrieve for each issue.
     * @param nextPageToken Token for pagination to fetch the next set of results.
     * @param authHeaders HttpHeaders containing authentication details.
     * @return JiraSearchResponse containing the search results.
     */
    private JiraSearchResponse performGetSearch(String baseUrl,
                                                String jql, List<String> fields, String nextPageToken,HttpHeaders authHeaders) {
        try {
            String getUrl = String.format(ENHANCED_SEARCH_JQL,baseUrl);
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(getUrl)
                    .queryParam("jql", jql)
                    .queryParam("fields", String.join(",", fields))
                    .queryParam("maxResults", jiraProperties.getMaxJqlResults());

            if (StringUtils.isNotEmpty(nextPageToken)) {
                uriBuilder.queryParam("nextPageToken", nextPageToken);
            }

            String url = uriBuilder.build().toUriString();
            HttpEntity<?> entity = new HttpEntity<>(authHeaders);

            log.debug("GET Request URL: {}", url);

            ResponseEntity<JiraSearchResponse> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, JiraSearchResponse.class);

            return response.getBody();

        } catch (HttpClientErrorException e) {
            throw new JiraClientRunTimeException("Error occurred during GET request to Jira: " + e.getMessage());
        }
    }

    /**
     * Performs a POST request to the Jira search API with the provided JQL and fields.
     * @param baseUrl Base URL of the Jira instance.
     * @param jql The JQL query string.
     * @param fields List of fields to retrieve for each issue.
     * @param nextPageToken Token for pagination to fetch the next set of results.
     * @param authHeaders HttpHeaders containing authentication details.
     * @return JiraSearchResponse containing the search results.
     */

    private JiraSearchResponse performPostSearch( String baseUrl,
                                                 String jql, List<String> fields, String nextPageToken,HttpHeaders authHeaders) {
        try {
            String url = String.format(ENHANCED_SEARCH_JQL,baseUrl);

            JiraSearchRequest request = new JiraSearchRequest();
            request.setJql(jql);
            request.setFields(fields);
            request.setMaxResults(jiraProperties.getMaxJqlResults());
            if (StringUtils.isNotEmpty(nextPageToken)) {
                request.setNextPageToken(nextPageToken);
            }

            HttpEntity<JiraSearchRequest> entity = new HttpEntity<>(request, authHeaders);

            log.debug("POST Request URL: {}, Body JQL={}, nextPageToken={}", url, jql, nextPageToken);

            return restTemplate.postForObject(url, entity, JiraSearchResponse.class);
        } catch (HttpClientErrorException e) {
            throw new JiraClientRunTimeException("Error occurred during POST request to Jira: " + e.getMessage(),e);
        }

    }

    /**
     * Maps a JiraIssue DTO to an Issue(jira rest client) domain object.
     * Only maps the most relevant fields; others are set to null or empty as needed.
     * @param dto JiraIssue DTO
     * @return Mapped Issue object
     */

    public Issue mapJiraIssueToIssue(JiraIssue dto) {
        if (dto == null || dto.getFields() == null) {
            log.debug("JiraIssue or its fields are null");
            return null;
        }

        Fields fields = dto.getFields();
        // Map only the most relevant fields; others set to null or empty as needed
        // we only need fields "key", "project", "issuetype", "summary", labels, "created", "updated", "status"
        return new Issue(
                fields.getSummary(), //summary
                dto.getSelf(), // self
                dto.getKey(), // key
                parseId(dto.getId()),  // id
                mapProject(fields.getProject()), // project
                mapIssueType(fields.getIssueType()),// issueType
                mapStatus(fields.getStatus()),// status
                convertAdfToMarkdown(fields.getDescription()), // description
                mapPriority(fields.getPriority()), // priority
                null, // resolution
                List.of(), // attachments
                null, // reporter
                null, // assignee
                toDateTime(fields.getCreated()), // created
                toDateTime(fields.getUpdated()), // updated
                null, // dueDate
                List.of(), // affectedVersions
                List.of(), // fixVersions
                List.of(), // components
                null, // timeTracking
                List.of(), // issueFields
                List.of(), // comments
                null, // transitionsUri
                List.of(), // issueLinks
                null, // votes
                List.of(), // worklogs
                null, // watchers
                List.of(), // expandos
                List.of(), // subtasks
                null, // changelog
                null, // operations
                fields.getLabels() // labels
        );
    }

    private BasicPriority mapPriority(com.checkmarx.flow.dto.jira.Priority priority) {
        if (priority == null) {
            log.debug("Priority is null");
            return null;
        }
        return new BasicPriority(priority.getSelf(),parseId(priority.getId()), priority.getName());
    }


    private Long parseId(String id) {
        try {
            return id != null ? Long.valueOf(id) : null;
        } catch (NumberFormatException e) {
            log.debug("Invalid issue ID: {}", id, e);
            return null;
        }
    }

    private BasicProject mapProject(com.checkmarx.flow.dto.jira.Project project) {
        if (project == null) {
            log.debug("Project is null");
            return null;
        }
        return new BasicProject(project.getSelf(), project.getKey(), project.getId(), project.getName());
    }

    private IssueType mapIssueType(com.checkmarx.flow.dto.jira.IssueType issueType) {
        if (issueType == null) {
            log.debug("IssueType is null");
            return null;
        }
        return new IssueType(
                issueType.getSelf(),
                issueType.getId(),
                issueType.getName(),
                issueType.isSubtask(),
                issueType.getDescription(),
                issueType.getIconUrl()
        );
    }

    private Status mapStatus(com.checkmarx.flow.dto.jira.Status status) {
        if (status == null) {
            log.debug("Status is null");
            return null;
        }
        com.checkmarx.flow.dto.jira.StatusCategory category = status.getStatusCategory();
        StatusCategory mappedCategory = null;
        if (category != null) {
            mappedCategory = new StatusCategory(
                    category.getSelf(),
                    category.getName(),
                    category.getId(),
                    category.getKey(),
                    category.getColorName()
            );
        }
        return new Status(
                status.getSelf(),
                status.getId(),
                status.getName(),
                status.getDescription(),
                status.getIconUrl(),
                mappedCategory
        );
    }

    private DateTime toDateTime(String date) {
        return date != null ? new DateTime(date) : null;
    }

    /**
     * Maps a JiraSearchResponse DTO to a SearchResult.
     * @param dto JiraSearchResponse DTO
     * @return SearchResult with mapped issues
     */
    public SearchResult mapJiraSearchResponseToSearchResult(JiraSearchResponse dto) {
        if (dto == null || dto.getIssues() == null) {
            log.debug("JiraSearchResponse or its issues are null");
            return new SearchResult(0, 0, 0, List.of());
        }
        List<Issue> mappedIssues = new ArrayList<>();
        for (com.checkmarx.flow.dto.jira.JiraIssue jiraIssue : dto.getIssues()) {
            Issue mapped = mapJiraIssueToIssue(jiraIssue);
            if (mapped != null) {
                mappedIssues.add(mapped);
            }
        }
        int startIndex = dto.getStartAt() != null ? dto.getStartAt() : 0;
        int maxResults = dto.getMaxResults() != null ? dto.getMaxResults() : mappedIssues.size();
        int total = dto.getTotal() != null ? dto.getTotal() : mappedIssues.size();
        return new SearchResult(startIndex, maxResults, total, mappedIssues);
    }


    // function to convert ADF (Atlassian Document Format) to Markdown
    //only used for testing purposes
    public String convertAdfToMarkdown(Object adfObject) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.valueToTree(adfObject);
            StringBuilder sb = new StringBuilder();

            // Handle root node with type "doc"
            if (root.has("type") && "doc".equals(root.get("type").asText()) && root.has("content")) {
                JsonNode content = root.get("content");
                if (content.isArray()) {
                    for (JsonNode node : content) {
                        processNode(node, sb);
                    }
                }
            } else if (root.has("doc") && root.get("doc").has("content")) {
                JsonNode content = root.get("doc").get("content");
                if (content.isArray()) {
                    for (JsonNode node : content) {
                        processNode(node, sb);
                    }
                }
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return null;
        }
    }
    //helper function to process each node based on its type
    private void processNode(JsonNode node, StringBuilder sb) {
        String type = node.get("type").asText();

        switch (type) {
            case "paragraph":
                JsonNode paragraphContent = node.get("content");
                if (paragraphContent != null) {
                    for (JsonNode child : paragraphContent) {
                        handleContent(child, sb);
                    }
                }
                sb.append("\n\n");
                break;

            case "rule":
                sb.append("---\n\n");
                break;

            case "codeBlock":
                sb.append("```");
                String language = node.has("attrs") && node.get("attrs").has("language")
                        ? node.get("attrs").get("language").asText()
                        : "";
                if (!language.isEmpty()) {
                    sb.append(language);
                }
                sb.append("\n");
                if (node.has("content")) {
                    for (JsonNode codeLine : node.get("content")) {
                        if (codeLine.has("text")) {
                            sb.append(codeLine.get("text").asText()).append("\n");
                        }
                    }
                }
                sb.append("```\n\n");
                break;

            default:
                // skip unsupported types
                break;
        }
    }
    //helper function to handle text and hardBreak nodes
    private void handleContent(JsonNode child, StringBuilder sb) {
        String type = child.get("type").asText();
        if ("text".equals(type)) {
            String text = child.get("text").asText();
            if (child.has("marks")) {
                for (JsonNode mark : child.get("marks")) {
                    String markType = mark.get("type").asText();
                    switch (markType) {
                        case "strong":
                            text = "**" + text + "**";
                            break;
                        case "link":
                            String href = mark.get("attrs").get("href").asText();
                            text = "[" + text + "](" + href + ")";
                            break;
                    }
                }
            }
            sb.append(text);
        } else if ("hardBreak".equals(type)) {
            sb.append("\n");
        }
    }


}
