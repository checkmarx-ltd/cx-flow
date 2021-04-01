package com.checkmarx.flow.handlers.bitbucket.server;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.EventResponse;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.dto.filtering.FilterConfiguration;

import org.slf4j.Logger;
import org.springframework.http.ResponseEntity;

import lombok.NonNull;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public class BitbucketServerPushHandler extends BitbucketServerScanEventHandler {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BitbucketServerPushHandler.class);

    @NonNull
    protected String branchFromRef;

    @NonNull
    protected String toHash;


    @Override
    public ResponseEntity<EventResponse> execute(String uid) {

        try {

            // set the default bug tracker as per yml
            webhookUtils.setBugTracker(configProvider.getFlowProperties(), controllerRequest);
            BugTracker.Type bugType = ScanUtils.getBugTypeEnum(controllerRequest.getBug(),
                    configProvider.getFlowProperties().getBugTrackerImpl());

            Optional.ofNullable(controllerRequest.getAppOnly())
                    .ifPresent(configProvider.getFlowProperties()::setTrackApplicationOnly);

            ScanRequest.Product p = ScanRequest.Product.valueOf(product.toUpperCase(Locale.ROOT));
            String currentBranch = ScanUtils.getBranchFromRef(branchFromRef);
            List<String> branches = webhookUtils.getBranches(controllerRequest, configProvider.getFlowProperties());
            String latestCommit = toHash;

            BugTracker bt = ScanUtils.getBugTracker(controllerRequest.getAssignee(), bugType,
                    configProvider.getJiraProperties(), controllerRequest.getBug());
            FilterConfiguration filter = configProvider.getFilterFactory().getFilter(controllerRequest,
                    configProvider.getFlowProperties());

            String gitUrl = getGitUrl();
            String gitAuthUrl = getGitAuthUrl(gitUrl);

            ScanRequest request = ScanRequest.builder().application(application).product(p)
                    .project(controllerRequest.getProject())
                    .team(controllerRequest.getTeam())
                    .namespace(getNamespace())
                    .repoName(repositoryName)
                    .repoUrl(gitUrl)
                    .repoUrlWithAuth(gitAuthUrl)
                    .repoType(ScanRequest.Repository.BITBUCKETSERVER)
                    .branch(currentBranch)
                    .refs(refId)
                    .email(emails)
                    .scanPreset(controllerRequest.getPreset())
                    .incremental(controllerRequest.getIncremental())
                    .excludeFolders(controllerRequest.getExcludeFolders())
                    .excludeFiles(controllerRequest.getExcludeFiles())
                    .bugTracker(bt)
                    .filter(filter)
                    .hash(latestCommit)
                    .build();

            webhookUtils.setScmInstance(controllerRequest, request);
            setBrowseUrl(request);
            fillRequestWithCommonAdditionalData(request, toProjectKey, toSlug, webhookPayload);
            checkForConfigAsCode(request);
            request.setId(uid);
            // only initiate scan/automation if target branch is applicable
            if (configProvider.getHelperService().isBranch2Scan(request, branches)) {
                configProvider.getFlowService().initiateAutomation(request);
            }
        } catch (IllegalArgumentException e) {
            return webhookUtils.getBadRequestMessage(e, controllerRequest, product);
        }
        return webhookUtils.getSuccessMessage();
    }
}
