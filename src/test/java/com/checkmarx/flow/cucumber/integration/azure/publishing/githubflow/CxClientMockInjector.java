package com.checkmarx.flow.cucumber.integration.azure.publishing.githubflow;

import com.checkmarx.sdk.service.CxClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

/**
 * It's important to use this class instead of just creating mock(...) in PublishingSteps.
 */
@Configuration
public class CxClientMockInjector {
    @Primary
    @Bean
    public CxClient getCxClient() {
        return mock(CxClient.class);
    }
}