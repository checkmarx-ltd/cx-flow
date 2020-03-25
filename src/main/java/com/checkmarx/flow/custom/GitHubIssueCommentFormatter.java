package com.checkmarx.flow.custom;

import com.checkmarx.flow.dto.Issue;
import com.checkmarx.flow.dto.github.IssueStatus;
import com.checkmarx.sdk.dto.ScanResults;
import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Data
@Builder
@AllArgsConstructor
@Slf4j
public class GitHubIssueCommentFormatter {

    private String issueUrl;
    private ScanResults.XIssue resultIssue;
    private Issue gitHubIssueBeforeUpdate;
    private com.checkmarx.flow.dto.github.Issue gitHubIssueAfterUpdate;
    private String commentToPublish;
    private IssueStatus issueStatus;
    private StringBuilder issueDescription;

    IssueStatus createNewIssueStatus(Issue issueBeforeUpdate, ScanResults.XIssue resultIssue,
                                     com.checkmarx.flow.dto.github.Issue issueAfterUpdate) {
        Map<Integer, ScanResults.IssueDetails> sastFalsePositiveIssuesFromResultMap = getSASTFalsePositiveIssuesFromResult(resultIssue);
        Map<String, String> sastResolvedIssuesFromResultsMap = getSASTResolvedIssuesFromResults(issueBeforeUpdate.getBody(), resultIssue);

        String newFalsePositiveLines = getNewFalsePositiveLines(sastFalsePositiveIssuesFromResultMap);
        int sizeOfGitHubVulnerabilitiesAfterFixing = extractGitHubIssueVulnerabilityCodeLines(issueAfterUpdate.getBody()).size();

        IssueStatus newIssueStatus = IssueStatus.builder()
                .sastResolvedIssuesFromResults(sastResolvedIssuesFromResultsMap)
                .openFalsePositiveLinesAsADescription(newFalsePositiveLines)
                .totalOpenLinesForIssueBeforeFixing(sizeOfGitHubVulnerabilitiesAfterFixing)
                .totalResolvedFalsePositiveLines(sastFalsePositiveIssuesFromResultMap.size())
                .totalResolvedLinesFromResults(sastResolvedIssuesFromResultsMap.size())
                .build();

        int totalOpenLinesForIssueBeforeFixing = newIssueStatus.getTotalOpenLinesForIssueBeforeFixing();
        newIssueStatus.setTotalLinesToFixLeft(totalOpenLinesForIssueBeforeFixing);

        return newIssueStatus;
    }

    StringBuilder getUpdatedIssueComment(IssueStatus issueStatus) {
        StringBuilder commentFormat = new StringBuilder();
        commentFormat.append("Issue still exists.\n");

        Map<String, String> sastResolvedIssuesFromResults = issueStatus.getSastResolvedIssuesFromResults();
        if (!sastResolvedIssuesFromResults.isEmpty()) {
            log.info("Found {} resolved vulnerabilities from results", sastResolvedIssuesFromResults);
            commentFormat.append("The following code lines snippets were resolved from the issue:\n\n");
            issueStatus.getSastResolvedIssuesFromResults().forEach((key, value) -> commentFormat.append("`Code line: ")
                    .append(key).append("`").append("\n`Snippet: ").append(value).append("`").append("\n"));
        }

        if (!issueStatus.getOpenFalsePositiveLinesAsADescription().isEmpty()) {
            log.info("Found false-positive results");
            commentFormat.append("\n");
            commentFormat.append(issueStatus.getOpenFalsePositiveLinesAsADescription()).append("\n\n");
        }

        commentFormat.append("\n### **SUMMARY**\n\n");
        // No Resolved vulnerabilities
        if ((issueStatus.getTotalResolvedLinesFromResults() + issueStatus.getTotalResolvedFalsePositiveLines()) == 0) {
            commentFormat.append("Issue has total **")
                    .append(issueStatus.getTotalOpenLinesForIssueBeforeFixing())
                    .append("** vulnerabilities left to be fix (Please scroll to the top for more information)");
        } else {
            commentFormat.append("- Total of vulnerabilities resolved on the last scan: **")
                    .append(issueStatus.getTotalResolvedLinesFromResults())
                    .append("**");
            commentFormat.append("\n- Total of vulnerabilities set as 'false positive' on the last scan: **")
                    .append(issueStatus.getTotalResolvedFalsePositiveLines())
                    .append("**");
            commentFormat.append("\n- Total of vulnerabilities left to fix for this issue: **")
                    .append(issueStatus.getTotalLinesToFixLeft())
                    .append("**")
                    .append(" (Please scroll to the top for more information)");
        }
        return commentFormat;
    }

