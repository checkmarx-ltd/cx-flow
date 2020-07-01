package com.checkmarx.flow.controller;

import com.checkmarx.flow.config.BitBucketProperties;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.dto.*;
import com.checkmarx.flow.dto.bitbucket.*;
import com.checkmarx.flow.exception.InvalidTokenException;
import com.checkmarx.flow.service.ConfigurationOverrider;
import com.checkmarx.flow.service.FilterFactory;
import com.checkmarx.flow.service.FlowService;
import com.checkmarx.flow.service.HelperService;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.dto.filtering.FilterConfiguration;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/" )
public class BitbucketCloudController extends WebhookController {

    private static final String EVENT = "X-Event-Key";
    private static final String PUSH = EVENT + "=repo:push";
    private static final String MERGE = EVENT + "=pullrequest:created";
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BitbucketCloudController.class);

    private final FlowProperties flowProperties;
    private final BitBucketProperties properties;
    private final CxProperties cxProperties;
    private final JiraProperties jiraProperties;
    private final FlowService flowService;
    private final HelperService helperService;
    private final FilterFactory filterFactory;
    private final ConfigurationOverrider configOverrider;

    /**
     * Push Request event webhook submitted.
     */
    @PostMapping(value = {"/{product}", "/"}, headers = MERGE)
    public ResponseEntity<EventResponse> pushRequest(
            @RequestBody MergeEvent body,
            @PathVariable(value = "product", required = false) String product,
            ControllerRequest controllerRequest,
            @RequestParam(value = "token") String token

    ){
        String uid = helperService.getShortUid();
        MDC.put("cx", uid);
        validateBitBucketRequest(token);
        log.info("Processing BitBucket MERGE request");
        FlowOverride o = ScanUtils.getMachinaOverride(controllerRequest.getOverride());
        controllerRequest = ensureNotNull(controllerRequest);

        try {
            Repository repository = body.getRepository();
            String app = repository.getName();
            if(!ScanUtils.empty(controllerRequest.getApplication())){
                app = controllerRequest.getApplication();
            }

            BugTracker.Type bugType = BugTracker.Type.BITBUCKETPULL;
            if (!ScanUtils.empty(controllerRequest.getBug())) {
                bugType = ScanUtils.getBugTypeEnum(controllerRequest.getBug(), flowProperties.getBugTrackerImpl());
            }

            if(controllerRequest.getAppOnly() != null){
                flowProperties.setTrackApplicationOnly(controllerRequest.getAppOnly());
            }

            if(ScanUtils.empty(product)){
                product = ScanRequest.Product.CX.getProduct();
            }
            ScanRequest.Product p = ScanRequest.Product.valueOf(product.toUpperCase(Locale.ROOT));
            Pullrequest pullRequest = body.getPullrequest();
            String currentBranch = pullRequest.getSource().getBranch().getName();
            String targetBranch = pullRequest.getDestination().getBranch().getName();
            List<String> branches = getBranches(controllerRequest, flowProperties);

            BugTracker bt = ScanUtils.getBugTracker(controllerRequest.getAssignee(), bugType, jiraProperties, controllerRequest.getBug());

            FilterConfiguration filter = filterFactory.getFilter(controllerRequest, flowProperties);

            setExclusionProperties(cxProperties, controllerRequest);

            String gitUrl = repository.getLinks().getHtml().getHref().concat(".git");
            String mergeEndpoint = pullRequest.getLinks().getComments().getHref();

            String scanPreset = cxProperties.getScanPreset();
            if(!ScanUtils.empty(controllerRequest.getPreset())){
                scanPreset = controllerRequest.getPreset();
            }

            ScanRequest request = ScanRequest.builder()
                    .application(app)
                    .product(p)
                    .project(controllerRequest.getProject())
                    .team(controllerRequest.getTeam())
                    .namespace(repository.getOwner().getDisplayName().replace(" ","_"))
                    .repoName(repository.getName())
                    .repoUrl(gitUrl)
                    .repoUrlWithAuth(gitUrl.replace(Constants.HTTPS, Constants.HTTPS.concat(properties.getToken()).concat("@")))
                    .repoType(ScanRequest.Repository.BITBUCKET)
                    .branch(currentBranch)
                    .mergeTargetBranch(targetBranch)
                    .mergeNoteUri(mergeEndpoint)
                    .refs(Constants.CX_BRANCH_PREFIX.concat(currentBranch))
                    .email(null)
                    .incremental(isScanIncremental(controllerRequest, cxProperties))
                    .scanPreset(scanPreset)
                    .excludeFolders(controllerRequest.getExcludeFolders())
                    .excludeFiles(controllerRequest.getExcludeFiles())
                    .bugTracker(bt)
                    .filter(filter)
                    .build();

            request = configOverrider.overrideMap(request, o);
            request.putAdditionalMetadata(ScanUtils.WEB_HOOK_PAYLOAD, body.toString());
            request.setId(uid);

            if (helperService.isBranch2Scan(request, branches)) {
                flowService.initiateAutomation(request);
            }

        } catch (IllegalArgumentException e) {
            return getBadRequestMessage(e, controllerRequest, product);
        }
        return getSuccessMessage();
    }


    /**
     * Receive Push event submitted from Bitbucket
     */
    @PostMapping(value = {"/{product}", "/"}, headers = PUSH)
    public ResponseEntity<EventResponse> pushRequest(
            @RequestBody PushEvent body,
            @PathVariable(value = "product", required = false) String product,
            ControllerRequest controllerRequest,
            @RequestParam(value = "token") String token

    ){
        String uid = helperService.getShortUid();
        MDC.put("cx", uid);
        validateBitBucketRequest(token);
        controllerRequest = ensureNotNull(controllerRequest);

        FlowOverride o = ScanUtils.getMachinaOverride(controllerRequest.getOverride());

        try {
            Repository repository = body.getRepository();
            String app = repository.getName();
            if(!ScanUtils.empty(controllerRequest.getApplication())){
                app = controllerRequest.getApplication();
            }

            //set the default bug tracker as per yml
            setBugTracker(flowProperties, controllerRequest);
            BugTracker.Type bugType = ScanUtils.getBugTypeEnum(controllerRequest.getBug(), flowProperties.getBugTrackerImpl());

            if(controllerRequest.getAppOnly() != null){
                flowProperties.setTrackApplicationOnly(controllerRequest.getAppOnly());
            }

            if(ScanUtils.empty(product)){
                product = ScanRequest.Product.CX.getProduct();
            }
            ScanRequest.Product p = ScanRequest.Product.valueOf(product.toUpperCase(Locale.ROOT));
            List<Change> changeList =  body.getPush().getChanges();
            String currentBranch = changeList.get(0).getNew().getName();
            List<String> branches = getBranches(controllerRequest, flowProperties);

            BugTracker bt = ScanUtils.getBugTracker(controllerRequest.getAssignee(), bugType, jiraProperties, controllerRequest.getBug());

            FilterConfiguration filter = filterFactory.getFilter(controllerRequest, flowProperties);

            setExclusionProperties(cxProperties, controllerRequest);

            /*Determine emails*/
            List<String> emails = new ArrayList<>();

            for(Change ch: changeList){
                for(Commit c: ch.getCommits()){
                    String author = c.getAuthor().getRaw();
                    if(!ScanUtils.empty(author)){
                        emails.add(author);
                    }
                }
            }

            String gitUrl = repository.getLinks().getHtml().getHref().concat(".git");

            String scanPreset = cxProperties.getScanPreset();
            if(!ScanUtils.empty(controllerRequest.getPreset())){
                scanPreset = controllerRequest.getPreset();
            }

            ScanRequest request = ScanRequest.builder()
                    .application(app)
                    .product(p)
                    .project(controllerRequest.getProject())
                    .team(controllerRequest.getTeam())
                    .namespace(repository.getOwner().getDisplayName().replace(" ","_"))
                    .repoName(repository.getName())
                    .repoUrl(gitUrl)
                    .repoUrlWithAuth(gitUrl.replace(Constants.HTTPS, Constants.HTTPS.concat(properties.getToken()).concat("@")))
                    .repoType(ScanRequest.Repository.BITBUCKET)
                    .branch(currentBranch)
                    .refs(Constants.CX_BRANCH_PREFIX.concat(currentBranch))
                    .email(emails)
                    .incremental(isScanIncremental(controllerRequest, cxProperties))
                    .scanPreset(scanPreset)
                    .excludeFolders(controllerRequest.getExcludeFolders())
                    .excludeFiles(controllerRequest.getExcludeFiles())
                    .bugTracker(bt)
                    .filter(filter)
                    .build();

            request = configOverrider.overrideMap(request, o);
            request.putAdditionalMetadata(ScanUtils.WEB_HOOK_PAYLOAD, body.toString());
            request.setId(uid);

            if (helperService.isBranch2Scan(request, branches)) {
                flowService.initiateAutomation(request);
            }
        } catch (IllegalArgumentException e) {
            return getBadRequestMessage(e, controllerRequest, product);
        }
        return getSuccessMessage();
    }

    /**
     * Token/Credential validation
     */
    private void validateBitBucketRequest(String token){
        log.info("Validating BitBucket request token");
        if(!properties.getWebhookToken().equals(token)){
            log.error("BitBucket request token validation failed");
            throw new InvalidTokenException();
        }
        log.info("Validation successful");
    }
}
