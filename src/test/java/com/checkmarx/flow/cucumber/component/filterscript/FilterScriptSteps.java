package com.checkmarx.flow.cucumber.component.filterscript;

import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.cucumber.common.Constants;
import com.checkmarx.flow.cucumber.common.utils.TestUtils;
import com.checkmarx.flow.service.FilterFactory;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.filtering.FilterConfiguration;
import com.checkmarx.sdk.exception.CheckmarxException;
import com.checkmarx.sdk.service.CxClient;
import com.checkmarx.utils.TestsParseUtils;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.SpringBootTest;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = {CxFlowApplication.class})
@RequiredArgsConstructor
public class FilterScriptSteps {
    private String sastReportFilename;
    private final FlowProperties flowProperties;
    private final CxClient cxClient;
    private final FilterFactory filterFactory;
    private Set<String> filenames;
    private Map<Integer, String> findingNumberToFilename = new HashMap<>();

    @Given("SAST report containing {int} findings, each in a different file and with a different vulnerability type")
    public void inputContainingFindings(int ignored) {
        sastReportFilename = "3-findings-filter-script-test.xml";
    }

    @And("finding #{int} has {string} severity, {string} status and {string} state")
    public void findingHasSeverityStatusAndState(int findingNumber, String severity, String status, String state) throws IOException, ParserConfigurationException, SAXException {
        Path relativePath = Paths.get(Constants.SAMPLE_SAST_RESULTS_DIR, sastReportFilename);
        String reportText = TestUtils.getResourceAsString(relativePath.toString());

        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(reportText);
        NodeList findings = doc.getElementsByTagName("Result");
        for (int i = 0; i < findings.getLength(); i++) {
            String filename = findings.item(i).getAttributes().getNamedItem("FileName").getTextContent();
            findingNumberToFilename.put(i + 1, filename);
        }

    }

    @When("CxFlow generates issues from the findings using {string}")
    public void parsingTheInputWith(String scriptText) throws CheckmarxException {
        flowProperties.setFilterScript(scriptText);
        FilterConfiguration filter = filterFactory.getFilter(null, null, null, null, flowProperties);
        ScanResults report = cxClient.getReportContent(333, filter);
        filenames = report.getXIssues()
                .stream()
                .map(ScanResults.XIssue::getFilename)
                .collect(Collectors.toSet());
    }

    @Then("CxFlow report is generated with issues corresponding to these findings: {string}")
    public void cxflowReportIsGenerated(String issueDescription) {
        if (issueDescription.equals("<none>")) {
            assertTrue(filenames.isEmpty());
        } else {
            Set<String> expectedFilenames = TestsParseUtils
                    .parseCsvToList(issueDescription)
                    .stream()
                    .map(findingNumber -> {
                        int key = Integer.parseInt(findingNumber);
                        return findingNumberToFilename.get(key);
                    })
                    .collect(Collectors.toSet());

            Set<String> actualFilenames = filenames;

            assertEquals(actualFilenames, expectedFilenames);
        }
    }
}
