package com.checkmarx.flow.cucumber.component.deletebranch;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/cucumber/features/componentTests/delete-branch.feature",
        tags = "not @Skip")
public class DeleteBranchRunner {
}
