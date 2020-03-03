package com.checkmarx.flow.cucumber.component.webhook;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/cucumber/features/componentTests/webhook.feature",
        tags = "@WebHookFeature and not @Skip")
public class WebHookTestRunner {
}
