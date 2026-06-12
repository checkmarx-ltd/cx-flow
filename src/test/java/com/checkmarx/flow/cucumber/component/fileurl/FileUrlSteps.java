package com.checkmarx.flow.cucumber.component.fileurl;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.custom.GitLabIssueTracker;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.utils.ScanUtils;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Method;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(classes = {CxFlowApplication.class})
@CucumberContextConfiguration
public class FileUrlSteps {

    @Autowired
    private GitLabIssueTracker gitLabIssueTracker;

    private ScanRequest scanRequest;
    private String fileUrl;

    @Given("a scan request with repo-url {string} and branch {string}")
    public void aScanRequestWith(String repoUrl, String branch) {
        scanRequest = ScanRequest.builder()
                .product(ScanRequest.Product.CX)
                .repoUrl(repoUrl)
                .branch(branch)
                .build();
    }

    @When("the file url is generated for {string} using the {word} tracker")
    public void theFileUrlIsGenerated(String filename, String tracker) throws Exception {
        if ("github".equalsIgnoreCase(tracker)) {
            scanRequest.setRepoType(ScanRequest.Repository.GITHUB);
            fileUrl = ScanUtils.getFileUrl(scanRequest, filename);
        } else {
            scanRequest.setRepoType(ScanRequest.Repository.GITLAB);
            // getFileUrl is private and uses no instance state, so reflection is sufficient.
            Method m = GitLabIssueTracker.class.getDeclaredMethod("getFileUrl", ScanRequest.class, String.class);
            m.setAccessible(true);
            fileUrl = (String) m.invoke(gitLabIssueTracker, scanRequest, filename);
        }
    }

    @Then("the generated file url is {string}")
    public void theGeneratedFileUrlIs(String expectedUrl) {
        assertThat(fileUrl, equalTo(expectedUrl));
    }
}
