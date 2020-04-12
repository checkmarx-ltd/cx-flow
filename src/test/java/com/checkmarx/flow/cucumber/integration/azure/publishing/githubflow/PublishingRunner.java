package com.checkmarx.flow.cucumber.integration.azure.publishing.githubflow;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/cucumber/features/integrationTests/azure/publishing-github-flow.feature",
        tags = "not @Skip")
public class PublishingRunner {
}
