package com.checkmarx.flow.custom;

import com.checkmarx.flow.dto.Issue;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.sdk.dto.ScanResults;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("EmailIssue")
@Slf4j
@RequiredArgsConstructor
public class EmailIssueTracker implements IssueTracker {
    private final JavaMailSender javaMailSender;

    @Override
    public void init(ScanRequest request, ScanResults results) throws MachinaException {

    }

    @Override
    public void complete(ScanRequest request, ScanResults results) throws MachinaException {
        // Compose the e-mail
        MimeMessagePreparator messagePreparator = mimeMessage -> {
            log.info("Sending issues through E-mail Issue Tracker.");
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage);

            messageHelper.setFrom("cx-flow@checkmarx.com");
            /* String contact = flowProperties.getContact();
            if (StringUtils.isNotEmpty(contact)) {

            } */

            // FlowProperties.Mail mailProperties = flowProperties.getMail();
            /* String[] ccList = new String[0];
            if (mailProperties != null) {
                List<String> cc = mailProperties.getCc();

                if (cc != null) {
                    ccList = cc.toArray(new String[0]);
                } else {
                    log.warn("Property cx-flow.mail.cc is not defined.");
                }
            } */

            String pusherEmail = request.getPusherEmail();
            if (pusherEmail != null) {
                messageHelper.setTo(pusherEmail);
            }

            /* if (CollectionUtils.isNotEmpty(recipients)) {
                messageHelper.setTo(recipients.toArray(new String[0]));
                messageHelper.setCc(ccList);
            } else {
                messageHelper.setTo(ccList);
/            } */

            messageHelper.setSubject("Test e-mail from CxFlow");
            // String content = generateContent(ctx, template);
            StringBuilder sb = new StringBuilder();
            sb.append("This is a test e-mail<br />");
            for (java.lang.String address: request.getEmail()) {
                sb.append(address).append("<br />");
            }

            messageHelper.setText(sb.toString(), true);
        };

        try {
            /* FlowProperties.Mail mail = flowProperties.getMail();
            if (mail != null) {
                emailSender.send(messagePreparator);
            } */
            javaMailSender.send(messagePreparator);
        } catch (MailException e) {
            log.error("Error occurred while attempting to send an issue tracker email", e);
        }
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
