package com.checkmarx.flow.cucumber.integration.cli.sca;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/cucumber/features/integrationTests/cli/scaCliScan.feature",
        glue = {"com.checkmarx.flow.cucumber.integration.cli.sca"},
        tags = "@SCA_CLI_SCAN and not @Skip")
public class ScaCliStepsRunner {
}

