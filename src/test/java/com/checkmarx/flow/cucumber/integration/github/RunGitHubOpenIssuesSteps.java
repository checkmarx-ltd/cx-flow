package com.checkmarx.flow.cucumber.integration.github;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(plugin = { "pretty", "summary", "html:build/cucumber/integration/github", "json:build/cucumber/integration/github/cucumber.json" },
        features = "src/test/resources/cucumber/features/integrationTests/github/publish-processing.feature",
        glue = {"com.checkmarx.flow.cucumber.common.steps", "com.checkmarx.flow.cucumber.integration.github"},
        tags = "@GitHubIntegrationTests and not @Skip")
public class RunGitHubOpenIssuesSteps {
}