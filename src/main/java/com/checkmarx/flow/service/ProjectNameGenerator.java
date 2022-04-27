package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaRuntimeException;
import com.checkmarx.sdk.config.CxPropertiesBase;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;


@Getter
@Slf4j
@Service
public class ProjectNameGenerator {
    private final HelperService helperService;
    private final CxPropertiesBase cxProperties;
    private final FlowProperties flowProperties;

    public ProjectNameGenerator(HelperService helperService, CxScannerService cxScannerService, FlowProperties flowProperties) {
        this.helperService = helperService;
        this.cxProperties = cxScannerService.getProperties();
        this.flowProperties = flowProperties;
    }

    /**
     * Determines effective project name that can be used by vulnerability scanners.
     *
     * @return project name based on a scan request or a Groovy script (if present).
     */
    public String determineProjectName(ScanRequest request) {
        String projectName;
        String repoName = request.getRepoName();
        String branch = request.getBranch();
        String namespace = request.getNamespace();

        log.debug("Determining project name for vulnerability scanner.");
        String nameOverride = tryGetProjectNameFromScript(request);
        if (StringUtils.isNotEmpty(nameOverride)) {
            log.debug("Project name override is present. Using the override: {}.", nameOverride);
            projectName = nameOverride;
        } else if (cxProperties.isMultiTenant() && StringUtils.isNotEmpty(repoName)) {
            projectName = repoName;
            if (StringUtils.isNotEmpty(branch)) {
                log.debug("Multi-tenant mode is enabled. Branch is specified. Using repo name and branch.");
                projectName = projectName.concat("-").concat(branch);
            } else {
                log.debug("Multi-tenant mode is enabled. Branch is not specified. Using repo name only.");
            }
        } else {
            if (StringUtils.isNotEmpty(namespace) && StringUtils.isNotEmpty(repoName) && StringUtils.isNotEmpty(branch)) {
                log.debug("Namespace, repo name and branch are specified. Using them all.");
                projectName = namespace.concat("-").concat(repoName).concat("-").concat(branch);
            } else if (StringUtils.isNotEmpty(request.getApplication())) {
                log.debug("Using application name.");
                projectName = request.getApplication();
            } else {
                final String message = "Namespace (--namespace)/RepoName(--repo-name)/Branch(--branch) OR Application (--app) must be provided if the Project is not provided (--cx-project)";
                log.error(message);
                throw new MachinaRuntimeException(String.format("Unable to determine project name. %s", message));
            }
        }

        return normalize(projectName, flowProperties.isPreserveProjectName());
    }

    private static String normalize(String rawProjectName, boolean preserveProjectName) {
        String result = null;
        if (rawProjectName != null) {
            if (!preserveProjectName) {
                //only allow specific chars in project name in checkmarx
                if(!rawProjectName.contains("#")) {
                    result = rawProjectName.replaceAll("[^a-zA-Z0-9-_.]+", "-");
                }
                else {
                    result = rawProjectName;
                }
                if (!result.equals(rawProjectName)) {
                   log.debug("Project name ({}) has been normalized to allow only valid characters.", rawProjectName);
                }
            } else {
                result = rawProjectName;
                log.info("Project name ({}) has not been normalized.", rawProjectName);
            }
            log.info("Project name being used: {}", result);
        } else {
            log.warn("Project name returned NULL");
        }
        return result;
    }

    private String tryGetProjectNameFromScript(ScanRequest request) {
        return helperService.getCxProject(request);
    }

    public String getCxComment(ScanRequest request, String cxflowScanMsg) {
        return helperService.getCxComment(request, cxflowScanMsg);
    }
}
