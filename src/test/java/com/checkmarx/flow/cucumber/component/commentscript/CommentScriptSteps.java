package com.checkmarx.flow.cucumber.component.commentscript;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.properties.FlowProperties;
import com.checkmarx.flow.config.properties.JiraProperties;
import com.checkmarx.flow.controller.FlowController;
import com.checkmarx.flow.cucumber.common.utils.TestUtils;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.BugTrackersDto;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.sastscanning.ScanRequestConverter;
import com.checkmarx.flow.service.*;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.CxProperties;

import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.cx.CxScanParams;
import com.checkmarx.sdk.exception.CheckmarxException;
import com.checkmarx.sdk.service.*;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {CxFlowApplication.class})
@CucumberContextConfiguration
@Slf4j
public class CommentScriptSteps {

    private final static String MEANINGLESS_STRING = "meaningless-string";
    private final static String WEBHOOK_TOKEN = "Token";
    private final static  String EMPTY_SCRIPT = "empty";
    private final static  String INVALID_SCRIPT = "invalid-syntax-script-comment";
    private final static  String EMPTY_STRING = "";
    final private CxService cxClientMock;
    private final ScanRequestConverter scanRequestConverterMock;
    private final SastScanner sastScanner;
    private final CxProperties cxProperties;
    private final FlowController flowController;
    private final FlowProperties flowProperties;
    private String branchName;
    private String commentMessageFromRequest;

    public CommentScriptSteps(FlowProperties flowProperties, CxProperties cxProperties, HelperService helperService,
                              JiraProperties jiraProperties, FilterFactory filterFactory, ConfigurationOverrider configOverrider,
                              ResultsService resultService, ProjectNameGenerator projectNameGenerator, BugTrackerEventTrigger btet){


        cxClientMock = mock(CxService.class);
        
        scanRequestConverterMock = mock(ScanRequestConverter.class, Mockito.withSettings().useConstructor(
                helperService,  flowProperties,  null,   null,   null,   null,  null, cxClientMock, cxProperties ));
        
        this.flowProperties = flowProperties;
        
        sastScanner = mock(SastScanner.class, Mockito.withSettings().useConstructor(
                resultService,
                cxProperties,
                flowProperties,
                null,
                projectNameGenerator,
                cxClientMock, new BugTrackersDto(null,btet,null, null, null, null, null)));
        
        this.cxProperties = cxProperties;
        FlowService flowService = new FlowService(Collections.singletonList(sastScanner), projectNameGenerator, resultService);

        CxScannerService scannerService = new CxScannerService(cxProperties, null, flowProperties, cxClientMock, null);
        this.flowController = new FlowController(flowProperties, scannerService, flowService, helperService, jiraProperties, filterFactory, configOverrider, sastScanner, null, null, null,null, null);
    }


    @Before("@ConfigureSastComment")
    public void setMockers() throws CheckmarxException {
        when(cxClientMock.getScanIdOfExistingScanIfExists(anyInt())).thenReturn(-1);
        when(cxClientMock.createScan(any(), anyString())).thenAnswer( invocation-> {
            commentMessageFromRequest = invocation.getArgument(1);
            return null;});
        when(cxClientMock.getReportContentByScanId(nullable(Integer.class), any())).thenReturn(new ScanResults());

        when(sastScanner.getScannerClient()).thenReturn(cxClientMock);
        when(sastScanner.getScanRequestConverter()).thenReturn(scanRequestConverterMock);
        when(sastScanner.getScanComment(any())).thenCallRealMethod();
        when(sastScanner.getCxPropertiesBase()).thenReturn(cxProperties);
       
                
        when(sastScanner.isEnabled()).thenReturn(true);
        when(sastScanner.scan(any())).thenCallRealMethod();

        CxScanParams cxScanParams = new CxScanParams();
        cxScanParams.setProjectId(1);
        when(scanRequestConverterMock.toScanParams(any())).thenReturn(cxScanParams);

        cxProperties.setProjectScript(EMPTY_STRING);
        flowProperties.setBugTracker(BugTracker.Type.NONE.toString());
        commentMessageFromRequest = EMPTY_STRING;
        branchName = EMPTY_STRING;
    }

    @Given("given 'sast-comment' script name is {string}")
    public void setCommentScriptName(String scriptName) {
        if(!scriptName.equals(EMPTY_SCRIPT)) {
            String fullName = scriptName + ".groovy";

            if (scriptName.equals(INVALID_SCRIPT)){
                fullName = fullName + ".invalid";
            }

            String commentScript = getScriptFullPath(fullName);
            flowProperties.setCommentScript(commentScript);
        }
        else{
            flowProperties.setCommentScript(EMPTY_STRING);
        }
    }

    @And("Scan request contains feature branch name {string}")
    public void setScanRequestBranch(String branchName){

        this.branchName = branchName;
    }

    private String getScriptFullPath(String scriptName) {
        String result;
        String path = "input-scripts-sample/" + scriptName;

        try {
            File fullPath = TestUtils.getFileFromResource(path);
            result = fullPath.getPath();
        }catch (IOException ex) {
            result = "script-not-exist";
        }

        return result;
    }


    @When("CxFlow triggers SAST scan")
    public void cxFlowTriggerScan() {
        FlowController.CxScanRequest cxScanRequest = new FlowController.CxScanRequest();
        cxScanRequest.setProduct(ScanRequest.Product.CX.toString());
        cxScanRequest.setProject(MEANINGLESS_STRING);
        cxScanRequest.setGitUrl(MEANINGLESS_STRING);

        String branch = ScanUtils.empty(branchName) ? MEANINGLESS_STRING : branchName;
        cxScanRequest.setBranch(branch);
        flowProperties.setToken(WEBHOOK_TOKEN);

        flowController.initiateScan(cxScanRequest, WEBHOOK_TOKEN);
    }

    @Then("CxFlow scan comment is equal to {string}")
    public void compareComments(String expectedComment){
        assertEquals(expectedComment, commentMessageFromRequest,
                "fail comparing expected comment to actual comment in scan request");
    }
}
