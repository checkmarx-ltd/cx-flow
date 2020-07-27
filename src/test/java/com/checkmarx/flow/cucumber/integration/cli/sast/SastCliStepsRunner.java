package com.checkmarx.flow.cucumber.integration.cli.sast;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/cucumber/features/integrationTests/cli/sastCliScan.feature",
        glue = {"com.checkmarx.flow.cucumber.integration.cli.sast"},
        tags = "@SAST_CLI_SCAN and not @Skip")
public class SastCliStepsRunner {
}

