package com.checkmarx.flow.custom;

import com.checkmarx.flow.config.properties.SlackProperties;
import com.checkmarx.flow.dto.Issue;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.service.ExternalScriptService;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.cx.CxScanSummary;
import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.api.ApiTestResponse;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service("Slack")
@Slf4j
@RequiredArgsConstructor
public class SlackIssueTracker implements IssueTracker {
    private final SlackProperties slackProperties;
    private final ExternalScriptService externalScriptService;

    @Override
    public void init(ScanRequest request, ScanResults results) throws MachinaException {
        Slack slack = Slack.getInstance();
        ApiTestResponse response = null;
        try {
            response = slack.methods().apiTest(r -> r.foo("bar"));
        } catch (IOException | SlackApiException e) {
            e.printStackTrace();
        }
        System.out.println(response);
    }

    @Override
    public void complete(ScanRequest request, ScanResults results) throws MachinaException {
        Slack slack = Slack.getInstance();
        String token = slackProperties.getBotToken();

        // Initialize an API Methods client with the given token
        MethodsClient methods = slack.methods(token);

        // If result is below the thresholds, not required to report.
        CxScanSummary scanSummary = results.getScanSummary();
        if (slackProperties.getHighsThreshold() > scanSummary.getHighSeverity() &&
            slackProperties.getMediumsThreshold() > scanSummary.getMediumSeverity()) {

            log.info("Scan results below the thresholds. Skipping Slack reporting...");
            log.info("High vulnerabilities threshold: " + slackProperties.getHighsThreshold() + "; " +
                    "High vulnerabilities found: " + scanSummary.getHighSeverity());
            log.info("Medium vulnerabilities threshold: " + slackProperties.getMediumsThreshold() + "; " +
                    "Medium vulnerabilities found: " + scanSummary.getMediumSeverity());
            return;
        }

        // Build a request object
        ChatPostMessageRequest chatPostMessageRequest = ChatPostMessageRequest.builder()
                .text("Project Name: " + results.getProject() + "\n" +
                        "Scan Results: " + results.getLink() + "\n" +
                        "Highs: " + scanSummary.getHighSeverity() + "\n" +
                        "Mediums: " + scanSummary.getMediumSeverity())
                .build();

        String channel = "#" + slackProperties.getChannelName();
        if (!ScanUtils.empty(slackProperties.getChannelScript())) {
            channel = executeScriptToDetermineChannelName(request, slackProperties.getChannelScript());
        }

        chatPostMessageRequest.setChannel(channel);

        try {
            ChatPostMessageResponse response = methods.chatPostMessage(chatPostMessageRequest);
            if (response.isOk()) {
                Message postedMessage = response.getMessage();
                log.debug("Message posted to Slack successfully. Channel: " + channel + "; Message: " + postedMessage.toString());
            } else {
                String errorCode = response.getError(); // e.g., "invalid_auth", "channel_not_found"
                log.error("Error posting message to Slack: " + errorCode);
            }

        } catch (IOException | SlackApiException e) {
            e.printStackTrace();
        }
    }

    /**
     * Calls the script defined in `slack.custom-script` (application.yml) or
     * SLACK_CUSTOM_SCRIPT (environment variable).
     * The script should return a string, which is the channel in Slack that the
     * port will be sent. This string should start by '#' if the target is a public channel,
     * or '@' if the target is a direct message to a user.
     *
     * @return
     */
    private String executeScriptToDetermineChannelName(ScanRequest scanRequest, String scriptFile) {
        return externalScriptService.getScriptExecutionResult(scanRequest, scriptFile, "Slack");
    }

    @Override
    public String getFalsePositiveLabel() throws MachinaException {
        return null;
    }

    @Override
    public List<Issue> getIssues(ScanRequest request) throws MachinaException {
        return null;
    }

    @Override
    public Issue createIssue(ScanResults.XIssue resultIssue, ScanRequest request) throws MachinaException {
        return null;
    }

    @Override
    public void closeIssue(Issue issue, ScanRequest request) throws MachinaException {

    }

    @Override
    public Issue updateIssue(Issue issue, ScanResults.XIssue resultIssue, ScanRequest request) throws MachinaException {
        return null;
    }

    @Override
    public String getIssueKey(Issue issue, ScanRequest request) {
        return null;
    }

    @Override
    public String getXIssueKey(ScanResults.XIssue issue, ScanRequest request) {
        return null;
    }

    @Override
    public boolean isIssueClosed(Issue issue, ScanRequest request) {
        return false;
    }

    @Override
    public boolean isIssueOpened(Issue issue, ScanRequest request) {
        return false;
    }
}
