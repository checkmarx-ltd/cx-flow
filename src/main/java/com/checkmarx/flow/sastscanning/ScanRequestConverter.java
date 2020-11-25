package com.checkmarx.flow.sastscanning;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.Sources;
import com.checkmarx.flow.service.*;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.ShardManager.ShardSession;
import com.checkmarx.sdk.ShardManager.ShardSessionTracker;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.CxPropertiesBase;
import com.checkmarx.sdk.dto.cx.CxScanParams;
import com.checkmarx.sdk.dto.cx.CxScanSettings;
import com.checkmarx.sdk.exception.CheckmarxException;
import com.cx.restclient.ScannerClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Optional;

import static com.checkmarx.sdk.config.Constants.UNKNOWN;
import static com.checkmarx.sdk.config.Constants.UNKNOWN_INT;

@Slf4j
@RequiredArgsConstructor
public class ScanRequestConverter {

    private final HelperService helperService;
    private final FlowProperties flowProperties;
    private final GitHubService gitService;
    private final GitLabService gitLabService;
    private final BitBucketService bitBucketService;
    private final ADOService adoService;
    private final ShardSessionTracker sessionTracker;
    private final ScannerClient scannerClient;
    private final CxPropertiesBase cxProperties;
    
    private static final String EMPTY_STRING = "";
    
    public CxScanParams toScanParams(ScanRequest scanRequest) throws CheckmarxException {
        String ownerId = determineTeamAndOwnerID(scanRequest);
        Integer projectId = determinePresetAndProjectId(scanRequest, ownerId);
        setScanConfiguration(scanRequest, projectId);
        return prepareScanParamsObject(scanRequest, null, ownerId, projectId);
    }

    private void setScanConfiguration(ScanRequest scanRequest, Integer projectId) {
        if (entityExists(projectId)) {
            log.debug("Scan request will contain scan configuration of the existing project.");
            CxScanSettings scanSettings = scannerClient.getScanSettingsDto(projectId);
            if (scanSettings != null && scanSettings.getEngineConfigurationId() != null) {
                Integer configId = scanSettings.getEngineConfigurationId();
                String configName = scannerClient.getScanConfigurationName(configId);
                log.debug("Using scan configuration ID: {}, name: '{}'.", configId, configName);
                scanRequest.setScanConfiguration(configName);
            } else {
                log.warn("Unable to retrieve scan settings for the existing project (ID {}).", projectId);
            }
        } else {
            log.debug("Project doesn't exist. Scan configuration from the global config will be used.");
        }
    }

    private static boolean entityExists(Integer id) {
        return id != null && id != UNKNOWN_INT;
    }

