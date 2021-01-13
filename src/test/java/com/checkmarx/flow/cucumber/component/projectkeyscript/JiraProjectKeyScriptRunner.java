package com.checkmarx.flow.cucumber.component.projectkeyscript;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/cucumber/features/componentTests/configure-jira-project-key-script.feature",
        tags = "not @Skip")
public class JiraProjectKeyScriptRunner {
}
