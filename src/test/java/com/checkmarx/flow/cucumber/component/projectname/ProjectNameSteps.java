package com.checkmarx.flow.cucumber.component.projectname;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.service.CxScannerService;
import com.checkmarx.flow.service.HelperService;
import com.checkmarx.flow.service.ProjectNameGenerator;
import com.checkmarx.sdk.config.CxPropertiesBase;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import org.junit.Before;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.Assert.assertEquals;

@SpringBootTest(classes = {CxFlowApplication.class})
@CucumberContextConfiguration
public class ProjectNameSteps {

    private final ProjectNameGenerator projectNameGenerator;
    private final FlowProperties flowProperties;
    private final CxScannerService cxScannerService;
    private ScanRequest scanRequest;
    private final CxPropertiesBase cxProperties;
    @MockBean
    private HelperService helperService;

    public ProjectNameSteps(CxScannerService cxScannerService, FlowProperties flowProperties, CxPropertiesBase cxProperties) {
        this.flowProperties = flowProperties;
        this.cxProperties = cxProperties;
        this.cxScannerService = cxScannerService;
        //?--
        this.projectNameGenerator = new ProjectNameGenerator(helperService, cxScannerService, flowProperties);
    }

    @Before
    public void setup() {
        int a = 1;
    }

    @Given("preserve-project-name is {} and is-multi-tenant true")
    public void setPresrveProjectName(boolean presrveProjectName) {
        flowProperties.setPreserveProjectName(presrveProjectName);
        cxProperties.setMultiTenant(true);
    }

    @When("scan request arrives with repo-name {} and branch is {string}")
    public void setRepoName(String repoName, String branch) {
        scanRequest.setRepoName(repoName);
        scanRequest.setBranch(branch);
    }

    @Then("project name used by scanner is {}")
    public void checkProjectNameGenerationResult(String projectNameResult) {
        String projectName = projectNameGenerator.determineProjectName(scanRequest);
        assertEquals(projectName, projectNameResult);
    }

    @Given("is-multi-tenant {string}")
    public void isMultiTenant(String arg0) {

    }

    @When("scan request arrives with namespace {}, repo-name {}, branch {} and application {}")
    public void scanRequestArrivesWithNamespaceRepoNameBranchAndApplication(String arg0, String arg1, String arg2, String arg3) {
    }
}
