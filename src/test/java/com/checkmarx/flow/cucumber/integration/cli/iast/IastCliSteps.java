package com.checkmarx.flow.cucumber.integration.cli.iast;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.CxFlowRunner;
import com.checkmarx.flow.config.*;
import com.checkmarx.flow.controller.IastController;
import com.checkmarx.flow.custom.GitHubIssueTracker;
import com.checkmarx.flow.custom.GitLabIssueTracker;
import com.checkmarx.flow.custom.IssueTracker;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.Issue;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.iast.common.model.enums.ManagementResultState;
import com.checkmarx.flow.dto.iast.common.model.enums.QueryDisplayType;
import com.checkmarx.flow.dto.iast.manager.dto.*;
import com.checkmarx.flow.dto.iast.manager.dto.description.VulnerabilityDescription;
import com.checkmarx.flow.dto.iast.ql.utils.Severity;
import com.checkmarx.flow.exception.IastThresholdsSeverityException;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.service.*;
import com.checkmarx.jira.JiraTestUtils;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.logging.log4j.util.Strings;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static com.atlassian.sal.api.xsrf.XsrfHeaderValidator.TOKEN_HEADER;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = {CxFlowApplication.class, JiraTestUtils.class})
@AutoConfigureMockMvc
@TestPropertySource(properties = { "spring.config.location=classpath:application-iast.yml" })
@RequiredArgsConstructor
@ActiveProfiles({"iast"})
@MockBean({IastServiceRequests.class, JiraService.class, GitHubIssueTracker.class, GitLabIssueTracker.class})
public class IastCliSteps {
    private static String ARGS = "--iast --assignee=email@mail.com --repo-name=repository --branch=master --namespace=test --jira.url=https://xxxx.atlassian.net --jira.username=email@gmail.com --jira.token=token --github.token=token --gitlab.token=token --jira.project=BCB --iast.url=http://localhost --iast.manager-port=8380 --iast.username=username --iast.password=password --iast.update-token-seconds=250 --jira.issue-type=Task --project-id=1";

    private CxFlowRunner cxFlowRunner;

    @Autowired
    private IastProperties iastProperties;
    @Autowired
    private JiraProperties jiraProperties;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private IastController iastController;
    @Autowired
    private IastService iastService;

    @Autowired
    private IastServiceRequests iastServiceRequests;
    @Autowired
    private JiraService jiraService;
    @Autowired
    private GitHubIssueTracker gitHubIssueTracker;
    @Autowired
    private GitLabIssueTracker gitLabIssueTracker;

    private ApplicationArguments args;

    private final FlowProperties flowProperties;
    private final CxScannerService cxScannerService;
    private final GitHubProperties gitHubProperties;
    private final GitLabProperties gitLabProperties;
    private final ADOProperties adoProperties;
    private final HelperService helperService;
    private final List<ThreadPoolTaskExecutor> executors;
    private final ResultsService resultsService;
    private final OsaScannerService osaScannerService;
    private final FilterFactory filterFactory;
    private final ConfigurationOverrider configOverrider;
    private final BuildProperties buildProperties;
    private final List<VulnerabilityScanner> scanners;
    private final ThresholdValidator thresholdValidator;

    private String urlRequest;
    private HttpHeaders headers;
    private JSONObject body;
    private MvcResult mvcResult;

    @Given("mock CLI runner {} {} {}")
    public void mockCliRunner(String scanTag, String bugTracker, String args) {
        scanTag = removeQuotes(scanTag);
        bugTracker = removeQuotes(bugTracker);
        args = removeQuotes(args);
        cxFlowRunner = new CxFlowRunner(
                flowProperties,
                cxScannerService,
                jiraProperties,
                gitHubProperties,
                gitLabProperties,
                iastService,
                adoProperties,
                helperService,
                executors,
                resultsService,
                osaScannerService,
                filterFactory,
                configOverrider,
                buildProperties,
                scanners,
                thresholdValidator);
        String arguments = ARGS + " --scan-tag=" + scanTag + " --bug-tracker=" + bugTracker + " " + args;
        String[] argsParam = arguments.split(" ");
        this.args = new DefaultApplicationArguments(argsParam);
    }

    @SneakyThrows
    @When("running cli {} {}")
    public void runningIastCli(String exitCode, String scanTag) {
        exitCode = removeQuotes(exitCode);
        try {
            Method method = cxFlowRunner.getClass().getDeclaredMethod("commandLineRunner", ApplicationArguments.class);
            method.setAccessible(true);
            method.invoke(cxFlowRunner, args);
        } catch (InvocationTargetException e) {
            //catch ExitThrowable. That exception throw when we try to finish application. That is normal situation whe we fall build by Thresholds Severity
            String messageExitCode = e.getTargetException().getMessage().toLowerCase(Locale.ROOT);

            System.out.println(scanTag);
            Assert.assertEquals("exit code:" + exitCode, messageExitCode);
        }
    }

