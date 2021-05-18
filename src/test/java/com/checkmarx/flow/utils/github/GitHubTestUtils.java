package com.checkmarx.flow.utils.github;

import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.controller.GitHubController;
import com.checkmarx.flow.cucumber.common.Constants;
import com.checkmarx.flow.cucumber.common.utils.TestUtils;
import com.checkmarx.flow.custom.GitHubIssueTracker;
import com.checkmarx.flow.dto.ControllerRequest;
import com.checkmarx.flow.dto.Issue;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.github.PullEvent;
import com.checkmarx.flow.dto.github.PushEvent;
import com.checkmarx.flow.exception.MachinaException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;

import javax.annotation.Nullable;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@TestComponent
@Slf4j
public class GitHubTestUtils {

    @Autowired
    private GitHubIssueTracker gitHubIssueTracker;

    @Autowired
    private GitHubProperties gitHubProperties;

    public List<Issue> getIssues(ScanRequest request) {
        return gitHubIssueTracker.getIssues(request);
    }

    public List<Issue> filterIssuesByState(List<Issue> issuesList, String state) {
        return issuesList.stream()
                .filter(issue -> issue.getState().equals(state))
                .collect(Collectors.toList());
    }

    public List<Issue> filterIssuesByStateAndByVulnerabilityName(List<Issue> issuesList, String state, String vulnerabilityName) {
        return issuesList.stream()
                .filter(issue -> issue.getState().equals(state) && issue.getTitle().contains(vulnerabilityName))
                .collect(Collectors.toList());
    }

    public int getIssueLinesCount(Issue issue) {
        String bodyLinePattern = "Line #[0-9]+";
        Pattern pattern = Pattern.compile(bodyLinePattern);
        Matcher matcher = pattern.matcher(issue.getBody());

        int count = 0;
        while (matcher.find()) {
            count++;
        }

        return count;
    }

    public void closeIssue(Issue issue, ScanRequest request) {
        try {
            gitHubIssueTracker.closeIssue(issue, request);
        } catch (MachinaException e) {
            e.printStackTrace();
        }
    }

    public void closeAllIssues(List<Issue> issuesList, ScanRequest request) {
        if (issuesList != null) {
            issuesList.forEach(issue ->
                    closeIssue(issue, request));
        }
    }

    public String createSignature(String requestBody) {
        final String HMAC_ALGORITHM = "HmacSHA1";
        String result = null;
        try {
            byte[] bodyBytes = requestBody.getBytes(StandardCharsets.UTF_8);

            byte[] tokenBytes = gitHubProperties.getWebhookToken().getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secret = new SecretKeySpec(tokenBytes, HMAC_ALGORITHM);

            Mac hmacCalculator = Mac.getInstance(HMAC_ALGORITHM);
            hmacCalculator.init(secret);

            byte[] hmacBytes = hmacCalculator.doFinal(bodyBytes);
            result = "sha1=" + DatatypeConverter.printHexBinary(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error generating GitHub signature.", e);
        }
        return result;
    }

    public String loadWebhookRequestBody(EventType eventType) {
        String filename = (eventType == GitHubTestUtils.EventType.PULL_REQUEST) ?
                "github-pull-request.json" : "github-push.json";

        String path = Paths.get(Constants.WEBHOOK_REQUEST_DIR, filename).toString();
        try {
            return TestUtils.getResourceAsString(path);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read resource stream.", e);
        }
    }

    public HttpEntity<String> prepareWebhookRequest(EventType eventType) {
        String body = loadWebhookRequestBody(eventType);

        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.add("X-GitHub-Event", eventType.getValue());
        headers.add("X-Hub-Signature", createSignature(body));
        return new HttpEntity<>(body, headers);
    }

    /**
     * Executes a controller method that corresponds to eventType.
     * No parameter overrides are passed to the call besides project name.
     */
    public void callController(GitHubController controller, EventType eventType, @Nullable String projectNameOverride) {
        String body = loadWebhookRequestBody(eventType);
        String signature = createSignature(body);
        ControllerRequest request = ControllerRequest.builder().project(projectNameOverride).build();
        ObjectMapper mapper = new ObjectMapper();
        if (eventType == EventType.PULL_REQUEST) {
            try {
                controller.pullRequest(mapper.readValue(body, PullEvent.class), signature, null, request);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        } else {
            try {
                controller.pushRequest(mapper.readValue(body, PushEvent.class), signature, null, request);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
    }

    @RequiredArgsConstructor
    @Getter
    public enum EventType {
        PUSH("push"),
        PULL_REQUEST("pull_request");

        private final String value;
    }
}