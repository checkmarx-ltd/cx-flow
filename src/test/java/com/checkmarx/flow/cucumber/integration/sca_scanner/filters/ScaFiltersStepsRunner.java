package com.checkmarx.flow.cucumber.integration.sca_scanner.filters;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/cucumber/features/integrationTests/sca/scanResultsProcessing.feature",
        glue = {"com.checkmarx.flow.cucumber.integration.sca_scanner.filters"},
        tags = "@SCA_Filtering and not @Skip")
public class ScaFiltersStepsRunner {
}