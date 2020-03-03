package com.checkmarx.flow.cucumber.component.parse;

import com.checkmarx.flow.cucumber.component.parse.matchscenario.ComparisonResult;
import com.checkmarx.flow.cucumber.component.parse.matchscenario.CxFlowReportComparer;
import com.checkmarx.utils.TestsParseUtils;
import com.fasterxml.jackson.databind.JsonNode;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Various checks of generated CxFlow reports.
 */
public class VerifyingCxFlowReportSteps {
    private final TestContext testContext;

    public VerifyingCxFlowReportSteps(TestContext context) {
        testContext = context;
    }

    @Then("the generated CxFlow report contains no issues and the summary is empty")
    public void verifyReportContainsNoIssuesAndEmptySummary() throws IOException {
        CxFlowReport report = CxFlowReport.parse(testContext);
        JsonNode issues = report.getIssues();
        assertEquals("issues array must be empty", 0, issues.size());

        verifySummaryFieldCount(report.getSummary(), 0);
    }

    @Then("CxFlow report is generated with {} issues")
    public void reportIsGeneratedWithNIssues(int expectedIssueCount) throws IOException {
        CxFlowReport report = CxFlowReport.parse(testContext);
        JsonNode issues = report.getIssues();
        assertEquals(expectedIssueCount, issues.size());
    }

    @And("each issue contains {} results and the same number of details")
    public void eachIssueContainsNumberOfDetails(int expectedResultCount) throws IOException {
        verifyCxFlowResultCount(expectedResultCount, true);
    }

    @And("each issue contains {int} result")
    public void eachIssueContainsResult(int expectedResultCount) throws IOException {
        verifyCxFlowResultCount(expectedResultCount, false);
    }

    @Then("CxFlow report summary contains {} severities")
    public void summaryContainsNumberOfSeverities(int expectedSeverityCount) throws IOException {
        CxFlowReport report = CxFlowReport.parse(testContext);
        verifySummaryFieldCount(report.getSummary(), expectedSeverityCount);
    }

    @And("{string} object in each CxFlow result corresponds to the {string} execution path node in input")
    public void objectInTheResultCorrespondsToExecutionPathNode(String jsonFieldName, String firstOrLast)
            throws IOException {
        CxFlowReport report = CxFlowReport.parse(testContext);
        Map<String, JsonNode> cxFlowResultMap = report.getResultMapByFilename();

        SastResultParser parser = new SastResultParser();
        Map<String, Element> sastPathMap = parser.getPathMapByFilename(testContext.getInputFilename());

        for (Map.Entry<String, JsonNode> cxFlowResult : cxFlowResultMap.entrySet()) {
            String filename = cxFlowResult.getKey();
            Element sastPath = sastPathMap.get(filename);
            verifyPathNodeMatch(cxFlowResult.getValue(), sastPath, jsonFieldName, firstOrLast);
        }
    }

    @And("issue severities are: {}")
    public void issueSeveritiesAre(String severities) throws IOException {
        String[] expectedSeverities = TestsParseUtils.parseCSV(severities).toArray(String[]::new);

        CxFlowReport report = CxFlowReport.parse(testContext);
        String[] actualSeverities = report.getSeverities();

        Arrays.sort(expectedSeverities);
        Arrays.sort(actualSeverities);
        assertArrayEquals(expectedSeverities, actualSeverities);
    }

    @Then("the generated CxFlow report matches a corresponding reference report")
    public void generatedCxFlowReportMatchesReferenceReport() throws IOException {
        CxFlowReportComparer comparer = new CxFlowReportComparer(testContext);
        List<String> baseFilenames = testContext.getBaseFilenames();
        ComparisonResult comparisonResult = comparer.compareActualReportsToReferenceReports(baseFilenames);
        if (comparisonResult.containsMismatches()) {
            fail("Mismatches detected: " + comparisonResult.toString());
        }
    }

    @Then("CxFlow report summary contains a {string} field with the value {int}")
    public void cxflowReportSummaryContainsAFieldWithTheValue(String fieldName, int expectedCount) throws IOException {
        CxFlowReport report = CxFlowReport.parse(testContext);
        Integer actualCount = report.getSeverityCounterFromSummary(fieldName);

        assertNotNull(actualCount);
        assertEquals(expectedCount, actualCount.intValue());
    }

    @And("CxFlow report summary contains only these {int} fields")
    public void cxflowReportSummaryContainsOnlyTheseFields(int expectedFieldCount) throws IOException {
        CxFlowReport report = CxFlowReport.parse(testContext);
        int actualFieldCount = report.getSummary().size();
        assertEquals(expectedFieldCount, actualFieldCount);
    }

    /**
     * Compares a &lt;PathNode&gt; XML element with a specific item in "results" array in JSON.
     */
    private void verifyPathNodeMatch(JsonNode cxFlowResult, Element sastPath, String jsonFieldName, String firstOrLast) {
        JsonNode cxFlowJsonResult = cxFlowResult.get(jsonFieldName);

        NodeList sastPathNodes = sastPath.getElementsByTagName("PathNode");
        int pathNodeIndex = firstOrLast.equals("first") ? 0 : sastPathNodes.getLength() - 1;
        Element sastPathXmlElement = (Element) sastPathNodes.item(pathNodeIndex);

        final String[][] XML_TO_JSON_MAPPING = {{"FileName", "file"}, {"Line", "line"}, {"Column", "column"}, {"Name", "object"}};
        for (String[] mapping : XML_TO_JSON_MAPPING) {
            String sastXmlAttribute = mapping[0];
            String cxFlowJsonField = mapping[1];
            verifyXmlElementEqualsJsonField(sastPathXmlElement, sastXmlAttribute, cxFlowJsonResult, cxFlowJsonField);
        }
    }

    private void verifyCxFlowResultCount(int expectedCount, boolean alsoCheckDetailCount) throws IOException {
        CxFlowReport report = CxFlowReport.parse(testContext);
        JsonNode issues = report.getIssues();
        for (JsonNode issue : issues) {
            if (alsoCheckDetailCount) {
                int actualDetailCount = CxFlowReport.getDetailCount(issue);
                assertEquals(expectedCount, actualDetailCount);
            }

            int actualResultCount = CxFlowReport.getResultCount(issue);
            assertEquals(expectedCount, actualResultCount);
        }
    }

    private void verifyXmlElementEqualsJsonField(Element parentXmlElement, String childElementTagName,
                                                 JsonNode node, String jsonFieldName) {
        String childElementText = parentXmlElement.getElementsByTagName(childElementTagName).item(0).getTextContent();
        String jsonFieldValue = node.findValue(jsonFieldName).textValue();

        assertEquals(childElementText, jsonFieldValue);
    }

    private static void verifySummaryFieldCount(JsonNode summary, int expectedCount) {
        assertEquals("Invalid summary field count", expectedCount, summary.size());
    }
}
