package com.checkmarx.flow.cucumber.integration.cli.iast;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;


@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/cucumber/features/integrationTests/cli/iastCli.feature",
        glue = {"com.checkmarx.flow.cucumber.integration.cli.iast"},
        tags = "@IastFeature and not @Skip")
public class IastCliStepsRunner {
}
