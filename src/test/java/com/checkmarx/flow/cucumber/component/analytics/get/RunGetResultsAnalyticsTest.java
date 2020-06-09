package com.checkmarx.flow.cucumber.component.analytics.get;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;

@RunWith(Cucumber.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@CucumberOptions(plugin = { "pretty", "summary", "html:build/cucumber/component/api", "json:build/cucumber/component/api/cucumber.json" },
        features = "classpath:cucumber/features/componentTests/analytics-get-results.feature",
        glue = { "com.checkmarx.flow.cucumber.common.steps", "com.checkmarx.flow.cucumber.component.analytics.get" },
        tags = "@Component and not @Skip")

public class RunGetResultsAnalyticsTest {
}
