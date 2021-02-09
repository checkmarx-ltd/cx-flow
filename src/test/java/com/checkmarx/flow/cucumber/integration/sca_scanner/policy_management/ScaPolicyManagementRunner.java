package com.checkmarx.flow.cucumber.integration.sca_scanner.policy_management;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/cucumber/features/integrationTests/sca/scanResultsProcessing.feature",
        tags = "@SCA_Policy_Management and not @Skip")
public class ScaPolicyManagementRunner {
}