package com.checkmarx.flow.service;

import com.checkmarx.flow.config.RepoProperties;
import com.checkmarx.flow.dto.report.PullRequestReport;
import com.checkmarx.sdk.dto.ScanResults;

public interface MergeResultEvaluator {
    boolean isMergeAllowed(ScanResults results, RepoProperties repoProperties, PullRequestReport pullRequestReport);
}
