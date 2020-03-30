package com.checkmarx.flow.cucumber.integration.jira.autoconfig;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(plugin = { "pretty", "summary", "html:build/cucumber/integration/scan"},
        features = "classpath:cucumber/features/integrationTests/jira/jira-auto-config.feature",
        glue = { "com.checkmarx.flow.cucumber.common.steps", "com.checkmarx.flow.cucumber.integration.jira.autoconfig" },
        tags = "@Integration and not @Skip")
public class AutoConfigRunner {
}
