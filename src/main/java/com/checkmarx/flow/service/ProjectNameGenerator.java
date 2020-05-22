package com.checkmarx.flow.service;

import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaRuntimeException;
import com.checkmarx.sdk.config.CxProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectNameGenerator {
    private final HelperService helperService;
    private final CxProperties cxProperties;
    private final ExternalScriptService scriptService;

    /**
     * Determines effective project name that can be used by vulnerability scanners.
     * @return project name based on a scan request or a Groovy script (if present).
     */
    public String determineProjectName(ScanRequest request) {
        String projectName;
        String repoName = request.getRepoName();
        String branch = request.getBranch();
        String namespace = request.getNamespace();

        String nameOverride = tryGetProjectNameFromScript(request);
        if (StringUtils.isNotEmpty(nameOverride)) {
            projectName = nameOverride;
        } else if (cxProperties.isMultiTenant() && StringUtils.isNotEmpty(repoName)) {
            projectName = repoName;
            if (StringUtils.isNotEmpty(branch)) {
                projectName = projectName.concat("-").concat(branch);
            }
        } else {
            if (StringUtils.isNotEmpty(namespace) && StringUtils.isNotEmpty(repoName) && StringUtils.isNotEmpty(branch)) {
                projectName = namespace.concat("-").concat(repoName).concat("-").concat(branch);
            } else if (StringUtils.isNotEmpty(request.getApplication())) {
                projectName = request.getApplication();
            } else {
                final String message = "Namespace (--namespace)/RepoName(--repo-name)/Branch(--branch) OR Application (--app) must be provided if the Project is not provided (--cx-project)";
                log.error(message);
                throw new MachinaRuntimeException(String.format("Unable to determine project name. %s", message));
            }
        }

        if (projectName != null) {
            //only allow specific chars in project name in checkmarx
            projectName = projectName.replaceAll("[^a-zA-Z0-9-_.]+", "-");
            log.info("Project Name being used {}", projectName);
        } else {
            log.warn("Project name returned NULL");
        }

        return projectName;
    }

    private String tryGetProjectNameFromScript(ScanRequest request) {
        String scriptFile = cxProperties.getProjectScript();
        String project = request.getProject();
        //note:  if script is provided, it is highest priority
        if (StringUtils.isNotEmpty(scriptFile)) {
            log.info("executing external script to determine the Project in Checkmarx to be used ({})", scriptFile);
            try {
                String script = helperService.getStringFromFile(scriptFile);
                HashMap<String, Object> bindings = new HashMap<>();
                bindings.put("request", request);
                Object result = scriptService.runScript(script, bindings);
                if (result instanceof String) {
                    return ((String) result);
                }
            } catch (IOException e) {
                log.error("Error reading script file for checkmarx project {}", scriptFile, e);
            }
        } else if (StringUtils.isNotEmpty(project)) {
            return project;
        }
        return null;  //null will indicate no override of team will take place
    }
}
