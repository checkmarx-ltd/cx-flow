package com.checkmarx.flow.controller.bitbucket.server;

import java.util.List;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.controller.WebhookController;
import com.checkmarx.flow.dto.ControllerRequest;
import com.checkmarx.flow.dto.EventResponse;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.service.BitBucketService;
import com.checkmarx.flow.utils.HTMLHelper;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.dto.sast.CxConfig;

import org.slf4j.Logger;
import org.springframework.http.ResponseEntity;

import lombok.NonNull;
import lombok.Singular;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public abstract class BitbucketServerEventHandler {
    protected static final String PROJECT_REPO_PATH = "/projects/{project}/repos/{repo}";
    protected static final String MERGE_COMMENT = "/pull-requests/{id}/comments";
    protected static final String BLOCKER_COMMENT = "/pull-requests/{id}/blocker-comments";
    protected static final String BUILD_API_PATH = "/rest/build-status/latest/commits/{commit}";
    

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BitbucketServerEventHandler.class);


    public abstract ResponseEntity<EventResponse> execute();

    @NonNull
    protected String application;

    protected String product;

    @NonNull
    protected String branchFromRef;

    @NonNull
    protected String toHash;

    @NonNull
    protected String fromProjectKey;

    @NonNull
    protected String fromSlug;

    @NonNull
    protected String toProjectKey;

    @NonNull
    protected String toSlug;

    @NonNull
    protected String repositoryName;

    @NonNull
    protected String refId;

    @NonNull
    protected String browseUrl;

    @NonNull
    protected String webhookPayload;

    @Singular
    protected final List<String> emails;

    @NonNull
    protected ControllerRequest controllerRequest;

    @NonNull
    protected ConfigContextProvider configProvider;

    
    // WebhookController probably should have been a static utility class.  Doing this to avoid affecting other
    // controllers and avoid copy/paste of SCM controller code.  If this extends WebhookController,
    // WebhookController would need to be annotated with @SuperBuilder and likely would cause problems.
    // This could be removed with a big refactor.
    class WebhookUtils extends WebhookController
    {
        public ResponseEntity<EventResponse> getSuccessMessage() {
            return super.getSuccessMessage();
        }
    
        public ResponseEntity<EventResponse> getBadRequestMessage(IllegalArgumentException cause, ControllerRequest controllerRequest, String product) {
            return super.getBadRequestMessage(cause, controllerRequest, product);
        }
    
        public void setBugTracker(FlowProperties flowProperties, ControllerRequest target) {
            super.setBugTracker(flowProperties, target);
        }
    
        public List<String> getBranches(ControllerRequest request, FlowProperties flowProperties) {
            return super.getBranches(request, flowProperties);
        }
        
    
        public ControllerRequest ensureNotNull(ControllerRequest requestToCheck) {
            return super.ensureNotNull(requestToCheck);
        }
    
        public void setScmInstance(ControllerRequest controllerRequest, ScanRequest request) {
            super.setScmInstance(controllerRequest, request);
        }
    
    }

    protected WebhookUtils webhookUtils = new WebhookUtils();

    
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

    protected String getNamespace() {
        return fromProjectKey.replace(" ", "_");
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

    protected void checkForConfigAsCode(ScanRequest request) {
        CxConfig cxConfig = configProvider.getBitbucketService().getCxConfigOverride(request);
        configProvider.getConfigOverrider().overrideScanRequestProperties(cxConfig, request);
    }

}
