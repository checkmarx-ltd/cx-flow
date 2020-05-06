package com.checkmarx.flow.cucumber.integration.jira.manualconfig;


import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(plugin = { "pretty", "summary", "html:build/cucumber/integration/scan"},
        features = "classpath:cucumber/features/integrationTests/jira/jira-manual-config.feature",
        glue = { "com.checkmarx.flow.cucumber.integration.jira.manualconfig" },
        tags = "@Integration and not @Skip")
public class ManualConfigRunner {
}
