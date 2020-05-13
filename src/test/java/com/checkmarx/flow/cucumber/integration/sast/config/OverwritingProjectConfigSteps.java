package com.checkmarx.flow.cucumber.integration.sast.config;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class OverwritingProjectConfigSteps {
    @Given("{string} project exists in SAST")
    public void projectExistsInSAST(String projectName) {
    }

    @And("project preset is {string}")
    public void projectPresetIs(String initialPreset) {
    }

    @And("project configuration is {string}")
    public void projectConfigurationIs(String initialConfig) {
    }

    @And("SAST configuration is set to {string} in CxFlow config")
    public void sastConfigurationIsSetToInCxFlowConfig(String globalConfig) {
    }

    @And("SAST preset is set to {string} in CxFlow config")
    public void sastPresetIsSetToInCxFlowConfig(String globalPreset) {
    }

    @And("all of {string}, {string}, {string}, {string} exist in SAST")
    public void allOfExistInSAST(String initialPreset, String initialConfig, String newPreset, String newConfig) {
    }

    @When("GitHub notifies CxFlow about a pull request for {string}")
    public void githubNotifiesCxFlowAboutAPullRequestFor(String projectName) {
    }

    @And("{string} parameter is not specified in GitHub request")
    public void presetParameterIsNotSpecifiedInGitHubRequest(String paramName) {
    }

    @And("GitHub repository does not contain a config-as-code file")
    public void githubRepositoryDoesNotContainAConfigAsCodeFile() {
    }

    @And("CxFlow starts a SAST scan")
    public void cxflowStartsASASTScan() {
    }

    @Then("{string} project still has {string} preset")
    public void projectStillHasPreset(String projectName, String preset) {
    }

    @And("{string} project still has {string} configuration")
    public void projectStillHasConfiguration(String projectName, String configuration) {
    }
}
