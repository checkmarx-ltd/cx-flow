package com.checkmarx.flow.cucumber.integration.negative_tests;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(plugin = { "pretty", "summary", "html:build/cucumber/integration/negative_tests", "json:build/cucumber/integration/negative_tests/cucumber.json" },
        features = "src/test/resources/cucumber/features/integrationTests/jira/publish-processing.feature",
        glue = {"com.checkmarx.flow.cucumber.common.steps", "com.checkmarx.flow.cucumber.integration.negative_tests"},
        tags = "@Integration and @Negative_test and @Error_Handling and not @Skip")
public class RunJiraNegativeSteps {
}