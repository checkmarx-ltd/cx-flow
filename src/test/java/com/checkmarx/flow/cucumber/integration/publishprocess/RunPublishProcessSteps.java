package com.checkmarx.flow.cucumber.integration.publishprocess;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.service.SastScanner;
import com.checkmarx.jira.IPublishUtils;
import com.checkmarx.jira.PublishUtils;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.ExitThrowable;
import com.checkmarx.jira.IJiraTestUtils;
import com.checkmarx.jira.JiraTestUtils;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.dto.sast.Filter;
import com.checkmarx.sdk.dto.filtering.FilterConfiguration;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@SpringBootTest(classes = {CxFlowApplication.class, JiraTestUtils.class, PublishUtils.class})
public class RunPublishProcessSteps {

    private BugTracker.Type bugTracker;

    private int numOfFindings;

    private static final String DIFFERENT_VULNERABILITIES_FILENAME_TEMPLATE = "cucumber/data/sample-sast-results/%d-findings-different-vuln-type-same-file.xml";
    private static final String SAME_VULNERABILITIES_FILENAME_TEMPLATE = "cucumber/data/sample-sast-results/%d-findings-same-vuln-type-same-file.xml";
    private static final String ISSUE_PRIORITY_BEFORE_UPDATE = "High";
    private static final String ISSUE_PRIORITY_AFTER_UPDATE = "Medium";

    @Autowired
    SastScanner sastScanner;

    @Autowired
    private IJiraTestUtils jiraUtils;

    @Autowired
    private CxProperties cxProperties;

    @Autowired
    private JiraProperties jiraProperties;

    @Autowired
    private IPublishUtils publishUtils;

    private FindingsType findingsType;

    private List<Filter> filters;

    private boolean needFilter;

    private int totalResults;

    private boolean useSanityFindingsFile = false;

    private boolean useOneFindingForUpdateClose = false;

    private long updateTime;

    private String issueUpdateVulnerabilityType;
    private String issueUpdateFilename;
    private Long issueId;

    private boolean updating;


    @Before("@PublishProcessing")
    public void setDefaults() {
        needFilter = false;
        useSanityFindingsFile = false;
        updating = false;
        issueUpdateVulnerabilityType = "";
        issueUpdateFilename = "";
        useOneFindingForUpdateClose = false;
        jiraProperties.setProject("JIT");
    }

    @Before("@PublishProcessing")
    public void setOfflineMode() {
        cxProperties.setOffline(true);
    }

    @Before("@PublishProcessing")
    public void cleanJiraProject() throws IOException {
        jiraUtils.ensureProjectExists(jiraProperties.getProject());
        //jiraUtils.ensureIssueTypeExists(jiraProperties.getIssueType());
        jiraUtils.cleanProject(jiraProperties.getProject());
    }

    @Given("target is JIRA")
    public void setTargetTypeToJira() {
        bugTracker = BugTracker.Type.JIRA;
    }

    @Given("there are {int} findings from which {int} results match the filter")
    public void setResultsAndFilters(int totalResults, int matchingResults) {
        findingsType = FindingsType.DIFFERENT_SEVERITIES;
        numOfFindings = matchingResults;
        this.totalResults = totalResults;
        needFilter = true;
        Filter filter = Filter.builder().type(Filter.Type.SEVERITY).value("High").build();
        filters = Collections.singletonList(filter);
    }

    @Given("filter-severity is {}")
    public void setSeverityFilterTypes(String types) {
        filters = createFiltersFromString(types, Filter.Type.SEVERITY);
        needFilter = true;
    }

    @Given("using sanity findings")
    public void setUseSanityFindingsFile() {
        useSanityFindingsFile = true;
    }

