package com.custodela.machina.config;

import com.custodela.machina.dto.ScanRequest;
import com.custodela.machina.utils.ScanUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;
import java.beans.ConstructorProperties;
import java.nio.charset.Charset;
import java.util.Properties;

@Configuration
public class MachinaConfig {

    private final MachinaProperties properties;
    public static final int QUEUE_CAPACITY = 10000;


    @ConstructorProperties({"properties"})
    public MachinaConfig(MachinaProperties properties) {
        this.properties = properties;
    }

    @Bean
    public RestTemplate getRestTemplate(){
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters()
                .add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));
        return restTemplate;
    }

    @Bean("webHook")
    public TaskExecutor webHookTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(properties.getWebHookQueue());
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix("machina-web");
        executor.initialize();
        return executor;
    }

    @Bean("scanRequest")
    public TaskExecutor scanRequestTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(properties.getScanResultQueue());
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix("scan-results");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }

    @Bean
    public JavaMailSender getJavaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(properties.getMail().getHost());
        mailSender.setPort(properties.getMail().getPort());

        Properties props = mailSender.getJavaMailProperties();

        if(!ScanUtils.empty(properties.getMail().getUsername()) && !ScanUtils.empty(properties.getMail().getUsername())){
            mailSender.setUsername(properties.getMail().getUsername());
            mailSender.setPassword(properties.getMail().getPassword());
            props.put("mail.smtp.auth", "true");

        }

        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.starttls.enable", "true");

        return mailSender;
    }

}
