package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.dto.ScanResults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
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

@Slf4j
@Service
public class EmailService {

    public static final String MESSAGE_KEY = "message";
    public static final String HEADING_KEY = "heading";
    public static final String COMPLETED_PROCESSING = "Successfully completed processing for ";
    private final FlowProperties flowProperties;
    private final TemplateEngine templateEngine;
    private final JavaMailSender emailSender;
    private final SendGridService sendGridService;

    @ConstructorProperties({"flowProperties", "templateEngine", "emailSender", "sendGridService"})
    public EmailService(FlowProperties flowProperties,
                        @Qualifier("cxFlowTemplateEngine") TemplateEngine templateEngine,
                        JavaMailSender emailSender,
                        SendGridService sendGridService) {
        this.flowProperties = flowProperties;
        this.templateEngine = templateEngine;
        this.emailSender = emailSender;
        this.sendGridService = sendGridService;
    }

    /**
     * Send email
     *
     * @param recipients The list of recipients.
     * @param subject    The subject.
     * @param ctx        The context. It has information to generate the content.
     * @param template   What template to use.
     */
    public void sendSmtpMail(List<String> recipients, @NotNull String subject, @NotNull Map<String, Object> ctx, String template) {

        MimeMessagePreparator messagePreparator = mimeMessage -> {
            log.info("Sending email notification.");
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage);

            String contact = flowProperties.getContact();
            if (StringUtils.isNotEmpty(contact)) {
                messageHelper.setFrom(contact);
            }

            FlowProperties.Mail mailProperties = flowProperties.getMail();
            String[] ccList = new String[0];
            if (mailProperties != null) {
                List<String> cc = mailProperties.getCc();

                if (cc != null) {
                    ccList = cc.toArray(new String[0]);
                } else {
                    log.warn("Property cx-flow.mail.cc is not defined.");
                }
            }

            if (CollectionUtils.isNotEmpty(recipients)) {
                messageHelper.setTo(recipients.toArray(new String[0]));
                messageHelper.setCc(ccList);
            } else {
                messageHelper.setTo(ccList);
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
     * @param ctx      The context. It has information to generate the content.
     * @param template What template to use.
     * @return The e-mail content, as HTML.
     */
    private String generateContent(Map<String, Object> ctx, String template) {
        Context context = new Context();
        context.setVariables(ctx);
        return templateEngine.process(template, context);
    }

    public void sendScanSubmittedEmail(ScanRequest request) {
        if (!isEmailNotificationAllowed()) {
            log.info("cx-flow.mail.notification not set or set to false. Skipping Scan Submitted e-mail...");
            return;
        }

        FlowProperties.Mail mail = flowProperties.getMail();
        String prefixMessage = "Checkmarx Scan submitted for %s/%s ";
        String scanSubmittedSubject = String.format(prefixMessage, request.getNamespace(), request.getRepoName());
        Map<String, Object> emailCtx = prepareEmailContext("Scan Request Submitted", scanSubmittedSubject, request.getRepoUrl());
        sendMail(request.getEmail(), mail, scanSubmittedSubject, emailCtx, "generic-event-message.html");
    }

    public void sendScanCompletedEmail(ScanRequest request, ScanResults results) {
        if (!isEmailNotificationAllowed()) {
            log.info("cx-flow.mail.notification not set or set to false. Skipping Scan Completed e-mail...");
            return;
        }

        if (request.getBugTracker() == null) {
            return;
        }

        BugTracker.Type bugTrackerType = request.getBugTracker().getType();
        if (bugTrackerType.equals(BugTracker.Type.NONE) ||
                !bugTrackerType.equals(BugTracker.Type.EMAIL)) {
            return;
        }

        prepareAndSendScanCompletedEmail(request, results);
    }

    public void handleEmailBugTracker(ScanRequest request, ScanResults results) {
        FlowProperties.Mail mail = flowProperties.getMail();
        if (mail == null) {
            return;
        }

        if (ScanUtils.empty(results.getXIssues()) && !mail.isEmptyMailAllowed()) {
            return;
        }

        prepareAndSendScanCompletedEmail(request, results);
    }

    private boolean isEmailNotificationAllowed() {
        return flowProperties.getMail() != null && flowProperties.getMail().isNotificationEnabled();
    }

    public void prepareAndSendScanCompletedEmail(ScanRequest request, ScanResults results) {
        String namespace = request.getNamespace();
        String repoName = request.getRepoName();
        String scanCompletedMessage = COMPLETED_PROCESSING.concat(namespace).concat("/").concat(repoName);
        String scanCompletedSubject = String.format("Checkmarx Scan Results: %s/%s", namespace, repoName);

        Map<String, Object> emailCtx = prepareEmailContext("Scan Successfully Completed", scanCompletedMessage, request.getRepoUrl());

        if (results != null && !ScanUtils.empty(results.getLink())) {
            emailCtx.put("issues", results.getXIssues());
            emailCtx.put("link", results.getLink());
        }

        emailCtx.put("repo_fullname", namespace.concat("/").concat(repoName));

        FlowProperties.Mail mail = flowProperties.getMail();
        String template = mail.getTemplate();

        if (ScanUtils.empty(template)) {
            template = "scan-completed-successfully.html";
        }

        sendMail(request.getEmail(), mail, scanCompletedSubject, emailCtx, template);
        log.info("Email notification sent.");
    }

    /**
     * Resolves how to send the Scan Submitted e-mail. If `cx-flow.mail.sendgrid` is set, sends through Sendgrid.
     * Otherwise sends through SMTP.
     *
     * @param emails               List of e-mails with all the recipients.
     * @param mailProperties       Configured properties for mail, through YAML, env variables or command-line arguments.
     * @param scanCompletedSubject The e-mail subject
     * @param emailCtx             The e-mail context. Used to generate the content.
     * @param template             The template chosen.
     */
    private void sendMail(List<String> emails, FlowProperties.Mail mailProperties, String scanCompletedSubject,
                          Map<String, Object> emailCtx, String template) {
        FlowProperties.SendGrid sendGrid = mailProperties.getSendgrid();
        if (sendGrid != null && sendGrid.getApiToken() != null) {
            log.info("Using Sendgrid to send the Scan Completed e-mail notification.");
            String content = generateContent(emailCtx, template);

            String from = flowProperties.getContact();
            if (StringUtils.isEmpty(from)) {
                from = "donotreply@checkmarx.com";
            }

            sendGridService.sendEmailThroughSendGrid(emails, from, sendGrid.getApiToken(), scanCompletedSubject, content);
        } else {
            log.info("Using SMTP to send the Scan Completed e-mail notification.");
            sendSmtpMail(emails, scanCompletedSubject, emailCtx, template);
        }
    }

    private Map<String, Object> prepareEmailContext(String heading, String message, String repoUrl) {
        Map<String, Object> emailCtx = new HashMap<>();
        emailCtx.put(HEADING_KEY, heading);
        emailCtx.put(MESSAGE_KEY, message);
        emailCtx.put("repo", repoUrl);
        return emailCtx;
    }

}