    @Given("there is an existing issue")
    public void createExistingIssueForUpdate() throws IOException, ExitThrowable, InterruptedException {
        ScanRequest request = getScanRequestWithDefaults();
        File file = getFileFromResourcePath("cucumber/data/sample-sast-results/1-finding-create-for-update.xml");
        innerPublishRequest(request, file);
        updateTime = jiraUtils.getIssueUpdatedTime(jiraProperties.getProject());
        issueId = jiraUtils.getFirstIssueId(jiraProperties.getProject());
        issueUpdateVulnerabilityType = jiraUtils.getIssueVulnerability(jiraProperties.getProject());
        issueUpdateFilename = jiraUtils.getIssueFilename(jiraProperties.getProject());
        Assert.assertTrue("Issue priority before update is incorrect",assertIssuePriority(ISSUE_PRIORITY_BEFORE_UPDATE));
        TimeUnit.SECONDS.sleep(2);
    }

    @Given("SAST results contain 1 finding, with the same vulnerability type and filename")
    public void setFilenameForUpdate() {
        updating = true;
    }

    @When("publishing same issue with different parameters")
    public void publishIssueForUpdate() throws IOException, ExitThrowable, InterruptedException {
        TimeUnit.SECONDS.sleep(2);
        ScanRequest request = getScanRequestWithDefaults();
        File file = getFileFromResourcePath("cucumber/data/sample-sast-results/1-finding-updated.xml");
        innerPublishRequest(request, file);
    }

    private void assertUpdateTime() {
        Long newUpdateTime = jiraUtils.getIssueUpdatedTime(jiraProperties.getProject());
        Assert.assertTrue(newUpdateTime > updateTime);
    }

    private boolean assertIssuePriority(String expectedPriority) {
        String issuePriority = jiraUtils.getIssuePriority(jiraProperties.getProject());
        return expectedPriority.equals(issuePriority);
    }


    @When("publishing results to JIRA")
    public void publishResults() throws ExitThrowable, IOException {
        ScanRequest request = getScanRequestWithDefaults();
        if (needFilter) {
            request.setFilter(FilterConfiguration.fromSimpleFilters(filters));
        }
        File file = getFileForPublish();
        innerPublishRequest(request, file);
    }

    private void innerPublishRequest(ScanRequest request, File file) throws ExitThrowable {
    publishUtils.publishRequest(request, file, bugTracker, sastScanner);
    }

    @When("results contain {} findings each having a different vulnerability type in one source file")
    public void setNumberOfFindingsForTest(int numOfFindings) {
        this.numOfFindings = numOfFindings;
        findingsType = FindingsType.DIFFERENT_TYPE;
    }

    @When("results contains {int} findings with the same type in one source file")
    public void getFindingsFileWithSameTypeVulnerabilities(int findings) {
        numOfFindings = findings;
        findingsType = FindingsType.SAME_TYPE;
    }

    @When("all issue's findings are false-positive")
    public void closeIssie() throws IOException, ExitThrowable {
        ScanRequest request = getScanRequestWithDefaults();
        File file = getFileFromResourcePath("cucumber/data/sample-sast-results/1-finding-closed.xml");
        innerPublishRequest(request,file);
    }


    @When("there are two existing issues")
    public void publishTwoIssues() throws IOException, ExitThrowable {
        ScanRequest request = getScanRequestWithDefaults();
        File findingsFile = getFileFromResourcePath("cucumber/data/sample-sast-results/2-findings-different-vuln-type-different-files.xml");
        innerPublishRequest(request, findingsFile);
    }

    @When("SAST result contains only one of the findings")
    public void setFileToOneFindingForClose() {
        useOneFindingForUpdateClose= true;
    }

    @Then("issue has the same vulnerability type and filename")
    public void assertVulnerabilityAndFilename() {
        assertSameFilename();
        assertSameVulnerability();
    }

    private void assertSameVulnerability() {
        String newVulnerability = jiraUtils.getIssueVulnerability(jiraProperties.getProject());
        Assert.assertEquals("Vulnerability type has changed during update", issueUpdateVulnerabilityType, newVulnerability);
    }

    private void assertSameFilename() {
        String newFilename = jiraUtils.getIssueFilename(jiraProperties.getProject());
        Assert.assertEquals("Filename has changed during update", issueUpdateFilename, newFilename);
    }

