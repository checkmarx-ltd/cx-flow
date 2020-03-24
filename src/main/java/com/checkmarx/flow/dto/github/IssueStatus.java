package com.checkmarx.flow.dto.github;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueStatus {

    private String openFalsePositiveLinesAsADescription;
    private Map<String, String> sastResolvedIssuesFromResults; // key is code line, e.g. '60' and value is the code snippet, e.g. 'password = txtPassword.Text'
    private int totalOpenLinesForIssueBeforeFixing;
    private int totalResolvedFalsePositiveLines;
    private int totalResolvedLinesFromResults;
    private int totalLinesToFixLeft;

}