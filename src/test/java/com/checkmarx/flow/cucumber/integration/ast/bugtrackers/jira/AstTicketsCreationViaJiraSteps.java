package com.checkmarx.flow.cucumber.integration.ast.bugtrackers.jira;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.cucumber.common.utils.JsonUtils;
import com.checkmarx.flow.cucumber.common.utils.TestUtils;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.service.JiraService;
import com.checkmarx.flow.service.ResultsService;
import com.checkmarx.jira.IJiraTestUtils;
import com.checkmarx.jira.JiraTestUtils;
import com.checkmarx.sdk.config.AstProperties;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.dto.Filter;
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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.checkmarx.flow.cucumber.common.Constants.CUCUMBER_DATA_DIR;

@SpringBootTest(classes = {CxFlowApplication.class, JiraTestUtils.class})
@RequiredArgsConstructor
public class AstTicketsCreationViaJiraSteps {

    private static final String INPUT_BASE_PATH = CUCUMBER_DATA_DIR + "/sample-ast-results/";
    private static final String INPUT_FILE = "5-findings-2-high-3-medium.json";
    private static final String PROJECT_KEY = "CIT";
    private static final String JIRA_URL = "https://cxflow.atlassian.net/";
    private static final int EXPECTED_TICKETS_NUM = 3;

    private final JiraProperties jiraProperties;
    private final JiraService jiraService;
    private final FlowProperties flowProperties;
    private final ResultsService resultsService;
    private final IJiraTestUtils jiraUtils;

    private List<Filter> filters;
    private BugTracker bugTracker;

    @Before("@AST_JIRA_issue_creation")
    public void init() {
        jiraProperties.setUrl(JIRA_URL);
        jiraService.init();

        filters = createFiltersFromString();
    }

    @After("@AST_JIRA_issue_creation")
    public void tearDown() {
        jiraUtils.cleanProject(PROJECT_KEY);
    }

    @Given("scan initiator is AST")
    public void setScanInitiator() {
        flowProperties.setEnabledVulnerabilityScanners(Collections.singletonList(AstProperties.CONFIG_PREFIX));
    }

    @And("bug tracker is JIRA")
    public void setBugTracker() {
        bugTracker = getBasicBugTrackerToJira();
        flowProperties.setBugTracker(bugTracker.getType().name());
    }

    @When("publishing new known unfiltered AST results")
    public void publishNewAstResults() throws IOException, MachinaException {
        ScanResults scanResults = JsonUtils.json2Object(TestUtils.getFileFromRelativeResourcePath(INPUT_BASE_PATH + INPUT_FILE), ScanResults.class);
        resultsService.processResults(getBasicScanRequest(), scanResults, null);
    }

    @Then("new JIRA tickets should be open respectively")
    public void validateJiraTicketsAreGettingOpen() {
        int actualNumberOfIssuesInProject = jiraUtils.getNumberOfIssuesInProject(PROJECT_KEY);

        Assert.assertEquals("Actual new AST-SAST tickets in JIRA are not as expected",
                EXPECTED_TICKETS_NUM, actualNumberOfIssuesInProject);
    }

    private List<Filter> createFiltersFromString() {
        String[] filterValArr = "High, Medium, Low".split(",");
        return Arrays.stream(filterValArr)
                .map(filterVal -> new Filter(Filter.Type.SEVERITY, filterVal))
                .collect(Collectors.toList());
    }

    private BugTracker getBasicBugTrackerToJira() {
        return BugTracker.builder()
                .projectKey(PROJECT_KEY)
                .type(BugTracker.Type.JIRA)
                .issueType("Bug")
                .build();
    }

    private ScanRequest getBasicScanRequest() {
        return ScanRequest.builder()
                .product(ScanRequest.Product.CX)
                .project("VB_3845-master")
                .namespace("cxflowtestuser")
                .repoName("VB_3845")
                .repoType(ScanRequest.Repository.GITHUB)
                .branch("master")
                .bugTracker(bugTracker)
                .refs(Constants.CX_BRANCH_PREFIX.concat("master"))
                .filter(FilterConfiguration.fromSimpleFilters(filters))
                .build();
    }
}