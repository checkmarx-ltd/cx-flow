package com.checkmarx.flow.cucumber.integration.ast.scans;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.cucumber.integration.sca_scanner.ScaCommonSteps;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.service.*;
import com.checkmarx.sdk.config.AstProperties;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.config.ScaProperties;
import com.checkmarx.sdk.dto.ScanResults;
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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
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

    private static final String PUBLIC_REPO = "https://github.com/cxflowtestuser/testsAST.git";
    private static final String SEPARATOR = ",";

    private ResultsService resultsServiceMock;

    private final CxProperties cxProperties;
    private final FlowProperties flowProperties;
    private final AstProperties astProperties;
    private final ScaProperties scaProperties;

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
            startScan(Collections.singletonList(astScanner));
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

    @Given("AST scan is initiated when AST is not available")
    public void configureAstInvalidMachine(){
        astProperties.setApiUrl(cxProperties.getBaseUrl());
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

    @Then("unavailable AST server expected error will be returned {string}")
    public void astScanWithInvalidAstMachine(String error){
        List<VulnerabilityScanner> scanners = new LinkedList<>();
        scanners.add(astScanner);
        try {
            startScan(scanners);
            fail("no exception was thrown");
        }catch(Exception e){
            assertTrue(e.getMessage().contains(error));
        }
    }

    @Given("enabled vulnerability scanners are {string}")
    public void setScanInitiator(String initiatorList) {
        String[] intiators ;
        List<VulnerabilityScanner> scanners = new LinkedList<>();
        if(!initiatorList.contains(SEPARATOR)){
            intiators = new String[1];
            intiators[0] = initiatorList;
        } else {
            intiators = initiatorList.split(SEPARATOR);
        }
        for (String scanType: intiators) {
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

        startScan(scanners);

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

    public void startScan(List<VulnerabilityScanner> scanners) {
        CxProperties cxProperties = new CxProperties();
        ExternalScriptService scriptService = new ExternalScriptService();
        HelperService helperService = new HelperService(flowProperties, cxProperties, scriptService);
     
        ProjectNameGenerator projectNameGenerator = new ProjectNameGenerator(helperService, cxProperties, scriptService);
        FlowService flowService = new FlowService(new ArrayList<>(), projectNameGenerator, resultsServiceMock);

        ScanRequest scanRequest = getBasicScanRequest();

        scanRequest.setVulnerabilityScanners(scanners);
        flowService.initiateAutomation(scanRequest);
    }

    private static ScanRequest getBasicScanRequest() {
        return ScanRequest.builder()
                .project(PUBLIC_PROJECT_NAME)
                .repoUrlWithAuth(PUBLIC_REPO)
                .branch("master")
                .repoType(ScanRequest.Repository.GITHUB)
                .build();
    }
}