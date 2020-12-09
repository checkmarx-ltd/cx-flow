package com.checkmarx.flow.cucumber.integration.azure.publishing.githubflow;

import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.cx.CxScanSummary;

import java.util.*;

/**
 * Instead of using this class, it's possible (in theory) to deserialize ScanResults from a JSON file.
 * However, an attempt to do this results in an error:
 *      Cannot construct instance of `com.checkmarx.sdk.dto.ScanResults$XIssue`
 */
public class ScanResultsBuilder {
    
    public ScanResults getScanResultsWithSingleFinding(String projectName) {
        return ScanResults.builder()
                .projectId("12")
                .team("myteam")
                .project(projectName)
                .link("http://example.com")
                .files("3")
                .loc("213")
                .scanType("Full")
                .additionalDetails(getAdditionalScanDetails())
                .xIssues(getXIssues())
                .scanSummary(getScanSummary())
                .build();
    }

    private static CxScanSummary getScanSummary() {
        CxScanSummary result = new CxScanSummary();
        result.setHighSeverity(1);
        result.setMediumSeverity(0);
        result.setLowSeverity(0);
        result.setInfoSeverity(0);
        result.setStatisticsCalculationDate("2020-01-19");
        return result;
    }

    public static List<ScanResults.XIssue> getXIssues() {
        ScanResults.XIssue xIssue = ScanResults.XIssue.builder()
                .vulnerability("Reflected_XSS_All_Clients")
                .similarityId("1000026")
                .cwe("79")
                .description("")
                .language("Java")
                .severity("High")
                .link("http://example.local/CxWebClient/ViewerMain.aspx?scanid=1000026&projectid=6&pathid=2")
                .file("DOS_Login.java")
                .description("Description: Reflected_XSS_All_Clients")
                .details(getIssueDetails())
                .additionalDetails(new HashMap<>())
                .build();
        return Collections.singletonList(xIssue);

    }



    public static List<ScanResults.XIssue> get2XIssues() {

        List<ScanResults.XIssue> collection = new ArrayList<>();
        ScanResults.XIssue xIssue = ScanResults.XIssue.builder()
                .vulnerability("SQL_Injection")
                .similarityId("1000027")
                .cwe("89")
                .description("")
                .language("Java")
                .severity("High")
                .link("http://example.local/CxWebClient/ViewerMain.aspx?scanid=1000026&projectid=6&pathid=2")
                .file("DOS_Login.java")
                .description("Description: SQL_Injection")
                .details(getIssueDetails())
                .additionalDetails(new HashMap<>())
                .build();
        collection.add(xIssue);
        
        collection.addAll(getXIssues());
        
        return collection;

    }
    
    private static Map<String, Object> getAdditionalScanDetails() {
        Map<String, Integer> flowSummary = Collections.singletonMap("High", 1);

        Map<String, Object> additionalDetails = new HashMap<>();
        additionalDetails.put("flow-summary", flowSummary);
        additionalDetails.put("scanId", 849104);
        additionalDetails.put("scanStartDate", "Sunday, January 19, 2020 2:40:11 AM");
        return additionalDetails;

    }

    private static Map<Integer, ScanResults.IssueDetails> getIssueDetails() {
        ScanResults.IssueDetails issueDetails = new ScanResults.IssueDetails();
        issueDetails.falsePositive(false);
        issueDetails.codeSnippet("username = s.getParser().getRawParameter(USERNAME);");
        return Collections.singletonMap(89, issueDetails);
    }
}
