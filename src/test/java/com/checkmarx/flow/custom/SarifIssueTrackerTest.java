package com.checkmarx.flow.custom;

import com.checkmarx.flow.config.SarifProperties;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.service.FilenameFormatter;
import com.checkmarx.flow.service.SanitizingFilenameFormatter;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.ScanResults.XIssue;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.Test;
import java.util.List;
import java.util.Map;

public class SarifIssueTrackerTest {

    @Test
    public void initWithNullPropertiesNullParameters() {
        SarifProperties properties = new SarifProperties();
        properties.setFilePath("./cx.sarif");
        FilenameFormatter filenameFormatter = new SanitizingFilenameFormatter() ;
        SarifIssueTracker sarifIssueTracker = new SarifIssueTracker(properties, filenameFormatter);
        try {
            sarifIssueTracker.init(null, null);
            assert false;
        } catch (MachinaException e) {
            assert true;
        }
    }

    @Test
    public void completeWithParameters() {
        SarifIssueTracker issueTracker = getInstance();
        try {
            ScanRequest request = getRequest();
            ScanResults results = getResults();
            request.setFilename("./sarif-result.json");
            issueTracker.complete(request, results);
            assert true;
        } catch (MachinaException e) {
            assert false;
        }
    }

    private ScanResults getResults() {
        Map<String, String> sourceMap = Maps.newHashMap();
        sourceMap.put("file", "src/main/webapp/vulnerability/DisplayMessage.jsp");
        sourceMap.put("column", "123");
        sourceMap.put("line", "3");

        Map<String, String> sinkMap = Maps.newHashMap();
        sinkMap.put("file", "src/main/webapp/vulnerability/DisplayMessage.jsp");

        Map<String, Object> addDetResMap = Maps.newHashMap();
        addDetResMap.put("sink", sinkMap);
        addDetResMap.put("source", sourceMap);

        Map<String, Object> addDetails = Maps.newHashMap();
        addDetails.put("results", addDetResMap);
        addDetails.put("recommendedFix", "https://ast.dev.checkmarx-ts.com/CxWebClient/ScanQueryDescription.aspx?");
        addDetails.put("categories", "PCI DSS v3.2;PCI DSS (3.2) - 6.5.7 - Cross-site scripting (XSS),OWASP Top 10 2013;A3-Cross-Sit");
        Map<Integer, ScanResults.IssueDetails> issueDetails = Maps.newHashMap();
        issueDetails.put(22, new ScanResults.IssueDetails());
        XIssue i1 =
                XIssue.builder()
                        .vulnerability("Stored_XSS")
                        .additionalDetails(addDetails)
                        .details(issueDetails)
                        .severity("High")
                        .cwe("79")
                        .description("Method rs=stmt.executeQuery at line 22 of src\\\\main\\\\webapp")
                        .link("https://ast.dev.checkmarx-ts.com/CxWebClient/ViewerMain.aspx?scanid=1000194&projec")
                        .build();
        XIssue i2 =
                XIssue.builder()
                    .vulnerability("SQL_Injection")
                    .additionalDetails(addDetails)
                    .details(issueDetails)
                    .severity("Medium")
                    .cwe("89")
                    .description("Method rs=stmt.executeQuery at line 22 of src\\\\main\\\\webapp")
                    .link("https://ast.dev.checkmarx-ts.com/CxWebClient/ViewerMain.aspx?scanid=1000194&projec")
                    .build();
        List<XIssue> issues = Lists.newArrayList();
        issues.add(i1);
        issues.add(i2);
        ScanResults results = new ScanResults();
        results.setXIssues(issues);
        return results;
    }

    private ScanRequest getRequest() {
        return ScanRequest.builder()
                .application("test_app")
                .repoName("test_repo")
                .namespace("checkmarx")
                .product(ScanRequest.Product.CX)
                .branch("develop")
                .team("\\CxServer\\SP\\Checkmarx").build();
    }

    private SarifIssueTracker getInstance() {
        SarifProperties sarifProperties = new SarifProperties();
        FilenameFormatter filenameFormatter = new SanitizingFilenameFormatter();
        return new SarifIssueTracker(sarifProperties, filenameFormatter);
    }
}
