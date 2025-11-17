package com.checkmarx.flow.custom.gitlabDashboard;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.gitlabdashboardv2.Scanner;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.sca.SCAResults;
import com.checkmarx.sdk.dto.sca.report.Finding;
import com.checkmarx.sdk.dto.sca.report.Package;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import com.checkmarx.flow.dto.gitlabdashboardv2.*;

import java.util.*;

@RequiredArgsConstructor
@Slf4j
public class GitLabDashboardV2 implements GitLabDashboardStrategy {
    private static final String ISSUE_FORMAT = "%s @ %s : %d";
    private static final String CHECKMARX = "Checkmarx";
    private final FlowProperties flowProperties;


    @Override
    public void generateSastDashboard(ScanRequest request, ScanResults results) throws MachinaException {
        getSastResultsDashboard(request, results);
    }

    @Override
    public void generateScaDashboard(ScanRequest request, ScanResults results) throws MachinaException {
        getScaResultsDashboard(request, results);
    }

    private void getSastResultsDashboard(ScanRequest request, ScanResults results) throws MachinaException {
        List<Vulnerability> vulns = new ArrayList<>();
        Scanner scanner = Scanner.builder().id("Checkmarx-SAST").name("Checkmarx-SAST").build();
        for(ScanResults.XIssue issue : results.getXIssues()) {
            if(issue.getDetails() != null) {
                issue.getDetails().forEach((k, v) -> {
                    Vulnerability vuln = Vulnerability.builder()
                            .category("sast")
                            .id(issue.getVulnerability().concat(":").concat(issue.getFilename()).concat(":").concat(k.toString()))
                            .cve(issue.getVulnerability().concat(":").concat(issue.getFilename()).concat(":").concat(k.toString()))
                            .name(issue.getVulnerability())
                            .message(String.format(ISSUE_FORMAT, issue.getVulnerability(), issue.getFilename(), k))
                            .description(issue.getVulnerability())
                            .severity(StringUtils.capitalize(issue.getSeverity().toLowerCase()))
                            .confidence(StringUtils.capitalize(issue.getSeverity().toLowerCase()))
                            .solution(issue.getLink())
                            .scanner(scanner)
                            .identifiers(getIdentifiers(issue))
                            .location(
                                    Location.builder()
                                            .file(issue.getFilename())
                                            .startLine(k)
                                            .endLine(k)
                                            .build()
                            )
                            .build();
                    vulns.add(vuln);
                });
            }
        }
        SecurityDashboard report  = SecurityDashboard.builder()
                .vulnerabilities(vulns)
                .build();

        writeJsonOutput(request, report, log);

    }

    private void getScaResultsDashboard(ScanRequest request, ScanResults results) throws MachinaException {
        List<Vulnerability> vulns = new ArrayList<>();
        Scanner scanner = Scanner.builder().id("Checkmarx-SCA").name("Checkmarx-SCA").build();
        List<Finding> findings = results.getScaResults().getFindings();
        List<com.checkmarx.sdk.dto.sca.report.Package> packages = new ArrayList<>(results.getScaResults()
                .getPackages());
        Map<String, com.checkmarx.sdk.dto.sca.report.Package> map = new HashMap<>();
        for (com.checkmarx.sdk.dto.sca.report.Package p : packages) map.put(p.getId(), p);
        for (Finding finding:findings){
            // for each finding, get the associated package list.
            // for each object of the associated list, check the occurrences of locations
            // if multiple locations exist, construct multiple objects.
            // if only single location exist, construct single object
            Package indPackage = map.get(finding.getPackageId());
            if(indPackage!=null){
                for(String loc : indPackage.getLocations()) {
                    vulns.add(Vulnerability.builder()
                            .category("dependency_scanning")
                            .id(UUID.nameUUIDFromBytes(finding.getPackageId().concat("@").concat(loc).concat(":").concat(finding.getCveName()).getBytes()).toString())
                            .name(finding.getPackageId().concat("@").concat(loc).concat(":").concat(finding.getCveName()))
                            .message(finding.getPackageId().concat("@").concat(loc).concat(":").concat(finding.getCveName()))
                            .description(finding.getDescription())
                            .severity(String.valueOf(finding.getSeverity()))
                            .confidence(String.valueOf(finding.getSeverity()))
                            .solution(finding.getFixResolutionText())
                            .location(Location.builder().file(loc).dependency(Dependency.builder().pkg(Name.builder().dependencyname(finding.getPackageId()).build()).version(finding.getPackageId().split("-")[finding.getPackageId().split("-").length-1]).build()).build())
                            .identifiers(getScaIdentifiers(results.getScaResults(),finding))
                            .scanner(scanner)
                            .build());
                }
            }

        }
        SecurityDashboard report  = SecurityDashboard.builder()
                .vulnerabilities(vulns)
                .build();

        writeJsonOutput(request, report, log);
    }

    private List<Identifier> getIdentifiers(ScanResults.XIssue issue){
        List<Identifier> identifiers = new ArrayList<>();
        identifiers.add(
                Identifier.builder()
                        .type("checkmarx_finding")
                        .name("Checkmarx-".concat(issue.getVulnerability()))
                        .value(issue.getVulnerability())
                        .url(issue.getLink())
                        .build()
        );
        if (!ScanUtils.empty(flowProperties.getMitreUrl())) {
            identifiers.add(
                    Identifier.builder()
                            .type("cwe")
                            .name("CWE-".concat(issue.getCwe()))
                            .value(issue.getCwe())
                            .url(String.format(flowProperties.getMitreUrl(), issue.getCwe()))
                            .build()
            );
        }else {
            log.info("mitre-url property is empty");
        }

        return identifiers;
    }

    private List<Identifier> getScaIdentifiers(SCAResults results, Finding finding) {
        List<Identifier> identifiers = new ArrayList<>();
        identifiers.add(
                Identifier.builder()
                        .type("checkmarx_finding")
                        .name(CHECKMARX.concat("-").concat(finding.getPackageId()))
                        .value(CHECKMARX.concat("-").concat(finding.getPackageId()))
                        .url(results.getWebReportLink())
                        .build()
        );
        if (!finding.getReferences().isEmpty()) {
            for(String reference:finding.getReferences()){
                identifiers.add(
                        Identifier.builder()
                                .type("cve")
                                .name(finding.getCveName() != null ? finding.getCveName().concat("(").concat(reference).concat(")") : reference)
                                .value(finding.getCveName() != null ? finding.getCveName().concat("(").concat(reference).concat(")") : reference)
                                .url(reference)
                                .build()
                );
            }
        }

        return identifiers;
    }
}
