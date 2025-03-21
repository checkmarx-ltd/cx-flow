package com.checkmarx.flow.service;

import com.checkmarx.flow.config.ADOProperties;
import com.checkmarx.flow.controller.ADOController;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.azure.AdoDetailsRequest;
import com.checkmarx.flow.dto.azure.Repository;
import com.checkmarx.flow.dto.azure.Resource;
import com.checkmarx.flow.dto.azure.ResourceContainers;
import com.checkmarx.flow.utils.HTMLHelper;
import com.checkmarx.sdk.dto.sast.CxConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ADOConfigService {
    private static final String BRANCH_DELETED_REF = StringUtils.repeat('0', 40);
    private static final int NAMESPACE_INDEX = 3;
    private static final String EMPTY_STRING = "";

    private final ADOProperties properties;
    private  final ConfigurationOverrider configOverrider;
    private final  ADOService adoService;


    public ADOConfigService (ADOProperties properties,ConfigurationOverrider configOverrider, ADOService adoService){
        this.configOverrider=configOverrider;
        this.properties=properties;
        this.adoService=adoService;
    }


    public void initAdoSpecificParams(AdoDetailsRequest request) {
        if (StringUtils.isEmpty(request.getAdoIssue())) {
            request.setAdoIssue(properties.getIssueType());
        }
        if (StringUtils.isEmpty(request.getAdoBody())) {
            request.setAdoBody(properties.getIssueBody());
        }
        if (StringUtils.isEmpty(request.getAdoOpened())) {
            request.setAdoOpened(properties.getOpenStatus());
        }
        if (StringUtils.isEmpty(request.getAdoClosed())) {
            request.setAdoClosed(properties.getClosedStatus());
        }
    }

    public void checkForConfigAsCode(ScanRequest request, String branch) {
        CxConfig cxConfig = adoService.getCxConfigOverride(request, branch);
        configOverrider.overrideScanRequestProperties(cxConfig, request);
    }

    public void fillRequestWithAdditionalData(ScanRequest request, Repository repository, String hookPayload) {
        request.putAdditionalMetadata(ADOService.REPO_ID, repository.getId());
        request.putAdditionalMetadata(ADOService.REPO_SELF_URL, repository.getUrl());
        request.putAdditionalMetadata(HTMLHelper.WEB_HOOK_PAYLOAD, hookPayload);
    }
    public String getConfigBranch(ScanRequest request, Resource resource, ADOController.Action action){
        String branch = request.getBranch();
        try{

            if (isDeleteBranchEvent(resource) && action.equals(ADOController.Action.PUSH)){
                branch = request.getDefaultBranch();
                log.debug("branch to read config-as-code: {}", branch);
            }
        }
        catch (Exception ex){
            log.info("failed to get branch for config as code. using default");
        }
        return branch;
    }


    public boolean isDeleteBranchEvent(Resource resource){
        if (resource.getRefUpdates().size() == 1){
            String newBranchRef = resource.getRefUpdates().get(0).getNewObjectId();

            if (newBranchRef.equals(BRANCH_DELETED_REF)){
                log.info("new-branch ref is empty - detect ADO DELETE event");
                return true;
            }
            return false;
        }

        int refCount = resource.getRefUpdates().size();
        log.warn("unexpected number of refUpdates in push event: {}", refCount);

        return false;
    }



    public String determineNamespace(ResourceContainers resourceContainers) {

        String namespace = EMPTY_STRING;
        try {
            log.debug("Trying to extract namespace from request body");
            String projectUrl = resourceContainers.getProject().getBaseUrl();
            namespace = projectUrl.split("/")[NAMESPACE_INDEX];
        }
        catch (Exception e){
            log.warn("can't find namespace in body resource containers: {}", e.getMessage());
        }

        log.info("using namespace: {}", namespace);
        return namespace;
    }

}
