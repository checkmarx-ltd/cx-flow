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
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SendGridService {
    public void sendEmailThroughSendGrid(List<String> emails, String apiToken, String content) {
        Mail mail = new Mail();
        mail.setFrom(new Email("leonel.sanches@checkmarx.com"));
        mail.setSubject("This is a SendGrid test");
        mail.addContent(new Content("text/plain", "Ironscales, that's a test, not junk."));

        Personalization personalization = new Personalization();
        for (String recipient: emails) {
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
