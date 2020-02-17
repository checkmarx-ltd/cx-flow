package com.checkmarx.flow.cucumber.component.webhook;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.GitHubProperties;
import cucumber.api.PendingException;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.commons.lang3.time.StopWatch;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class WebHookSteps {
    private final static Logger logger = LoggerFactory.getLogger(WebHookSteps.class);
    private final List<CompletableFuture<Long>> requestSendingTasks = new ArrayList<>();
    private final GitHubProperties properties;

    public WebHookSteps(GitHubProperties properties) {
        this.properties = properties;
    }

    @Given("CxFlow is running as a service")
    public void runAsService() {
        SpringApplication.run(CxFlowApplication.class, "--web");
    }

    @When("GitHub sends WebHook requests to CxFlow {int} times per second")
    public void githubSendsWebHookRequests(int timesPerSecond) {
        final int MAX_TOTAL_REQUESTS = 10,
                MILLISECONDS_IN_SECOND = 1000;

        int intervalBetweenRequestsMs = MILLISECONDS_IN_SECOND / timesPerSecond;

        logger.info("Starting to send WebHook requests with the interval of {} ms.", intervalBetweenRequestsMs);
        for (int i = 0; i < MAX_TOTAL_REQUESTS; i++) {
            logger.info("Sending request #{}.", i + 1);
            startRequestSendingTaskAsync();
            chillOutFor(intervalBetweenRequestsMs);
        }

        waitForAllTasksToComplete(requestSendingTasks);
    }

    private static void chillOutFor(int durationMs) {
        // Using Awaitility, because SonarLint considers Thread.sleep a code smell.
        Awaitility.with()
                .pollDelay(durationMs, TimeUnit.MILLISECONDS)
                .await()
                .until(() -> true);
    }

    private void startRequestSendingTaskAsync() {
        CompletableFuture<Long> task = CompletableFuture.supplyAsync(this::getRequestExecutionTimeMs);
        requestSendingTasks.add(task);
    }

    private long getRequestExecutionTimeMs() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        sendWebHookRequest();
        return stopWatch.getTime();
    }

    private void sendWebHookRequest() {
        throw new PendingException();
    }

    private void waitForAllTasksToComplete(List<CompletableFuture<Long>> tasks) {
        logger.info("Waiting for all the requests to complete.");
        CompletableFuture[] futureArray = tasks.toArray(new CompletableFuture[0]);
        CompletableFuture<Void> combinedTask = CompletableFuture.allOf(futureArray);
        Assert.assertFalse(combinedTask.isCompletedExceptionally());
    }

    @Then("each of the requests is answered in at most {int} ms")
    public void eachOfTheRequestsIsAnsweredInAtMostMs(long expectedMaxDurationMs) {
        Optional<Long> actualMaxDurationMs = requestSendingTasks.stream()
                .map(WebHookSteps::toExecutionTimeMs)
                .max(Long::compare);

        Assert.assertTrue(actualMaxDurationMs.isPresent());

        String message = String.format("Actual duration (%d ms) is greater than the expected max duration (%d ms).",
                actualMaxDurationMs.get(),
                expectedMaxDurationMs);

        Assert.assertTrue(message, actualMaxDurationMs.get() <= expectedMaxDurationMs);
    }

    private static long toExecutionTimeMs(CompletableFuture<Long> task) {
        try {
            return task.get();
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }
}
