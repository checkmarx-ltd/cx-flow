package com.checkmarx.flow.custom;

import com.checkmarx.flow.config.SarifProperties;
import com.checkmarx.flow.dto.Issue;
import com.checkmarx.sdk.dto.ScanResults;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.service.FilenameFormatter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
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
import java.util.*;
import java.util.stream.Collectors;

@Service("Sarif")
@RequiredArgsConstructor
@Slf4j
public class SarifIssueTracker extends ImmutableIssueTracker {
    private final SarifProperties properties;
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
        log.info("Finalizing SARIF output");

        // Filter issues with all fals-positives
        List<ScanResults.XIssue> filteredXIssues =
                results.getXIssues()
                        .stream()
                        .filter(x -> !x.isAllFalsePositive())
                        .collect(Collectors.toList());
        //Distinct list of Vulns
        List<ScanResults.XIssue> filteredByVulns =
                results.getXIssues()
                        .stream()
                        .collect(Collectors.toCollection(() ->
                                new TreeSet<>(Comparator.comparing(ScanResults.XIssue::getVulnerability))))
                        .stream()
                        .filter(x -> !x.isAllFalsePositive())
                        .collect(Collectors.toList());
        //Distinct list of files in the result set
        List<ScanResults.XIssue> filteredByFile =
                results.getXIssues()
                        .stream()
                        .collect(Collectors.toCollection(() ->
                                new TreeSet<>(Comparator.comparing(ScanResults.XIssue::getFilename))))
                        .stream()
                        .filter(x -> !x.isAllFalsePositive())
                        .collect(Collectors.toList());
        // Build the collection of the rules objects (Vulnerabilities)
        List<Rule> rules = filteredByVulns.stream().map(xss -> Rule.builder()
                .id(xss.getVulnerability())
                .shortDescription(ShortDescription.builder().text(xss.getVulnerability()).build())
                .helpUri((String) xss.getAdditionalDetails().get("recommendedFix"))
                .properties(Properties.builder()
                        .category((String) xss.getAdditionalDetails().get("categories"))
                        .build())
                .build()).collect(Collectors.toList());
        // Build collection of the atrifacts
        List<SarifIssueTracker.Artifact> artifacts = Lists.newArrayList();
        //Unique files (artifacts) in the result set
        filteredByFile.forEach(
                issue -> {
                    artifacts.add(
                            Artifact.builder().location(
                                    ArtifactLocation.builder()
                                            .uri("file:///".concat(issue.getFilename()))
                                            .build())
                                    .build()
                    );
                }
        );
        //All issues to create the results/locations that are not all false positive
        List<Result> resultList = Lists.newArrayList();
        filteredXIssues.forEach(
                issue -> {
                    List<Location> locations = Lists.newArrayList();
                    Integer column = null;
                    List<Map<String, Map<String, String>>> vulnPathList = (List<Map<String, Map<String, String>>>) issue.getAdditionalDetails().get("results");
                    if(vulnPathList != null && !vulnPathList.isEmpty()) {
                        Map<String, String> sourceMap = vulnPathList.get(0).get("source");
                        if(sourceMap != null){
                            column = Integer.parseInt(sourceMap.get("column"));
                        }
                    }
                    Integer finalColumn = column;
                    issue.getDetails().forEach((k, v) -> {
                                locations.add(Location.builder()
                                        .physicalLocation(PhysicalLocation.builder()
                                                .artifactLocation(ArtifactLocation.builder()
                                                        .uri("file:///".concat(issue.getFilename()))
                                                        //.index(0) Artifact
                                                        .build())
                                                .region(Region.builder()
                                                        .startLine(k)
                                                        .startColumn(finalColumn)
                                                        .build())
                                                .build())
                                        .build());
                    });
                    // Build collection of the results -> locations
                    resultList.add(
                            Result.builder()
                            .level(issue.getSeverity())
                            .locations(locations)
                            .message(Message.builder()
                                    .text(issue.getDescription())
                                    .build())
                            .ruleId(issue.getVulnerability())
                            //.ruleIndex(0) Rule
                            .build()
                    );

                }
        );

        // Build the run object
        SarifVulnerability run =
                SarifVulnerability
                        .builder()
                        .tool(Tool.builder()
                                .driver(Driver
                                            .builder()
                                            .name(properties.getScannerName())
                                            //.informationUri(properties.getDriverInfoUrl())
                                            .rules(rules)
                                            .build())
                                .build())
                        .artifacts(artifacts)
                        .results(resultList)
                        .build();

        SarifReport report = SarifReport.builder()
                .runs(Collections.singletonList(run))
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
    public static class SarifReport {
        @Builder.Default
        @JsonProperty("version")
        String version = "2.1.0";
        @JsonProperty("runs")
        List<SarifVulnerability> runs;
    }

    @Data
    @Builder
    public static class SarifVulnerability {
        @JsonProperty("tool")
        public Tool tool;
        @JsonProperty("artifacts")
        public List<Artifact> artifacts;
        @JsonProperty("results")
        public List<Result> results;
    }

    @Data
    @Builder
    public static class Artifact {
        @JsonProperty("location")
        public ArtifactLocation location;
    }

    @Data
    @Builder
    public static class Tool {
        @JsonProperty("driver")
        public SarifIssueTracker.Driver driver;
    }

    @Data
    @Builder
    public static class Driver {
        @JsonProperty("name")
        public String name;
        @JsonProperty("informationUri")
        public String informationUri;
        @JsonProperty("rules")
        public List<SarifIssueTracker.Rule> rules;
    }

    @Data
    @Builder
    public static class ShortDescription {
        @JsonProperty("text")
        public String text;
    }

    @Data
    @Builder
    public static class Properties {
        @JsonProperty("category")
        public String category;
        @JsonProperty("cwe")
        public String cwe;
    }

    @Data
    @Builder
    public static class Rule {
        @JsonProperty("id")
        public String id;
        @JsonProperty("shortDescription")
        public ShortDescription shortDescription;
        @JsonProperty("helpUri")
        public String helpUri;
        @JsonProperty("properties")
        public Properties properties;
    }

    @Data
    @Builder
    public static class Message {
        @JsonProperty("text")
        public String text;
    }

    @Data
    @Builder
    public static class Result {
        @JsonProperty("level")
        public String level;
        @JsonProperty("message")
        public Message message;
        @JsonProperty("locations")
        private List<Location> locations;
        @JsonProperty("ruleId")
        private String ruleId;
        @JsonProperty("ruleIndex")
        private Integer ruleIndex;
    }

    @Data
    @Builder
    public static class Location {
        @JsonProperty("physicalLocation")
        public PhysicalLocation physicalLocation;
    }

    @Data
    @Builder
    public static class ArtifactLocation {
        @JsonProperty("uri")
        public String uri;
        @JsonProperty("index")
        public int index;
    }

    @Data
    @Builder
    public static class Region {
        @JsonProperty("startLine")
        public Integer startLine;
        @JsonProperty("startColumn")
        public Integer startColumn;
    }

    @Data
    @Builder
    public static class PhysicalLocation {
        @JsonProperty("artifactLocation")
        public ArtifactLocation artifactLocation;
        @JsonProperty("region")
        public Region region;
    }
}
