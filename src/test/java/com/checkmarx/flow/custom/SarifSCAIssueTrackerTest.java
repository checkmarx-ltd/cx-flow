package com.checkmarx.flow.custom;

import com.checkmarx.flow.config.SarifProperties;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.service.FilenameFormatter;
import com.checkmarx.flow.service.SanitizingFilenameFormatter;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.sca.SCAResults;
import com.checkmarx.sdk.dto.sca.report.Finding;
import com.checkmarx.sdk.dto.sca.report.Package;
import com.checkmarx.sdk.dto.scansummary.Severity;
import com.google.common.collect.Lists;
import org.junit.Test;
import java.util.List;


public class SarifSCAIssueTrackerTest {
    
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
            request.setFilename("./sarif-sca-result.json");
            issueTracker.complete(request, results);
            assert true;
        } catch (MachinaException e) {
            assert false;
        }
    }

    private ScanResults getResults() {
        Finding f1 = new Finding();
        f1.setId("Npm-brace-expansion-1.1.6");
        f1.setCveName("CVE-2020-28500");
        f1.setScore(5.6);
        f1.setSeverity(Severity.HIGH);
        f1.setDescription("Npm-brace-expansion-1.1.6");
        f1.setPackageId("Npm-brace-expansion-1.1.6");

        Finding f2 = new Finding();
        f2.setId("Npm-brace-expansion-1.1.6");
        f2.setCveName("CVE-2017-18077");
        f2.setScore(6.5);
        f2.setSeverity(Severity.HIGH);
        f2.setDescription("Npm-brace-expansion-1.1.6");
        f2.setPackageId("Npm-brace-expansion-1.1.6");

        List<Finding> findings = Lists.newArrayList();
        findings.add(f1);
        findings.add(f2);

        Package p1 = new Package();
        p1.setId("Npm-brace-expansion-1.1.6");
        p1.setName("Npm-brace-expansion-1.1.6");
        p1.setVersion("1.1.6");
        
        List<Package> packages = Lists.newArrayList();
        packages.add(p1);

        SCAResults s1 = 
            SCAResults.builder()
                    .scanId("scanId1")
                    .webReportLink("http://foo.bar")
                    .findings(findings)
                    .packages(packages)
                    .build();
        
        ScanResults results = new ScanResults();

        results.setScaResults(s1);
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
