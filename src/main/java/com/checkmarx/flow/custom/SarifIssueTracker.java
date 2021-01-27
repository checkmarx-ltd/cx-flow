package com.checkmarx.flow.custom;

import com.checkmarx.flow.config.SarifProperties;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.service.FilenameFormatter;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.sca.report.Finding;
import com.checkmarx.sdk.dto.sca.report.Package;
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
    private static final String MARKDOWN_TABLE_FORMAT = "| %s | %s | %s | %s |";
    private static final String RECOMMENDED_FIX = "recommendedFix";

    @Override
    public void init(ScanRequest request, ScanResults results) throws MachinaException {
        fileInit(request, results, properties.getFilePath(), filenameFormatter, log);
    }

    @Override
    public void complete(ScanRequest request, ScanResults results) throws MachinaException {
        log.info("Finalizing SARIF output");
        List<SarifVulnerability> run=Lists.newArrayList();
        if(results.getXIssues() != null) {
            log.info("Generating SAST Sarif Report");
            generateSastResults(results,run);
            // Filter issues without false-positives
            // Build the run object
        }
        if(results.getScaResults() != null ){
            log.info("Generating SCA Sarif Report");
            generateScaResults(results,run);
        }

        // Build the report
        SarifReport report = SarifReport.builder()
                .schema(properties.getSarifSchema())
                .version(properties.getSarifVersion())
                .runs(run)
                .build();

        writeJsonOutput(request, report, log);
    }

    private void generateScaResults(ScanResults results, List<SarifVulnerability> run) {
        List<Rule> scaScanrules = Lists.newArrayList();
        List<Result> scaScanresultList = Lists.newArrayList();
        Map<String, List<Finding>> findingsMap = results.getScaResults()
                .getFindings().stream().collect(Collectors.groupingBy(Finding::getPackageId));

        List<Package> packages = new ArrayList<>(results.getScaResults()
                .getPackages());

        Map<String, Package> map = new HashMap<>();
        for (Package p : packages) map.put(p.getId(), p);


        for (Map.Entry<String, List<Finding>> entry : findingsMap.entrySet()) {
            String key = entry.getKey();
            StringBuilder markDownValue = new StringBuilder();
            markDownValue.append(String.format(MARKDOWN_TABLE_FORMAT, "CVE Name", "Description", "Score", "References")).append("\r");
            markDownValue.append(String.format(MARKDOWN_TABLE_FORMAT, "---", "---", "---", "---")).append("\r");
            List<Finding> val = entry.getValue();
            List<String> tags = new ArrayList<>();
            val.forEach(v -> {
                markDownValue.append(String.format(MARKDOWN_TABLE_FORMAT, v.getCveName(), v.getDescription(), v.getScore(), v.getReferences())).append("\r");
                if (!tags.contains(String.valueOf(v.getScore())))
                    tags.add(String.valueOf(v.getScore()));
            });
            tags.replaceAll(s -> "CVSS-" + s);
            tags.add("security");
            Rule rule = Rule.builder().id(key)
                    .shortDescription(ShortDescription.builder().text(key).build())
                    .fullDescription(FullDescription.builder().text(key).build())
                    .help(Help.builder().markdown(String.valueOf(markDownValue).replace("\n", " ").replace("[", "").replace("]", "")).text(String.valueOf(markDownValue).replace("\n", " ")).build())
                    .properties(Properties.builder().tags(tags).build())
                    .build();

            List<Location> locations = Lists.newArrayList();
            List<String> locationString = Lists.newArrayList();
            map.get(key).getLocations().forEach(k -> {
                if (!locationString.contains(k)) {
                    locationString.add(k);
                }
            });


            locations.add(Location.builder()
                    .physicalLocation(PhysicalLocation.builder()
                            .artifactLocation(ArtifactLocation.builder()
                                    .uri(locationString.stream().map(String::valueOf).collect(Collectors.joining(",")))
                                    .build())
                            .build())
                    .build());

            // Build collection of the results -> locations
            scaScanresultList.add(
                    Result.builder()
                            .level(properties.getSeverityMap().get(map.get(key).getSeverity()) != null ? properties.getSeverityMap().get(map.get(key).getSeverity().toString()) : DEFAULT_LEVEL)
                            .locations(locations)
                            .message(Message.builder()
                                    .text(key)
                                    .build())
                            .ruleId(key)
                            .build()
            );

            scaScanrules.add(rule);

        }
        run.add(SarifVulnerability
                .builder()
                .tool(Tool.builder()
                        .driver(Driver.builder()
                                .name(properties.getScaScannerName())
                                .organization(properties.getScaOrganization())
                                .semanticVersion(properties.getSemanticVersion())
                                .rules(scaScanrules)
                                .build())
                        .build())
                .results(scaScanresultList)
                .build());
    }

    private void generateSastResults(ScanResults results, List<SarifVulnerability> run) {
        List<Rule> sastScanrules;
        List<Result> sastScanresultList = Lists.newArrayList();
        List<ScanResults.XIssue> filteredXIssues =
                results.getXIssues()
                        .stream()
                        .filter(x -> x.getVulnerability()!=null)
                        .filter(x -> !x.isAllFalsePositive())
                        .collect(Collectors.toList());
        //Distinct list of Vulns (Rules)
        List<ScanResults.XIssue> filteredByVulns =
                results.getXIssues()
                        .stream()
                        .filter(x -> x.getVulnerability()!=null)
                        .collect(Collectors.toCollection(() ->
                                new TreeSet<>(Comparator.comparing(ScanResults.XIssue::getVulnerability))))
                        .stream()
                        .filter(x -> !x.isAllFalsePositive())
                        .collect(Collectors.toList());
        // Build the collection of the rules objects (Vulnerabilities)
        sastScanrules = filteredByVulns.stream().map(i -> Rule.builder()
                .id(i.getVulnerability())
                .name(i.getVulnerability())
                .shortDescription(ShortDescription.builder().text(i.getVulnerability()).build())
                .fullDescription(FullDescription.builder().text(i.getVulnerability()).build())
                .help(Help.builder()
                        .markdown(String.format("[%s Details](%s)",
                                i.getVulnerability(),
                                (i.getAdditionalDetails().get(RECOMMENDED_FIX)==null) ? "":i.getAdditionalDetails().get(RECOMMENDED_FIX)))
                        .text((String)((i.getAdditionalDetails().get(RECOMMENDED_FIX)==null) ? "Fix not available.":i.getAdditionalDetails().get(RECOMMENDED_FIX)))
                        .build())
                .properties(Properties.builder()
                        .tags(Arrays.asList("security", "external/cwe/cwe-".concat(i.getCwe())))
                        .build())
                .build()).collect(Collectors.toList());
        //All issues to create the results/locations that are not all false positive

        filteredXIssues.forEach(
                issue -> {
                    List<Location> locations = Lists.newArrayList();
                    issue.getDetails().forEach((k, v) -> {
                        if (!v.isFalsePositive()) {
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
                    sastScanresultList.add(
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
        run.add(SarifVulnerability
                .builder()
                .tool(Tool.builder()
                        .driver(Driver.builder()
                                .name(properties.getSastScannerName())
                                .organization(properties.getSastOrganization())
                                .semanticVersion(properties.getSemanticVersion())
                                .rules(sastScanrules)
                                .build())
                        .build())
                .results(sastScanresultList)
                .build());
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
