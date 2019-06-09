package com.checkmarx.flow.config;

import com.checkmarx.flow.utils.ScanUtils;
import org.apache.http.impl.client.HttpClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;
import java.beans.ConstructorProperties;
import java.nio.charset.Charset;
import java.util.Properties;

@Configuration
public class FlowConfig {

    public static final int HTTP_CONNECTION_TIMEOUT = 30000;
    public static final int HTTP_READ_TIMEOUT = 30000;
    private final FlowProperties properties;

    @ConstructorProperties({"properties"})
    public FlowConfig(FlowProperties properties) {
        this.properties = properties;
    }

    @Bean
    public RestTemplate getRestTemplate(){
        RestTemplate restTemplate = new RestTemplate();
        HttpComponentsClientHttpRequestFactory requestFactory = new
                HttpComponentsClientHttpRequestFactory(HttpClients.createDefault());
        requestFactory.setConnectTimeout(HTTP_CONNECTION_TIMEOUT);
        requestFactory.setReadTimeout(HTTP_READ_TIMEOUT);
        restTemplate.setRequestFactory(requestFactory);
        restTemplate.getMessageConverters()
                .add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));
        return restTemplate;
    }

    @Bean
    public JavaMailSender getJavaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        FlowProperties.Mail mail = properties.getMail();

        if(mail == null || !mail.isEnabled()){
            return mailSender;
        }
        Properties props = mailSender.getJavaMailProperties();

        if(!ScanUtils.empty(mail.getUsername()) &&
                mail.getPort() != null && !ScanUtils.empty(mail.getHost())){
            mailSender.setHost(mail.getHost());
            mailSender.setPort(mail.getPort());
            mailSender.setUsername(mail.getUsername());
            mailSender.setPassword(mail.getPassword());
            props.put("mail.smtp.auth", "true");

        }

        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.starttls.enable", "true");

        return mailSender;
    }

}
