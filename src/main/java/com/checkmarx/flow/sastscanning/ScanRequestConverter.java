package com.checkmarx.flow.sastscanning;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.Sources;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.service.GitHubService;
import com.checkmarx.flow.service.GitLabService;
import com.checkmarx.flow.service.HelperService;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.dto.cx.CxScanParams;
import com.checkmarx.sdk.exception.CheckmarxException;
import com.checkmarx.sdk.service.CxClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;

import static com.checkmarx.sdk.config.Constants.UNKNOWN;
import static com.checkmarx.sdk.config.Constants.UNKNOWN_INT;

@Component
@Slf4j
@RequiredArgsConstructor
public class ScanRequestConverter {

    private final HelperService helperService;
    private final CxProperties cxProperties;
    private final CxClient cxService;
    private final FlowProperties flowProperties;
    private final GitHubService gitService;
    private final GitLabService gitLabService;

    public String determineTeamAndOwnerID(ScanRequest request) throws CheckmarxException {
        String ownerId;
        String namespace = request.getNamespace();

        String team = helperService.getCxTeam(request);
        if (!ScanUtils.empty(team)) {
            if (!team.startsWith(cxProperties.getTeamPathSeparator()))
                team = cxProperties.getTeamPathSeparator().concat(team);
            log.info("Overriding team with {}", team);
            ownerId = cxService.getTeamId(team);
        } else {
            team = cxProperties.getTeam();
            if (!team.startsWith(cxProperties.getTeamPathSeparator()))
                team = cxProperties.getTeamPathSeparator().concat(team);
            log.info("Using Checkmarx team: {}", team);
            ownerId = cxService.getTeamId(team);

            if (cxProperties.isMultiTenant() && !ScanUtils.empty(namespace)) {
                String fullTeamName = cxProperties.getTeam().concat(cxProperties.getTeamPathSeparator()).concat(namespace);
                log.info("Using multi tenant team name: {}", fullTeamName);
                request.setTeam(fullTeamName);
                String tmpId = cxService.getTeamId(fullTeamName);
                if (tmpId.equals(UNKNOWN)) {
                    ownerId = cxService.createTeam(ownerId, namespace);
                } else {
                    ownerId = tmpId;
                }
            } else {
                request.setTeam(team);
            }
        }

        //Kick out if the team is unknown
        if (ownerId.equals(UNKNOWN)) {
            throw new CheckmarxException(getTeamErrorMessage());
        }
        return ownerId;
    }

    public String determineProjectName(ScanRequest request) throws MachinaException {
        String projectName;
        String repoName = request.getRepoName();
        String branch = request.getBranch();
        String namespace = request.getNamespace();

        String project = helperService.getCxProject(request);
        if (!ScanUtils.empty(project)) {
            projectName = project;
        } else if (cxProperties.isMultiTenant() && !ScanUtils.empty(repoName)) {
            projectName = repoName;
            if (!ScanUtils.empty(branch)) {
                projectName = projectName.concat("-").concat(branch);
            }
        } else {
            if (!ScanUtils.empty(namespace) && !ScanUtils.empty(repoName) && !ScanUtils.empty(branch)) {
                projectName = namespace.concat("-").concat(repoName).concat("-").concat(branch);
            } else if (!ScanUtils.empty(request.getApplication())) {
                projectName = request.getApplication();
            } else {
                log.error("Namespace (--namespace)/RepoName(--repo-name)/Branch(--branch) OR Application (--app) must be provided if the Project is not provided (--cx-project)");
                throw new MachinaException("Namespace (--namespace)/RepoName(--repo-name)/Branch(--branch) OR Application (--app) must be provided if the Project is not provided (--cx-project)");
            }
        }

        //only allow specific chars in project name in checkmarx
        projectName = projectName.replaceAll("[^a-zA-Z0-9-_.]+", "-");
        log.info("Project Name being used {}", projectName);

        return projectName;
    }

    public Integer determinePresetAndProjectId(ScanRequest request, String ownerId, String projectName) {
        Boolean projectExists = false;
        Integer projectId = UNKNOWN_INT;
        if (flowProperties.isAutoProfile() && !request.isScanPresetOverride()) {

            projectId = cxService.getProjectId(ownerId, projectName);
            if (projectId != UNKNOWN_INT) {
                int presetId = cxService.getProjectPresetId(projectId);
                if (presetId != UNKNOWN_INT) {
                    String preset = cxService.getPresetName(presetId);
                    request.setScanPreset(preset);
                    projectExists = true;
                }
            }
        }
        if (!projectExists || flowProperties.isAlwaysProfile()) {
            log.info("Project is new, profiling source...");
            Sources sources = new Sources();
            switch (request.getRepoType()) {
                case GITHUB:
                    sources = gitService.getRepoContent(request);
                    break;
                case GITLAB:
                    sources = gitLabService.getRepoContent(request);
                    break;
                case BITBUCKET:
                    log.warn("Profiling is not available for BitBucket Cloud");
                    break;
                case BITBUCKETSERVER:
                    log.warn("Profiling is not available for BitBucket Server");
                    break;
                case ADO:
                    log.warn("Profiling is not available for Azure DevOps");
                    break;
                default:
                    break;
            }
            String preset = helperService.getPresetFromSources(sources);
            if (!ScanUtils.empty(preset)) {
                request.setScanPreset(preset);
            }

        }
        return projectId;
    }

    public CxScanParams prepareScanParamsObject(ScanRequest request, File cxFile, String ownerId, String projectName, Integer projectId) {
        CxScanParams params = new CxScanParams()
                .teamId(ownerId)
                .withTeamName(request.getTeam())
                .projectId(projectId)
                .withProjectName(projectName)
                .withScanPreset(request.getScanPreset())
                .withGitUrl(request.getRepoUrlWithAuth())
                .withIncremental(request.isIncremental())
                .withForceScan(request.isForceScan())
                .withFileExclude(request.getExcludeFiles())
                .withFolderExclude(request.getExcludeFolders());
        if (!com.checkmarx.sdk.utils.ScanUtils.empty(request.getBranch())) {
            params.withBranch(Constants.CX_BRANCH_PREFIX.concat(request.getBranch()));
        }
        if (cxFile != null) {
            params.setSourceType(CxScanParams.Type.FILE);
            params.setFilePath(cxFile.getAbsolutePath());
        }
        return params;
    }

    private String getTeamErrorMessage() {
        return "Parent team could not be established. Please ensure correct team is provided.\n" +
                "Some hints:\n" +
                "\t- team name is case-sensitive\n" +
                "\t- trailing slash is not allowed\n" +
                "\t- team name separator depends on Checkmarx product version specified in CxFlow configuration:\n" +
                String.format("\t\tCheckmarx version: %s%n", cxProperties.getVersion()) +
                String.format("\t\tSeparator that should be used: %s%n", cxProperties.getTeamPathSeparator());
    }
}