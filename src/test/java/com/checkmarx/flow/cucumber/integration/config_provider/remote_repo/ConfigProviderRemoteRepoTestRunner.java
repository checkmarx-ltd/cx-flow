package com.checkmarx.flow.cucumber.integration.config_provider.remote_repo;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/cucumber/features/integrationTests/configProvider/remoteRepo/configProviderRemoteRepo.feature",
        tags = "@ConfigProviderRemoteRepoFeature and not @Skip")
public class ConfigProviderRemoteRepoTestRunner {
}