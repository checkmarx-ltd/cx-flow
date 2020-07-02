package com.checkmarx.flow.cucumber.component.thresholds.scaPR;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.ADOProperties;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.service.ThresholdValidator;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.config.ScaProperties;
import com.checkmarx.sdk.dto.Filter;
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

    private enum ThresholdFeatureKeys {
        THRESHOLD_NAME, THRESHOLD_FOR_HIGH, THRESHOLD_FOR_MEDIUM, THRESHOLD_FOR_LOW;

        protected String toKey() {
            return this.name().toLowerCase().replace('_', '-');
        }
    }

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
    private SCAResults scaResults;

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
        log.info("setting scan engine to CxSca");
        flowProperties.setEnabledVulnerabilityScanners(Collections.singletonList(ScaProperties.CONFIG_PREFIX));
    }

    private void initMocks() {
        // try {
        // when(cxClientMock.getReportContentByScanId(anyInt(),
        // any())).thenAnswer(answerer);
        // } catch (CheckmarxException e) {
        // Assert.fail("Error initializing mock." + e);
        // }
    }

    @Given("the following thresholds-severitys:")
    public void the_following_thresholds_severitys(List<Map<String, String>> thresholds) {
        log.info("found {} threshold-severitys definitions", thresholds.size());
        if (log.isDebugEnabled()) {
            thresholds.forEach(threshold -> {
                log.debug("{} --> high: {}, medium: {}, low: {}",
                        threshold.get(ThresholdFeatureKeys.THRESHOLD_NAME.toKey()),
                        threshold.get(ThresholdFeatureKeys.THRESHOLD_FOR_HIGH.toKey()),
                        threshold.get(ThresholdFeatureKeys.THRESHOLD_FOR_MEDIUM.toKey()),
                        threshold.get(ThresholdFeatureKeys.THRESHOLD_FOR_LOW.toKey()));
            });
        }
        thresholdDefs = thresholds;
    }

    @Given("the following scan findings:")
    public void the_following_scan_findings(List<Map<String, String>> findings) {
        log.info("found {} findings definitions", findings.size());
        if (log.isDebugEnabled()) {
            findings.forEach(finding -> {
                log.debug("{} --> high: {}, medium: {}, low: {}", finding.get("name"), finding.get("high"),
                        finding.get("medium"), finding.get("low"));
            });
        }
        findingsDefs = findings;
    }

    @When("threshold-severity is cofigured to {word}")
    public void threshold_severity_is_cofigured_to_normal(String selectedConfig) {
        log.info("selected threshold is {}", selectedConfig);
        // TODO: set thresholds !!
        // throw new io.cucumber.java.PendingException();
    }

    @When("scan finding is {word}")
    public void scan_finding_is_(String findings) {
        this.scaResults = getFakeSCAResults(findings);
    }

    @Then("pull request should {word}")
    public void pull_request_should_fail(String expected) {
        // Write code here that turns the phrase above into concrete actions
        throw new io.cucumber.java.PendingException();
    }

    @When("max findings score is {word} threshold-score")
    public void max_findings_score_threshold_score(String scoreType) {
        double thresholdScore = 7.5;
        Double findingsScore = thresholdScore + (scoreType.equals("over") ? 1.0
                : scoreType.equals("under") ? -1.0 
                : scoreType.equals("exact") ? 0.0 
                : null);

        log.info("findings score is {} -> score: {} threshold: {}", scoreType, findingsScore, thresholdScore);
        // Write code here that turns the phrase above into concrete actions
        throw new io.cucumber.java.PendingException();
    }

    @When("the folowing threashold\\/s {word} fails")
    public void the_folowing_threashold_fails(String failType) {
        // Write code here that turns the phrase above into concrete actions
        throw new io.cucumber.java.PendingException();
    }

    private SCAResults getFakeSCAResults(String findingsName) {
        SCAResults scaResults = new SCAResults();
        scaResults.setScanId("1");
        Summary summary = new Summary();
        Map<Filter.Severity, Integer> summaryMap = new EnumMap<>(Filter.Severity.class);
        List<Finding> findings = new LinkedList<Finding>();
        Map<String, String> specMap = findingsDefs.stream()
                .filter(findingsDef -> findingsDef.get("name").equals(findingsName)).findAny().get();

        EnumSet.allOf(Severity.class).forEach(severity -> {
            String spec = specMap.get(severity.name().toLowerCase());
            log.info("{}-spec: {}", severity, spec);

            /* create findings */
            Integer count = Arrays.asList(spec.split("-than-")).stream()
                    .mapToInt(v -> "more".equals(v) ? 3 : "less".equals(v) ? -3 : Integer.valueOf((String) v))
                    .reduce(0, Integer::sum);
            log.debug("going to generate {} issues with {} severity", count, severity);
            summaryMap.put(Filter.Severity.valueOf(severity.name()), count);
            for (int i = 0; i < count; i++) {
                Finding fnd = new Finding();
                fnd.setSeverity(severity);
                fnd.setPackageId("");
                findings.add(fnd);
            }
        });
        summary.setFindingCounts(summaryMap);
        scaResults.setFindings(findings);
        scaResults.setSummary(summary);
        return scaResults;
    }
}