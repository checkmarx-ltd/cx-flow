package com.checkmarx.flow.cucumber.component.pullrequestanalytics;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/cucumber/features/componentTests/pull-request-analytics.feature",
        tags = "@ComponentTest and @PullRequestAnalyticsFeature and not @Skip")
public class AnalyticsRunner {
}
