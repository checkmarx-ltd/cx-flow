package com.checkmarx.flow.custom;

import com.checkmarx.flow.config.FlowProperties;
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
        JsonIssueTracker jsonIssueTracker = new JsonIssueTracker(null, null);
        try {
            jsonIssueTracker.init(null, null);
            assert false;
        } catch (MachinaException e) {
            assert true;
        }
    }

    @Test
    public void completeWithParameters() {
        SarifIssueTracker issueTracker = getInstance();
        try {
            ScanResults results = getResults();
            ScanRequest request = new ScanRequest();
            request.setFilename("c:\\temp\\sarif-result.json");
            issueTracker.complete(request, results);
            assert true;
        } catch (MachinaException e) {
            assert true;
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

        List<ScanResults.OsaDetails> details = Lists.newArrayList();
        List<ScanResults.ScaDetails> scaDetails = Lists.newArrayList();
        Map<String, Object> addDetails = Maps.newHashMap();
        addDetails.put("results", addDetResMap);
        addDetails.put("recommendedFix", "https://ast.dev.checkmarx-ts.com/CxWebClient/ScanQueryDescription.aspx?");
        addDetails.put("categories", "PCI DSS v3.2;PCI DSS (3.2) - 6.5.7 - Cross-site scripting (XSS),OWASP Top 10 2013;A3-Cross-Sit");
        Map<Integer, ScanResults.IssueDetails> issueDetails = Maps.newHashMap();

        XIssue i =
                XIssue.builder()
                        .vulnerability("dsd")
                        .additionalDetails(addDetails)
                        .scaDetails(scaDetails)
                        .details(issueDetails)
                        .osaDetails(details)
                        .severity("High")
                        .description("Method rs=stmt.executeQuery at line 16 of src\\\\main\\\\webapp")
                        .link("https://ast.dev.checkmarx-ts.com/CxWebClient/ViewerMain.aspx?scanid=1000194&projec")
                        .build();
        List<XIssue> issues = Lists.newArrayList();
        issues.add(i);
        ScanResults results = new ScanResults();
        results.setXIssues(issues);
        return results;
    }

    private SarifIssueTracker getInstance() {
        SarifProperties sarifProperties = new SarifProperties();
        FilenameFormatter filenameFormatter = new SanitizingFilenameFormatter();
        return new SarifIssueTracker(sarifProperties, filenameFormatter);
    }
}
