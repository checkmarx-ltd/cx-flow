package com.checkmarx.flow.cucumber.integration.cli.iast;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.CxFlowRunner;
import com.checkmarx.flow.config.IastProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.dto.iast.common.model.enums.ManagementResultState;
import com.checkmarx.flow.dto.iast.common.model.enums.QueryDisplayType;
import com.checkmarx.flow.dto.iast.manager.dto.*;
import com.checkmarx.flow.dto.iast.ql.utils.Severity;
import com.checkmarx.flow.exception.JiraClientException;
import com.checkmarx.flow.service.IastService;
import com.checkmarx.flow.service.IastServiceRequests;
import com.checkmarx.flow.service.JiraService;
import com.checkmarx.jira.JiraTestUtils;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.*;


@SpringBootTest(classes = {CxFlowApplication.class, JiraTestUtils.class})
@Slf4j
@RequiredArgsConstructor
@ActiveProfiles({"iast"})
public class IastCliSteps {
    private static final String ARGS = " --iast --bug-tracker=\"jira\" --assignee=\"email@mail.com\" --jira.url=https://xxxx.atlassian.net --jira.username=email@gmail.com --jira.token=token --jira.project=BCB --iast.url=\"http://localhost\" --iast.manager-port=8380 --iast.username=\"username\" --iast.password=\"password\" --iast.update-token-seconds=150 --jira.issue-type=\"Task\"”\n";

    private final CxFlowRunner cxFlowRunner;
    private String scanTag;
    private String numberOfIssue;

    @Autowired
    private IastProperties iastProperties;
    @Autowired
    private JiraProperties jiraProperties;


    private JiraService jiraService = mock(JiraService.class);
    private IastServiceRequests iastServiceRequests = mock(IastServiceRequests.class);

    private IastService iastService;


    @SneakyThrows
    @Given("mock services {} {}")
    public void mockServices(String scanTag, String filter) {
        filter = filter.replaceAll("\"", "");
        List<Severity> filterSeverity = new ArrayList<>(4);
        String[] filterNames = filter.split(",");
        for (int i = 0; i < filterNames.length; i++) {
            filterSeverity.add(Severity.valueOf(filterNames[i].trim()));
        }
        iastProperties.setFilterSeverity(filterSeverity);

        this.scanTag = scanTag;
        this.iastService = new IastService(jiraProperties, jiraService, iastProperties, iastServiceRequests);
        Scan scan = mockIastServiceRequestsApiScansScanTagFinish(scanTag);
        ScanVulnerabilities scanVulnerabilities = mockIastServiceRequestsApiScanVulnerabilities(scan);
        mockIastServiceRequestsApiScanResults(scan, scanVulnerabilities.getVulnerabilities().get(0));
        mockIastServiceRequestsApiScanResults(scan, scanVulnerabilities.getVulnerabilities().get(1));
        mockJiraServiceCreateIssue();

//        tryRunCxFlow(" --scan-tag=\"" + scanTag + "\" " + ARGS);
    }

    @SneakyThrows
    @When("running iast cli {}")
    public void runningIastCli(String scanTag) {
        iastService.stopScanAndCreateJiraIssueFromIastSummary(scanTag);
    }

    @SneakyThrows
    @Then("check how many create issue {}")
    public void checkHowManyCreateIssue(String createJiraIssue) {
        verify(jiraService, times(Integer.parseInt(createJiraIssue.replaceAll("\"", "")))).createIssue(anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString());
    }


    private void mockJiraServiceCreateIssue() throws JiraClientException {
        when(jiraService.createIssue(anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString()
        )).thenReturn("BCB-202");
    }

