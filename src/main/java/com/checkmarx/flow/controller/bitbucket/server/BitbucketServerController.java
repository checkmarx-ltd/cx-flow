package com.checkmarx.flow.controller.bitbucket.server;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.annotation.PostConstruct;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import com.checkmarx.flow.config.BitBucketProperties;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.constants.FlowConstants;
import com.checkmarx.flow.dto.ControllerRequest;
import com.checkmarx.flow.dto.EventResponse;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.bitbucketserver.PullEvent;
import com.checkmarx.flow.dto.bitbucketserver.PushEvent;
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
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(value = "/")
@RequiredArgsConstructor
public class BitbucketServerController implements BitBucketConfigContextProvider {

    private static final String SIGNATURE = "X-Hub-Signature";
    private static final String EVENT = "X-Event-Key";
    private static final String PING = EVENT + "=diagnostics:ping";
    private static final String PUSH = EVENT + "=repo:refs_changed";
    private static final String MERGE = EVENT + "=pr:opened";
    private static final String PR_SOURCE_BRANCH_UPDATED = EVENT + "=pr:from_ref_updated";
    private static final String HMAC_ALGORITHM = "HMACSha256";
    protected static final Charset CHARSET = StandardCharsets.UTF_8;
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BitbucketServerController.class);
    protected static final int INDEX_FROM_CHANGES = 0;
    protected static final int INDEX_FROM_SELF = 0;

    private final FlowProperties flowProperties;
    private final BitBucketProperties bitBucketProperties;

    private final CxScannerService cxScannerService;
    private final JiraProperties jiraProperties;
    private final FlowService flowService;
    private final HelperService helperService;
    private final BitBucketService bitbucketService;
    private final FilterFactory filterFactory;
    private final ConfigurationOverrider configOverrider;


    private Mac hmac;

    @PostConstruct
    public void init() throws NoSuchAlgorithmException, InvalidKeyException {
        // initialize HMAC with SHA1 algorithm and secret
        if (!ScanUtils.empty(bitBucketProperties.getWebhookToken())) {
            SecretKeySpec secret = new SecretKeySpec(bitBucketProperties.getWebhookToken().getBytes(CHARSET), HMAC_ALGORITHM);
            hmac = Mac.getInstance(HMAC_ALGORITHM);
            hmac.init(secret);
        }
    }

    @PostMapping(value = {"/{product}", "/"}, headers = PING)
    public String pingEvent(
            @PathVariable(value = "product", required = false) String product) {
        log.info("Processing Bitbucket Server PING request");
        return "ok";
    }

    /**
     * Push Request event webhook submitted.
     */
    @PostMapping(value = {"/{product}", "/"}, headers = MERGE)
    public ResponseEntity<EventResponse> mergeRequest(
            @RequestBody String body,
            @PathVariable(value = "product", required = false) String product,
            @RequestHeader(value = SIGNATURE) String signature,
            ControllerRequest controllerRequest
    ) {
        return doMergeEvent(body, product, signature, controllerRequest);
    }

    /**
     * PR Source Branch Updated Request event webhook submitted.
     */
    @PostMapping(value = {"/{product}", "/"}, headers = PR_SOURCE_BRANCH_UPDATED)
    public ResponseEntity<EventResponse> prSourceBranchUpdateRequest(
            @RequestBody String body,
            @PathVariable(value = "product", required = false) String product,
            @RequestHeader(value = SIGNATURE) String signature,
            ControllerRequest controllerRequest
    ) {
        return doMergeEvent(body, product, signature, controllerRequest);
    }

    private ResponseEntity<EventResponse> doMergeEvent(String body, String product, String signature,
            ControllerRequest controllerRequest) {

        String uid = helperService.getShortUid();
        MDC.put(FlowConstants.MAIN_MDC_ENTRY, uid);

        log.info("Processing BitBucket MERGE request");
        
        verifyHmacSignature(body, signature);
        ObjectMapper mapper = new ObjectMapper();
        PullEvent event;

        try {
            event = mapper.readValue(body, PullEvent.class);
            log.debug("Successfully consumed request payload : {}",body);
        } catch (IOException e) {
            log.debug("Error occurred while consuming request payload, body {}, error {}",body,e);
            throw new MachinaRuntimeException(e);
        }

        String application = event.getPullRequest().getFromRef().getRepository().getName();

        if (!ScanUtils.empty(controllerRequest.getApplication())) {
            application = controllerRequest.getApplication();
        }

        if (ScanUtils.empty(product)) {
            product = ScanRequest.Product.CX.getProduct();
        }

        BitbucketServerEventHandler handler = BitbucketServerMergeHandler.builder()
                .controllerRequest(controllerRequest)
                .application(application)
                .currentBranch(event.getPullRequest().getFromRef().getDisplayId())
                .targetBranch(event.getPullRequest().getToRef().getDisplayId())
                .fromRefLatestCommit(event.getPullRequest().getFromRef().getLatestCommit())
                .fromProjectKey(event.getPullRequest().getFromRef().getRepository().getProject().getKey())
                .fromSlug(event.getPullRequest().getFromRef().getRepository().getSlug())
                .toProjectKey(event.getPullRequest().getToRef().getRepository().getProject().getKey())
                .toSlug(event.getPullRequest().getToRef().getRepository().getSlug())
                .pullRequestId(event.getPullRequest().getId().toString())
                .repositoryName(event.getPullRequest().getFromRef().getRepository().getName())
                .refId(event.getPullRequest().getFromRef().getId())
                .browseUrl(event.getPullRequest().getFromRef().getRepository().getLinks().getSelf().get(INDEX_FROM_SELF)
                        .getHref())
                .webhookPayload(body)
                .configProvider(this)
                .product(product)
                .build();

        return handler.execute(uid);
    }


    /**
     * Receive Push event submitted from Bitbucket
     */
    @PostMapping(value = {"/{product}", "/"}, headers = PUSH)
    public ResponseEntity<EventResponse> pushRequest(
            @RequestBody String body,
            @PathVariable(value = "product", required = false) String product,
            @RequestHeader(value = SIGNATURE) String signature,
            ControllerRequest controllerRequest

    ) {
        String uid = helperService.getShortUid();
        MDC.put(FlowConstants.MAIN_MDC_ENTRY, uid);

        log.info("Processing BitBucket PUSH request");

        verifyHmacSignature(body, signature);

        ObjectMapper mapper = new ObjectMapper();
        PushEvent event;
        log.debug(body);


        try {
            event = mapper.readValue(body, PushEvent.class);
            
        } catch (Exception e) {

            log.error(ExceptionUtils.getStackTrace(e));
            throw new MachinaRuntimeException(e);
        }

        if(event.getChanges().get(0).getType().equalsIgnoreCase("DELETE")){
            log.info("Push event is associated with a Delete branch event...ignoring request");
            return handleDeleteEvent(body,uid,event,signature,product,controllerRequest);
        }

        String application = event.getRepository().getName();

        log.info("Application {} ",application);

        if (!ScanUtils.empty(controllerRequest.getApplication())) {
            application = controllerRequest.getApplication();
        }

        if (ScanUtils.empty(product)) {
            product = ScanRequest.Product.CX.getProduct();
        }

        BitbucketServerEventHandler handler = BitbucketServerPushHandler.builder()
                .controllerRequest(controllerRequest)
                .branchFromRef(event.getChanges().get(INDEX_FROM_CHANGES).getRefId())
                .toHash(event.getChanges().get(INDEX_FROM_CHANGES).getToHash())
                .email(event.getActor().getEmailAddress()).fromProjectKey(event.getRepository().getProject().getKey())
                .fromSlug(event.getRepository().getSlug()).toProjectKey(event.getRepository().getProject().getKey())
                .toSlug(event.getRepository().getSlug()).repositoryName(event.getRepository().getName())
                .refId(event.getChanges().get(INDEX_FROM_CHANGES).getRefId())
                .browseUrl(event.getRepository().getLinks().getSelf().get(INDEX_FROM_SELF).getHref())
                .webhookPayload(body)
                .configProvider(this)
                .product(product)
                .application(application)
                .build();        

        return handler.execute(uid);
    }

    /**
     * Validates the received body using the BB hook secret.
     */
    private void verifyHmacSignature(String message, String signature) {
        byte[] sig = hmac.doFinal(message.getBytes(CHARSET));
        String computedSignature = "sha256=" + DatatypeConverter.printHexBinary(sig);
        if (!computedSignature.equalsIgnoreCase(signature)) {
            log.error("Fail to verify signature: BodySignature: {} != Signature: {}", computedSignature, signature);
            log.error("Please make sure the Webhook Secret configured on Bitbucket Server matches bitbucket.webhook-token in application.yml");
            throw new InvalidTokenException();
        }
        log.info("Signature verified");
    }

    public ResponseEntity<EventResponse> handleDeleteEvent(String body, String uid, PushEvent event, String signature, String product, ControllerRequest controllerRequest){
        log.info("Processing BitBucket DELETE branch request");
        if(flowProperties == null){
            log.error("Properties have null values");
            throw new MachinaRuntimeException();
        }

        verifyHmacSignature(body, signature);

        String application = event.getRepository().getName();
        if(!ScanUtils.empty(controllerRequest.getApplication())){
            application = controllerRequest.getApplication();
        }

        if(ScanUtils.empty(product)){
            product = ScanRequest.Product.CX.getProduct();
        }

        BitbucketServerEventHandler handler = BitbucketServerDeleteHandler.builder()
                .controllerRequest(controllerRequest)
                .branchNameForDelete(event.getChanges().get(INDEX_FROM_CHANGES).getRefId())
                .fromProjectKey(event.getRepository().getProject().getKey())
                .repositoryName(event.getRepository().getName())
                .product(product)
                .application(application)
                .webhookPayload(body)
                .configProvider(this)
                .build();

        return handler.execute(uid);
    }

    public FlowProperties getFlowProperties() {
        return flowProperties;
    }

    public BitBucketProperties getBitBucketProperties() {
        return bitBucketProperties;
    }

    public CxScannerService getCxScannerService() {
        return cxScannerService;
    }

    public JiraProperties getJiraProperties() {
        return jiraProperties;
    }

    public FlowService getFlowService() {
        return flowService;
    }

    public HelperService getHelperService() {
        return helperService;
    }

    public BitBucketService getBitbucketService() {
        return bitbucketService;
    }

    public FilterFactory getFilterFactory() {
        return filterFactory;
    }

    public ConfigurationOverrider getConfigOverrider() {
        return configOverrider;
    }

}
