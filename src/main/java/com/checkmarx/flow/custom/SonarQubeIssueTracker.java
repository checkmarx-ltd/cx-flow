package com.checkmarx.flow.custom;

import com.checkmarx.flow.config.SonarQubeProperties;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.service.FilenameFormatter;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.sca.report.Finding;
import com.checkmarx.sdk.dto.sca.report.Package;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Output according to the sonarqube Generic Issue Import Format
 * https://docs.sonarqube.org/latest/analysis/generic-issue
 */
@Service("SonarQube")
@RequiredArgsConstructor
@Slf4j
public class SonarQubeIssueTracker extends ImmutableIssueTracker {
    private final SonarQubeProperties properties;
    private final FilenameFormatter filenameFormatter;
    private static final String DEFAULT_LEVEL = "MAJOR";

    @Override
    public void init(ScanRequest request, ScanResults results) throws MachinaException {
        fileInit(request, results, properties.getFilePath(), filenameFormatter, log);
    }

    @Override
    public void complete(ScanRequest request, ScanResults results) throws MachinaException {
        log.info("Finalizing Sonar Qube output");
        List<Issue> issues = Lists.newArrayList();
        if (results.getXIssues() != null) {
            log.info("Generating SAST Sonar Qube Report");
            generateSastResults(results, issues);
            // Filter issues without false-positives
            // Build the run object
        }
        if (results.getScaResults() != null) {
            log.info("Generating SCA Sonar Qube Report");
            generateScaResults(results, issues);
        }

        // Build the report
        SonarQubeReport report = SonarQubeReport.builder()
                .issues(issues)
                .build();

        writeJsonOutput(request, report, log);
    }

    private void generateScaResults(ScanResults results, List<Issue> sonarIssues) {
        // Sonar Report for Sca Result
        Map<String, List<Finding>> findingsMap = results.getScaResults()
                .getFindings().stream().collect(Collectors.groupingBy(Finding::getPackageId));

        List<Package> packages = new ArrayList<>(results.getScaResults()
                .getPackages());

        Map<String, Package> map = new HashMap<>();
        for (Package p : packages) map.put(p.getId(), p);

        for (Map.Entry<String, List<Finding>> entry : findingsMap.entrySet()) {

            String key = entry.getKey();
            Package vulnerablePackage = map.get(key);
            StringBuilder messageBuilder = new StringBuilder();
            List<Finding> val = entry.getValue();
            List<String> tags = new ArrayList<>();
            val.forEach(v -> {
                vulnerablePackage.getLocations().forEach(k -> {
                    messageBuilder.append("Package:").append(v.getPackageId()).append(",")
                            .append("Description:").append(v.getDescription()).append(",")
                            .append("Score:").append(v.getScore());
                    sonarIssues.add(Issue.builder().engineId(properties.getScaScannerName())
                            .ruleId(v.getId())
                            .severity(properties.getSeverityMap().get(v.getSeverity()) != null ? properties.getSeverityMap().get(v.getSeverity()) : DEFAULT_LEVEL)
                            .type("VULNERABILITY")
                            .primaryLocation(ILocation.builder()
                                    .filePath(k)
                                    .message(messageBuilder.toString())
                                    .textRange(TextRange.builder().
                                            startLine(1)
                                            .endLine(1).build()).build())
                            .build());
                });
            });
        }
    }

