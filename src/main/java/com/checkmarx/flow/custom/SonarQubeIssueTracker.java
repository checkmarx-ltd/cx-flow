package com.checkmarx.flow.custom;

import com.checkmarx.flow.config.SonarQubeProperties;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.service.FilenameFormatter;
import com.checkmarx.sdk.dto.ScanResults;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        List<Issue> issues=Lists.newArrayList();
        if(results.getXIssues() != null) {
            log.info("Generating SAST Sonar Qube Report");
            generateSastResults(results,issues);
            // Filter issues without false-positives
            // Build the run object
        }
        if(results.getScaResults() != null ){
            log.info("Generating SCA Sonar Qube Report");
            generateScaResults(results,issues);
        }

        // Build the report
        SonarQubeReport report = SonarQubeReport.builder()
                .issues(issues)
                .build();

        writeJsonOutput(request, report, log);
    }

    private void generateScaResults(ScanResults results, List<Issue> sonarIssues) {
        // Sonar Report for Sca Result

    }

    private void generateSastResults(ScanResults results, List<Issue> sonarIssues) {
        List<ScanResults.XIssue> filteredXIssues =
                results.getXIssues()
                        .stream()
                        .filter(x -> x.getVulnerability()!=null)
                        .filter(x -> !x.isAllFalsePositive())
                        .collect(Collectors.toList());
        //All issues to create the results/locations that are not all false positive
        filteredXIssues.forEach(
                issue -> {
                    issue.getDetails().forEach((k, v) -> {
                        if (!v.isFalsePositive()) {
                            sonarIssues.add(Issue.builder().engineId(properties.getSastScannerName())
                                    .ruleId(issue.getVulnerability())
                                    .severity(properties.getSeverityMap().get(issue.getSeverity()) != null ? properties.getSeverityMap().get(issue.getSeverity()):DEFAULT_LEVEL)
                                    .type("VULNERABILITY")
                                    .primaryLocation(ILocation.builder()
                                            .filePath(issue.getFilename())
                                            .message(StringUtils.isEmpty(issue.getDescription()) ? issue.getVulnerability() : issue.getDescription())
                                            .textRange(TextRange.builder()
                                                    .startLine(k<1?1:k)
                                                    .endLine(k<1?1:k).build()).build()).build());

                        }
                    });
                }
        );
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
        List<ILocation> secondaryLocations;
    }

    @Data
    @Builder
    public static class ILocation {
        @JsonProperty("message")
        private String message;
        @JsonProperty("filePath")
        String filePath;
        @JsonProperty("textRange")
        TextRange textRange;
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
}
