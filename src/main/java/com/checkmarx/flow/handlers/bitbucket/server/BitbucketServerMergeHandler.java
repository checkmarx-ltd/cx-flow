package com.checkmarx.flow.handlers.bitbucket.server;

import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.EventResponse;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.dto.filtering.FilterConfiguration;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@SuperBuilder
public class BitbucketServerMergeHandler extends BitbucketServerScanEventHandler {

    @NonNull
    private String currentBranch;

    @NonNull
    private String targetBranch;

    @NonNull
    private String fromRefLatestCommit;

    @NonNull
    private String pullRequestId;


    @Override
    public ResponseEntity<EventResponse> execute(String uid) {

        try {

            BugTracker.Type bugType = BugTracker.Type.BITBUCKETSERVERPULL;
            if (!ScanUtils.empty(controllerRequest.getBug())) {
                bugType = ScanUtils.getBugTypeEnum(controllerRequest.getBug(), configProvider.getFlowProperties().getBugTrackerImpl());
            }
            Optional.ofNullable(controllerRequest.getAppOnly()).ifPresent(configProvider.getFlowProperties()::setTrackApplicationOnly);

            ScanRequest.Product p = ScanRequest.Product.valueOf(product.toUpperCase(Locale.ROOT));
            List<String> branches = webhookUtils.getBranches(controllerRequest, configProvider.getFlowProperties());

            BugTracker bt = ScanUtils.getBugTracker(controllerRequest.getAssignee(), bugType,
                    configProvider.getJiraProperties(), controllerRequest.getBug());

            FilterConfiguration filter = configProvider.getFilterFactory().getFilter(controllerRequest,
                    configProvider.getFlowProperties());

            String gitUrl = getGitUrl();
            String gitAuthUrl = getGitAuthUrl(gitUrl);

            String repoSelfUrl = getRepoSelfUrl(toProjectKey, toSlug);

            String mergeEndpoint = repoSelfUrl.concat(MERGE_COMMENT);
            mergeEndpoint = mergeEndpoint.replace("{id}", pullRequestId);

            String buildStatusEndpoint = configProvider.getBitBucketProperties().getUrl().concat(BUILD_STATUS);
            buildStatusEndpoint = buildStatusEndpoint.replace("{commit}", fromRefLatestCommit);

            String blockerCommentUrl = repoSelfUrl.concat(BLOCKER_COMMENT);
            blockerCommentUrl = blockerCommentUrl.replace("{id}", pullRequestId);

            ScanRequest request = ScanRequest.builder()
                    .application(application)
                    .product(p)
                    .project(controllerRequest.getProject())
                    .team(controllerRequest.getTeam())
                    .namespace(getNamespace())
                    .repoName(repositoryName)
                    .repoUrl(gitUrl)
                    .repoUrlWithAuth(gitAuthUrl)
                    .repoType(ScanRequest.Repository.BITBUCKETSERVER)
                    .branch(currentBranch)
                    .mergeTargetBranch(targetBranch)
                    .mergeNoteUri(mergeEndpoint)
                    .refs(refId)
                    .email(null)
                    .incremental(controllerRequest.getIncremental())
                    .scanPreset(controllerRequest.getPreset())
                    .excludeFolders(controllerRequest.getExcludeFolders())
                    .excludeFiles(controllerRequest.getExcludeFiles())
                    .bugTracker(bt)
                    .filter(filter)
                    .hash(fromRefLatestCommit)
                    .build();

            webhookUtils.setScmInstance(controllerRequest, request);
            setBrowseUrl(request);
            fillRequestWithCommonAdditionalData(request, toProjectKey, toSlug, webhookPayload);
            checkForConfigAsCode(request);
            request.putAdditionalMetadata("buildStatusUrl", buildStatusEndpoint);
            request.putAdditionalMetadata("cxBaseUrl", configProvider.getCxScannerService().getProperties().getBaseUrl());
            request.putAdditionalMetadata("blocker-comment-url", blockerCommentUrl);
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
