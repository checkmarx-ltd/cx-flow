package com.checkmarx.flow.cucumber.integration.publishprocess;

import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.service.CxAuthClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;

//@Configuration
public class PublishProcessTestConfiguration {


//    @Autowired
    private  CxProperties properties;

//    @Bean(name = {"cxRestTemplate"})
    public RestTemplate getRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(HttpClients.createDefault());
        requestFactory.setConnectTimeout(this.properties.getHttpConnectionTimeout());
        requestFactory.setReadTimeout(this.properties.getHttpReadTimeout());
        restTemplate.setRequestFactory(requestFactory);
        restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));
        return restTemplate;
    }

}