    @When("I am using these parameters {} {} {} {} {}")
    public void requestBodyBuild(String projectId, String namespace, String assignee, String bugTrackerProject, String repoName) {
        body = new JSONObject();
        if (!Strings.isEmpty(projectId)) {
            body.put("project-id", Integer.valueOf(removeQuotes(projectId, false)));
        }
        if (!Strings.isEmpty(namespace)) {
            body.put("namespace", removeQuotes(namespace, false));
        }
        if (!Strings.isEmpty(assignee)) {
            body.put("assignee", removeQuotes(assignee, false));
        }
        if (!Strings.isEmpty(bugTrackerProject)) {
            body.put("bugTrackerProject", removeQuotes(bugTrackerProject, false));
        }
        if (!Strings.isEmpty(repoName)) {
            body.put("repoName", removeQuotes(repoName, false));
        }
    }

    @When("I do a request to bug tracker {} using this scan tag {} and this request token {}")
    public void doRequest(String bugTracker, String scanTag, String requestToken) throws Exception {
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(TOKEN_HEADER, requestToken);

        urlRequest = "/iast/stop-scan-and-create-" + removeQuotes(bugTracker, false) + "-issue/" + removeQuotes(scanTag, false);

        mvcResult = mvc.perform(MockMvcRequestBuilders.post(urlRequest)
                .content(body.toString())
                .headers(headers)
        ).andReturn();
    }

    @SneakyThrows
    @Then("the status code should be equals to {}")
    public void checkStatusCode(int statusCode) {
        Assert.assertEquals(mvcResult.getResponse().getStatus(), statusCode);
    }

    @Given("mock services {} {} {}")
    public void mockServices(String scanTag, String filter, String thresholdsSeverity) throws Exception {
        scanTag = removeQuotes(scanTag);
        filter = removeQuotes(filter);
        thresholdsSeverity = removeQuotes(thresholdsSeverity);

        List<Severity> filterSeverity = new ArrayList<>(4);
        String[] filterNames = filter.split(",");
        for (String filterName : filterNames) {
            filterSeverity.add(Severity.valueOf(filterName.trim()));
        }
        iastProperties.setFilterSeverity(filterSeverity);

        String[] thresholdsSeverityArray = thresholdsSeverity.split(",");
        Map<Severity, Integer> thresholdsSeverityMap = new HashMap<>();
        for (String s : thresholdsSeverityArray) {
            String[] split = s.split("=");
            thresholdsSeverityMap.put(Severity.valueOf(split[0]), new Integer(split[1]));
        }
        iastProperties.setThresholdsSeverity(thresholdsSeverityMap);

        Mockito.reset(jiraService, gitHubIssueTracker, gitLabIssueTracker);

        Scan scan = mockIastServiceRequestsApiScansScanTagFinish(scanTag);
        ScanVulnerabilities scanVulnerabilities = mockIastServiceRequestsApiScanVulnerabilities(scan);
        mockIastServiceRequestsApiScanResults(scan, scanVulnerabilities.getVulnerabilities().get(0));
        mockIastServiceRequestsApiScanResults(scan, scanVulnerabilities.getVulnerabilities().get(1));
        mockServiceCreateIssue();
    }

    private String removeQuotes(String text) {
        return removeQuotes(text, true);
    }

    private String removeQuotes(String text, boolean trim) {
        String retText = text.replaceAll("\"", "");

        if (trim) {
            return retText.trim();
        }
        return retText;
    }

    @SneakyThrows
    @When("running iast service {}")
    public void runningIastService(String scanTag) {
        scanTag = removeQuotes(scanTag);
        try {
            BugTracker.Type bugType = BugTracker.Type.GITHUBISSUE;
            String assignee = "test_user";
            BugTracker bt = BugTracker.builder()
                    .type(bugType)
                    .assignee(assignee)
                    .build();

            ScanRequest request = ScanRequest.builder()
                    .bugTracker(bt)
                    .build();

            iastService.stopScanAndCreateIssue(request, scanTag);
        } catch (IastThresholdsSeverityException e) {
            //that is ok. Just Thresholds Severity
        }
    }

    @SneakyThrows
    @Then("check how many create issue {} {}")
    public void checkHowManyCreateIssue(String createIssue, String bugTracker) {
        int createdIssues = Integer.parseInt(removeQuotes(createIssue));
        IssueTracker issueTracker = null;
        switch (bugTracker) {
            case "jira":
                verify(jiraService, times(createdIssues)).createIssue(any(), any());
                return;
            case "githubissue":
                issueTracker = gitHubIssueTracker;
                break;
            case "gitlabissue":
                issueTracker = gitLabIssueTracker;
                break;
        }
        if(issueTracker != null) {
            verify(issueTracker, times(createdIssues)).createIssue(any(), any());
        }
    }


    private void mockServiceCreateIssue() throws MachinaException {
        when(jiraService.createIssue(any(), any())).thenReturn("BCB-202");

        when(gitHubIssueTracker.createIssue(any(), any())).thenReturn(mock(Issue.class));

        when(gitLabIssueTracker.createIssue(any(), any())).thenReturn(mock(Issue.class));

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

        when(iastServiceRequests.apiScanVulnerabilities(scan.getScanId())).thenReturn(scanVulnerabilities);
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

        when(iastServiceRequests.apiScansScanTagFinish(scanTag)).thenReturn(scan);

        VulnerabilityDescription vulnerabilityDescription = mock(VulnerabilityDescription.class);
        when(iastServiceRequests.apiVulnerabilitiesDescription(any(), any())).thenReturn(vulnerabilityDescription);
        when(vulnerabilityDescription.getRisk()).thenReturn("MOCK_RISK");
        return scan;
    }
}
