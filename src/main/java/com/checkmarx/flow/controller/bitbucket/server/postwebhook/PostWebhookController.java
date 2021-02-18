package com.checkmarx.flow.controller.bitbucket.server.postwebhook;

import java.io.IOException;
import java.util.Base64;

import com.checkmarx.flow.config.BitBucketProperties;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.constants.FlowConstants;
import com.checkmarx.flow.controller.bitbucket.server.BitbucketServerEventHandler;
import com.checkmarx.flow.controller.bitbucket.server.BitbucketServerMergeHandler;
import com.checkmarx.flow.controller.bitbucket.server.BitbucketServerPushHandler;
import com.checkmarx.flow.controller.bitbucket.server.ConfigContextProvider;
import com.checkmarx.flow.dto.ControllerRequest;
import com.checkmarx.flow.dto.EventResponse;
import com.checkmarx.flow.dto.bitbucketserver.plugin.postwebhook.BitbucketPushChange;
import com.checkmarx.flow.dto.bitbucketserver.plugin.postwebhook.BitbucketPushEvent;
import com.checkmarx.flow.dto.bitbucketserver.plugin.postwebhook.BitbucketServerPullRequestEvent;
import com.checkmarx.flow.exception.InvalidTokenException;
import com.checkmarx.flow.exception.MachinaRuntimeException;
import com.checkmarx.flow.service.BitBucketService;
import com.checkmarx.flow.service.ConfigurationOverrider;
import com.checkmarx.flow.service.CxScannerService;
import com.checkmarx.flow.service.FilterFactory;
import com.checkmarx.flow.service.FlowService;
import com.checkmarx.flow.service.HelperService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(value = "/")
@RequiredArgsConstructor
public class PostWebhookController implements ConfigContextProvider {

    private static final String EVENT = "X-Event-Key";
    private static final String PUSH = EVENT + "=repo:push";
    private static final String MERGE = EVENT + "=pullrequest:created";
    private static final String MERGED = EVENT + "=pullrequest:fulfilled";
    private static final String PR_SOURCE_BRANCH_UPDATED = EVENT + "=pullrequest:updated";
    private static final String AUTH_HEADER = "Authorization";
    private static final String ROOT_PATH = "/postwebhook";
    private static final String TOKEN_PARAM = "token";
    private static final int PASSWORD_INDEX = 1;
    private static final int CREDS_INDEX = 1;
    private static final int CHANGE_INDEX = 0;
    private static final int BROWSE_URL_INDEX = 0;


    private static final Logger log = org.slf4j.LoggerFactory.getLogger(PostWebhookController.class);

    private final FlowProperties flowProperties;
    private final BitBucketProperties bitBucketProperties;
    private final CxScannerService cxScannerService;
    private final JiraProperties jiraProperties;
    private final FlowService flowService;
    private final HelperService helperService;
    private final BitBucketService bitbucketService;
    private final FilterFactory filterFactory;
    private final ConfigurationOverrider configOverrider;

    private void validateCredentials (String authHeader, String tokenParam)
    {
        if (authHeader == null && tokenParam == null)
            throw new InvalidTokenException("Basic authorization header OR token parameter is required.");

        if (tokenParam != null && tokenParam.compareTo(bitBucketProperties.getWebhookToken()) == 0)
            return;

        if (authHeader != null) {
            if (!authHeader.matches("^Basic.*") )
                throw new InvalidTokenException("Authorization method not supported.");

            String[] headerComponents = authHeader.split(" ");
            String creds = new String (Base64.getDecoder().decode (headerComponents[CREDS_INDEX]));
            String[] credComponents = creds.split(":");

            if (credComponents[PASSWORD_INDEX].compareTo(bitBucketProperties.getWebhookToken()) != 0)
                throw new InvalidTokenException();
        }
    }

    @PostMapping(value = { ROOT_PATH + "/{product}", ROOT_PATH }, headers = PUSH)
    public ResponseEntity<EventResponse> pushRequest(@RequestBody String body,
            @PathVariable(value = "product", required = false) String product,
            @RequestHeader(value = AUTH_HEADER, required = false) String credentials,
            @RequestParam(value = TOKEN_PARAM, required = false) String token, 
            ControllerRequest controllerRequest) {
        String uid = helperService.getShortUid();
        MDC.put(FlowConstants.MAIN_MDC_ENTRY, uid);

        log.info("Processing BitBucket(Post Web Hook) PUSH request");
        validateCredentials(credentials, token);

        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        BitbucketPushEvent event;

        try {
            event = mapper.readValue(body, BitbucketPushEvent.class);
            
        } catch (IOException e) {
            throw new MachinaRuntimeException(e);
        }

        // Post web hook sends a push on branch delete.  Return success for now, this
        // might be a spot to implement deletion of the SAST project in the future.
        BitbucketPushChange change = event.getPush().getChanges()[CHANGE_INDEX];
        if (change.isClosed() && change.getNewState() == null)
        {
            log.debug("Branch {} deleted in repository {} at last commit {}",
            change.getOldState().getName(),
            event.getRepository().getFullName(),
            change.getOldState().getTarget().getHash());
            return ResponseEntity.status(HttpStatus.OK).body(
                    EventResponse.builder().message("Branch deletion handled successfully.")
                    .success(true)
                    .build());
        }


        BitbucketServerEventHandler handler = BitbucketServerPushHandler.builder()
                .controllerRequest(controllerRequest)
                .application(event.getRepository().getSlug())
                .toSlug(event.getRepository().getSlug())
                .repositoryName(event.getRepository().getSlug())
                .fromSlug(event.getRepository().getSlug())
                .branchFromRef(event.getPush().getChanges()[CHANGE_INDEX].getOldState().getName())
                .toHash(event.getPush().getChanges()[CHANGE_INDEX].getNewState().getTarget().getHash())
                .email(event.getActor().getEmailAddress())
                .fromProjectKey(event.getRepository().getProject().getKey())
                .toProjectKey(event.getRepository().getProject().getKey())
                .refId(event.getPush().getChanges()[CHANGE_INDEX].getNewState().getName())
                .browseUrl(event.getRepository().getLinks().get("self").get(BROWSE_URL_INDEX).getHref() )
                .webhookPayload(body)
                .configProvider(this)
                .product(product)
                .build();        

        return handler.execute(uid);
    }

