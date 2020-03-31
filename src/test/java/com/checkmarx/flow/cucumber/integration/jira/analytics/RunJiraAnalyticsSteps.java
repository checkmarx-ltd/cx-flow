package com.checkmarx.flow.cucumber.integration.jira.analytics;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(plugin = { "pretty", "summary", "html:build/cucumber/integration/jira/analytics", "json:build/cucumber/integration/jira/analytics/cucumber.json" },
        features = "src/test/resources/cucumber/features/integrationTests/jira/publish-processing.feature",
        glue = {"com.checkmarx.flow.cucumber.common.steps", "com.checkmarx.flow.cucumber.integration.jira.analytics"},
        tags = "@Jira_Analytics and not @Skip")
public class RunJiraAnalyticsSteps {
}