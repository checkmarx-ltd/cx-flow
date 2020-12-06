package com.checkmarx.flow.cucumber.integration.ast.scans;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.cucumber.integration.sca_scanner.ScaCommonSteps;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.service.*;
import com.checkmarx.sdk.config.AstProperties;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.config.ScaProperties;
import com.checkmarx.sdk.dto.CxConfig;
import com.checkmarx.sdk.dto.ScanResults;
import com.cx.restclient.ast.dto.sast.report.Finding;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@CucumberContextConfiguration
@SpringBootTest(classes = {CxFlowApplication.class})
@Slf4j
@RequiredArgsConstructor
public class AstRemoteRepoScanSteps {

    final String EMPTY_STRING_INDICATOR = "<empty>";
    private static final String PUBLIC_PROJECT_NAME = "Public-Test-Test-Repo";

    private static final String BRANCH = "master";

    private static final String GITHUB_REPO_FOR_DESCRIPTION_TEST = "https://github.com/cxflowtestuser/public-rest-repo";
    
    private static final String PUBLIC_REPO = "https://github.com/cxflowtestuser/testsAST.git";
    private static final String SEPARATOR = ",";

    private ResultsService resultsServiceMock;

    private final CxProperties cxProperties;
    private final FlowProperties flowProperties;
    private final AstProperties astProperties;
    private final ScaProperties scaProperties;
    private final ConfigurationOverrider configOverrider;
    private final ScaConfigurationOverrider scaConfigOverrider;

    private ScanResults scanResults;
    private boolean isScaEnabled;
    private boolean isAstEnabled;
    private Exception scanException;

    @Autowired
    private SCAScanner scaScanner;

    @Autowired
    private ASTScanner astScanner;

    private String validClientId;
    private String validClientSecret;

    private class ScanResultsInterceptor implements Answer<CompletableFuture<ScanResults>> {
        @Override
        public CompletableFuture<ScanResults> answer(InvocationOnMock invocation) {
            scanResults = invocation.getArgument(1);
            return null;
        }
    }
 
    @Before("@ASTRemoteRepoScan")
    public void init() {
        ScaCommonSteps.initSCAConfig(scaProperties);
        resultsServiceMock = mock(ResultsService.class);
        ScanResultsInterceptor answerer = new ScanResultsInterceptor();
        when(resultsServiceMock.publishCombinedResults(any(), any())).thenAnswer(answerer);
    }

    @Before("@InvalidCredentials")
    public void backupValidCredentials() {
        log.info("Saving valid credentials.");
        validClientId = astProperties.getClientId();
        validClientSecret = astProperties.getClientSecret();
    }

    @After("@InvalidCredentials")
    public void restoreValidCredentials() {
        // The @InvalidCredentials changes credentials in astProperties,
        // so here we need to restore them for other tests to work.
        log.info("Restoring valid credentials.");
        astProperties.setClientId(validClientId);
        astProperties.setClientSecret(validClientSecret);
    }

    @When("CxFlow tries to start AST scan with the {string} and {string} credentials")
    public void configureToken(String clientIdDescr, String clientSecretDescr){
        astProperties.setClientId(toActualClientId(clientIdDescr));
        astProperties.setClientSecret(toActualClientSecret(clientSecretDescr));
        isAstEnabled = true;
        try {
            startScan(Collections.singletonList(astScanner), BRANCH, PUBLIC_REPO);
        } catch (Exception e) {
            scanException = e;
        }
    }

    private String toActualClientSecret(String clientSecretDescr) {
        String result;
        if (clientSecretDescr.equals(EMPTY_STRING_INDICATOR)) {
            result = "";
        } else if (clientSecretDescr.equals("<valid-secret>")) {
            result = validClientSecret;
        } else {
            result = clientSecretDescr;
        }
        return result;
    }

    private String toActualClientId(String clientIdDescr) {
        String result;
        if (clientIdDescr.equals(EMPTY_STRING_INDICATOR)) {
            result = "";
        } else if (clientIdDescr.equals("<valid-client-id>")) {
            result = validClientId;
        } else {
            result = clientIdDescr;
        }
        return result;
    }

    @When("AST scan is initiated with API url: {string}")
    public void configureAstInvalidMachine(String url) {
        String effectiveUrl = url.replace("<ast url>", astProperties.getApiUrl())
                .replace("<sast url>", cxProperties.getBaseUrl());

        log.info("Setting url to {}", effectiveUrl);
        astProperties.setApiUrl(effectiveUrl);
        flowProperties.setEnabledVulnerabilityScanners(Collections.singletonList(AstProperties.CONFIG_PREFIX));
        isAstEnabled = true;
    }

    @Then("an error will be thrown with the message containing {string}")
    public void errorWillBeThrown(String messageSubstring) {
        assertNotNull("Expected an exception but didn't get any.", scanException);

        String testFailureMessage = String.format("The exception message ('%s') doesn't contain the expected text ('%s')",
                scanException.getMessage(),
                messageSubstring);

        assertTrue(testFailureMessage, StringUtils.containsIgnoreCase(scanException.getMessage(), messageSubstring));
    }

    @Then("an exception of type {string} will be thrown with the message containing {string}")
    public void astScanWithInvalidAstMachine(String exceptionType, String errorSubstring) {
        List<VulnerabilityScanner> scanners = new LinkedList<>();
        scanners.add(astScanner);
        try {
            startScan(scanners, BRANCH, PUBLIC_REPO);
            fail("no exception was thrown");
        } catch (Exception e) {
            assertEquals("Unexpected exception type.",exceptionType, e.getClass().getSimpleName());

            String message = String.format("Expected error message to contain '%s'.", errorSubstring);
            boolean containsSubstring = StringUtils.containsIgnoreCase(e.getMessage(), errorSubstring);
            assertTrue(message, containsSubstring);
        }
    }

