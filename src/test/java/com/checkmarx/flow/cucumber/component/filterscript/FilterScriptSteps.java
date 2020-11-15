package com.checkmarx.flow.cucumber.component.filterscript;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.cucumber.common.Constants;
import com.checkmarx.flow.cucumber.common.utils.TestUtils;
import com.checkmarx.flow.service.FilterFactory;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.cx.CxScanSummary;
import com.checkmarx.sdk.dto.filtering.FilterConfiguration;
import com.checkmarx.sdk.exception.CheckmarxException;
import com.checkmarx.sdk.exception.CheckmarxRuntimeException;
import com.checkmarx.sdk.service.*;
import com.checkmarx.utils.TestsParseUtils;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {CxFlowApplication.class})
@RequiredArgsConstructor
@CucumberContextConfiguration
@Slf4j
public class FilterScriptSteps {
    private final static String SAST_REPORT_FILENAME = "3-findings-filter-script-test.xml";

    private final FlowProperties flowProperties;
    private final FilterFactory filterFactory;
    private Map<String, Integer> findingFilenameToNumber;
    private final CxProperties cxProperties;
    private final CxLegacyService cxLegacyService;
    private final FilterInputFactory filterInputFactory;
    private final FilterValidator filterValidator;

    private Set<Integer> findingNumbersAfterFiltering;
    private String sastReportPath;
    private Exception reportGenerationException;


    @Given("SAST report containing 3 findings, each in a different file and with a different vulnerability type")
    public void inputContainingFindings() {
        sastReportPath = Paths.get(Constants.SAMPLE_SAST_RESULTS_DIR, SAST_REPORT_FILENAME).toString();
    }

    @And("finding #{int} has {string} severity, {string} status and {string} state")
    public void findingHasSeverityStatusAndState(int findingNumber, String severity, String status, String state)
            throws IOException, ParserConfigurationException, SAXException {

        NodeList findings = getXmlFindingsFromResource(sastReportPath);
        findingFilenameToNumber = mapFilenamesToNumbers(findings);
    }

    private static NodeList getXmlFindingsFromResource(String path) throws IOException, SAXException, ParserConfigurationException {
        File sastReportFile = TestUtils.getFileFromResource(path);
        Document parsedReport = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(sastReportFile);
        return parsedReport.getElementsByTagName("Result");
    }

    @And("no simple filters are defined")
    public void noSimpleFilters() {
        flowProperties.setFilterSeverity(null);
        flowProperties.setFilterCwe(null);
        flowProperties.setFilterCategory(null);
        flowProperties.setFilterStatus(null);
        flowProperties.setFilterState(null);
    }

    @And("filter script is {string}")
    public void filterScriptId(String scriptText) {
        if (scriptText.equals("<not specified>")) {
            scriptText = null;
        }
        log.info("Using filter script: '{}'", scriptText);
        flowProperties.setFilterScript(scriptText);
    }

    @When("CxFlow generates issues from findings")
    public void cxFlowGeneratesIssues() throws CheckmarxException, IOException {
        RestTemplate restTemplateMock = getRestTemplateMock();
        CxAuthService authClientMock = getAuthClientMock();
        CxClient cxClientSpy = getCxClientSpy(restTemplateMock, authClientMock);
        generateIssues(cxClientSpy);
    }

    @Then("CxFlow report is generated with issues corresponding to these findings: {string}")
    public void cxflowReportIsGenerated(String issueDescription) {
        if (reportGenerationException != null) {
            fail("Unexpected exception while getting CxFlow report.", reportGenerationException);
        }

        if (issueDescription.equals("<none>")) {
            assertTrue(findingNumbersAfterFiltering.isEmpty());
        } else {
            Set<Integer> expectedFindingNumbers = TestsParseUtils.parseCsvToList(issueDescription)
                    .stream()
                    .map(Integer::parseInt)
                    .collect(Collectors.toSet());
            assertEquals(expectedFindingNumbers, findingNumbersAfterFiltering);
        }
    }

    @Given("status filter is set to {string}")
    public void statusFilterIsSetTo(String status) {
        flowProperties.setFilterStatus(Collections.singletonList(status));
    }

    @Then("CheckmarxRuntimeException is thrown")
    public void checkmarxruntimeexceptionIsThrown() {
        assertNotNull(reportGenerationException, "Expected an exception, but didn't get any.");

        assertTrue(reportGenerationException instanceof CheckmarxRuntimeException,
                () -> String.format("Expected CheckmarxRuntimeException to be thrown, but the actual exception was %s.",
                        reportGenerationException.getClass().getName()));
    }

    @And("the exception message contains the text: {string}")
    public void theExceptionMessageContainsTheText(String text) {
        boolean messageContainsExpectedText = reportGenerationException.getMessage()
                .toLowerCase(Locale.ROOT)
                .contains(text.toLowerCase(Locale.ROOT));

        assertTrue(messageContainsExpectedText,
                String.format("Expected the exception message to contain '%s', but the actual message was '%s'.",
                        text,
                        reportGenerationException.getMessage()));
    }

    private FilterConfiguration getFilterConfiguration() {
        return filterFactory.getFilter(null, flowProperties);
    }

    private void generateIssues(CxClient cxClientSpy) {
        // Avoid additional API calls that we don't care about.
        cxProperties.setOffline(true);

        try {
            FilterConfiguration filter = getFilterConfiguration();
            ScanResults report = cxClientSpy.getReportContent(333333, filter);

            findingNumbersAfterFiltering = report.getXIssues()
                    .stream()
                    .map(xIssue -> findingFilenameToNumber.get(xIssue.getFilename()))
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            reportGenerationException = e;
        }
    }

    /**
     * Remember which filename corresponds to which finding number, in no particular order.
     * This will later allow to map resulting XIssues back to finding numbers, since the filename is preserved during
     * finding -> xIssue conversion.
     */
    private static Map<String, Integer> mapFilenamesToNumbers(NodeList findings) {
        Map<String, Integer> result = new HashMap<>();
        for (int i = 0; i < findings.getLength(); i++) {
            String filename = findings.item(i)
                    .getAttributes()
                    .getNamedItem("FileName")
                    .getTextContent();
            result.put(filename, i + 1);
        }
        return result;
    }

    private CxClient getCxClientSpy(RestTemplate restTemplateMock, CxAuthService authClientMock) throws CheckmarxException {
        CxClient cxClient = new CxService(authClientMock,
                cxProperties,
                cxLegacyService,
                restTemplateMock,
                null,
                filterInputFactory,
                filterValidator);

        CxClient cxClientSpy = spy(cxClient);
        doReturn(new CxScanSummary()).when(cxClientSpy).getScanSummaryByScanId(any());
        return cxClientSpy;
    }

    private CxAuthService getAuthClientMock() {
        CxAuthService authClientMock = mock(CxAuthService.class);
        doReturn(new HttpHeaders()).when(authClientMock).createAuthHeaders();
        doReturn(null).when(authClientMock).getLegacySession();
        return authClientMock;
    }

    private RestTemplate getRestTemplateMock() throws IOException {
        RestTemplate restTemplateMock = mock(RestTemplate.class);
        String sastReportText = TestUtils.getResourceAsString(sastReportPath);
        ResponseEntity<String> sastReportEntity = new ResponseEntity<>(sastReportText, HttpStatus.OK);
        doReturn(sastReportEntity).when(restTemplateMock).exchange(any(), any(), any(), (Class<Object>) any(), any(Integer.class));
        return restTemplateMock;
    }
}
