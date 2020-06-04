package com.checkmarx.flow.cucumber.component.filterscript;

import com.checkmarx.flow.CxFlowApplication;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {CxFlowApplication.class})
public class FilterScriptSteps {
    @Given("SAST report containing {int} findings, each in a different file and with a different vulnerability type")
    public void inputContainingFindings(int findingCount) {
    }

    @And("finding #{int} has {string} severity, {string} status and {string} state")
    public void findingHasSeverityStatusAndState(int findingNumber, String severity, String status, String state) {
    }

    @When("CxFlow generates issues from the findings using {string}")
    public void parsingTheInputWith(String scriptText) {
    }

    @Then("CxFlow report is generated with issues corresponding to these findings: {string}")
    public void cxflowReportIsGeneratedWithIssuesCorrespondingToTheseFindings(String issueDescription) {
    }
}
