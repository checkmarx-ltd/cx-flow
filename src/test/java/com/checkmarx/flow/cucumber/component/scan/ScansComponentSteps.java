package com.checkmarx.flow.cucumber.component.scan;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.test.flow.config.CxFlowMocksConfig;
import io.cucumber.java.PendingException;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = { CxFlowMocksConfig.class, CxFlowApplication.class })
public class ScansComponentSteps {

    @Given("there is a SAST environment configured and running")
    public void there_is_a_SAST_environment_configured_and_running() {
        // Write code here that turns the phrase above into concrete actions
        throw new PendingException();
    }

    @When("running a scan with source name as {string} and output format as {string}")
    public void running_a_scan_with_source_name_as_and_output_format_as(String string, String string2) {
        // Write code here that turns the phrase above into concrete actions
        throw new PendingException();
    }

    @Then("the retrieved SAST output's high severity number should be {int} and medium severity number should be {int}")
    public void the_retrieved_SAST_output_s_high_severity_number_should_be_and_medium_severity_number_should_be(Integer int1, Integer int2) {
        // Write code here that turns the phrase above into concrete actions
        throw new PendingException();
    }

    @When("running {int} scans in parallel, each with different expected output")
    public void running_scans_in_parallel_each_with_different_expected_output(Integer int1) {
        // Write code here that turns the phrase above into concrete actions
        throw new PendingException();
    }

    @Then("{int} output files are getting created on the output data folder after the scan is completed")
    public void output_files_are_getting_created_on_the_output_data_folder_after_the_scan_is_completed(Integer int1) {
        // Write code here that turns the phrase above into concrete actions
        throw new PendingException();
    }
}