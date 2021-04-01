package com.checkmarx.flow.handlers.bitbucket.server;

import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.service.BitBucketService;
import com.checkmarx.flow.utils.HTMLHelper;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.Constants;
import lombok.NonNull;
import lombok.Singular;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;

@SuperBuilder
public abstract class BitbucketServerScanEventHandler extends BitbucketServerEventHandler {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BitbucketServerScanEventHandler.class);
    @Singular
    protected final List<String> emails;
    @NonNull
    protected String fromSlug;
    @NonNull
    protected String toProjectKey;
    @NonNull
    protected String toSlug;
    @NonNull
    protected String refId;
    @NonNull
    protected String browseUrl;

    protected String getGitUrl() {
        // This is messy since there is no request at the point where the URL is determined, unlike in other SCMs.
        String url = Optional.ofNullable(controllerRequest.getScmInstance())
                .map(key -> configProvider.getBitBucketProperties().getOptionalInstances().get(key).getUrl())
                .orElse(configProvider.getBitBucketProperties().getUrl());

        return url.concat("/scm/").concat(fromProjectKey.concat("/"))
                .concat(fromSlug).concat(".git");
    }

    protected String getGitAuthUrl(String gitUrl) {
        String gitAuthUrl = gitUrl.replace(Constants.HTTPS,
                Constants.HTTPS.concat(getEncodedAccessToken()).concat("@"));
        return gitAuthUrl.replace(Constants.HTTP, Constants.HTTP.concat(getEncodedAccessToken()).concat("@"));
    }

    protected String getEncodedAccessToken() {
        final String CREDENTIAL_SEPARATOR = ":";

        String token = Optional.ofNullable(controllerRequest.getScmInstance())
                .map(key -> configProvider.getBitBucketProperties().getOptionalInstances().get(key).getToken())
                .orElse(configProvider.getBitBucketProperties().getToken());

        String[] basicAuthCredentials = token.split(CREDENTIAL_SEPARATOR);
        String accessToken = basicAuthCredentials[1];

        String encodedTokenString = ScanUtils.getStringWithEncodedCharacter(accessToken);

        return basicAuthCredentials[0].concat(CREDENTIAL_SEPARATOR).concat(encodedTokenString);
    }

    protected void setBrowseUrl(ScanRequest targetRequest) {
        try {
            targetRequest.putAdditionalMetadata("BITBUCKET_BROWSE", browseUrl);
        } catch (NullPointerException e) {
            log.warn("Not able to determine file url for browsing", e);
        }
    }

    protected void fillRequestWithCommonAdditionalData(ScanRequest request, String projectKey, String slug,
                                                       String hookPayload) {
        String repoSelfUrl = getRepoSelfUrl(projectKey, slug);
        request.putAdditionalMetadata(BitBucketService.REPO_SELF_URL, repoSelfUrl);
        request.putAdditionalMetadata(HTMLHelper.WEB_HOOK_PAYLOAD, hookPayload);
    }

    protected String getRepoSelfUrl(String projectKey, String repoSlug) {

        String url = Optional.ofNullable(controllerRequest.getScmInstance())
                .map(key -> configProvider.getBitBucketProperties().getOptionalInstances().get(key).getApiUrl())
                .orElse(configProvider.getBitBucketProperties().getUrl()
                        .concat(configProvider.getBitBucketProperties().getApiPath()));

        String repoSelfUrl = url.concat(PROJECT_REPO);
        repoSelfUrl = repoSelfUrl.replace("{project}", projectKey);
        repoSelfUrl = repoSelfUrl.replace("{repo}", repoSlug);
        return repoSelfUrl;
    }

}
