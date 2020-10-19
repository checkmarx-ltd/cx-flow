package com.checkmarx.test.flow.config;

import com.checkmarx.flow.controller.GitHubController;
import com.checkmarx.flow.service.FlowService;
import com.checkmarx.flow.service.HelperService;
import com.checkmarx.sdk.service.CxClient;
import com.cx.restclient.ScaClientImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@Configuration
public class CxFlowMocksConfig {

    @Primary
    @Bean
    public RestTemplate getRestTemplate() {
        return mock(RestTemplate.class);
    }

    @Primary
    @Bean
    public CxClient getCxClient() {
        return mock(CxClient.class);
    }

    @Primary
    @Bean
    public ScaClientImpl getScaClientImpl() { return mock(ScaClientImpl.class); }

    @Primary
    @Bean
    public FlowService getFlowService() {
        return mock(FlowService.class);
    }

}
