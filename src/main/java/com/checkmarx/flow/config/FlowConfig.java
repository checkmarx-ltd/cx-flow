package com.checkmarx.flow.config;

import com.checkmarx.flow.filter.CaseTransformingFilter;
import com.checkmarx.flow.utils.ScanUtils;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
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

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.beans.ConstructorProperties;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.Duration;
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
        RestTemplate restTemplate = new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofMillis(properties.getHttpConnectionTimeout()))
                .setReadTimeout(Duration.ofMillis(properties.getHttpReadTimeout()))
                .build();

        restTemplate.getMessageConverters()
                .add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        return restTemplate;
    }

    @Bean(name = "SSLRestTemplate")
    public RestTemplate restTemplate(RestTemplateBuilder builder) throws NoSuchAlgorithmException, KeyManagementException {

        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
        };
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext);

        HttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(socketFactory)
                .build();

        org.apache.hc.client5.http.impl.classic.CloseableHttpClient httpClient = org.apache.hc.client5.http.impl.classic.HttpClients.custom()
                .setConnectionManager(connectionManager)
                .evictExpiredConnections()
                .build();
        HttpComponentsClientHttpRequestFactory customRequestFactory = new HttpComponentsClientHttpRequestFactory();
        customRequestFactory.setHttpClient(httpClient);
        return builder.requestFactory(() -> customRequestFactory).build();
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
    public jakarta.servlet.Filter supportKebabCaseInControllerRequests() {
        return new CaseTransformingFilter();
    }
}