    private void mockIastServiceRequestsApiScanResults(Scan scan, VulnerabilityInfo vulnerabilityInfo) throws IOException {
        List<ResultInfo> scansResultsQuery = new ArrayList<>();
        ResultInfo scansResultQuery = new ResultInfo();
        scansResultQuery.setResultId(vulnerabilityInfo.getId());
        scansResultQuery.setName("SSRF");
        scansResultQuery.setUrl("bank-gateway/name?name=test_CheckBalance");
        scansResultQuery.setDate(new Date().toInstant());
        if (vulnerabilityInfo.getId() == 73L) {
            scansResultQuery.setSeverity(Severity.MEDIUM);
        } else {
            scansResultQuery.setSeverity(Severity.LOW);
        }
        scansResultQuery.setHttpMethod(HttpMethod.GET);
        scansResultQuery.setNewResult(true);
        scansResultQuery.setResolved(ResolutionStatus.NOT_RESOLVED);
        scansResultQuery.setCorrelated(false);
        scansResultQuery.setResultState(ManagementResultState.TO_VERIFY);
        scansResultQuery.setCwe(918);
        scansResultQuery.setAssignedToUser(false);
        scansResultsQuery.add(scansResultQuery);

        when(iastServiceRequests.apiScanResults(scan.getScanId(), vulnerabilityInfo.getId())).thenReturn(scansResultsQuery);
    }

    @SneakyThrows
    private ScanVulnerabilities mockIastServiceRequestsApiScanVulnerabilities(Scan scan) {
        ScanVulnerabilities scanVulnerabilities = new ScanVulnerabilities();
        scanVulnerabilities.setScanId(scan.getScanId());
        scanVulnerabilities.setProjectId(scan.getProjectId());

        List<VulnerabilityInfo> vulnerabilities = new ArrayList<>(2);
        scanVulnerabilities.setVulnerabilities(vulnerabilities);


        VulnerabilityInfo vulnerabilityInfo = new VulnerabilityInfo();
        vulnerabilityInfo.setId(73L);
        vulnerabilityInfo.setName("SSRF");
        vulnerabilityInfo.setHighestSeverity(Severity.MEDIUM);
        vulnerabilityInfo.setCount(1);
        vulnerabilityInfo.setNewCount(1);
        vulnerabilityInfo.setQueryDisplayType(QueryDisplayType.SIMPLE);
        vulnerabilities.add(vulnerabilityInfo);


        VulnerabilityInfo vulnerabilityInfo2 = new VulnerabilityInfo();
        vulnerabilityInfo2.setId(76L);
        vulnerabilityInfo2.setName("Missing_Expect_CT_Header");
        vulnerabilityInfo2.setHighestSeverity(Severity.LOW);
        vulnerabilityInfo2.setCount(1);
        vulnerabilityInfo2.setNewCount(1);
        vulnerabilityInfo2.setQueryDisplayType(QueryDisplayType.RESPONSE);
        vulnerabilities.add(vulnerabilityInfo2);


        when((iastServiceRequests.apiScanVulnerabilities(scan.getScanId()))).thenReturn(scanVulnerabilities);
        return scanVulnerabilities;
    }

    @SneakyThrows
    private Scan mockIastServiceRequestsApiScansScanTagFinish(String scanTag) {
        Scan scan = new Scan();
        scan.setScanId(1443L);
        scan.setStartTime(new Date().toInstant());
        scan.setProjectId(734L);
        scan.setProjectName("bank-gateway");
        scan.setRiskScore(0);
        scan.setTag(scanTag);
        scan.setCoverage(0.585);
        scan.setApiCoverage(5.56);

        when((iastServiceRequests.apiScansScanTagFinish(scanTag))).thenReturn(scan);
        return scan;
    }
//
//    @Given("IAST running env {}")
//    public void iast_running_env(String scanTag) {
//        System.out.println("IAST running env {}" + scanTag + " " + numberOfIssue );
//        // Write code here that turns the phrase above into concrete actions
//
//    }
//
//    private void tryRunCxFlow(String commandLine) {
//        try {
//            TestUtils.runCxFlow(cxFlowRunner, commandLine);
//        } catch (Throwable e) {
//            log.info("Caught CxFlow execution exception: {}.", e.getClass().getSimpleName());
//        }
//    }

}
