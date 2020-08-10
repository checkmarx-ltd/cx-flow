package com.checkmarx.flow.cucumber.integration.ast.bugtrackers.jira;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/cucumber/features/integrationTests/ast/astScanProcessing.feature",
        tags = "@AST_JIRA_issue_creation and not @Skip")
public class RunAstTicketsCreationViaJiraSteps {
}