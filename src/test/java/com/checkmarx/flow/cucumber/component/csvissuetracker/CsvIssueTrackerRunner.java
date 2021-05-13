package com.checkmarx.flow.cucumber.component.csvissuetracker;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/cucumber/features/componentTests/csv-issue-tracker.feature",
        tags = "not @Skip")
public class CsvIssueTrackerRunner {
}
