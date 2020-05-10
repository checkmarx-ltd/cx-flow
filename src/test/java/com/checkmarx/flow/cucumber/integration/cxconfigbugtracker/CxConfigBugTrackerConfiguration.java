package com.checkmarx.flow.cucumber.integration.cxconfigbugtracker;

import com.checkmarx.flow.service.FlowService;
import com.checkmarx.sdk.service.CxService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class CxConfigBugTrackerConfiguration {


    @Bean
    @Primary
    public CxService getCxService() {
        return Mockito.mock(CxService.class);
    }

}
