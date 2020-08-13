package com.checkmarx.flow.custom;

import com.checkmarx.flow.config.SarifProperties;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.service.FilenameFormatter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Output according to the following Spec (SARIF)
 * https://docs.github.com/en/github/finding-security-vulnerabilities-and-errors-in-your-code/sarif-support-for-code-scanning
 */
@Service("Sarif")
@RequiredArgsConstructor
@Slf4j
public class SarifIssueTracker extends ImmutableIssueTracker {
    private final SarifProperties properties;
    private final FilenameFormatter filenameFormatter;
    private static final String DEFAULT_LEVEL = "error";

    @Override
    public void init(ScanRequest request, ScanResults results) throws MachinaException {
        fileInit(request, results, properties.getFilePath(), filenameFormatter, log);
    }

    @Override
    public void complete(ScanRequest request, ScanResults results) throws MachinaException {
        log.info("Finalizing SARIF output");

        // Filter issues without false-positives
        List<ScanResults.XIssue> filteredXIssues =
                results.getXIssues()
                        .stream()
                        .filter(x -> !x.isAllFalsePositive())
                        .collect(Collectors.toList());
        //Distinct list of Vulns (Rules)
        List<ScanResults.XIssue> filteredByVulns =
                results.getXIssues()
                        .stream()
                        .collect(Collectors.toCollection(() ->
                                new TreeSet<>(Comparator.comparing(ScanResults.XIssue::getVulnerability))))
                        .stream()
                        .filter(x -> !x.isAllFalsePositive())
                        .collect(Collectors.toList());
        // Build the collection of the rules objects (Vulnerabilities)
        List<Rule> rules = filteredByVulns.stream().map(i -> Rule.builder()
                .id(i.getVulnerability())
                .name(i.getVulnerability())
                .shortDescription(ShortDescription.builder().text(i.getVulnerability()).build())
                .fullDescription(FullDescription.builder().text(i.getVulnerability()).build())
                .help(Help.builder()
                        .markdown(String.format("[%s Details](%s)",
                                i.getVulnerability(),
                                i.getAdditionalDetails().get("recommendedFix")))
                        .text((String) i.getAdditionalDetails().get("recommendedFix"))
                        .build())
                .properties(Properties.builder()
                        .tags(Arrays.asList("security", "external/cwe/cwe-".concat(i.getCwe())))
                        .build())
                .build()).collect(Collectors.toList());
        //All issues to create the results/locations that are not all false positive
        List<Result> resultList = Lists.newArrayList();
        filteredXIssues.forEach(
                issue -> {
                    List<Location> locations = Lists.newArrayList();
                    issue.getDetails().forEach((k, v) -> {
                        if(!v.isFalsePositive()) {
                            locations.add(Location.builder()
                                    .physicalLocation(PhysicalLocation.builder()
                                            .artifactLocation(ArtifactLocation.builder()
                                                    .uri(issue.getFilename())
                                                    .build())
                                            .region(Region.builder()
                                                    .startLine(k)
                                                    .endLine(k)
                                                    .build())
                                            .build())
                                    .build());

                        }
                    });
                    // Build collection of the results -> locations
                    resultList.add(
                            Result.builder()
                            .level(properties.getSeverityMap().get(issue.getSeverity()) != null ? properties.getSeverityMap().get(issue.getSeverity()) : DEFAULT_LEVEL)
                            .locations(locations)
                            .message(Message.builder()
                                    .text(issue.getDescription())
                                    .build())
                            .ruleId(issue.getVulnerability())
                            .build()
                    );

                }
        );

        // Build the run object
        SarifVulnerability run =
                SarifVulnerability
                        .builder()
                        .tool(Tool.builder()
                                .driver(Driver.builder()
                                    .name(properties.getScannerName())
                                    .organization(properties.getOrganization())
                                    .semanticVersion(properties.getSemanticVersion())
                                    .rules(rules)
                                    .build())
                                .build())
                        .results(resultList)
                        .build();

        // Build the report
        SarifReport report = SarifReport.builder()
                .schema(properties.getSarifSchema())
                .version(properties.getSarifVersion())
                .runs(Collections.singletonList(run))
                .build();

        writeJsonOutput(request, report, log);
    }

    @Data
    @Builder
    public static class SarifReport {
        @JsonProperty("$schema")
        private String schema;
        @JsonProperty("version")
        String version;
        @JsonProperty("runs")
        List<SarifVulnerability> runs;
    }

    @Data
    @Builder
    public static class SarifVulnerability {
        @JsonProperty("tool")
        public Tool tool;
        @JsonProperty("results")
        public List<Result> results;
    }

    @Data
    @Builder
    public static class Tool {
        @JsonProperty("driver")
        public Driver driver;
    }

    @Data
    @Builder
    public static class Driver {
        @JsonProperty("name")
        public String name;
        @JsonProperty("organization")
        public String organization;
        @JsonProperty("semanticVersion")
        public String semanticVersion;
        @JsonProperty("rules")
        public List<Rule> rules;
    }

    @Data
    @Builder
    public static class ShortDescription {
        @JsonProperty("text")
        public String text;
    }

    @Data
    @Builder
    public static class FullDescription {
        @JsonProperty("text")
        public String text;
    }

    @Data
    @Builder
    public static class DefaultConfiguration {
        @JsonProperty("level")
        public String level;
    }

    @Data
    @Builder
    public static class Help {
        @JsonProperty("text")
        public String text;
        @JsonProperty("markdown")
        public String markdown;
    }

    @Data
    @Builder
    public static class Properties {
        @JsonProperty("tags")
        List<String> tags;
        @JsonProperty("precision")
        @Builder.Default
        private String precision = "";
    }

    @Data
    @Builder
    public static class Rule {
        @JsonProperty("id")
        public String id;
        @JsonProperty("name")
        public String name;
        @JsonProperty("shortDescription")
        public ShortDescription shortDescription;
        @JsonProperty("fullDescription")
        public FullDescription fullDescription;
        @JsonProperty("help")
        public Help help;
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
        @JsonProperty("ruleId")
        private String ruleId;
        @JsonProperty("ruleIndex")
        private Integer ruleIndex;
        @JsonProperty("level")
        public String level;
        @JsonProperty("message")
        public Message message;
        @JsonProperty("locations")
        private List<Location> locations;
        @JsonProperty("partialFingerprints")
        private PartialFingerprints partialFingerprints;
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
    }

    @Data
    @Builder
    public static class PartialFingerprints {
        @JsonProperty("primaryLocationLineHash")
        public String primaryLocationLineHash;
    }

    @Data
    @Builder
    public static class Region {
        @JsonProperty("startLine")
        public Integer startLine;
        @JsonProperty("endLine")
        public Integer endLine;
        @JsonProperty("startColumn")
        public Integer startColumn;
        @JsonProperty("endColumn")
        public Integer endColumn;
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
