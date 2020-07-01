package com.checkmarx.flow.cucumber.component.thresholds.scaPR;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.ADOProperties;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.service.ThresholdValidator;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.config.ScaProperties;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.sca.SCAResults;
import com.checkmarx.sdk.dto.sca.Summary;
import com.checkmarx.sdk.service.CxClient;
import com.checkmarx.test.flow.config.CxFlowMocksConfig;
import com.cx.restclient.dto.scansummary.Severity;
import com.cx.restclient.sca.dto.report.Finding;

import org.assertj.core.util.Arrays;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest(classes = { CxFlowMocksConfig.class, CxFlowApplication.class })
@Slf4j
public class ScaThresholdsSteps {

    private final CxClient cxClientMock;
    private final RestTemplate restTemplateMock;
    private final ThresholdValidator thresholdValidator;
    private final FlowProperties flowProperties;
    private final CxProperties cxProperties;
    private final GitHubProperties gitHubProperties;
    private final ADOProperties adoProperties;
    private ScanResults scanResultsToInject;
	private List<Map<String, String>> thresholdDefs;
    private List<Map<String, String>> findingsDefs;

    public ScaThresholdsSteps(CxClient cxClientMock, RestTemplate restTemplateMock, FlowProperties flowProperties,
            ADOProperties adoProperties, CxProperties cxProperties, GitHubProperties gitHubProperties,
            ThresholdValidator thresholdValidator) {
        this.cxClientMock = cxClientMock;
        this.restTemplateMock = restTemplateMock;

        flowProperties.setThresholds(new HashMap<>());
        this.flowProperties = flowProperties;

        this.cxProperties = cxProperties;

        gitHubProperties.setCxSummary(false);
        this.gitHubProperties = gitHubProperties;

        this.adoProperties = adoProperties;

        this.thresholdValidator = thresholdValidator;

    }

    @Before("@ThresholdsFeature")
    public void prepareServices() {
        initMocks();
    }

    private void initMocks() {
        // try {
        // when(cxClientMock.getReportContentByScanId(anyInt(),
        // any())).thenAnswer(answerer);
        // } catch (CheckmarxException e) {
        // Assert.fail("Error initializing mock." + e);
        // }
    }

    @Given("the following thresholds:")
    public void the_following_thresholds(List<Map<String, String>> thresholds) { 
        log.info("setting scan engine to CxSca");
        flowProperties.setEnabledVulnerabilityScanners(Collections.singletonList(ScaProperties.CONFIG_PREFIX));
       
        log.info("found {} threshold definitions", thresholds.size());
        if (log.isInfoEnabled() ) {
            thresholds.forEach(threshold -> {
                log.info("{} --> high: {}, medium: {}, low: {}", threshold.get("threshold-name"),
                        threshold.get("threshold-for-high"), threshold.get("threshold-for-medium"),
                        threshold.get("threshold-for-low"));
            });
        }
        thresholdDefs = thresholds;
    }

    @Given("the following scan findings:")
    public void the_following_scan_findings(List<Map<String, String>> findings) {
        log.info("found {} findings definitions", findings.size());
        if (log.isInfoEnabled() ) {
            findings.forEach(finding -> {
                log.info("{} --> high: {}, medium: {}, low: {}", finding.get("name"), finding.get("max-score-high"),
                        finding.get("max-score-medium"), finding.get("max-score-low"));
            });
        }
        findingsDefs = findings;
    }

    @When("threshold is cofigured to {word}")
    public void threshold_is_cofigured(String selectedConfig) {
        log.info("selected threshold is {}", selectedConfig);
        // TODO: set thresholds !!
        // throw new io.cucumber.java.PendingException();
    }

    @When("scan finding is {word}; using CxSca")
    public void scan_triggered_is_using_CxSca(String findings) {
        SCAResults scaResults = getFakeSCAResults(findings, 1);
        // Write code here that turns the phrase above into concrete actions
        throw new io.cucumber.java.PendingException();
    }

    @Then("pull request should {word}")
    public void pull_request_should_fail(String expected) {
        // Write code here that turns the phrase above into concrete actions
        throw new io.cucumber.java.PendingException();
    }

    private SCAResults getFakeSCAResults(String findingsName , int scanId) {
        SCAResults scaResults = new SCAResults();
        scaResults.setScanId(String.valueOf(scanId));
        Summary summary = new Summary();
        List<Finding> findings = new LinkedList<Finding>();
        Map<String, String> specMap = findingsDefs.stream()
                .filter(findingsDef -> findingsDef.get("name").equals(findingsName)).findAny().get();
        EnumSet.allOf(Severity.class).forEach(severity -> {
            String spec = specMap.get("max-score-" + severity.name().toLowerCase());
            log.info("{}-spec: {}", severity, spec);
            /* create findings */
            Double score = Arrays.asList(spec.split("-")).stream()
                    .map(v -> v.equals("under") ? "0.0" : (String)v)
                    .map(v -> v.equals("over") ? "10.0" : (String)v)
                    .mapToDouble(f -> Float.valueOf(f))
                    .average()
                    .getAsDouble();
            log.info("setting score: {}", score);
            Finding fnd = new Finding();
            fnd.setSeverity(severity);
            fnd.setPackageId("");
            fnd.setScore(score);
            findings.add(fnd);

        });

        scaResults.setFindings(findings);
        scaResults.setSummary(summary);
        return scaResults;
    }
}