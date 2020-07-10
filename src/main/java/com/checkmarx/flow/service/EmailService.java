package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.dto.ScanResults;
import org.slf4j.Logger;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.validation.constraints.NotNull;
import java.beans.ConstructorProperties;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    public static final String MESSAGE_KEY = "message";
    public static final String HEADING_KEY = "heading";
    public static final String COMPLETED_PROCESSING = "Successfully completed processing for ";
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(EmailService.class);
    private final FlowProperties flowProperties;
    private final TemplateEngine templateEngine;
    private final JavaMailSender emailSender;

    @ConstructorProperties({"flowProperties", "templateEngine", "emailSender"})
    public EmailService(FlowProperties flowProperties, TemplateEngine templateEngine, JavaMailSender emailSender) {
        this.flowProperties = flowProperties;
        this.templateEngine = templateEngine;
        this.emailSender = emailSender;
    }

    /**
     * Send email
     *
     * @param recipients
     * @param subject
     * @param ctx
     * @param template
     */
    public void sendmail(List<String> recipients, @NotNull String subject, @NotNull Map<String, Object> ctx, String template) {

        MimeMessagePreparator messagePreparator = mimeMessage -> {
            log.info("Sending email notification.");
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage);

            String contact = flowProperties.getContact();
            if (!ScanUtils.empty(contact)) {
                messageHelper.setFrom(contact);
            }

            if (!ScanUtils.empty(recipients)) {
                messageHelper.setTo(recipients.toArray(new String[0]));
                messageHelper.setCc((flowProperties.getMail().getCc()).toArray(new String[0]));
            } else {
                messageHelper.setTo((flowProperties.getMail().getCc()).toArray(new String[0]));
            }

            messageHelper.setSubject(subject);
            String content = generateContent(ctx, template);
            messageHelper.setText(content, true);
        };
        try {
            FlowProperties.Mail mail = flowProperties.getMail();
            if (mail != null) {
                emailSender.send(messagePreparator);
            }
        } catch (MailException e) {
            log.error("Error occurred while attempting to send an email", e);
        }
    }

    /**
     * Generate HTML content for email
     *
     * @param ctx
     * @param template
     * @return
     */
    private String generateContent(Map<String, Object> ctx, String template) {
        Context context = new Context();
        context.setVariables(ctx);
        return templateEngine.process(template, context);
    }

    public void sendScanSubmittedEmail(ScanRequest request) {
        if (isEmailNotificationAllowed()) {
            String scanSubmittedSubject = "Checkmarx Scan Submitted for ".concat(request.getNamespace()).concat("/").concat(request.getRepoName());
            String scanSubmittedMessage = "Checkmarx Scan has been submitted for ".concat(request.getNamespace()).concat("/").concat(request.getRepoName())
                    .concat(" - ");
            Map<String, Object> emailCtx = prepareEmailContext("Scan Request Submitted", scanSubmittedMessage, request.getRepoUrl());
            sendmail(request.getEmail(), scanSubmittedSubject, emailCtx, "message.html");
        }
    }

    public void sendScanCompletedEmail(ScanRequest request, ScanResults results) {
        BugTracker.Type bugTrackerType = request.getBugTracker().getType();
        if (isEmailNotificationAllowed() && !bugTrackerType.equals(BugTracker.Type.NONE) &&
                !bugTrackerType.equals(BugTracker.Type.EMAIL)) {
            prepareAndSendEmail(request, results);
        }
    }

    public void handleEmailBugTracker(ScanRequest request, ScanResults results) {
        if (flowProperties.getMail() != null) {
            if (ScanUtils.empty(results.getXIssues())) {
                if (flowProperties.getMail().isEmptyMailAllowed()) {
                    prepareAndSendEmail(request, results);
                }
            } else {
                prepareAndSendEmail(request, results);
            }
        }
    }

    private boolean isEmailNotificationAllowed() {
        return flowProperties.getMail() != null && flowProperties.getMail().isNotificationEnabled();
    }

    public void prepareAndSendEmail(ScanRequest request, ScanResults results) {
        String namespace = request.getNamespace();
        String repoName = request.getRepoName();
        String scanCompletedMessage = COMPLETED_PROCESSING.concat(namespace).concat("/").concat(repoName);
        String scanCompletedSubject = "Checkmarx Scan Results: ".concat(namespace).concat("/").concat(repoName);

        Map<String, Object> emailCtx = prepareEmailContext("Scan Successfully Completed", scanCompletedMessage, request.getRepoUrl());

        if (results != null && !ScanUtils.empty(results.getLink())) {
            emailCtx.put("issues", results.getXIssues());
            emailCtx.put("link", results.getLink());
        }
        emailCtx.put("repo_fullname", namespace.concat("/").concat(repoName));
        sendmail(request.getEmail(), scanCompletedSubject, emailCtx, "template-demo.html");
        log.info("Email notification sent.");
    }

    private Map<String, Object> prepareEmailContext(String heading, String message, String repoUrl) {
        Map<String, Object> emailCtx = new HashMap<>();
        emailCtx.put(HEADING_KEY, heading);
        emailCtx.put(MESSAGE_KEY, message);
        emailCtx.put("repo", repoUrl);
        return emailCtx;
    }

}

