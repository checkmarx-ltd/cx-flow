package com.checkmarx.flow.cucumber.integration.ast.scans;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitHubProperties;

import com.checkmarx.flow.cucumber.integration.ast.AstCommonSteps;

import com.checkmarx.flow.dto.ScanRequest;

import com.checkmarx.flow.service.*;

import com.checkmarx.sdk.config.AstProperties;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.config.ScaProperties;
import com.checkmarx.sdk.dto.ScanResults;

import io.cucumber.java.Before;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.spring.CucumberContextConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
public class AstRemoteRepoScanSteps extends AstCommonSteps {

    private static final String PUBLIC_PROJECT_NAME = "Public-Test-Test-Repo";

    private static final String PUBLIC_REPO = "https://github.com/cxflowtestuser/testsAST.git";
    private static final String SEPARATOR = ",";

    private static ResultsService resultsService;

    private final GitHubProperties gitHubProperties;


    private ScanRequest scanRequest;

    private ScanResults scanResults;
    private boolean isScaEnabled;
    private boolean isAstEnabled;

    @Autowired
    private SCAScanner scaScanner;
    
    @Autowired
    private ASTScanner astScanner;
    
    public AstRemoteRepoScanSteps(FlowProperties flowProperties, AstProperties astProperties, ScaProperties scaProperteis,
                                  GitHubProperties gitHubProperties) {
        super(flowProperties, astProperties, scaProperteis);
        this.gitHubProperties = gitHubProperties;

    }

    /**
     * Returns scan results as if they were produced by SAST.
     */
    private class ResultsServiceAnswerer implements Answer<CompletableFuture<ScanResults>> {
        
        @Override
        public CompletableFuture<ScanResults> answer(InvocationOnMock invocation) {

            ScanResults scanResultsLocal = invocation.getArgument(1);
            scanResults = scanResultsLocal;
            return null;
        }
    }
 
    @Before("@ASTRemoteRepoScan")
    public void init() {
        initAstConfig();
        initSCAConfig();
        resultsService = mock(ResultsService.class);
        ResultsServiceAnswerer answerer = new ResultsServiceAnswerer();
        when(resultsService.publishCombinedResults(any(), any())).thenAnswer(answerer);
    }

    @Given("scan initiator list is {string}")
    public void setScanInitiator(String initiatorList) {
        String[] intiators ;
        List<VulnerabilityScanner> scanners = new LinkedList<>();
        if(!initiatorList.contains(SEPARATOR)){
            intiators = new String[1];
            intiators[0] = initiatorList;
        }
        else{
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
                //scanners.add(new ASTScanner(new AstClientImpl(astProperties),flowProperties));
                scanners.add(astScanner);
            }
            
        }

        startScan(scanners);

    }


    @Then("the returned contain populated results for all initiators")
    public void validateResults() {
        if(isScaEnabled) {
            assertNotNull("SCA results are not null.", scanResults.getScaResults());
            assertNotEquals("SCA scan ID is not empty", StringUtils.isEmpty(scanResults.getScaResults().getScanId()));
            assertEquals("Sca vulnerable packages: ", 13, scanResults.getScaResults().getPackages().size() );
        }
        if(isAstEnabled) {
            assertNotNull("AST results are not null.", scanResults.getAstResults());
            assertNotEquals("AST scan ID is not empty", StringUtils.isEmpty(scanResults.getAstResults().getResults().getScanId()));
        }
     }
     
     @And("sca finding count will be {string} and ast findings count {string} will be accordingly")
     public void validateNumberOfFindings(String scaFindings, String astFindings){
         if(isScaEnabled) {
             assertEquals("Sca vulnerable packages: ", 13, scanResults.getScaResults().getPackages().size() );
             assertEquals("Sca findings: ", 13, scanResults.getScaResults().getFindings().size() );

         }
         if(isAstEnabled) {
             assertEquals("AST findings: ", 11, scanResults.getAstResults().getResults().getFindings().size() );

         }
     }


    public void startScan(List<VulnerabilityScanner> scanners) {
        
        CxProperties cxProperties = new CxProperties();
        ExternalScriptService scriptService = new ExternalScriptService();
        HelperService helperService = new HelperService(flowProperties, cxProperties, scriptService);
     
        ProjectNameGenerator projectNameGenerator = new ProjectNameGenerator(helperService, cxProperties, scriptService);
        FlowService flowService = new FlowService(new ArrayList<>(), projectNameGenerator, resultsService);

        scanRequest = getBasicScanRequest(PUBLIC_PROJECT_NAME, PUBLIC_REPO);

        scanRequest.setVulnerabilityScanners(scanners);
        flowService.initiateAutomation(scanRequest);
    }

  

  
}