package com.checkmarx.flow.controller.bitbucket.cloud;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.checkmarx.flow.config.BitBucketProperties;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.config.ScmConfigOverrider;
import com.checkmarx.flow.constants.FlowConstants;
import com.checkmarx.flow.controller.WebhookController;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ControllerRequest;
import com.checkmarx.flow.dto.EventResponse;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.bitbucket.Change;
import com.checkmarx.flow.dto.bitbucket.Commit;
import com.checkmarx.flow.dto.bitbucket.MergeEvent;
import com.checkmarx.flow.dto.bitbucket.Pullrequest;
import com.checkmarx.flow.dto.bitbucket.PushEvent;
import com.checkmarx.flow.dto.bitbucket.Repository;
import com.checkmarx.flow.exception.InvalidTokenException;
import com.checkmarx.flow.service.BitBucketService;
import com.checkmarx.flow.service.ConfigurationOverrider;
import com.checkmarx.flow.service.FilterFactory;
import com.checkmarx.flow.service.FlowService;
import com.checkmarx.flow.service.GitAuthUrlGenerator;
import com.checkmarx.flow.service.HelperService;
import com.checkmarx.flow.utils.HTMLHelper;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.dto.filtering.FilterConfiguration;
import com.checkmarx.sdk.dto.sast.CxConfig;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/")
public class BitbucketCloudController extends WebhookController {

    private static final String EVENT = "X-Event-Key";
    private static final String PUSH = EVENT + "=repo:push";
    private static final String MERGE = EVENT + "=pullrequest:created";

    private final FlowProperties flowProperties;
    private final BitBucketProperties properties;
    private final JiraProperties jiraProperties;
    private final FlowService flowService;
    private final HelperService helperService;
    private final BitBucketService bitbucketService;
    private final FilterFactory filterFactory;
    private final ConfigurationOverrider configOverrider;
    private final GitAuthUrlGenerator gitAuthUrlGenerator;
    private final ScmConfigOverrider scmConfigOverrider;

