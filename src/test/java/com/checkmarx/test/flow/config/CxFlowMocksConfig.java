package com.checkmarx.test.flow.config;

import com.checkmarx.flow.service.FlowService;

import com.checkmarx.sdk.service.CxService;
import com.checkmarx.sdk.service.scanner.ScaScanner;

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
    public CxService getCxClient() {
        return mock(CxService.class);
    }

    @Primary
    @Bean
    public ScaScanner getScaClientImpl() { return mock(ScaScanner.class); }

    @Primary
    @Bean
    public FlowService getFlowService() {
        return mock(FlowService.class);
    }

}
