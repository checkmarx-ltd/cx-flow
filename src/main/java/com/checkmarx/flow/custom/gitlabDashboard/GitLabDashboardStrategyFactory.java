package com.checkmarx.flow.custom.gitlabDashboard;

import com.checkmarx.flow.config.FlowProperties;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GitLabDashboardStrategyFactory {
    public static GitLabDashboardStrategy getStrategy(String version, FlowProperties flowProperties) {
        return switch (version) {
            case "2.0" -> new GitLabDashboardV2(flowProperties);
            case "14.1.2" -> new GitLabDashboardV14(flowProperties);
            default -> new GitLabDashboardV15(flowProperties);
        };
    }
}