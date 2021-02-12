package com.checkmarx.flow.controller.bitbucket.server.postwebhook;

import com.checkmarx.flow.config.BitBucketProperties;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.dto.ControllerRequest;
import com.checkmarx.flow.dto.EventResponse;
import com.checkmarx.flow.service.BitBucketService;
import com.checkmarx.flow.service.ConfigurationOverrider;
import com.checkmarx.flow.service.CxScannerService;
import com.checkmarx.flow.service.FilterFactory;
import com.checkmarx.flow.service.FlowService;
import com.checkmarx.flow.service.HelperService;

import org.slf4j.Logger;
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
public class PostWebhookController {

    private static final String EVENT = "X-Event-Key";
    private static final String PUSH = EVENT + "=repo:push";
    private static final String MERGE = EVENT + "=pullrequest:created";
    private static final String MERGED = EVENT + "=pullrequest:fulfilled";
    private static final String PR_SOURCE_BRANCH_UPDATED = EVENT + "=pullrequest:updated";
    private static final String AUTH = "Authorization";
    private static final String ROOT_PATH = "/postwebhook";

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

    
    // TODO: Validate the base64 encoded credentials for the token.

    @PostMapping(value = {ROOT_PATH + "/{product}", ROOT_PATH}, headers = PUSH)
    public ResponseEntity<EventResponse> pushRequest(
            @RequestBody String body,
            @PathVariable(value = "product", required = false) String product,
            @RequestHeader(value = AUTH) String credentials,
            ControllerRequest controllerRequest
    ) {
        log.debug("PostWebhookController:pushRequest:" + credentials);
        return null;
    }

    @PostMapping(value = {ROOT_PATH + "/{product}", ROOT_PATH}, headers = MERGE)
    public ResponseEntity<EventResponse> mergeRequest(
            @RequestBody String body,
            @PathVariable(value = "product", required = false) String product,
            @RequestHeader(value = AUTH) String credentials,
            ControllerRequest controllerRequest
    ) {
        log.debug("PostWebhookController:mergeRequest");
        return null;
    }

    @PostMapping(value = {ROOT_PATH + "/{product}", ROOT_PATH}, headers = MERGED)
    public ResponseEntity<EventResponse> mergedRequest(
            @RequestBody String body,
            @PathVariable(value = "product", required = false) String product,
            @RequestHeader(value = AUTH) String credentials,
            ControllerRequest controllerRequest
    ) {
        log.debug("PostWebhookController:mergedRequest");
        return null;
    }

    @PostMapping(value = {ROOT_PATH + "/{product}", ROOT_PATH}, headers = PR_SOURCE_BRANCH_UPDATED)
    public ResponseEntity<EventResponse> prSourceBranchUpdateRequest(
            @RequestBody String body,
            @PathVariable(value = "product", required = false) String product,
            @RequestHeader(value = AUTH) String credentials,
            ControllerRequest controllerRequest
    ) {
        log.debug("PostWebhookController:prSourceBranchUpdateRequest");
        return null;
    }
    
}
