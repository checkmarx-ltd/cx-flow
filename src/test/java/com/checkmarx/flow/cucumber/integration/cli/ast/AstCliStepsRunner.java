package com.checkmarx.flow.cucumber.integration.cli.ast;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/cucumber/features/integrationTests/cli/astCliScan.feature",
        glue = {"com.checkmarx.flow.cucumber.integration.cli.ast"},
        tags = "@AST_CLI_SCAN and not @Skip")
public class AstCliStepsRunner {
}