    @Given("enabled vulnerability scanners are {string} and branch is {string}")
    public void setScanInitiatorAndBranch(String initiatorList, String branch) {
        List<VulnerabilityScanner> scanners = setScanInitiator(initiatorList);
        startScan(scanners, branch, GITHUB_REPO_FOR_DESCRIPTION_TEST);
    }
    
    @And("each finding will contain AST populated description field")
    public void validateAdditionalFields(){
        assertTrue("AST description field is empty", StringUtils.isNotEmpty(scanResults.getAstResults().getResults().getFindings().get(0).getDescription()));
    }

    @And("finding with the same queryId will have the same description and there will be a unique finding description for each queryId")
    public void validateDescriptions() {
        validateDescriptions(scanResults.getAstResults().getResults().getFindings());
    }

    private void validateDescriptions(List<Finding> findings) {

        Map<String, Set<String>> mapDescriptions = new HashMap<>();

        findings.forEach(finding -> {
            Set<String> listDescriptions = mapDescriptions.get(finding.getQueryID());
            if(listDescriptions == null){
                listDescriptions = new HashSet<>();
            }
            listDescriptions.add(finding.getDescription());
            mapDescriptions.put(finding.getQueryID(), listDescriptions);
        });

        Set<String> uniqueDescriptions = new HashSet<>();

        //validate for each queryId there is exactly one corresponding description
        for( Map.Entry<String, Set<String>> entry :mapDescriptions.entrySet()){
            Assert.assertEquals( 1, entry.getValue().size());
            uniqueDescriptions.add((String)entry.getValue().toArray()[0]);
        }

        //validate all descriptions are unique
        Assert.assertEquals(uniqueDescriptions.size(),mapDescriptions.size() );
    }
    
    @Given("enabled vulnerability scanners are {string}")
    public void startScanForInitiator(String initiatorList) {
        List<VulnerabilityScanner> scanners = setScanInitiator(initiatorList);
        startScan(scanners, BRANCH, PUBLIC_REPO);

    }

    private List<VulnerabilityScanner> setScanInitiator(String initiatorList) {
        String[] initiators  = initiatorList.split(SEPARATOR);
        List<VulnerabilityScanner> scanners = new LinkedList<>();
        
        for (String scanType: initiators) {
           if(scanType.equalsIgnoreCase(ScaProperties.CONFIG_PREFIX)){
                flowProperties.setEnabledVulnerabilityScanners(Collections.singletonList(ScaProperties.CONFIG_PREFIX));
                this.isScaEnabled = true;
                //scanners.add(new SCAScanner(new ScaClientImpl(scaProperties),flowProperties));
               scanners.add(scaScanner);
            }
            if(scanType.equalsIgnoreCase(AstProperties.CONFIG_PREFIX)){
                flowProperties.setEnabledVulnerabilityScanners(Collections.singletonList(AstProperties.CONFIG_PREFIX));
                this.isAstEnabled = true;
                scanners.add(astScanner);
            }
            
        }
        return scanners;
    }


    @Then("scan results contain populated results for all scanners")
    public void validateResults() {
        if(isScaEnabled) {
            assertNotNull("SCA results are null", scanResults.getScaResults());
            assertTrue("SCA scan ID is empty", StringUtils.isNotEmpty(scanResults.getScaResults().getScanId()));
        }
        if(isAstEnabled) {
            assertNotNull("AST results are null.", scanResults.getAstResults());
            assertTrue("AST scan ID is empty", StringUtils.isNotEmpty(scanResults.getAstResults().getResults().getScanId()));
        }
     }

    @And("sca finding count will be {string} and ast findings count {string} will be accordingly")
    public void validateNumberOfFindings(String scaFindings, String astFindings) {
        if (isScaEnabled) {
            validateFindingCount(scaFindings, scanResults.getScaResults().getFindings(), "SCA");
        }
        if (isAstEnabled) {
            validateFindingCount(astFindings, scanResults.getAstResults().getResults().getFindings(), "AST");
        }
    }

    private void validateFindingCount(String expectedCountDescr, List<?> actualFindings, String scannerType) {
        int actualSize = actualFindings.size();
        String message = String.format("Expected that %s finding count would be %s, but got %d.",
                scannerType,
                expectedCountDescr,
                actualSize);
        boolean countIsExpected = expectedCountDescr.equals("0") ? actualSize == 0 : actualSize > 0;
        assertTrue(message, countIsExpected);
    }

    public void startScan(List<VulnerabilityScanner> scanners, String branch, String repo) {
        CxProperties cxProperties = new CxProperties();
        ExternalScriptService scriptService = new ExternalScriptService();
        CxScannerService cxScannerService = new CxScannerService(cxProperties,null, null, null, null );
        HelperService helperService = new HelperService(flowProperties, cxScannerService, scriptService);
     
        ProjectNameGenerator projectNameGenerator = new ProjectNameGenerator(helperService, cxScannerService);
        FlowService flowService = new FlowService(new ArrayList<>(), projectNameGenerator, resultsServiceMock);

        ScanRequest scanRequest = getBasicScanRequest(branch, repo);

        scanRequest = configOverrider.overrideScanRequestProperties(new CxConfig(), scanRequest);
        scanRequest.setVulnerabilityScanners(scanners);
        flowService.initiateAutomation(scanRequest);
    }

    private ScanRequest getBasicScanRequest(String branch, String repo) {
        ScanRequest result = ScanRequest.builder()
                .project(PUBLIC_PROJECT_NAME)
                .repoUrlWithAuth(repo)
                .branch(branch)
                .repoType(ScanRequest.Repository.GITHUB)
                .build();
        scaConfigOverrider.initScaConfig(result);
        return result;
    }
}