    @PostMapping(value = { ROOT_PATH + "/{product}", ROOT_PATH }, headers = MERGE)
    public ResponseEntity<EventResponse> mergeRequest(@RequestBody String body,
            @PathVariable(value = "product", required = false) String product,
            @RequestHeader(value = AUTH_HEADER, required = false) String credentials,
            @RequestParam(value = TOKEN_PARAM, required = false) String token, ControllerRequest controllerRequest) {
        
        return doMerge(body, product, credentials, token, controllerRequest, "MERGE");

    }

    @PostMapping(value = { ROOT_PATH + "/{product}", ROOT_PATH }, headers = PR_SOURCE_BRANCH_UPDATED)
    public ResponseEntity<EventResponse> prSourceBranchUpdateRequest(@RequestBody String body,
            @PathVariable(value = "product", required = false) String product,
            @RequestHeader(value = AUTH_HEADER, required = false) String credentials,
            @RequestParam(value = TOKEN_PARAM, required = false) String token, 
            ControllerRequest controllerRequest) {

        return doMerge(body, product, credentials, token, controllerRequest, "PR UPDATE");
    }

    private ResponseEntity<EventResponse> doMerge(String body,
            String product, String credentials, String token, 
            ControllerRequest controllerRequest, String eventType)
    {
        String uid = helperService.getShortUid();
        MDC.put(FlowConstants.MAIN_MDC_ENTRY, uid);
        
        log.info("Processing BitBucket(Post Web Hook) {} request", eventType);
        validateCredentials(credentials, token);
        
        ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        BitbucketServerPullRequestEvent event;

        try {
            event = mapper.readValue(body, BitbucketServerPullRequestEvent.class);
        } catch (IOException e) {
            throw new MachinaRuntimeException(e);
        }

        BitbucketServerEventHandler handler = BitbucketServerMergeHandler.builder()
                .controllerRequest(controllerRequest)

                .application(event.getPullrequest().getFromRef().getRepository().getSlug())
                .currentBranch(event.getPullrequest().getFromRef().getBranch().getName())
                .targetBranch(event.getPullrequest().getToRef().getBranch().getName())
                .fromRefLatestCommit(event.getPullrequest().getFromRef().getCommit().getHash())
                .fromProjectKey(event.getPullrequest().getFromRef().getRepository().getProject().getKey())
                .fromSlug(event.getPullrequest().getFromRef().getRepository().getSlug())
                .toProjectKey(event.getPullrequest().getToRef().getRepository().getProject().getKey())
                .toSlug(event.getPullrequest().getToRef().getRepository().getSlug())
                .pullRequestId(event.getPullrequest().getId())
                .repositoryName(event.getPullrequest().getFromRef().getRepository().getSlug())
                .refId(event.getPullrequest().getFromRef().getBranch().getName())
                .browseUrl(event.getPullrequest().getFromRef().getRepository().getLinks()
                    .get("self").get(BROWSE_URL_INDEX).getHref())
                .webhookPayload(body)
                .configProvider(this)
                .product(product)
                .build();

        return handler.execute(uid);
    }

    @Override
    public BitBucketProperties getBitBucketProperties() {
        return bitBucketProperties;
    }

    @Override
    public BitBucketService getBitbucketService() {
        return bitbucketService;
    }

    @Override
    public ConfigurationOverrider getConfigOverrider() {
        return configOverrider;
    }

    @Override
    public CxScannerService getCxScannerService() {
        return cxScannerService;
    }

    @Override
    public FilterFactory getFilterFactory() {
        return filterFactory;
    }

    @Override
    public FlowProperties getFlowProperties() {
        return flowProperties;
    }

    @Override
    public FlowService getFlowService() {
        return flowService;
    }

    @Override
    public HelperService getHelperService() {
        return this.helperService;
    }

    @Override
    public JiraProperties getJiraProperties() {
        return jiraProperties;
    }
    
}
