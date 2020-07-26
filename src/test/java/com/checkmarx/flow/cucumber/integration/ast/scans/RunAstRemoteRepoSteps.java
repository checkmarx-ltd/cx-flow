package com.checkmarx.flow.cucumber.integration.ast.scans;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(plugin = { "pretty", "summary", "html:build/cucumber/integration/ast", "json:build/cucumber/integration/sca/cucumber.json" },
        features = "src/test/resources/cucumber/features/integrationTests/ast/astScanProcessing.feature",
        glue = {"com.checkmarx.flow.cucumber.integration.ast.scans"},
        tags = "@AstRemoteRepoScan and not @Skip")
public class RunAstRemoteRepoSteps {
}