    private Map<Integer, ScanResults.IssueDetails> getSASTFalsePositiveIssuesFromResult(ScanResults.XIssue resultIssue) {
        Map<Integer, ScanResults.IssueDetails> sastIssuesDetails = resultIssue.getDetails();
        if (sastIssuesDetails != null) {
            return sastIssuesDetails.entrySet()
                    .stream()
                    .filter(detailsEntry -> detailsEntry.getValue().isFalsePositive())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } else {
            return new HashMap<>();
        }
    }

    private String getNewFalsePositiveLines(Map<Integer, ScanResults.IssueDetails> newFalsePositiveIssuesMap) {
        StringBuilder sb = new StringBuilder();

        if (!newFalsePositiveIssuesMap.isEmpty()) {
            sb.append("Following code lines were resolved by being defined as false-positive:\n\n");

            newFalsePositiveIssuesMap.forEach((key, value) -> {
                sb.append("`Code line: ").append(key).append("`").append("\n");
                sb.append("`Snippet: ").append(value.getCodeSnippet().trim()).append("`").append("\n");
            });
        }
        return sb.toString();
    }

    private Map<String, String> getSASTResolvedIssuesFromResults(String issueBody, ScanResults.XIssue resultIssue) {
        Sets.SetView<String> differentCodeLinesSet;

        Set<String> currentIssueCodeLines = extractGitHubIssueVulnerabilityCodeLines(issueBody);
        Set<String> currentSASTResultCodeLines = extractSASTResultCodeLines(resultIssue);
        if (currentIssueCodeLines.size() != currentSASTResultCodeLines.size()) {
            // leaves only the different code lines which resolved
            differentCodeLinesSet = Sets.difference(currentIssueCodeLines, currentSASTResultCodeLines);
            return extractGitHubIssueVulnerabilityCodeSnippet(differentCodeLinesSet, issueBody);
        } else {
            return new HashMap<>();
        }

    }

    private Set<String> extractSASTResultCodeLines(ScanResults.XIssue resultIssue) {
        return resultIssue.getDetails().entrySet()
                .stream()
                .map(e -> e.getKey().toString())
                .collect(Collectors.toSet());
    }

    private Map<String, String> extractGitHubIssueVulnerabilityCodeSnippet(Set<String> resolvedIssueCodeLines, String issueBody) {
        String codeSnippetForCodeLinePattern = "\\(Line #%s\\).*\\W\\`{3}\\W+(.*)(?=\\W+\\`{3})"; // an example for such a line: [Code (Line #42):](null#L42)```     password = txtPassword.Text
        Map<String, String> sastResolvedIssuesMap = new HashMap<>();

        for (String currentResolvedIssue : resolvedIssueCodeLines) {
            String currentCodeLinePattern = String.format(codeSnippetForCodeLinePattern, currentResolvedIssue);

            Pattern pattern = Pattern.compile(currentCodeLinePattern, Pattern.UNIX_LINES);
            Matcher matcher = pattern.matcher(issueBody);

            while (matcher.find()) {
                sastResolvedIssuesMap.put(currentResolvedIssue, matcher.group(1));
            }
        }
        return sastResolvedIssuesMap;
    }

    private Set<String> extractGitHubIssueVulnerabilityCodeLines(String issueBody) {
        final String allNumbersAfterLinePattern = "Line #[0-9]+";
        final String allNumbersPattern = "[0-9]+";

        final Pattern allNumAfterLinePattern = Pattern.compile(allNumbersAfterLinePattern, Pattern.MULTILINE);
        final Matcher allNumAfterLineMatcher = allNumAfterLinePattern.matcher(issueBody);

        Set<String> codeLinesSet = new HashSet<>();

        while(allNumAfterLineMatcher.find()) {
            Pattern allNumPattern = Pattern.compile(allNumbersPattern, Pattern.MULTILINE);
            Matcher allNumMatcher = allNumPattern.matcher(allNumAfterLineMatcher.group());

            if (allNumMatcher.find()) {
                codeLinesSet.add(allNumMatcher.group());
            }
        }
        return codeLinesSet;
    }
}