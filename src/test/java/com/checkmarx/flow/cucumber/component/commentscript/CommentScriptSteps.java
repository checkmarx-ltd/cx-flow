package com.checkmarx.flow.cucumber.component.commentscript;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.controller.FlowController;
import com.checkmarx.flow.cucumber.common.utils.TestUtils;
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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
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
    private final static String TOKEN = "Token";
    private final static  String EMPTY_STRING = "";
    private String branchName;
    private String commentMessageFromRequest;
    private CxClient cxClientMock;
    ScanRequestConverter scanRequestConverterMock;
    SastScanner sastScanner;
    CxProperties cxProperties;
    private FlowController flowController;
    private FlowProperties flowProperties;

    public CommentScriptSteps(FlowProperties flowProperties, CxProperties cxProperties, HelperService helperService,
                              JiraProperties jiraProperties, FilterFactory filterFactory, ConfigurationOverrider configOverrider,
                              ResultsService resultService, ProjectNameGenerator projectNameGenerator, BugTrackerEventTrigger btet){


        cxClientMock = mock(CxClient.class);
        scanRequestConverterMock = mock(ScanRequestConverter.class);

        this.flowProperties = flowProperties;

        sastScanner = mock(SastScanner.class, Mockito.withSettings().useConstructor(resultService, cxClientMock,
                helperService, cxProperties, flowProperties, null, null, scanRequestConverterMock, btet, null));

        this.cxProperties = cxProperties;
        FlowService flowService = new FlowService(Collections.singletonList(sastScanner), projectNameGenerator, resultService);

        this.flowController = new FlowController(flowProperties, cxProperties, flowService, helperService, jiraProperties, filterFactory, configOverrider, sastScanner);
    }


    @Before("@DeleteBranchFeature")
    public void setMockers() throws CheckmarxException {
        CreateScanAnswerer createScanAnswerer = new CreateScanAnswerer();
        when(cxClientMock.getScanIdOfExistingScanIfExists(anyInt())).thenReturn(-1);
        when(cxClientMock.createScan(any(), anyString())).thenAnswer(createScanAnswerer);
        when(cxClientMock.getReportContentByScanId(nullable(Integer.class), any())).thenReturn(new ScanResults());

        when(sastScanner.isEnabled()).thenReturn(true);
        when(sastScanner.scan(any())).thenCallRealMethod();

        CxScanParams cxScanParams = new CxScanParams();
        cxScanParams.setProjectId(1);
        when(scanRequestConverterMock.toScanParams(any())).thenReturn(cxScanParams);

        cxProperties.setProjectScript(EMPTY_STRING);
        commentMessageFromRequest = EMPTY_STRING;
        branchName = EMPTY_STRING;
    }

    private class CreateScanAnswerer implements Answer<Object> {
        @Override
        public Object answer(InvocationOnMock invocation) {

            commentMessageFromRequest = invocation.getArgument(1);
            return null;
        }
    }


    @Given("given 'sast-comment' script name is {string}")
    public void setCommentScriptName(String scriptName) {
        if(!scriptName.equals("empty")) {
            String commentScript = getScriptFullPath(scriptName + ".groovy");
            flowProperties.setCommentScript(commentScript);
        }
    }

    @And("Scan request contain feature branch name {string}")
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


    @When("CxFlow Triggering sast scan")
    public void cxFlowTriggerScan() {
        FlowController.CxScanRequest cxScanRequest = new FlowController.CxScanRequest();
        cxScanRequest.setProduct(ScanRequest.Product.CX.toString());
        cxScanRequest.setProject(MEANINGLESS_STRING);
        cxScanRequest.setGitUrl(MEANINGLESS_STRING);

        String branch = ScanUtils.empty(branchName) ? MEANINGLESS_STRING : branchName;
        cxScanRequest.setBranch(branch);
        flowProperties.setToken(TOKEN);

        flowController.initiateScan(cxScanRequest, TOKEN);
    }

    @Then("CxFlow scan comment is equal to {string}")
    public void compareComments(String expectedComment){
        log.info("Comparing expected comment '{} to actual comment '{}", expectedComment, commentMessageFromRequest);

        assertEquals(expectedComment, commentMessageFromRequest);
    }
}
