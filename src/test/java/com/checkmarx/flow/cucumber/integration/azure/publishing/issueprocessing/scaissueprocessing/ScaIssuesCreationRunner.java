package com.checkmarx.flow.cucumber.integration.azure.publishing.issueprocessing.scaissueprocessing;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/cucumber/features/integrationTests/sca/scanResultsProcessing.feature",
        glue = {"com.checkmarx.flow.cucumber.integration.azure.publishing.issueprocessing.scaissueprocessing"},
        tags = "@SCA_Issues_Creation and not @Skip")
public class ScaIssuesCreationRunner {
}