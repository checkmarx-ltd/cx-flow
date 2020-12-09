package com.checkmarx.flow.cucumber.integration.codebashing;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(plugin = { "pretty", "summary", "html:build/cucumber/integration/codebashing", "json:build/cucumber/integration/codebashing/cucumber.json" },
        features = "src/test/resources/cucumber/features/integrationTests/codebashing/codebashingLessons.feature",
        glue = {"com.checkmarx.flow.cucumber.integration.codebashing"},
        tags = "@CodeBashingIntegrationTests and not @Skip")
public class RunCodeBashingRunner {
}