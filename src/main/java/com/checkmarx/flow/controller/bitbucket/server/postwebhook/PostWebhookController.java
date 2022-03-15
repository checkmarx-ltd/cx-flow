package com.checkmarx.flow.controller.bitbucket.server.postwebhook;

import java.io.IOException;
import java.util.Base64;

import com.checkmarx.flow.config.properties.BitBucketProperties;
import com.checkmarx.flow.config.properties.FlowProperties;
import com.checkmarx.flow.config.properties.JiraProperties;
import com.checkmarx.flow.constants.FlowConstants;
import com.checkmarx.flow.dto.ControllerRequest;
import com.checkmarx.flow.dto.EventResponse;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.bitbucketserver.plugin.postwebhook.BitbucketPushChange;
import com.checkmarx.flow.dto.bitbucketserver.plugin.postwebhook.BitbucketPushEvent;
import com.checkmarx.flow.dto.bitbucketserver.plugin.postwebhook.BitbucketServerPullRequestEvent;
import com.checkmarx.flow.exception.InvalidTokenException;
import com.checkmarx.flow.exception.MachinaRuntimeException;
import com.checkmarx.flow.handlers.bitbucket.server.BitbucketServerDeleteHandler;
import com.checkmarx.flow.handlers.bitbucket.server.BitbucketServerEventHandler;
import com.checkmarx.flow.handlers.bitbucket.server.BitbucketServerMergeHandler;
import com.checkmarx.flow.handlers.bitbucket.server.BitbucketServerPushHandler;
import com.checkmarx.flow.handlers.config.BitBucketConfigContextProvider;
import com.checkmarx.flow.service.BitBucketService;
import com.checkmarx.flow.service.ConfigurationOverrider;
import com.checkmarx.flow.service.CxScannerService;
import com.checkmarx.flow.service.FilterFactory;
import com.checkmarx.flow.service.FlowService;
import com.checkmarx.flow.service.HelperService;
import com.checkmarx.flow.utils.ScanUtils;
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
public class PostWebhookController implements BitBucketConfigContextProvider {

    private static final String EVENT = "X-Event-Key";
    private static final String PUSH = EVENT + "=repo:push";
    private static final String MERGE = EVENT + "=pullrequest:created";
    private static final String PR_SOURCE_BRANCH_UPDATED = EVENT + "=pullrequest:updated";
    private static final String AUTH_HEADER = "Authorization";
    private static final String ROOT = "/postwebhook";
    private static final String TOKEN_PARAM = "token";
    private static final int PASSWORD_INDEX = 1;
    private static final int CREDS_INDEX = 1;
    private static final int CHANGE_INDEX = 0;
    private static final int BROWSE_URL_INDEX = 0;
    private static final String BROWSE_LINK_NAME = "self";

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

    private ResponseEntity<EventResponse> getEmptyCommitMessage() {
        return ResponseEntity.status(HttpStatus.OK).body(EventResponse.builder()
                .message("Empty commit has been handled.")
                .success(true)
                .build());
    }


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

    @PostMapping(value = { ROOT + "/{product}", ROOT }, headers = PUSH)
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


        String application = event.getRepository().getSlug();

        if (!ScanUtils.empty(controllerRequest.getApplication())) {
            application = controllerRequest.getApplication();
        }

        if (ScanUtils.empty(product)) {
            product = ScanRequest.Product.CX.getProduct();
        }

        if (event.getPush().getChanges() == null || event.getPush().getChanges().length == 0)
        {
            log.warn("Empty commit, nothing to handle.");
            return getEmptyCommitMessage();
        }

        BitbucketPushChange change = event.getPush().getChanges()[CHANGE_INDEX];
        if (change.isClosed() && change.getNewState() == null)
        {
            return handleDelete(event, controllerRequest, product, application, body, uid);
        }



        BitbucketServerEventHandler handler = BitbucketServerPushHandler.builder()
                .controllerRequest(controllerRequest)
                .toSlug(event.getRepository().getSlug())
                .repositoryName(event.getRepository().getSlug())
                .fromSlug(event.getRepository().getSlug())
                .branchFromRef(!change.isCreated() ? change.getOldState().getName() : change.getNewState().getName() )
                .toHash(change.getNewState().getTarget().getHash())
                .email(event.getActor().getEmailAddress())
                .fromProjectKey(event.getRepository().getProject().getKey())
                .toProjectKey(event.getRepository().getProject().getKey())
                .refId(change.getNewState().getName())
                .browseUrl(event.getRepository().getLinks().get(BROWSE_LINK_NAME).get(BROWSE_URL_INDEX).getHref() )
                .webhookPayload(body)
                .configProvider(this)
                .product(product)
                .application(application)
                .build();        

        return handler.execute(uid);
    }


    private ResponseEntity<EventResponse> handleDelete(BitbucketPushEvent event, 
            ControllerRequest controllerRequest, String product, String application,
            String body, String uid) {
        

        BitbucketPushChange change = event.getPush().getChanges()[CHANGE_INDEX];

        log.debug("{} {} deleted in repository {} at last commit {}",
                change.getOldState().getType(), 
                change.getOldState().getName(), 
                event.getRepository().getFullName(),
                change.getOldState().getTarget().getHash());

        if (change.getOldState().getType().compareTo("branch") != 0)
            return BitbucketServerDeleteHandler.getSuccessMessage();

        BitbucketServerEventHandler handler = BitbucketServerDeleteHandler.builder()
                .controllerRequest(controllerRequest)
                .webhookPayload(body)
                .configProvider(this)
                .application(application)
                .product(product)
                .repositoryName(event.getRepository().getSlug())
                .branchNameForDelete(change.getOldState().getName())
                .fromProjectKey(event.getRepository().getProject().getKey())
                .build();

        return handler.execute(uid);
    }

    @PostMapping(value = { ROOT + "/{product}", ROOT }, headers = MERGE)
    public ResponseEntity<EventResponse> mergeRequest(@RequestBody String body,
            @PathVariable(value = "product", required = false) String product,
            @RequestHeader(value = AUTH_HEADER, required = false) String credentials,
            @RequestParam(value = TOKEN_PARAM, required = false) String token, ControllerRequest controllerRequest) {
        
        return doMerge(body, product, credentials, token, controllerRequest, "MERGE");

    }

    @PostMapping(value = { ROOT + "/{product}", ROOT }, headers = PR_SOURCE_BRANCH_UPDATED)
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

        String application = event.getPullrequest().getFromRef().getRepository().getSlug();

        if (!ScanUtils.empty(controllerRequest.getApplication())) {
            application = controllerRequest.getApplication();
        }

        if (ScanUtils.empty(product)) {
            product = ScanRequest.Product.CX.getProduct();
        }


        BitbucketServerEventHandler handler = BitbucketServerMergeHandler.builder()
                .controllerRequest(controllerRequest)
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
                .application(application)
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
