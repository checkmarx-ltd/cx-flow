package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.BugTrackersDto;
import com.checkmarx.flow.dto.ExitCode;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.ExitThrowable;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.sastscanning.ScanRequestConverter;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.config.CxPropertiesBase;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.cx.CxProject;
import com.checkmarx.sdk.exception.CheckmarxException;
import com.checkmarx.sdk.service.CxClient;
import com.checkmarx.sdk.service.CxOsaClient;
import com.cx.restclient.ScannerClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.checkmarx.flow.exception.ExitThrowable.exit;
import static com.checkmarx.sdk.config.Constants.UNKNOWN_INT;

@Service
@Slf4j
public class SastScanner extends AbstractVulnerabilityScanner {
    
    private final CxClient cxService;
    private final CxOsaClient osaService;
    private final CxProperties cxProperties;
    private final ScanRequestConverter scanRequestConverter;
    
    public SastScanner(ResultsService resultsService,
                       CxProperties cxProperties,
                       FlowProperties flowProperties,
                       CxOsaClient osaService,
                       ProjectNameGenerator projectNameGenerator,
                       CxClient cxService,
                       BugTrackersDto bugTrackersDto) {
        
        super(resultsService, flowProperties, projectNameGenerator, bugTrackersDto);
        this.osaService = osaService;
        this.cxService = cxService;
        this.scanRequestConverter = new ScanRequestConverter(projectNameGenerator.getHelperService(),flowProperties,bugTrackersDto.getGitService(),bugTrackersDto.getGitLabService(),bugTrackersDto.getBitBucketService(),bugTrackersDto.getAdoService(),bugTrackersDto.getSessionTracker(),cxService,cxProperties);
        this.cxProperties = cxProperties;
    }

    @Override
    public ScanRequestConverter getScanRequestConverter() {
        return scanRequestConverter;
    }
    
    @Override
    public boolean isEnabled() {
        boolean result = false;
        List<String> enabledScanners = flowProperties.getEnabledVulnerabilityScanners();
        if (enabledScanners == null || enabledScanners.isEmpty()) {
            log.info("None of the vulnerability scanners is enabled in the configuration. Using CxSAST scanner by default.");
            result = true;
        } else if (StringUtils.containsIgnoreCase(enabledScanners.toString(), CxProperties.CONFIG_PREFIX)) {
            result = true;
        }
        return result;
    }

    public ScannerClient getScannerClient() {
        return cxService;
    }

    public void cxParseResults(ScanRequest request, File file) throws ExitThrowable {
        try {
            ScanResults results = cxService.getReportContent(file, request.getFilter());
            resultsService.processResults(request, results, scanDetails);
            if (flowProperties.isBreakBuild() && results != null && results.getXIssues() != null && !results.getXIssues().isEmpty()) {
                log.error(ERROR_BREAK_MSG);
                exit(ExitCode.BUILD_INTERRUPTED);
            }
        } catch (MachinaException | CheckmarxException e) {
            log.error("Error occurred while processing results file", e);
            exit(3);
        }
    }
    /**
     * Process Projects in batch mode - JIRA ONLY
     */
    public void cxBatch(ScanRequest originalRequest) throws ExitThrowable {
        try {
            
            List<CxProject> projects;
            List<CompletableFuture<ScanResults>> processes = new ArrayList<>();
            //Get all projects
            if (ScanUtils.empty(originalRequest.getTeam())) {
                projects = cxService.getProjects();
            } else { //Get projects for the provided team
                String team = originalRequest.getTeam();
                if (!team.startsWith(cxProperties.getTeamPathSeparator())) {
                    team = cxProperties.getTeamPathSeparator().concat(team);
                }
                String teamId = cxService.getTeamId(team);
                projects = cxService.getProjects(teamId);
            }
            for (CxProject project : projects) {
                ScanRequest request = new ScanRequest(originalRequest);
                String name = project.getName().replaceAll("[^a-zA-Z0-9-_]+", "_");
                //TODO set team when entire instance batch mode
                projectNameGenerator.getHelperService().getShortUid(request); //update new request object with a unique id for thread log monitoring
                request.setProject(name);
                request.setApplication(name);
                processes.add(getLatestScanResultsAsync(request, project));
            }
            log.info("Waiting for processing to complete");
            processes.forEach(CompletableFuture::join);

        } catch (CheckmarxException e) {
            log.error("Error occurred while processing projects in batch mode", e);
            exit(3);
        }
    }

    public String createOsaScan(ScanRequest request, Integer projectId) throws GitAPIException, CheckmarxException {
        String osaScanId = null;
        if (Boolean.TRUE.equals(cxProperties.getEnableOsa())) {
            String path = cxProperties.getGitClonePath().concat("/").concat(UUID.randomUUID().toString());
            File pathFile = new File(path);

            Git.cloneRepository()
                    .setURI(request.getRepoUrlWithAuth())
                    .setBranch(request.getBranch())
                    .setBranchesToClone(Collections.singleton(Constants.CX_BRANCH_PREFIX.concat(request.getBranch())))
                    .setDirectory(pathFile)
                    .call();
            osaScanId = osaService.createScan(projectId, path);
        }
        return osaScanId;
    }

    @Override
    protected CxPropertiesBase getCxPropertiesBase() {
        return cxProperties;
    }

    public void deleteProject(ScanRequest request) {
        try {
            String ownerId = scanRequestConverter.determineTeamAndOwnerID(request);

            String projectName = projectNameGenerator.determineProjectName(request);
            request.setProject(projectName);

            log.info("Going to delete CxProject: {}", projectName);
            Integer projectId = scanRequestConverter.determinePresetAndProjectId(request, ownerId);

            if (canDeleteProject(projectId, request)) {
                cxService.deleteProject(projectId);
            }
        } catch (CheckmarxException e) {
            log.error("Error delete branch " + e.getMessage());
        }
    }

    private boolean canDeleteProject(Integer projectId, ScanRequest request) {
        boolean result = false;
        if (projectId == null || projectId == UNKNOWN_INT) {
            log.warn("{} project with the provided name is not found, nothing to delete.", SCAN_TYPE);
        } else {
            boolean branchIsProtected = projectNameGenerator.getHelperService().isBranchProtected(request.getBranch(),
                    flowProperties.getBranches(),
                    request);

            if (branchIsProtected) {
                log.info("Unable to delete {} project, because the corresponding repo branch is protected.", SCAN_TYPE);
            } else {
                result = true;
            }
        }
        return result;
    }
}