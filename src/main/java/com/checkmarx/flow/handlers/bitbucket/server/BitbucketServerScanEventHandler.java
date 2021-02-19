package com.checkmarx.flow.handlers.bitbucket.server;

import java.util.List;

import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.service.BitBucketService;
import com.checkmarx.flow.utils.HTMLHelper;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.Constants;

import org.slf4j.Logger;

import lombok.NonNull;
import lombok.Singular;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public abstract class BitbucketServerScanEventHandler extends BitbucketServerEventHandler {
    
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

    @Singular
    protected final List<String> emails;

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BitbucketServerScanEventHandler.class);


    protected String getGitUrl() {
        return configProvider.getBitBucketProperties().getUrl().concat("/scm/").concat(fromProjectKey.concat("/"))
                .concat(fromSlug).concat(".git");
    }

    protected String getGitAuthUrl(String gitUrl) {
        String gitAuthUrl = gitUrl.replace(Constants.HTTPS,
                Constants.HTTPS.concat(getEncodedAccessToken()).concat("@"));
        return gitAuthUrl.replace(Constants.HTTP, Constants.HTTP.concat(getEncodedAccessToken()).concat("@"));
    }

    protected String getEncodedAccessToken() {
        final String CREDENTIAL_SEPARATOR = ":";
        String[] basicAuthCredentials = configProvider.getBitBucketProperties().getToken().split(CREDENTIAL_SEPARATOR);
        String accessToken = basicAuthCredentials[1];

        String encodedTokenString = ScanUtils.getStringWithEncodedCharacter(accessToken);

        return basicAuthCredentials[0].concat(CREDENTIAL_SEPARATOR).concat(encodedTokenString);
    }

    protected void setBrowseUrl(ScanRequest targetRequest) {
        try {
            // targetRequest.putAdditionalMetadata("BITBUCKET_BROWSE",
            // repo.getLinks().getSelf().get(0).getHref());
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
        String repoSelfUrl = configProvider.getBitBucketProperties().getUrl()
                .concat(configProvider.getBitBucketProperties().getApiPath()).concat(PROJECT_REPO_PATH);
        repoSelfUrl = repoSelfUrl.replace("{project}", projectKey);
        repoSelfUrl = repoSelfUrl.replace("{repo}", repoSlug);
        return repoSelfUrl;
    }
    
}
