package com.checkmarx.flow.cucumber.integration.azure.publishing.githubflow;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.cucumber.common.utils.TestUtils;
import com.checkmarx.flow.cucumber.integration.azure.publishing.AzureDevopsClient;
import com.checkmarx.flow.cucumber.integration.azure.publishing.PublishingStepsBase;
import com.checkmarx.flow.utils.github.GitHubTestUtils;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.Assert;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;

@Slf4j
@SpringBootTest(classes = {CxFlowApplication.class, GitHubTestUtils.class})
@RequiredArgsConstructor
public class PublishingSteps extends PublishingStepsBase {
    private final FlowProperties flowProperties;
    private final AzureDevopsClient adoClient;

    private String projectName;
    private String cxFlowPort;
    private final GitHubTestUtils testUtils;

    private HttpEntity<String> webhookRequestToSend;

    @Before
    public void init() throws IOException {
        projectName = getProjectName();
        adoClient.ensureProjectExists(projectName);

        cxFlowPort = TestUtils.runCxFlowAsService();
    }

    @Given("issue tracker is ADO")
    public void issueTrackerIsADO() {
        flowProperties.setBugTracker(ISSUE_TRACKER_NAME);
    }

    @And("ADO doesn't contain any issues")
    public void adoDoesnTContainAnyIssues() throws IOException {
        adoClient.deleteProjectIssues(projectName);
    }

    @And("CxFlow filters are disabled")
    public void cxflowFiltersAreDisabled() {
        flowProperties.setFilterSeverity(new ArrayList<>());
    }

    // TODO: share with WebHookSteps.
    @When("GitHub notifies CxFlow about a {string}")
    public void githubNotifiesCxFlowAboutEvent(String eventName) throws IOException {
        GitHubTestUtils.EventType eventType = determineEventType(eventName);
        String path = determineRequestFilePath(eventType);
        webhookRequestToSend = testUtils.prepareWebhookRequest(path, eventType);
    }

    private static GitHubTestUtils.EventType determineEventType(String eventName) throws IOException {
        GitHubTestUtils.EventType result;
        if (eventName.equals("pull request")) {
            result = GitHubTestUtils.EventType.PULL_REQUEST;
        } else if (eventName.equals("push")) {
            result = GitHubTestUtils.EventType.PUSH;
        } else {
            throw new IOException("Bad event name.");
        }
        return result;
    }

    @And("SAST scan returns a report with 1 finding")
    public void sastScanReturnsAReportWithFinding() {
        sendWebHookRequest(webhookRequestToSend);
        log.info("sastScanReturnsAReportWithFinding");
    }

    @Then("after CxFlow publishes the report, ADO contains {int} issue")
    public void adoContainsIssue(int issueCount) {
        Duration timeout = Duration.ofMinutes(5),
                pollInterval = Duration.ofSeconds(10);
        try {
            Awaitility.await().atMost(timeout)
                    .pollInterval(pollInterval)
                    .until(() -> adoContainsIssues(issueCount));
        } catch (ConditionTimeoutException e) {
            String message = String.format("Waited for %s but didn't get the expected ADO issue count (%d).",
                    timeout, issueCount);

            Assert.fail(message);
        }
    }

    private void sendWebHookRequest(HttpEntity<String> request) {
        RestTemplate client = new RestTemplate();
        String url = "http://localhost:" + cxFlowPort;
        client.exchange(url, HttpMethod.POST, request, String.class);
    }

    private String determineRequestFilePath(GitHubTestUtils.EventType eventType) {
        return eventType == GitHubTestUtils.EventType.PULL_REQUEST ?
                "github-pull-request-full.json" : "github-push.json";
    }

    private boolean adoContainsIssues(int count) throws IOException {
        return adoClient.getIssueCount(projectName) == count;
    }
}
