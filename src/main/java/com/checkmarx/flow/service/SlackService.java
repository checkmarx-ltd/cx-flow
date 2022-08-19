package com.checkmarx.flow.service;

import com.checkmarx.flow.config.SlackProperties;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.cx.CxScanSummary;
import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.model.Message;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.StringUtils;

import java.io.IOException;
import java.util.Optional;

@Service
public class SlackService {
    private final SlackProperties slackProperties;
    private final ExternalScriptService externalScriptService;
    private final boolean isEnabled;

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(SlackService.class);

    public SlackService(SlackProperties slackProperties, ExternalScriptService externalScriptService) {
        this.slackProperties = slackProperties;
        this.externalScriptService = externalScriptService;

        String botToken = Optional.ofNullable(slackProperties).map(SlackProperties::getBotToken).orElse("");
        this.isEnabled = !StringUtils.isEmpty(botToken);
    }

    public void notifyChannel(ScanRequest scanRequest, ScanResults scanResults) {
        if (!this.isEnabled) {
            log.info("Slack Service not configured. Skipping Slack notification...");
            return;
        }

        Slack slack = Slack.getInstance();
        String token = slackProperties.getBotToken();

        // Initialize an API Methods client with the given token
        MethodsClient methods = slack.methods(token);

        // If result is below the thresholds, not required to report.
        CxScanSummary scanSummary = scanResults.getScanSummary();
        if (scanSummary == null) {
            log.info("Scan result didn't provide a Summary. Please check whether the scan executed successfully. Skipping Slack notification...");
            return;
        }

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
                .text("Project Name: " + scanResults.getProject() + "\n" +
                        "Scan Results: " + scanResults.getLink() + "\n" +
                        "Highs: " + scanSummary.getHighSeverity() + "\n" +
                        "Mediums: " + scanSummary.getMediumSeverity())
                .build();

        String channel = "#" + slackProperties.getChannelName();
        if (!ScanUtils.empty(slackProperties.getChannelScript())) {
            channel = executeScriptToDetermineChannelName(scanRequest, slackProperties.getChannelScript());
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
     * @return The channel name.
     */
    private String executeScriptToDetermineChannelName(ScanRequest scanRequest, String scriptFile) {
        return externalScriptService.getScriptExecutionResult(scanRequest, scriptFile, "Slack");
    }
}
