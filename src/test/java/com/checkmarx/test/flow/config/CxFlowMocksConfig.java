package com.checkmarx.test.flow.config;

import com.checkmarx.flow.service.FlowService;
import com.checkmarx.flow.service.ResultsService;
import com.checkmarx.sdk.service.CxClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

@Configuration
public class CxFlowMocksConfig {

    @Primary
    @Bean
    public CxClient getCxClient() {
        return mock(CxClient.class);
    }

    @Primary
    @Bean
    public FlowService getFlowService() {
        return mock(FlowService.class);
    }
}
