package com.checkmarx.flow.cucumber.integration.pullrequest.updatecomments;

import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.service.*;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@TestConfiguration
public class UpdatePullRequestConfiguration {

    @Bean
    @Primary
    public CxService getCxService(CxAuthClient authClient,
                                  CxProperties cxProperties,
                                  CxLegacyService cxLegacyService,
                                  @Qualifier("cxRestTemplate") RestTemplate restTemplate,
                                  ScanSettingsClient scanSettingsClient,
                                  FilterValidator filterValidator) {


        return Mockito.mock(CxService.class, Mockito.withSettings().useConstructor(authClient, cxProperties, cxLegacyService, restTemplate, scanSettingsClient, filterValidator));
    }

}