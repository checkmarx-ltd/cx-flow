package com.checkmarx.flow.cucumber.integration.multiScmInstances;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.properties.FlowProperties;
import com.checkmarx.flow.config.properties.GitHubProperties;
import com.checkmarx.flow.config.properties.OptionalScmInstanceProperties;
import com.checkmarx.flow.cucumber.common.utils.JsonUtils;
import com.checkmarx.flow.cucumber.common.utils.TestUtils;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.Issue;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.service.ResultsService;
import com.checkmarx.flow.utils.github.GitHubTestUtils;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.dto.sast.Filter;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.filtering.FilterConfiguration;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import org.junit.Assert;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.checkmarx.flow.cucumber.common.Constants.CUCUMBER_DATA_DIR;

@SpringBootTest(classes = {CxFlowApplication.class, GitHubTestUtils.class})
@RequiredArgsConstructor
public class PublishTicketsWithOptionalScmConfSteps {

    private static final String INPUT_BASE_PATH = CUCUMBER_DATA_DIR + "/sample-ast-results/";
    private static final String INPUT_FILE = "5-findings-2-high-3-medium.json";
    private static final String REPO_NAME = "VB_3845";
    private static final String SCM_INSTANCE_NAME = "instance1";

    private final GitHubTestUtils gitHubTestUtils;
    private final GitHubProperties gitHubProperties;
    private final ResultsService resultsService;
    private final FlowProperties flowProperties;

    private List<Filter> filters;
    private BugTracker bugTracker;
    private ScanRequest scanRequest;

    @Before("@Scm_Optional_Instance")
    public void init() {
        filters = createFiltersFromString();
    }

    @After("@Scm_Optional_Instance")
    public void closeIssues() {
        List<Issue> openIssues = gitHubTestUtils.filterIssuesByState(gitHubTestUtils.getIssues(scanRequest), "open");
        gitHubTestUtils.closeAllIssues(openIssues, scanRequest);
    }

    @Given("bug tracker is GitHub")
    public void setBugTracker() {
        bugTracker = getBasicBugTrackerToJira();
        flowProperties.setBugTracker(bugTracker.getType().name());
    }

    @And("GitHub optional instance configuration is defined")
    public void defineOptionalScmInstance() {
        setGitHubProperties();
    }

    @When("processing the results")
    public void resultsProcessing() throws IOException, MachinaException {
        scanRequest = getBasicScanRequest();
        ScanResults scanResults = JsonUtils.json2Object(TestUtils.getFileFromRelativeResourcePath(INPUT_BASE_PATH + INPUT_FILE), ScanResults.class);
        resultsService.processResults(scanRequest, scanResults, null);
    }

    @Then("new GitHub Issues should be open respectively")
    public void validateGitHubIssues() {
        List<Issue> actualOpenIssues = gitHubTestUtils.filterIssuesByState(gitHubTestUtils.getIssues(scanRequest), "open");

        Assert.assertEquals("Expected GitHub issues for optional SCM instance are not as expected",
                3, actualOpenIssues.size());
    }

    private void setGitHubProperties() {
        OptionalScmInstanceProperties instanceProperties = new OptionalScmInstanceProperties();
        instanceProperties.setWebhookToken("1234");
        instanceProperties.setToken(gitHubProperties.getToken());
        instanceProperties.setApiUrl(gitHubProperties.getApiUrl());
        instanceProperties.setUrl(gitHubProperties.getUrl());

        Map<String, OptionalScmInstanceProperties> instancePropertiesMap = new LinkedHashMap<>();
        instancePropertiesMap.put(SCM_INSTANCE_NAME, instanceProperties);
        gitHubProperties.setOptionalInstances(instancePropertiesMap);
    }

    private ScanRequest getBasicScanRequest() {
        return ScanRequest.builder()
                .product(ScanRequest.Product.CX)
                .project("VB_3845-master")
                .namespace("cxflowtestuser")
                .repoName(REPO_NAME)
                .repoType(ScanRequest.Repository.GITHUB)
                .branch("master")
                .bugTracker(bugTracker)
                .refs(Constants.CX_BRANCH_PREFIX.concat("master"))
                .filter(FilterConfiguration.fromSimpleFilters(filters))
                .scmInstance(SCM_INSTANCE_NAME)
                .build();
    }

    private List<Filter> createFiltersFromString() {
        String[] filterValArr = "High, Medium, Low".split(",");
        return Arrays.stream(filterValArr)
                .map(filterVal -> new Filter(Filter.Type.SEVERITY, filterVal))
                .collect(Collectors.toList());
    }

    private BugTracker getBasicBugTrackerToJira() {
        return BugTracker.builder()
                .type(BugTracker.Type.CUSTOM)
                .customBean("GitHub")
                .build();
    }

}