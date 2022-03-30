package com.checkmarx.flow.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SendGridService {
    public void sendEmailThroughSendGrid(List<String> emails, @NotNull String from,
                                         @NotNull String apiToken, @NotNull String subject, @NotNull String content) {
        Mail mail = new Mail();
        mail.setFrom(new Email(from));
        mail.setSubject(subject);
        mail.addContent(new Content("text/html", content));

        Personalization personalization = new Personalization();
        for (String recipient: new HashSet<>(emails)) {
            personalization.addTo(new Email(recipient));
        }

        mail.addPersonalization(personalization);

        SendGrid sg = new SendGrid(apiToken);
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            log.debug(String.valueOf(response.getStatusCode()));
            log.debug(response.getBody());
            log.debug(String.valueOf(response.getHeaders()));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