    public String determineTeamAndOwnerID(ScanRequest request) throws CheckmarxException {
        String ownerId;
        String namespace = Optional.ofNullable(request.getNamespace()).orElse(EMPTY_STRING);

        String team = helperService.getCxTeam(request);
        if (!ScanUtils.empty(team)) {
            if (!team.startsWith(cxProperties.getTeamPathSeparator()))
                team = cxProperties.getTeamPathSeparator().concat(team);
            log.info("Overriding team with {}", team);
            if (cxProperties.getEnableShardManager()) {
                ShardSession shard = sessionTracker.getShardSession();
                shard.setTeam(team);
                shard.setProject(request.getProject());
            }

            ownerId = determineOwnerId(request, team);

        } else {
            team = cxProperties.getTeam();
            if (!team.startsWith(cxProperties.getTeamPathSeparator()))
                team = cxProperties.getTeamPathSeparator().concat(team);
            log.info("Using Checkmarx team: {}", team);
            String fullTeamName = cxProperties.getTeam().concat(cxProperties.getTeamPathSeparator()).concat(namespace);
            if (cxProperties.getEnableShardManager()) {
                ShardSession shard = sessionTracker.getShardSession();
                shard.setTeam(fullTeamName);
                shard.setProject(request.getProject());
            }

            ownerId = determineOwnerId(request, team);
            if (cxProperties.isMultiTenant() && !ScanUtils.empty(namespace)) {
                ownerId = aquireTeamMultiTenant(request, ownerId, namespace, fullTeamName);
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

    private String determineOwnerId(ScanRequest request, String team) throws CheckmarxException {
        String ownerId;
        ownerId = (request.getClientSecret() != null)
                ? scannerClient.getTeamIdByClientSecret(team, request.getClientSecret())
                : scannerClient.getTeamId(team);
        return ownerId;
    }

    private String aquireTeamMultiTenant(ScanRequest request, String ownerId, String namespace, String fullTeamName) throws CheckmarxException {
        request.setTeam(fullTeamName);
        String tmpId = scannerClient.getTeamId(fullTeamName);
        log.info("Existing team with " + fullTeamName + " was not found. Creating one ...");
        if (tmpId.equals(UNKNOWN)) {
            try {
                ownerId = scannerClient.createTeam(ownerId, namespace);
            }catch(Exception e){
                log.error("Existing team with " + fullTeamName + " was not found.");
                ownerId = UNKNOWN;
            }
        } else {
            ownerId = tmpId;
        }
        return ownerId;
    }

    public Integer determinePresetAndProjectId(ScanRequest request, String ownerId) {
        Integer projectId = scannerClient.getProjectId(ownerId, request.getProject());
        boolean projectExists = entityExists(projectId);

        boolean needToProfile = flowProperties.isAlwaysProfile() ||
                (flowProperties.isAutoProfile() && !projectExists && !request.isScanPresetOverride());

        log.debug("Determining scan preset based on the following flags: isAlwaysProfile: {}, isAutoProfile: {}, projectExists: {}, isScanPresetOverride: {}",
                flowProperties.isAlwaysProfile(),
                flowProperties.isAutoProfile(),
                projectExists,
                request.isScanPresetOverride());

        if (needToProfile) {
            setPresetBasedOnSources(request);
        } else if (projectExists && !request.isScanPresetOverride()) {
            setPresetBasedOnExistingProject(request, projectId);
        }

        log.debug("Using preset: '{}'", request.getScanPreset());

        return projectId;
    }

    private void setPresetBasedOnExistingProject(ScanRequest request, Integer projectId) {
        log.debug("Setting scan preset based on an existing project (ID {})", projectId);
        int presetId = scannerClient.getProjectPresetId(projectId);
        if (entityExists(presetId)) {
            String preset = scannerClient.getPresetName(presetId);
            request.setScanPreset(preset);
        } else {
            log.warn("Unable to get preset for the existing project.");
        }
    }

    private void setPresetBasedOnSources(ScanRequest request) {
        log.debug("Setting scan preset based on the source repo.");
        Sources sources = getRepoContent(request);
        String preset = helperService.getPresetFromSources(sources);
        if (!StringUtils.isEmpty(preset)) {
            request.setScanPreset(preset);
        } else {
            log.warn("Unable to get preset from the source repo.");
        }
    }

    private Sources getRepoContent(ScanRequest request) {
        Sources sources = new Sources();
        switch (request.getRepoType()) {
            case GITHUB:
                sources = gitService.getRepoContent(request);
                break;
            case GITLAB:
                sources = gitLabService.getRepoContent(request);
                break;
            case BITBUCKET:
            case BITBUCKETSERVER:
                sources = bitBucketService.getRepoContent(request);
                break;
            case ADO:
                sources = adoService.getRepoContent(request);
                break;
            default:
                log.info("Nothing to profile");
                break;
        }
        return sources;
    }

    public CxScanParams prepareScanParamsObject(ScanRequest request, File cxFile, String ownerId, Integer projectId) {
        CxScanParams params = new CxScanParams()
                .teamId(ownerId)
                .withTeamName(request.getTeam())
                .projectId(projectId)
                .withProjectName(request.getProject())
                .withScanPreset(request.getScanPreset())
                .withGitUrl(request.getRepoUrlWithAuth())
                .withIncremental(request.isIncremental())
                .withForceScan(request.isForceScan())
                .withFileExclude(request.getExcludeFiles())
                .withFolderExclude(request.getExcludeFolders())
                .withScanConfiguration(request.getScanConfiguration())
                .withClientSecret(request.getClientSecret());

        if (StringUtils.isNotEmpty(request.getBranch())) {
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
                "\t- team name separator depends on Checkmarx product version specified in CxFlow configuration: (github.com/checkmarx-ltd/cx-flow/wiki/Configuration)";
    }
}