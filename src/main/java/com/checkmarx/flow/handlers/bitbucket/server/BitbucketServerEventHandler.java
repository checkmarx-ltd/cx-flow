package com.checkmarx.flow.handlers.bitbucket.server;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.controller.WebhookController;
import com.checkmarx.flow.dto.ControllerRequest;
import com.checkmarx.flow.dto.EventResponse;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.handlers.config.BitBucketConfigContextProvider;
import com.checkmarx.sdk.dto.sast.CxConfig;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import org.springframework.http.ResponseEntity;

import java.util.List;


@SuperBuilder
public abstract class BitbucketServerEventHandler {
    protected static final String PROJECT_REPO = "/projects/{project}/repos/{repo}";
    protected static final String MERGE_COMMENT = "/pull-requests/{id}/comments";
    protected static final String BLOCKER_COMMENT = "/pull-requests/{id}/blocker-comments";
    protected static final String BUILD_STATUS = "/rest/build-status/latest/commits/{commit}";

    protected final WebhookUtils webhookUtils = new WebhookUtils();
    @NonNull
    protected String application;

    protected String product;

    @NonNull
    protected String fromProjectKey;

    @NonNull
    protected String webhookPayload;

    @NonNull
    protected String repositoryName;

    @NonNull
    protected ControllerRequest controllerRequest;

    @NonNull
    protected BitBucketConfigContextProvider configProvider;

    public abstract ResponseEntity<EventResponse> execute(String uid);

    protected String getNamespace() {
        return fromProjectKey.replace(" ", "_");
    }

    protected void checkForConfigAsCode(ScanRequest request) {
        CxConfig cxConfig = configProvider.getBitbucketService().getCxConfigOverride(request);
        configProvider.getConfigOverrider().overrideScanRequestProperties(cxConfig, request);
    }

    // WebhookController probably should have been a static utility class.  Doing this to avoid affecting other
    // controllers and avoid copy/paste of SCM controller code.  If this extends WebhookController,
    // WebhookController would need to be annotated with @SuperBuilder and likely would cause problems.
    // This could be removed with a big refactor.
    public static class WebhookUtils extends WebhookController {

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

}
