package com.checkmarx.flow.service;

import com.checkmarx.flow.config.RepoProperties;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.report.PullRequestReport;
import com.checkmarx.sdk.dto.ScanResults;

public interface ThresholdValidator {
    boolean isMergeAllowed(ScanResults results, RepoProperties repoProperties, PullRequestReport pullRequestReport);
    boolean thresholdsExceeded(ScanRequest request, ScanResults results);

    //Satyam :: Adding function for checking threshold of Direct Dependency
    boolean thresholdsExceededDirectDependency(ScanRequest request, ScanResults results);
    boolean isThresholdsConfigurationExist(ScanRequest scanRequest);
}
