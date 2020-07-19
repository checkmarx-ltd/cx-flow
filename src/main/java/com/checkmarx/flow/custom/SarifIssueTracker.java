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
    private static final String ISSUE_FORMAT = "%s @ %s : %d";
    private final SarifProperties properties;
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
        log.info("Finalizing SARIF output");

        // removed duplicates (by link) and false positive
        List<ScanResults.XIssue> filteredXIssues =
                results.getXIssues()
                        .stream()
                        .collect(Collectors.toCollection(() ->
                                new TreeSet<>(Comparator.comparing(ScanResults.XIssue::getLink))))
                        .stream()
                        .filter(x -> x.getFalsePositiveCount() == 0)
                        .collect(Collectors.toList());

        // Build collection of the results -> locations
        List<Location> resultLocations = Lists.newArrayList();
        // Build collection of the atrifacts
        List<SarifIssueTracker.Artifact> artifacts = Lists.newArrayList();
        filteredXIssues.stream().forEach(xss ->
            ((Map<String, Map<String, Map<String, String>>>)
                    xss.getAdditionalDetails()
                            .get("results"))
                            .entrySet().stream()
                            .filter(v -> v.getKey().equalsIgnoreCase("source"))
                            .forEach(r -> {
                                    artifacts.add(
                                            Artifact.builder().location(
                                                ArtifactLocation.builder()
                                                                .uri("file:///" + r.getValue().get("file"))
                                                                .build())
                                            .build()
                                            );

                                    resultLocations.add(
                                            Location.builder()
                                                    .physicalLocation(
                                                            PhysicalLocation.builder()
                                                                    .artifactLocation(
                                                                            ArtifactLocation.builder()
                                                                            .uri("file:///" + r.getValue().get("file"))
                                                                            .index(0)
                                                                            .build())
                                                                    .region(Region.builder()
                                                                            .startColumn(String.valueOf(r.getValue().get("column")))
                                                                            .startLine(String.valueOf(r.getValue().get("line")))
                                                                            .build())
                                                                    .build())
                                                    .build());
                            })
        );

        // Build the collection of the tool -> driver -> rules objects
        List<SarifIssueTracker.Rule> rules = Lists.newArrayList();
        filteredXIssues.stream().forEach(xss ->
                rules.add(Rule.builder()
                            .id(xss.getVulnerability())
                            .shortDescription(ShortDescription.builder().text(xss.getDescription()).build())
                            .helpUri((String) xss.getAdditionalDetails().get("recommendedFix"))
                            .properties(Properties.builder()
                                        .category((String) xss.getAdditionalDetails().get("categories"))
                                        .build())
                            .build())
        );

        // Build the collection of the SARIF results
        List<SarifIssueTracker.Result> sarifResults = filteredXIssues
                .stream().map(xss ->
                        SarifIssueTracker.Result
                                .builder()
                                .level(xss.getSeverity())
                                .message(Message.builder().text(xss.getDescription()).build())
                                .locations(resultLocations)
                                .ruleId("")
                                .ruleIndex("0")
                                .build())
                .collect(Collectors.toList());

        // Build the root object
        SarifIssueTracker.SarifVulnerability report =
                SarifVulnerability
                        .builder()
                        .tool(Tool.builder()
                                .driver(Driver
                                            .builder()
                                            .name(results.getTeam())
                                            .informationUri(results.getLink())
                                            .rules(rules)
                                            .build())
                                .build())
                        .artifacts(artifacts)
                        .results(sarifResults)
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
    public static class SarifVulnerability {
        @JsonProperty("tool")
        public SarifIssueTracker.Tool tool;
        @JsonProperty("artifacts")
        public List<SarifIssueTracker.Artifact> artifacts;
        @JsonProperty("results")
        public List<SarifIssueTracker.Result> results;
    }

    @Data
    @Builder
    public static class Artifact {
        @JsonProperty("location")
        public SarifIssueTracker.ArtifactLocation location;
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
    }

    @Data
    @Builder
    public static class Rule {
        @JsonProperty("id")
        public String id;
        @JsonProperty("shortDescription")
        public SarifIssueTracker.ShortDescription shortDescription;
        @JsonProperty("helpUri")
        public String helpUri;
        @JsonProperty("properties")
        public SarifIssueTracker.Properties properties;
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
        private String ruleIndex;
    }

    @Data
    @Builder
    public static class Location {
        @JsonProperty("physicalLocation")
        public SarifIssueTracker.PhysicalLocation physicalLocation;
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
        public String startLine;
        @JsonProperty("startColumn")
        public String startColumn;
    }

    @Data
    @Builder
    public static class PhysicalLocation {
        @JsonProperty("artifactLocation")
        public SarifIssueTracker.ArtifactLocation artifactLocation;
        @JsonProperty("region")
        public SarifIssueTracker.Region region;
    }
}