    private void generateSastResults(ScanResults results, List<Issue> sonarIssues) {
        List<ScanResults.XIssue> filteredXIssues =
                results.getXIssues()
                        .stream()
                        .filter(x -> x.getVulnerability() != null)
                        .filter(x -> !x.isAllFalsePositive())
                        .collect(Collectors.toList());
        //All issues to create the results/locations that are not all false positive
        filteredXIssues.forEach(
                issue -> {
                    issue.getDetails().forEach((k, v) -> {
                        if (!v.isFalsePositive()) {
                            sonarIssues.add(Issue.builder().engineId(properties.getSastScannerName())
                                    .ruleId(issue.getVulnerability())
                                    .severity(properties.getSeverityMap().get(issue.getSeverity()) != null ? properties.getSeverityMap().get(issue.getSeverity()) : DEFAULT_LEVEL)
                                    .type("VULNERABILITY")
                                    .primaryLocation(ILocation.builder()
                                            .filePath(issue.getFilename())
                                            .message(StringUtils.isEmpty(issue.getDescription()) ? issue.getVulnerability() : issue.getDescription())
                                            .textRange(TextRange.builder()
                                                    .startLine(k < 1 ? 1 : k)
                                                    .endLine(k < 1 ? 1 : k).build()).build())
                                    //
                                    .secondaryLocations(findSecondaryLocation(issue, v.getCodeSnippet()))
                                    .build());

                        }
                    });
                }
        );
    }

    private List<ILocationSecondary> findSecondaryLocation(ScanResults.XIssue issue, String codeSnippet) {
        List<ILocationSecondary> secondaryLocationList = new ArrayList<>();

        try {
            List<Object> secondarypathListAllOccurence = (List<Object>) issue.getAdditionalDetails().get("results");

            for (int i = 0; i < secondarypathListAllOccurence.size(); i++) {
                HashMap<String, Object> mapPerIssue = (HashMap<String, Object>) secondarypathListAllOccurence.get(i);
                HashMap<String, String> mapFirstIssue = (HashMap<String, String>) mapPerIssue.get("1");

                mapPerIssue.forEach((k, v) -> {
                    if (mapFirstIssue.get("snippet").equalsIgnoreCase(codeSnippet) && !k.equalsIgnoreCase("state")
                            && !k.equalsIgnoreCase("sink") && !k.equalsIgnoreCase("source")) {
                        HashMap<String, String> IlicationDetails = (HashMap<String, String>) v;

                        ILocationSecondary locationObj = new ILocationSecondary(IlicationDetails.get("snippet"), IlicationDetails.get("file"), new TextRangeSecondary(
                                Integer.valueOf(IlicationDetails.get("line")), Integer.valueOf(IlicationDetails.get("line"))));
                        secondaryLocationList.add(locationObj);
                    }

                });
            }
        } catch (Exception e) {
            log.debug("Error Parsing Secondary Path in Sonarqube : " + e.getStackTrace());
        }
        return secondaryLocationList;
    }

    @Data
    @Builder
    public static class SonarQubeReport {
        @JsonProperty("issues")
        List<Issue> issues;
    }

    @Data
    @Builder
    public static class Issue {
        @JsonProperty("engineId")
        String engineId;
        @JsonProperty("ruleId")
        String ruleId;
        @JsonProperty("severity")
        String severity;
        @JsonProperty("type")
        String type;
        @JsonProperty("primaryLocation")
        ILocation primaryLocation;
        @JsonProperty("secondaryLocations")
        List<ILocationSecondary> secondaryLocations;
    }

    @Data
    @Builder
    @Getter
    @Setter
    public static class ILocation {
        @JsonProperty("message")
        private String message;
        @JsonProperty("filePath")
        String filePath;
        @JsonProperty("textRange")
        TextRange textRange;
    }


    @Getter
    @Setter
    public static class ILocationSecondary {
        private String message;
        String filePath;
        TextRangeSecondary textRange;

        public ILocationSecondary(String message, String filePath, TextRangeSecondary textRange) {
            this.message = message;
            this.filePath = filePath;
            this.textRange = textRange;
        }
    }

    @Data
    @Builder
    public static class TextRange {
        @JsonProperty("startLine")
        Integer startLine;
        @JsonProperty("endLine")
        Integer endLine;
        @JsonProperty("startColumn")
        Integer startColumn;
        @JsonProperty("endColumn")
        Integer endColumn;
    }

    @Getter
    @Setter

    public static class TextRangeSecondary {
        Integer startLine;
        Integer endLine;

        public TextRangeSecondary(Integer startLine, Integer endLine) {
            this.startLine = startLine;
            this.endLine = endLine;
        }
    }


}