    @Then("JIRA still contains 1 issue")
    public void validateJirahasOneIssue() {
        Assert.assertEquals("JIRA should contain exactly one issue", 1, jiraUtils.getNumberOfIssuesInProject(jiraProperties.getProject()));
    }

    @Then("issue ID hasn't changed")
    public void assertIssueIdIsTheSame() {
        Long newIssueId = jiraUtils.getFirstIssueId(jiraProperties.getProject());
        Assert.assertEquals("Issue ID is different", issueId, newIssueId);
    }

    @Then("issue's updated field is set to a more recent timestamp")
    public void assertUpateIsLater() {
        assertUpdateTime();
    }

    @And("there is status change in the issue")
    public void statusChange(){}
    
    @And("the updated issue has the new status field in the body")
    public void validateUpdatedIssueVulnerabilityStatus(){
        String vulStatus = jiraUtils.getIssueVulnerabilityStatus(jiraProperties.getProject());
        
        Assert.assertTrue( vulStatus.contains("URGENT"));
    }

    @And("the updated issue has a recommended fix link")
    public void validateIssueContainsRecommendedFixLink() {
        String issueRecommendedFixLink = jiraUtils.getIssueRecommendedFixLink(jiraProperties.getProject());

        Assert.assertTrue(issueRecommendedFixLink.contains("Recommended Fix"));
    }

    @Then("the issue should be closed")
    public void assertIssueIsClosed() {
        Assert.assertTrue("Issue is not in closed status", jiraProperties.getClosedStatus().contains(jiraUtils.getIssueStatus(jiraProperties.getProject())));
    }

    @Then("original issues is updated both with 'last updated' value and with new body content")
    public void assertUpdateIssue() {
        Assert.assertTrue(assertIssuePriority(ISSUE_PRIORITY_AFTER_UPDATE));
        assertUpdateTime();
    }

    @Then("verify results contains {int}, {int}, {int}, {int} for severities {}")
    public void verifyNumOfIssuesForSeverities(int high, int medium, int low, int info, String severities) {
        List<Filter> filters = createFiltersFromString(severities, Filter.Type.SEVERITY);
        Map<Filter.Severity, Integer> actualJira = jiraUtils.getIssuesPerSeverity(jiraProperties.getProject());
        for (Filter filter: filters) {
            Filter.Severity severity = Filter.Severity.valueOf(filter.getValue().toUpperCase());
            switch (severity) {
                case HIGH:
                    Assert.assertEquals("HIGH issues does not match", (int) actualJira.get(Filter.Severity.HIGH), high);
                    break;
                case MEDIUM:
                    Assert.assertEquals("Medium issues does not match", (int) actualJira.get(Filter.Severity.MEDIUM), medium);
                    break;
                case LOW:
                    Assert.assertEquals("Medium issues does not match", (int) actualJira.get(Filter.Severity.LOW), low);
                    break;
                case INFO:
                    Assert.assertEquals("Medium issues does not match", (int) actualJira.get(Filter.Severity.INFO), info);
                    break;
            }
        }
    }


    @Then("there should be one closed and one open issue")
    public void assertOneClosedAndOneOpenIssue() {
        Map<String, Integer> issuesPerStatus = jiraUtils.getIssuesByStatus(jiraProperties.getProject());
        int closed = getClosedIssues(issuesPerStatus);
        int open  = getOpenIssues(issuesPerStatus);
        Assert.assertEquals("Closed issues number is incorrect", 1, closed);
        Assert.assertEquals("Open issues number is incorrect", 1, open);
    }

    private int getOpenIssues(Map<String, Integer> issues) {
        return getIssuesPerStatuses(issues, jiraProperties.getOpenStatus());
    }

    private int getClosedIssues(Map<String, Integer> issues) {
        return getIssuesPerStatuses(issues,jiraProperties.getClosedStatus());
    }

    private int getIssuesPerStatuses(Map<String, Integer> issues, List<String> statuses) {
        int result = 0;
        for (String key: issues.keySet()) {
            if (statuses.stream().map(String::toLowerCase).collect(Collectors.toList()).contains(key.toLowerCase())) {
                result += issues.get(key);
            }
        }
        return result;

    }


