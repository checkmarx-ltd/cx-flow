package com.checkmarx.flow.cucumber.component.thresholds.scaPR;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.RepoProperties;
import com.checkmarx.flow.cucumber.common.utils.TestUtils;
import com.checkmarx.flow.cucumber.integration.cli.IntegrationTestContext;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.report.PullRequestReport;
import com.checkmarx.flow.exception.ExitThrowable;
import com.checkmarx.flow.service.ThresholdValidator;
import com.checkmarx.flow.service.ThresholdValidatorImpl;
import com.checkmarx.sdk.config.ScaProperties;
import com.checkmarx.sdk.dto.AstScaResults;
import com.checkmarx.sdk.dto.sast.Filter;
import com.checkmarx.sdk.dto.ScanResults;

import com.checkmarx.sdk.dto.sca.SCAResults;
import com.checkmarx.sdk.dto.ast.Summary;
import com.checkmarx.sdk.service.scanner.ScaScanner;
import com.checkmarx.test.flow.config.CxFlowMocksConfig;

import com.checkmarx.sdk.dto.scansummary.Severity;
import com.checkmarx.sdk.dto.sca.report.Finding;

import io.cucumber.java.en.And;
import org.junit.Assert;
import org.springframework.boot.test.context.SpringBootTest;

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

        static ThresholdFeatureKeys fromKey(String key) {
            return valueOf(key.toUpperCase().replace('-', '_'));
        }

        protected Severity toSeverity() {
            return this == THRESHOLD_NAME ? null : Severity.valueOf(name().substring("THRESHOLD_FOR_".length()));
        }
    }

    private static final String CLI_COMMAND = "--project  --cx-project=test --app=MyApp --branch=master --repo-name=CLI-Tests --namespace=CxFlow --blocksysexit";
    private final String DEFAULT_THRESHOLDS_LIMIT = "10";
    private final String DEFAULT_FINDINGS_CONFIG = "default";
    private final FlowProperties flowProperties;
    private final IntegrationTestContext testContext;
    private List<Map<String, String>> thresholdDefs;
    private List<Map<String, String>> findingsDefs;
    private SCAResults scaResults;
    private ScaProperties scaProperties;
    private ThresholdValidatorImpl thresholdValidatorImpl;
    private ScaScanner scaClientMock;
    private boolean thresholdsSectionExist;

    public ScaThresholdsSteps(ThresholdValidatorImpl thresholdValidatorImpl,
                              FlowProperties flowProperties, ThresholdValidator thresholdValidator,
                              ScaProperties scaProperties, IntegrationTestContext testContext, ScaScanner scaClient) {
                flowProperties.setThresholds(new HashMap<>());
        this.flowProperties = flowProperties;
        this.scaProperties = scaProperties;
        this.thresholdValidatorImpl = thresholdValidatorImpl;
        this.scaClientMock = scaClient;
        this.testContext = testContext;
        initMock();
    }

    @Before("@ThresholdsFeature")
    public void prepareServices() {
        log.info("setting scan engine to CxSCA");
        flowProperties.setEnabledVulnerabilityScanners(Collections.singletonList(ScaProperties.CONFIG_PREFIX));
        resetThresholds();
    }

    @Given("the following thresholds-severities:")
    public void the_following_thresholds_severities(List<Map<String, String>> thresholds) {
        log.info("found {} threshold-severities definitions", thresholds.size());
        if (log.isDebugEnabled()) {
            thresholds.forEach(threshold ->
                    log.debug("{} --> high: {}, medium: {}, low: {}",
                            threshold.get(ThresholdFeatureKeys.THRESHOLD_NAME.toKey()),
                            threshold.get(ThresholdFeatureKeys.THRESHOLD_FOR_HIGH.toKey()),
                            threshold.get(ThresholdFeatureKeys.THRESHOLD_FOR_MEDIUM.toKey()),
                            threshold.get(ThresholdFeatureKeys.THRESHOLD_FOR_LOW.toKey())));
        }
        thresholdDefs = thresholds;
    }

    @Given("the following scan findings:")
    public void the_following_scan_findings(List<Map<String, String>> findings) {
        log.info("found {} findings definitions", findings.size());
        if (log.isDebugEnabled()) {
            findings.forEach(finding -> log.debug("{} --> high: {}, medium: {}, low: {}", finding.get("name"), finding.get("high"),
                    finding.get("medium"), finding.get("low")));
        }
        findingsDefs = findings;
    }

    @Given("SCA thresholds section {} in cxflow configuration")
    public void setThresholdsSectionExist(boolean thresholdsExist) {
        thresholdsSectionExist = thresholdsExist;
    }

    @And("SCA thresholds {} by scan findings")
    public void setScaThresholds(boolean exceeded) {
        if (thresholdsSectionExist){
            int thresholdLimit;
            if (exceeded){thresholdLimit = Integer.parseInt(DEFAULT_THRESHOLDS_LIMIT) - 1;}
            else{thresholdLimit = Integer.parseInt(DEFAULT_THRESHOLDS_LIMIT) + 1;}

            Map<Severity, Integer> map = new HashMap<>();
            map.put(Severity.HIGH, thresholdLimit);
            scaProperties.setThresholdsSeverity(map);
        }
    }

    @And("cx-flow.break-build property is set to {}")
    public void setBreakBuildProperty(boolean breakBuild){
        flowProperties.setBreakBuild(breakBuild);
    }

    @When("cxflow called with get-latest-sca-project-results cli command")
    public void runCxFlowFromCommandLine() {

        flowProperties.setBugTracker(BugTracker.Type.NONE.toString());

        Throwable exception = null;
        try {
            TestUtils.runCxFlow(testContext.getCxFlowRunner(), CLI_COMMAND);
        } catch (Throwable e) {
            exception = e;
        }
        testContext.setCxFlowExecutionException(exception);
    }

    private void setDefaultFindings(){
        List<Map<String, String>> findings= new ArrayList<>();
        Map<String, String> map = new HashMap<>();
        map.put("name", DEFAULT_FINDINGS_CONFIG);
        map.put("high", DEFAULT_THRESHOLDS_LIMIT);
        map.put("medium", DEFAULT_THRESHOLDS_LIMIT);
        map.put("low", DEFAULT_THRESHOLDS_LIMIT);

        findings.add(map);
        findingsDefs = findings;
    }

    @When("threshold-severity is configured to {word}")
    public void threshold_severity_is_configured_to_normal(String selectedConfig) {
        configureThreshold(selectedConfig);
    }

    @Then("cxflow SCA should exit with the correct {}")
    public void validateExitCode(int expectedExitCode) {
        Throwable exception = testContext.getCxFlowExecutionException();

        Assert.assertNotNull("Expected an exception to be thrown.", exception);
        Assert.assertEquals(InvocationTargetException.class, exception.getClass());

        Throwable targetException = ((InvocationTargetException) exception).getTargetException();
        Assert.assertTrue(targetException instanceof ExitThrowable);

        int actualExitCode = ((ExitThrowable) targetException).getExitCode();

        Assert.assertEquals("The expected exist code did not match",
                expectedExitCode, actualExitCode);
    }

    private void configureThreshold(String selectedConfig) {
        log.info("selected threshold is {}", selectedConfig);

        Map<Severity, Integer> thresholdsSeverity = thresholdDefs.stream()
                .filter(spec -> spec.get(ThresholdFeatureKeys.THRESHOLD_NAME.toKey()).equals(selectedConfig))
                .flatMap(aMap -> aMap.entrySet().stream())
                .filter(entry -> !ThresholdFeatureKeys.THRESHOLD_NAME.toKey().equals(entry.getKey()))
                .filter(entry -> !entry.getValue().equals("<omitted>"))
                .collect(Collectors.toMap(entry -> ThresholdFeatureKeys.fromKey(entry.getKey()).toSeverity(),
                        entry -> Integer.parseInt(entry.getValue())));

        scaProperties.setThresholdsSeverity(thresholdsSeverity);
    }

    @When("scan finding is {word}")
    public void scan_finding_is_(String findings) {
        this.scaResults = getFakeSCAResults(findings);
    }

    @Then("pull request should {word}")
    public void pull_request_should_fail(String expected) {
        RepoProperties repoProperties = new RepoProperties();
        repoProperties.setErrorMerge(true);
        ScanResults scanResults = new ScanResults();
        scanResults.setScaResults(scaResults);
        PullRequestReport pullRequestReport = new PullRequestReport();
        boolean actual = thresholdValidatorImpl.isMergeAllowed(scanResults, repoProperties, pullRequestReport);
        log.info("is merged allowed = {} (expecting: {})", actual, expected);
        assertEquals(expected.equals("pass"), actual, "is merged allowed = " + actual + ", but was expecting: " + expected);
    }

    @When("max findings score is {word} threshold-score")
    public void max_findings_score_threshold_score(String scoreType) {
        Double findingsScore = generateScoreThresholds(scoreType);
        scaResults = new SCAResults();
        scaResults.setScanId("2");
        Summary summary = new Summary();
        summary.setRiskScore(findingsScore);
        List<Finding> findings = new ArrayList<>();
        Stream<com.checkmarx.sdk.dto.sast.Filter.Severity> severityStream = Arrays.stream(Filter.Severity.values());
        Arrays.stream(Severity.values()).forEach(severity -> populateFindings(findings, severity, 10));
        scaResults.setFindings(findings);
        Map<Filter.Severity, Integer> findingCounts = severityStream
                .collect(Collectors.toMap(Function.identity(), v -> 10));
        summary.setFindingCounts(findingCounts);
        scaResults.setSummary(summary);
    }

    private Double generateScoreThresholds(String scoreType) {
        double thresholdScore = 7.5;
        scaProperties.setThresholdsScore(thresholdScore);
        Double findingsScore = thresholdScore + (scoreType.equals("over") ? 1.0
                : scoreType.equals("under") ? -1.0
                : scoreType.equals("exact") ? 0.0
                : null);

        log.info("findings score is {} -> score: {} threshold: {}", scoreType, findingsScore, thresholdScore);
        return findingsScore;
    }

    @When("the following thresholds fails on {word}")
    public void the_following_threshold_fails(String failType) {
        boolean isPassSeverity = Arrays.asList("score", "none").contains(failType);
        boolean isPassScore = Arrays.asList("count", "none").contains(failType);

        thresholdDefs = Collections.singletonList(Arrays.stream(ThresholdFeatureKeys.values())
                .collect(Collectors.toMap(
                        key -> key.name().toLowerCase().replace('_', '-'),
                        key -> key == ThresholdFeatureKeys.THRESHOLD_NAME ? "spec" : "10")));

        findingsDefs = new ArrayList<>();

        Map<String, String> map = new HashMap<>();
        map.put("name", "findings-severity");
        Arrays.stream(Severity.values())
                .forEach(key -> map.put(key.name().toLowerCase(), isPassSeverity ? "5" : "15"));
        findingsDefs.add(map);

        configureThreshold("spec");
        scaResults = getFakeSCAResults("findings-severity");

        Double findingsScore = generateScoreThresholds(isPassScore ? "under" : "over");
        scaResults.getSummary().setRiskScore(findingsScore);
    }

    private SCAResults getFakeSCAResults(String findingsName) {
        SCAResults scaResults = new SCAResults();
        scaResults.setScanId("1");
        Summary summary = new Summary();
        Map<Filter.Severity, Integer> summaryMap = new EnumMap<>(Filter.Severity.class);
        List<Finding> findings = new LinkedList<>();
        Map<String, String> specMap = findingsDefs.stream()
                .filter(findingsDef -> findingsDef.get("name").equals(findingsName)).findAny().get();

        EnumSet.allOf(Severity.class).forEach(severity -> {
            String spec = specMap.get(severity.name().toLowerCase());
            log.info("{}-spec: {}", severity, spec);

            /* create findings */
            Integer count = Arrays.stream(spec.split("-than-"))
                    .mapToInt(v -> "more".equals(v) ? 3 : "less".equals(v) ? -3 : Integer.parseInt(v))
                    .reduce(0, Integer::sum);
            log.info("going to generate {} issues with {} severity", count, severity);
            summaryMap.put(Filter.Severity.valueOf(severity.name()), count);
            populateFindings(findings, severity, count);
        });
        summary.setFindingCounts(summaryMap);
        scaResults.setFindings(findings);
        scaResults.setSummary(summary);
        return scaResults;
    }

    private void populateFindings(List<Finding> findings, Severity severity, Integer count) {
        for (int i = 0; i < count; i++) {
            Finding fnd = new Finding();
            fnd.setSeverity(severity);
            fnd.setPackageId("");
            findings.add(fnd);
        }
    }

    private void initMock()
    {
        setDefaultFindings();
        SCAResults scaResults = getFakeSCAResults(DEFAULT_FINDINGS_CONFIG);

        AstScaResults wrapper = new AstScaResults();
        wrapper.setScaResults(scaResults);
        when(scaClientMock.getLatestScanResults(any())).thenReturn(wrapper);
    }

    private void resetThresholds() {
        flowProperties.setThresholds(null);
        scaProperties.setThresholdsSeverity(null);
        scaProperties.setThresholdsScore(null);
    }
}