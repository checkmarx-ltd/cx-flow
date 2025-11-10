package com.checkmarx.flow.custom;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitLabProperties;
import com.checkmarx.flow.custom.gitlabDashboard.GitLabDashboardStrategy;
import com.checkmarx.flow.custom.gitlabDashboard.GitLabDashboardStrategyFactory;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.service.FilenameFormatter;
import com.checkmarx.sdk.dto.ScanResults;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service("GitLabDashboard")
@RequiredArgsConstructor
@Slf4j
public class GitLabSecurityDashboard extends ImmutableIssueTracker {
    private static final String ISSUE_FORMAT = "%s @ %s : %d";
    private static final String CHECKMARX = "Checkmarx";
    private final GitLabProperties properties;
    private final FlowProperties flowProperties;
    private final FilenameFormatter filenameFormatter;

    @Override
    public void init(ScanRequest request, ScanResults results){
        log.info("In the GitLab Security Dashboard Init method");
    }

    @Override
    public void complete(ScanRequest request, ScanResults results) throws MachinaException {
        deleteFilesIfExist(properties.getSastFilePath(),properties.getScaFilePath());
        GitLabDashboardStrategy strategy = GitLabDashboardStrategyFactory.getStrategy(properties.getGitlabdashboardversion(), flowProperties);
        if (results.getXIssues() != null) {
            log.info("Finalizing SAST Dashboard output");
            fileInit(request, results, properties.getSastFilePath(), filenameFormatter, log);
            strategy.generateSastDashboard(request, results);
        }
        if (results.getScaResults() != null) {
            log.info("Finalizing SCA Dashboard output");
            fileInit(request, results, properties.getScaFilePath(), filenameFormatter, log);
            strategy.generateScaDashboard(request, results);
        }
    }

    private void deleteFilesIfExist(String sastFilePath, String scaFilePath) {
        try {
            Files.deleteIfExists(Paths.get(sastFilePath));
            Files.deleteIfExists(Paths.get(scaFilePath));
        }
        catch (IOException e) {
            log.error("Issue deleting existing files {} or {}", sastFilePath,scaFilePath, e);
        }

    }
}
