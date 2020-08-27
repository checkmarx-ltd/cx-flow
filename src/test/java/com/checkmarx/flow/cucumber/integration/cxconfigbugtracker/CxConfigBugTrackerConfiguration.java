package com.checkmarx.flow.cucumber.integration.cxconfigbugtracker;

import com.checkmarx.sdk.service.CxService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class CxConfigBugTrackerConfiguration {


    @Bean(name = "cxConfigurationTestBean")
    public CxService getCxService() {
        return Mockito.mock(CxService.class);
    }

}
