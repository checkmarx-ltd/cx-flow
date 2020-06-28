package com.checkmarx.flow.custom;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitLabProperties;
import com.checkmarx.flow.dto.Issue;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.service.FilenameFormatter;
import com.checkmarx.sdk.dto.ScanResults;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service("GitLabDashboard")
@RequiredArgsConstructor
@Slf4j
public class GitLabSecurityDashboard extends ImmutableIssueTracker {
    private static final String ISSUE_FORMAT = "%s @ %s : %d";

    private final GitLabProperties properties;
    private final FlowProperties flowProperties;
    private final FilenameFormatter filenameFormatter;

    @Override
    public void init(ScanRequest request, ScanResults results) throws MachinaException {
        Path fullPath = Paths.get(properties.getFilePath());
        Path parentDir = fullPath.getParent();
        Path filename = fullPath.getFileName();

        String formattedPath = filenameFormatter.formatPath(request, filename.toString(), parentDir.toString());
        request.setFilename(formattedPath);
        log.info("Creating file {}", formattedPath);
        try {
            Files.deleteIfExists(Paths.get(formattedPath));
            Files.createFile(Paths.get(formattedPath));
        } catch (IOException e) {
            log.error("Issue deleting existing file or writing initial {}", filename, e);
        }
    }

    @Override
    public void complete(ScanRequest request, ScanResults results) throws MachinaException {
        log.info("Finalizing Dashboard output");
        List<Vulnerability> vulns = new ArrayList<>();
        Scanner scanner = Scanner.builder().build();
        results.getXIssues().forEach( issue ->
            issue.getDetails().forEach( (k,v) -> {
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
            })
        );
        SecurityDashboard report  = SecurityDashboard.builder()
                .vulnerabilities(vulns)
                .build();
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            mapper.writeValue(new File(request.getFilename()), report);
        } catch (IOException e) {
            log.error("Issue occurred while writing file {}", request.getFilename(), e);
            throw new MachinaException();
        }
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
        identifiers.add(
                Identifier.builder()
                        .type("cwe")
                        .name("CWE-".concat(issue.getCwe()))
                        .value(issue.getCwe())
                        .url(String.format(flowProperties.getMitreUrl(), issue.getCwe()))
                        .build()
        );
        return identifiers;
    }

    @Override
    public String getFalsePositiveLabel() throws MachinaException {
        return null;
    }

    @Override
    public List<Issue> getIssues(ScanRequest request) throws MachinaException {
        return new ArrayList<>();
    }

    @Override
    public Issue createIssue(ScanResults.XIssue issue, ScanRequest request) throws MachinaException {
        return null;
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
        public String id = "Checkmarx";
        @JsonProperty("name")
        @Builder.Default
        public String name = "Checkmarx";
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