package com.checkmarx.flow.cucumber.component.projectname;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/cucumber/features/componentTests/project-name-generator.feature",
        tags = "not @Skip")
public class ProjectNameRunner {
}
