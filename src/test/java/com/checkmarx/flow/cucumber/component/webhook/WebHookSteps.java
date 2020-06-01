package com.checkmarx.flow.cucumber.component.webhook;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.cucumber.common.utils.TestUtils;
import com.checkmarx.flow.utils.github.GitHubTestUtils;
import com.checkmarx.flow.utils.github.GitHubTestUtilsImpl;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Send N requests to the GitHub controller with a fixed interval (without waiting for responses).
 * Wait until all the responses are received.
 * Check the timing for each of the responses.
 */
@SpringBootTest(classes = { CxFlowApplication.class, GitHubTestUtils.class})
@Slf4j
@RequiredArgsConstructor
public class WebHookSteps {

    private final List<CompletableFuture<Long>> requestSendingTasks = new ArrayList<>();

    private final GitHubTestUtilsImpl testUtils;

    private HttpEntity<String> webHookRequest;
    private String cxFlowPort;

    private Properties testProperties;

    @Before("@WebHook")
    public void loadProperties() throws IOException {
        testProperties = TestUtils.getPropertiesFromResource("cucumber/features/componentTests/webhook.properties");
    }

    @Given("CxFlow is running as a service")
    public void runAsService() {
        ConfigurableApplicationContext context = TestUtils.runCxFlowAsService();
        List<String> branchesAllowedForScan = context.getBean(FlowProperties.class).getBranches();

        // As a result of this, the automation won't start in the background. This allows to avoid orphaned SAST scans.
        // The automation flow is asynchronous, so request timing won't change.
        branchesAllowedForScan.clear();

        cxFlowPort = context.getEnvironment().getProperty("server.port");
    }

    @When("GitHub sends WebHook requests to CxFlow {int} times per second")
    public void githubSendsWebHookRequests(int timesPerSecond) {
        final int MILLISECONDS_IN_SECOND = 1000;

        webHookRequest = testUtils.prepareWebhookRequest(GitHubTestUtils.EventType.PUSH);
        sendWarmUpRequest();

        int totalRequestCount = Integer.parseUnsignedInt(testProperties.getProperty("totalRequestCount"));
        Duration intervalBetweenRequests = Duration.ofMillis(MILLISECONDS_IN_SECOND / timesPerSecond);
        log.info("Starting to send {} WebHook requests with the interval of {} ms.",
                totalRequestCount,
                intervalBetweenRequests.toMillis());

        for (int i = 0; i < totalRequestCount; i++) {
            chillOutFor(intervalBetweenRequests);
            CompletableFuture<Long> task = startRequestSendingTaskAsync(i);
            requestSendingTasks.add(task);
        }

        waitForAllTasksToComplete(requestSendingTasks);
    }

    /**
     * First request can take much longer time than subsequent requests due to web server "warm up",
     * therefore first request should not be included into the measurement.
     */
    private void sendWarmUpRequest() {
        log.info("Sending a warm-up request.");
        Duration timeout = Duration.parse(testProperties.getProperty("maxWarmUpRequestDuration"));
        CompletableFuture<Void> task = CompletableFuture.runAsync(this::sendWebHookRequest);
        Awaitility.await().atMost(timeout).until(task::isDone);
    }

    private static void chillOutFor(Duration duration) {
        // Using Awaitility, because SonarLint considers Thread.sleep a code smell.
        Awaitility.with()
                .pollDelay(duration)
                .await()
                .until(() -> true);
    }

    private CompletableFuture<Long> startRequestSendingTaskAsync(int index) {
        log.info("Sending request #{}.", index + 1);
        return CompletableFuture.supplyAsync(this::sendRequestAndMeasureDuration);
    }

    private long sendRequestAndMeasureDuration() throws RuntimeException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        sendWebHookRequest();
        return stopWatch.getTime();
    }

    private void sendWebHookRequest() {
        RestTemplate client = new RestTemplate();
        String url = "http://localhost:" + cxFlowPort;
        client.exchange(url, HttpMethod.POST, webHookRequest, String.class);
    }

    private void waitForAllTasksToComplete(List<CompletableFuture<Long>> tasks) {
        log.info("Waiting for all the requests to complete.");
        Duration timeout = Duration.parse(testProperties.getProperty("maxAwaitTimeForAllRequests"));
        CompletableFuture[] taskArray = tasks.toArray(new CompletableFuture[0]);
        CompletableFuture<Void> combinedTask = CompletableFuture.allOf(taskArray);
        Awaitility.await()
                .atMost(timeout)
                .until(combinedTask::isDone);
        log.info("All of the requests finished execution.");
    }

    @Then("each of the requests is answered in at most {int} ms")
    public void eachOfTheRequestsIsAnsweredInAtMostMs(long expectedMaxDurationMs) {
        List<Long> taskDurations = requestSendingTasks.stream()
                .map(WebHookSteps::toExecutionTimeMs)
                .collect(Collectors.toList());

        log.info("Durations, ms: {}", Arrays.toString(taskDurations.toArray()));

        boolean allRequestsCompletedSuccessfully = taskDurations.stream().allMatch(Objects::nonNull);
        Assert.assertTrue("Some of the requests failed.", allRequestsCompletedSuccessfully);

        Long actualMaxDurationMs = taskDurations.stream().max(Long::compare)
                .orElseThrow(() -> new AssertionError("Actual max duration is not defined."));

        String message = String.format("Actual max duration (%d ms) is greater than the expected max duration (%d ms).",
                actualMaxDurationMs,
                expectedMaxDurationMs);
        Assert.assertTrue(message, actualMaxDurationMs <= expectedMaxDurationMs);
    }

    private static Long toExecutionTimeMs(CompletableFuture<Long> task) {
        try {
            return task.get();
        } catch (Exception e) {
            log.error("Task {} didn't complete successfully.", task);
            return null;
        }
    }
}
