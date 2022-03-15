package com.checkmarx.flow.custom;

import com.checkmarx.flow.config.properties.FlowProperties;
import com.checkmarx.flow.config.properties.GitLabProperties;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.service.FilenameFormatter;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.sca.SCAResults;
import com.checkmarx.sdk.dto.sca.report.Finding;
import com.checkmarx.sdk.dto.sca.report.Package;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Service("GitLabDashboard")
@RequiredArgsConstructor
@Slf4j
public class GitLabSecurityDashboard extends ImmutableIssueTracker {
    private static final String ISSUE_FORMAT = "%s @ %s : %d";
    private static final String CHECKMARX = "Checkmarx";
    private final GitLabProperties properties;
    private final FlowProperties flowProperties;
    private final FilenameFormatter filenameFormatter;

    @Override
    public void init(ScanRequest request, ScanResults results){
        log.info("In the GitLab Security Dashboard Init method");
    }

    @Override
    public void complete(ScanRequest request, ScanResults results) throws MachinaException {
        deleteFilesifExist(properties.getSastFilePath(),properties.getScaFilePath());
        if(results.getXIssues() != null) {
            log.info("Finalizing SAST Dashboard output");
            fileInit(request, results, properties.getSastFilePath(), filenameFormatter, log);
            getSastResultsDashboard(request,results);
        }
        if(results.getScaResults() != null) {
            log.info("Finalizing SCA Dashboard output");
            fileInit(request, results, properties.getScaFilePath(), filenameFormatter, log);
            getScaResultsDashboard(request,results);

        }
    }

    private void deleteFilesifExist(String sastFilePath, String scaFilePath) {
        try {
            Files.deleteIfExists(Paths.get(sastFilePath));
            Files.deleteIfExists(Paths.get(scaFilePath));
        }
        catch (IOException e) {
            log.error("Issue deleting existing files {} or {}", sastFilePath,scaFilePath, e);
        }

    }

    private void getScaResultsDashboard(ScanRequest request, ScanResults results) throws MachinaException {
        List<Vulnerability> vulns = new ArrayList<>();
        Scanner scanner = Scanner.builder().id("Checkmarx-SCA").name("Checkmarx-SCA").build();
        List<Finding> findings = results.getScaResults().getFindings();
        List<Package> packages = new ArrayList<>(results.getScaResults()
                .getPackages());
        Map<String, Package> map = new HashMap<>();
        for (Package p : packages) map.put(p.getId(), p);
        for (Finding finding:findings){
            // for each finding, get the associated package list.
            // for each object of the associated list, check the occurences of locations
            // if multiple locations exist, construct multiple objects.
            // if only single location exist, construct single object
            Package indPackage = map.get(finding.getPackageId());
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
        SecurityDashboard report  = SecurityDashboard.builder()
                .vulnerabilities(vulns)
                .build();

        writeJsonOutput(request, report, log);
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
                            .severity(issue.getSeverity())
                            .confidence(issue.getSeverity())
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

    @Data
    @Builder
    public static class SecurityDashboard {
        @JsonProperty("version")
        @Builder.Default
        public Double version = 2.0;
        @JsonProperty("vulnerabilities")
        public List<Vulnerability> vulnerabilities;
        @JsonProperty("remediations")
        public List<String> remediations;
    }

    @Data
    @Builder
    public static class Vulnerability {
        @JsonProperty("id")
        public String id;
        @JsonProperty("cve")
        public String cve;
        @JsonProperty("category")
        @Builder.Default
        public String category = "sast";
        @JsonProperty("name")
        public String name;
        @JsonProperty("message")
        public String message;
        @JsonProperty("description")
        public String description;
        @JsonProperty("location")
        public Location location;
        @JsonProperty("severity")
        public String severity;
        @JsonProperty("confidence")
        public String confidence;
        @JsonProperty("solution")
        public String solution;
        @JsonProperty("scanner")
        public Scanner scanner;
        @JsonProperty("identifiers")
        public List<Identifier> identifiers;
    }

    @Data
    @Builder
    public static class Scanner {
        @JsonProperty("id")
        @Builder.Default
        public String id = CHECKMARX;
        @JsonProperty("name")
        @Builder.Default
        public String name = CHECKMARX;
    }

    @Data
    @Builder
    public static class Location {
        @JsonProperty("file")
        public String file;
        @JsonProperty("start_line")
        public Integer startLine;
        @JsonProperty("end_line")
        public Integer endLine;
        @Builder.Default
        @JsonProperty("class")
        public String clazz = "N/A";
        @Builder.Default
        @JsonProperty("method")
        public String method = "N/A";
        @JsonProperty("dependency")
        public Dependency dependency;
    }


    @Data
    @Builder
    public static class Dependency {
        @JsonProperty("package")
        public Object pkg;
        @JsonProperty("version")
        public String version;
    }

    @Data
    @Builder
    public static class Name {
        @JsonProperty("name")
        public String dependencyname;
    }

    @Data
    @Builder
    public static class Identifier {
        @JsonProperty("type")
        public String type;
        @JsonProperty("name")
        public String name;
        @JsonProperty("value")
        public String value;
        @JsonProperty("url")
        public String url;
    }
}
