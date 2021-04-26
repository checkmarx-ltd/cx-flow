package com.checkmarx.flow.handlers.bitbucket.server;

import java.util.Locale;

import com.checkmarx.flow.dto.EventResponse;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.utils.ScanUtils;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import lombok.NonNull;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public class BitbucketServerDeleteHandler extends BitbucketServerEventHandler {


    @NonNull
    String branchNameForDelete;

    @Override
    // This implementation is currently limited.
    //
    // Does not work if using config as code because it can't get the CaC settings for team and/or project name
    // when the CaC file is deleted with the branch.  
    //
    // It will work if the project name is calculated or scripted and the team assigned to the project matches the 
    // default team in the configuration.
    public ResponseEntity<EventResponse> execute(String uid) {

        ScanRequest.Product p = ScanRequest.Product.valueOf(product.toUpperCase(Locale.ROOT));

        ScanRequest request = ScanRequest.builder()
                .application(application)
                .product(p)
                .project(controllerRequest.getProject())
                .team(controllerRequest.getTeam())
                .namespace(getNamespace())
                .repoName(repositoryName)
                .branch(ScanUtils.getBranchFromRef(branchNameForDelete))
                .build();

        webhookUtils.setScmInstance(controllerRequest, request);
        checkForConfigAsCode(request);
        request.setId(uid);    

        configProvider.getFlowService().deleteProject(request);

        return getSuccessMessage();
    }

    public static ResponseEntity<EventResponse> getSuccessMessage()
    {
        return ResponseEntity.status(HttpStatus.OK)
                .body(EventResponse.builder()
                .message("Deletion handled successfully.")
                .success(true)
                .build());
    }
    
}