    /**
     * Push Request event webhook submitted.
     */
    @PostMapping(value = {"/{product}", "/"}, headers = MERGE)
    public ResponseEntity<EventResponse> pushRequest(
            @RequestBody MergeEvent body,
            @PathVariable(value = "product", required = false) String product,
            ControllerRequest controllerRequest,
            @RequestParam(value = "token") String token

    ) {
        String uid = helperService.getShortUid();
        MDC.put(FlowConstants.MAIN_MDC_ENTRY, uid);
        validateBitBucketRequest(token);
        log.info("Processing BitBucket MERGE request");
        controllerRequest = ensureNotNull(controllerRequest);

        try {
            Repository repository = body.getRepository();
            String app = repository.getName();
            if (!ScanUtils.empty(controllerRequest.getApplication())) {
                app = controllerRequest.getApplication();
            }

            BugTracker.Type bugType = BugTracker.Type.BITBUCKETPULL;
            if (!ScanUtils.empty(controllerRequest.getBug())) {
                bugType = ScanUtils.getBugTypeEnum(controllerRequest.getBug(), flowProperties.getBugTrackerImpl());
            }

            if (controllerRequest.getAppOnly() != null) {
                flowProperties.setTrackApplicationOnly(controllerRequest.getAppOnly());
            }

            if (ScanUtils.empty(product)) {
                product = ScanRequest.Product.CX.getProduct();
            }
            ScanRequest.Product p = ScanRequest.Product.valueOf(product.toUpperCase(Locale.ROOT));
            Pullrequest pullRequest = body.getPullrequest();
            String currentBranch = pullRequest.getSource().getBranch().getName();
            String targetBranch = pullRequest.getDestination().getBranch().getName();
            List<String> branches = getBranches(controllerRequest, flowProperties);
            String hash = pullRequest.getSource().getCommit().getHash();

            BugTracker bt = ScanUtils.getBugTracker(controllerRequest.getAssignee(), bugType, jiraProperties, controllerRequest.getBug());

            FilterConfiguration filter = filterFactory.getFilter(controllerRequest, flowProperties);

            String gitUrl = repository.getLinks().getHtml().getHref().concat(".git");
            String configToken = scmConfigOverrider.determineConfigToken(properties, controllerRequest.getScmInstance());
            String gitAuthUrl = gitAuthUrlGenerator.addCredToUrl(ScanRequest.Repository.BITBUCKET, gitUrl, configToken);
            String mergeEndpoint = pullRequest.getLinks().getComments().getHref();


            ScanRequest request = ScanRequest.builder()
                    .application(app)
                    .product(p)
                    .project(controllerRequest.getProject())
                    .team(controllerRequest.getTeam())
                    .namespace(getProjectNamespace(repository))
                    .repoName(repository.getName())
                    .repoUrl(gitUrl)
                    .repoUrlWithAuth(gitAuthUrl)
                    .repoType(ScanRequest.Repository.BITBUCKET)
                    .branch(currentBranch)
                    .mergeTargetBranch(targetBranch)
                    .mergeNoteUri(mergeEndpoint)
                    .refs(Constants.CX_BRANCH_PREFIX.concat(currentBranch))
                    .email(null)
                    .scanPreset(controllerRequest.getPreset())
                    .incremental(controllerRequest.getIncremental())
                    .excludeFolders(controllerRequest.getExcludeFolders())
                    .excludeFiles(controllerRequest.getExcludeFiles())
                    .bugTracker(bt)
                    .filter(filter)
                    .hash(hash)
                    .organizationId(getOrganizationid(repository))
                    .gitUrl(gitUrl)
                    .build();

            setScmInstance(controllerRequest, request);
            fillRequestWithAdditionalData(request, repository, body.toString());
            checkForConfigAsCode(request);
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

    ) {
        String uid = helperService.getShortUid();
        MDC.put(FlowConstants.MAIN_MDC_ENTRY, uid);
        validateBitBucketRequest(token);
        controllerRequest = ensureNotNull(controllerRequest);

        try {
            Repository repository = body.getRepository();
            String app = repository.getName();
            if (!ScanUtils.empty(controllerRequest.getApplication())) {
                app = controllerRequest.getApplication();
            }

            //set the default bug tracker as per yml
            setBugTracker(flowProperties, controllerRequest);
            BugTracker.Type bugType = ScanUtils.getBugTypeEnum(controllerRequest.getBug(), flowProperties.getBugTrackerImpl());

            if (controllerRequest.getAppOnly() != null) {
                flowProperties.setTrackApplicationOnly(controllerRequest.getAppOnly());
            }

            if (ScanUtils.empty(product)) {
                product = ScanRequest.Product.CX.getProduct();
            }
            ScanRequest.Product p = ScanRequest.Product.valueOf(product.toUpperCase(Locale.ROOT));
            List<Change> changeList = body.getPush().getChanges();
            String currentBranch = changeList.get(0).getNew().getName();
            List<String> branches = getBranches(controllerRequest, flowProperties);
            String hash = changeList.get(0).getNew().getTarget().getHash();

            BugTracker bt = ScanUtils.getBugTracker(controllerRequest.getAssignee(), bugType, jiraProperties, controllerRequest.getBug());

            FilterConfiguration filter = filterFactory.getFilter(controllerRequest, flowProperties);

            /*Determine emails*/
            List<String> emails = new ArrayList<>();

            for (Change ch : changeList) {
                for (Commit c : ch.getCommits()) {
                    String author = c.getAuthor().getRaw();
                    if (!ScanUtils.empty(author)) {
                        emails.add(author);
                    }
                }
            }

            String gitUrl = repository.getLinks().getHtml().getHref().concat(".git");
            String configToken = scmConfigOverrider.determineConfigToken(properties, controllerRequest.getScmInstance());
            String gitAuthUrl = gitAuthUrlGenerator.addCredToUrl(ScanRequest.Repository.BITBUCKET, gitUrl, configToken);
            
            ScanRequest request = ScanRequest.builder()
                    .application(app)
                    .product(p)
                    .project(controllerRequest.getProject())
                    .team(controllerRequest.getTeam())
                    .namespace(getProjectNamespace(repository))
                    .repoName(repository.getName())
                    .repoUrl(gitUrl)
                    .repoUrlWithAuth(gitAuthUrl)
                    .repoType(ScanRequest.Repository.BITBUCKET)
                    .branch(currentBranch)
                    .refs(Constants.CX_BRANCH_PREFIX.concat(currentBranch))
                    .email(emails)
                    .scanPreset(controllerRequest.getPreset())
                    .incremental(controllerRequest.getIncremental())
                    .excludeFolders(controllerRequest.getExcludeFolders())
                    .excludeFiles(controllerRequest.getExcludeFiles())
                    .bugTracker(bt)
                    .filter(filter)
                    .hash(hash)
                    .organizationId(getOrganizationid(repository))
                    .gitUrl(gitUrl)
                    .build();

            setScmInstance(controllerRequest, request);
            fillRequestWithAdditionalData(request, repository, body.toString());
            checkForConfigAsCode(request);
            request.setId(uid);

            if (helperService.isBranch2Scan(request, branches)) {
                flowService.initiateAutomation(request);
            }
        } catch (IllegalArgumentException e) {
            return getBadRequestMessage(e, controllerRequest, product);
        }
        return getSuccessMessage();
    }

    private String getProjectNamespace(Repository repository) {
        return repository.getOwner().getDisplayName().replace(" ", "_");
    }

    /**
     * Token/Credential validation
     */
    private void validateBitBucketRequest(String token) {
        log.info("Validating BitBucket request token");
        if (!properties.getWebhookToken().equals(token)) {
            log.error("BitBucket request token validation failed");
            throw new InvalidTokenException();
        }
        log.info("Validation successful");
    }

    private void checkForConfigAsCode(ScanRequest request) {
        CxConfig cxConfig = bitbucketService.getCxConfigOverride(request);
        configOverrider.overrideScanRequestProperties(cxConfig, request);
    }

    private void fillRequestWithAdditionalData(ScanRequest request, Repository repository, String hookPayload) {
        String repoSelfUrl = repository.getLinks().getSelf().getHref();
        request.putAdditionalMetadata(BitBucketService.REPO_SELF_URL, repoSelfUrl);
        request.putAdditionalMetadata(HTMLHelper.WEB_HOOK_PAYLOAD, hookPayload);
    }

    private String getOrganizationid(Repository repository) {
        // E.g. "cxflowtestuser/VB_3845" ==> "cxflowtestuser"
        return StringUtils.substringBefore(repository.getFullName(), "/");
    }

}
