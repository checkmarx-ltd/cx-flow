package com.checkmarx.flow.cucumber.component.webhook;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.cucumber.common.utils.TestUtils;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@SpringBootTest
public class WebHookSteps {
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    private static final String WEBHOOK_REQUEST_RESOURCE_PATH = "sample-webhook-requests/from-github.json";

    private static final int MAX_TOTAL_REQUESTS = 20;

    private static final Logger logger = LoggerFactory.getLogger(WebHookSteps.class);
    private static final Duration maxAwaitTimeForAllRequests = Duration.ofSeconds(2 * MAX_TOTAL_REQUESTS);
    private static final Duration maxWarmUpRequestDuration = Duration.ofSeconds(5);
    private final List<CompletableFuture<Long>> requestSendingTasks = new ArrayList<>();

    @Autowired
    private GitHubProperties properties;

    private HttpEntity<String> webHookRequest;
    private String cxFlowPort;

    @Given("CxFlow is running as a service")
    public void runAsService() {
        ConfigurableApplicationContext appContext = SpringApplication.run(CxFlowApplication.class, "--web");
        cxFlowPort = appContext.getEnvironment().getProperty("server.port");
    }

    @When("GitHub sends WebHook requests to CxFlow {int} times per second")
    public void githubSendsWebHookRequests(int timesPerSecond) {
        final int MILLISECONDS_IN_SECOND = 1000;

        webHookRequest = prepareWebHookRequest();
        sendWarmUpRequest();
        Duration intervalBetweenRequests = Duration.ofMillis(MILLISECONDS_IN_SECOND / timesPerSecond);

        logger.info("Starting to send WebHook requests with the interval of {} ms.", intervalBetweenRequests.toMillis());
        for (int i = 0; i < MAX_TOTAL_REQUESTS; i++) {
            chillOutFor(intervalBetweenRequests);
            CompletableFuture<Long> task = startRequestSendingTaskAsync(i);
            requestSendingTasks.add(task);
        }

        waitForAllTasksToComplete(requestSendingTasks);
    }

    private void sendWarmUpRequest() {
        // First request can take much longer time than subsequent requests due to web server "warm up"
        // => first request should not be included into the measurement.
        logger.info("Sending a warm-up request.");
        CompletableFuture<Void> task = CompletableFuture.runAsync(this::sendWebHookRequest);
        Awaitility.await().atMost(maxWarmUpRequestDuration).until(task::isDone);
    }

    private HttpEntity<String> prepareWebHookRequest() {
        InputStream input = TestUtils.getResourceAsStream(WEBHOOK_REQUEST_RESOURCE_PATH);
        String body;
        try {
            body = IOUtils.toString(input, DEFAULT_CHARSET);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read resource stream.", e);
        }

        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.add("X-GitHub-Event", "push");
        headers.add("X-Hub-Signature", getSignature(body));
        return new HttpEntity<>(body, headers);
    }

    private static void chillOutFor(Duration duration) {
        // Using Awaitility, because SonarLint considers Thread.sleep a code smell.
        Awaitility.with()
                .pollDelay(duration)
                .await()
                .until(() -> true);
    }

    private CompletableFuture<Long> startRequestSendingTaskAsync(int index) {
        logger.info("Sending request #{}.", index + 1);
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

    private String getSignature(String requestBody) {
        final String HMAC_ALGORITHM = "HmacSHA1";
        String result = null;
        try {
            byte[] bodyBytes = requestBody.getBytes(DEFAULT_CHARSET);

            byte[] tokenBytes = properties.getWebhookToken().getBytes(DEFAULT_CHARSET);
            SecretKeySpec secret = new SecretKeySpec(tokenBytes, HMAC_ALGORITHM);

            Mac hmacCalculator = Mac.getInstance(HMAC_ALGORITHM);
            hmacCalculator.init(secret);

            byte[] hmacBytes = hmacCalculator.doFinal(bodyBytes);
            result = "sha1=" + DatatypeConverter.printHexBinary(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("Error generating HMAC signature.", e);
        }
        return result;
    }

    private static void waitForAllTasksToComplete(List<CompletableFuture<Long>> tasks) {
        logger.info("Waiting for all the requests to complete.");
        CompletableFuture[] taskArray = tasks.toArray(new CompletableFuture[0]);
        CompletableFuture<Void> combinedTask = CompletableFuture.allOf(taskArray);
        Awaitility.await()
                .atMost(maxAwaitTimeForAllRequests)
                .until(combinedTask::isDone);
        logger.info("All requests completed.");
        Assert.assertFalse("Some of the requests failed.", combinedTask.isCompletedExceptionally());
    }

    @Then("each of the requests is answered in at most {int} ms")
    public void eachOfTheRequestsIsAnsweredInAtMostMs(long expectedMaxDurationMs) {
        List<Long> taskDurations = requestSendingTasks.stream()
                .map(WebHookSteps::toExecutionTimeMs)
                .collect(Collectors.toList());

        logger.info("Durations, ms: {}", Arrays.toString(taskDurations.toArray()));

        boolean allRequestsCompletedSuccessfully = taskDurations.stream().allMatch(Objects::nonNull);
        Assert.assertTrue("Some of the requests failed.", allRequestsCompletedSuccessfully);

        Optional<Long> actualMaxDurationMs = taskDurations.stream().max(Long::compare);
        Assert.assertTrue(actualMaxDurationMs.isPresent());

        String message = String.format("Actual duration (%d ms) is greater than the expected max duration (%d ms).",
                actualMaxDurationMs.get(),
                expectedMaxDurationMs);
        Assert.assertTrue(message, actualMaxDurationMs.get() <= expectedMaxDurationMs);
    }

    private static Long toExecutionTimeMs(CompletableFuture<Long> task) {
        try {
            return task.get();
        } catch (Exception e) {
            logger.error("Task {} didn't complete successfully.", task);
            return null;
        }
    }
}
