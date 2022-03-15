package com.checkmarx.flow.config;

import com.checkmarx.flow.config.properties.FlowProperties;
import com.checkmarx.flow.filter.CaseTransformingFilter;
import com.checkmarx.flow.utils.ScanUtils;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import java.beans.ConstructorProperties;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Configuration
public class FlowConfig {

    private final FlowProperties properties;

    @Autowired
    private ApplicationContext applicationContext;

    @ConstructorProperties({"properties"})
    public FlowConfig(FlowProperties properties) {
        this.properties = properties;
    }

    @Bean(name = "flowRestTemplate")
    public RestTemplate getRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        HttpComponentsClientHttpRequestFactory requestFactory = new
                HttpComponentsClientHttpRequestFactory(HttpClientBuilder.create().useSystemProperties().build());
        requestFactory.setConnectTimeout(properties.getHttpConnectionTimeout());
        requestFactory.setReadTimeout(properties.getHttpReadTimeout());
        restTemplate.setRequestFactory(requestFactory);

        restTemplate.getMessageConverters()
                .add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        return restTemplate;
    }

    @Bean
    public JavaMailSender getJavaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        FlowProperties.Mail mail = properties.getMail();
        if (mail == null) {
            return mailSender;
        }
        Properties props = mailSender.getJavaMailProperties();

        if (!ScanUtils.empty(mail.getUsername()) &&
                mail.getPort() != null && !ScanUtils.empty(mail.getHost())) {
            mailSender.setHost(mail.getHost());
            mailSender.setPort(mail.getPort());
            mailSender.setUsername(mail.getUsername());
            mailSender.setPassword(mail.getPassword());
            props.put("mail.smtp.auth", "true");

        }
        props.put("mail.transport.protocol", JavaMailSenderImpl.DEFAULT_PROTOCOL);
        props.put("mail.smtp.starttls.enable", "true");

        return mailSender;
    }

    @Bean("cxFlowTemplateEngine")
    public TemplateEngine getTemplateEngine() {

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.addTemplateResolver(getSpringTemplateResolver());
        templateEngine.addTemplateResolver(getFileTemplateResolver());
        return templateEngine;

    }

    private SpringResourceTemplateResolver getSpringTemplateResolver() {
        SpringResourceTemplateResolver springTemplateResolver = new SpringResourceTemplateResolver();
        springTemplateResolver.setPrefix("classpath:/templates/");
        springTemplateResolver.setSuffix(".html");
        springTemplateResolver.setTemplateMode(TemplateMode.HTML);
        springTemplateResolver.setCharacterEncoding(StandardCharsets.UTF_8.toString());
        springTemplateResolver.setOrder(1);
        springTemplateResolver.setCheckExistence(true);
        springTemplateResolver.setCacheable(false);
        springTemplateResolver.setApplicationContext(applicationContext);

        return springTemplateResolver;
    }

    private FileTemplateResolver getFileTemplateResolver() {
        FileTemplateResolver fileTemplateResolver = new FileTemplateResolver();
        fileTemplateResolver.setSuffix(".html");
        fileTemplateResolver.setTemplateMode(TemplateMode.HTML);
        fileTemplateResolver.setCharacterEncoding(StandardCharsets.UTF_8.toString());
        fileTemplateResolver.setOrder(2);
        fileTemplateResolver.setCheckExistence(true);

        return fileTemplateResolver;
    }

    /**
     * Support for binding kebab-case controller parameters to POJO fields.
     * E.g. if query looks like '?exclude-files=a,b,c&app-only=true', then the resulting
     * {@link com.checkmarx.flow.dto.ControllerRequest} will have excludeFiles: ["a","b","c"]
     * and appOnly: true.<br/>
     *
     * Without this functionality, both excludeFiles and appOnly would be null. As for parameters that
     * don't contain dashes, Spring Boot binds them automatically.
     *
     * @return a filter that transforms parameter case.
     */
    @Bean
    public javax.servlet.Filter supportKebabCaseInControllerRequests() {
        return new CaseTransformingFilter();
    }
}
