package com.checkmarx.flow.cucumber.integration.sca_scanner.bugtrackers.github;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/cucumber/features/integrationTests/sca/scanResultsProcessing.feature",
        tags = "@SCA_Resolve_Issue and not @Skip")
public class ScaResolveIssueRunner {
}