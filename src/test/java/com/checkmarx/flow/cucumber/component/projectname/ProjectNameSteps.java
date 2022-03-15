package com.checkmarx.flow.cucumber.component.projectname;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.properties.FlowProperties;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.service.CxScannerService;
import com.checkmarx.flow.service.HelperService;
import com.checkmarx.flow.service.ProjectNameGenerator;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;

@SpringBootTest(classes = {CxFlowApplication.class})
@CucumberContextConfiguration
public class ProjectNameSteps {

    private ProjectNameGenerator projectNameGenerator;
    private final FlowProperties flowProperties;
    private final CxScannerService cxScannerService;
    private final ScanRequest scanRequest = new ScanRequest();
    private final HelperService helperService;

    public ProjectNameSteps(CxScannerService cxScannerService, FlowProperties flowProperties, ProjectNameGenerator projectNameGenerator, HelperService helperService) {
        this.flowProperties = flowProperties;
        this.cxScannerService = cxScannerService;
        this.projectNameGenerator = projectNameGenerator;
        this.helperService = mock(HelperService.class);
    }

    @Given("preserve-project-name is {} and multi-tenant is true")
    public void setPresrveProjectName(boolean presrveProjectName) {
        flowProperties.setPreserveProjectName(presrveProjectName);
    }

    @When("scan request arrives with repo-name {} and branch is {string}")
    public void setRepoName(String repoName, String branch) {
        scanRequest.setRepoName(repoName);
        scanRequest.setBranch(branch);
    }

    @Then("project name used by scanner is {}")
    public void checkProjectNameGenerationResult(String projectNameResult) {
        String projectName = projectNameGenerator.determineProjectName(scanRequest);
        assertThat(projectName, equalTo(projectNameResult));
    }

    @Given("multi-tenant is {}")
    public void isMultiTenant(boolean isMultiTenant) {
        cxScannerService.getProperties().setMultiTenant(isMultiTenant);
    }

    @When("scan request arrives with namespace {}, repo-name {}, branch {} and application {}")
    public void scanRequestArrivesWithNamespaceRepoNameBranchAndApplication(String namespace, String repoName, String branch, String application) {
        scanRequest.setNamespace(namespace);
        scanRequest.setRepoName(repoName);
        scanRequest.setBranch(branch);
        scanRequest.setApplication(application);
    }
}