    @Then("verify {int} new issues got created")
    public void verifyNumberOfIssues(int wantedNumOfIssues) {
        int actualNumOfIssues = jiraUtils.getNumberOfIssuesInProject(jiraProperties.getProject());
        Assert.assertEquals("Wrong number of issues in JIRA", wantedNumOfIssues,  actualNumOfIssues);
    }


    @Then("verify {int} findings in body")
    public void verifyNumOfFindingsInBodyForOneIssue(int findings) {
        int actualFindings = jiraUtils.getFirstIssueNumOfFindings(jiraProperties.getProject());
        Assert.assertEquals("Wrong number of findigs", findings, actualFindings);
     }

    @And("issue status will be present in the body")
    public void verifyIssueStatus() {

        int actualFindings = jiraUtils.getFirstIssueNumOfFindings(jiraProperties.getProject());
        if(actualFindings > 0) {
            String vulStatus = jiraUtils.getIssueVulnerabilityStatus(jiraProperties.getProject());
            Assert.assertNotEquals(null, vulStatus);
            Assert.assertTrue(vulStatus.contains("TO VERIFY"));
        }
        
     }

    private File getDifferentVulnerabilityTypeFindings() throws IOException {
        return getFileFromResourcePath(String.format(DIFFERENT_VULNERABILITIES_FILENAME_TEMPLATE, numOfFindings));
    }

    private File getSameVulnerabilityTypeFindings() throws IOException {
        return getFileFromResourcePath(String.format(SAME_VULNERABILITIES_FILENAME_TEMPLATE, numOfFindings));
    }

    private File getDifferentSeveritiesFindings() throws IOException {
        if (numOfFindings ==1 && totalResults == 3) {
            return getFileFromResourcePath("cucumber/data/sample-sast-results/3-findings-different-severity-medium-high-critical.xml");
        }
        if (numOfFindings == 5 && totalResults == 10) {
            return getFileFromResourcePath("cucumber/data/sample-sast-results/different-severities-10-5.xml");
        }
        if (numOfFindings == 10 && totalResults == 10) {
            return getFileFromResourcePath("cucumber/data/sample-sast-results/different-severities-10-10.xml");
        }
        return null;
    }

    private List<Filter> createFiltersFromString(String filterValue, Filter.Type type) {
        if (StringUtils.isEmpty(filterValue)) {
            return Collections.emptyList();
        }
        String[] filterValArr = filterValue.split(",");
        return Arrays.stream(filterValArr).map(filterVal -> new Filter(type, filterVal)).collect(Collectors.toList());
    }

    private File getFileFromResourcePath(String path) throws IOException{
        return publishUtils.getFileFromResourcePath(path);
    }

    private ScanRequest getScanRequestWithDefaults() {
        return publishUtils.getScanRequestWithDefaults();
    }



    private File getFileForPublish() throws IOException {

        if (useOneFindingForUpdateClose) {
            return getFileFromResourcePath("cucumber/data/sample-sast-results/1-finding-for-update-close.xml");
        }
        if (updating) {
            return getFileFromResourcePath("cucumber/data/sample-sast-results/1-finding-updated.xml");
        }
        if (useSanityFindingsFile) {
            return getFileFromResourcePath("cucumber/data/sample-sast-results/findings-sanity.xml");
        }
        if (numOfFindings == 0) {
            return getFileFromResourcePath("cucumber/data/sample-sast-results/empty-results.xml");
        }
        if (numOfFindings == 1) {
            return getFileFromResourcePath("cucumber/data/sample-sast-results/1-finding.xml");
        }
        switch (findingsType) {
            case SAME_TYPE:
                return getSameVulnerabilityTypeFindings();
            case DIFFERENT_TYPE:
                return getDifferentVulnerabilityTypeFindings();
            case DIFFERENT_SEVERITIES:
                return getDifferentSeveritiesFindings();
            default:
                return null;
        }
    }





    enum FindingsType {
        SAME_TYPE,
        DIFFERENT_TYPE,
        DIFFERENT_SEVERITIES
    }

}
