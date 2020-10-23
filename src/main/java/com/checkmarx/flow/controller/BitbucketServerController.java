package com.checkmarx.flow.controller;

import com.checkmarx.flow.config.BitBucketProperties;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.constants.FlowConstants;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ControllerRequest;
import com.checkmarx.flow.dto.EventResponse;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.bitbucketserver.*;
import com.checkmarx.flow.exception.InvalidTokenException;
import com.checkmarx.flow.exception.MachinaRuntimeException;
import com.checkmarx.flow.service.*;
import com.checkmarx.flow.utils.HTMLHelper;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.dto.CxConfig;
import com.checkmarx.sdk.dto.filtering.FilterConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@RestController
@RequestMapping(value = "/")
@RequiredArgsConstructor
public class BitbucketServerController extends WebhookController {

    private static final String SIGNATURE = "X-Hub-Signature";
    private static final String EVENT = "X-Event-Key";
    private static final String PING = EVENT + "=diagnostics:ping";
    private static final String PUSH = EVENT + "=repo:refs_changed";
    private static final String MERGE = EVENT + "=pr:opened";
    private static final String MERGED = EVENT + "=pr:merged";
    private static final String PR_SOURCE_BRANCH_UPDATED = EVENT + "=pr:from_ref_updated";
    private static final String HMAC_ALGORITHM = "HMACSha256";
    private static final String PROJECT_REPO_PATH = "/projects/{project}/repos/{repo}";
    private static final String MERGE_COMMENT = "/pull-requests/{id}/comments";
    private static final String BLOCKER_COMMENT = "/pull-requests/{id}/blocker-comments";
    private static final String BUILD_API_PATH = "/rest/build-status/latest/commits/{commit}";
    private static final Charset CHARSET = StandardCharsets.UTF_8;
    private static final int INDEX_FROM_CHANGES = 0;
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BitbucketServerController.class);

    private final FlowProperties flowProperties;
    private final BitBucketProperties properties;
    private final CxProperties cxProperties;
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
        if (!ScanUtils.empty(properties.getWebhookToken())) {
            SecretKeySpec secret = new SecretKeySpec(properties.getWebhookToken().getBytes(CHARSET), HMAC_ALGORITHM);
            hmac = Mac.getInstance(HMAC_ALGORITHM);
            hmac.init(secret);
        }
    }

    @PostMapping(value = { "/{product}", "/" }, headers = PING)
    public String pingEvent(@PathVariable(value = "product", required = false) String product) {
        log.info("Processing Bitbucket Server PING request");
        return "ok";
    }

    /**
     * Push Request event webhook submitted.
     */
    @PostMapping(value = { "/{product}", "/" }, headers = MERGE)
    public ResponseEntity<EventResponse> mergeRequest(@RequestBody String body,
            @PathVariable(value = "product", required = false) String product,
            @RequestHeader(value = SIGNATURE) String signature, ControllerRequest controllerRequest) {
        return doMergeEvent(body, product, signature, controllerRequest);
    }

    /**
     * Push Request event webhook submitted.
     */
    @PostMapping(value = { "/{product}", "/" }, headers = MERGED)
    public ResponseEntity<EventResponse> mergedRequest(@RequestBody String body,
            @PathVariable(value = "product", required = false) String product,
            @RequestHeader(value = SIGNATURE) String signature, ControllerRequest controllerRequest) {
        return doMergeEvent(body, product, signature, controllerRequest);
    }

    /**
     * PR Source Branch Updated Request event webhook submitted.
     */
    @PostMapping(value = { "/{product}", "/" }, headers = PR_SOURCE_BRANCH_UPDATED)
    public ResponseEntity<EventResponse> prSourceBranchUpdateRequest(@RequestBody String body,
            @PathVariable(value = "product", required = false) String product,
            @RequestHeader(value = SIGNATURE) String signature, ControllerRequest controllerRequest) {
        return doMergeEvent(body, product, signature, controllerRequest);
    }

    private ResponseEntity<EventResponse> doMergeEvent(String body, String product, String signature,
            ControllerRequest controllerRequest) {
        String uid = helperService.getShortUid();
        MDC.put(FlowConstants.MAIN_MDC_ENTRY, uid);
        verifyHmacSignature(body, signature);
        controllerRequest = ensureNotNull(controllerRequest);

        ObjectMapper mapper = new ObjectMapper();
        PullEvent event;

        try {
            event = mapper.readValue(body, PullEvent.class);
        } catch (IOException e) {
            throw new MachinaRuntimeException(e);
        }

        log.info("Processing BitBucket MERGE request");

        try {
            PullRequest pullRequest = event.getPullRequest();
            FromRef fromRef = pullRequest.getFromRef();
            ToRef toRef = pullRequest.getToRef();
            Repository fromRefRepository = fromRef.getRepository();
            Repository_ toRefRepository = toRef.getRepository();
            String app = fromRefRepository.getName();
            if (!ScanUtils.empty(controllerRequest.getApplication())) {
                app = controllerRequest.getApplication();
            }

            BugTracker.Type bugType = BugTracker.Type.BITBUCKETSERVERPULL;
            if (!ScanUtils.empty(controllerRequest.getBug())) {
                bugType = ScanUtils.getBugTypeEnum(controllerRequest.getBug(), flowProperties.getBugTrackerImpl());
            }
            Optional.ofNullable(controllerRequest.getAppOnly()).ifPresent(flowProperties::setTrackApplicationOnly);

            if (ScanUtils.empty(product)) {
                product = ScanRequest.Product.CX.getProduct();
            }
            ScanRequest.Product p = ScanRequest.Product.valueOf(product.toUpperCase(Locale.ROOT));
            String currentBranch = fromRef.getDisplayId();
            String targetBranch = toRef.getDisplayId();
            List<String> branches = getBranches(controllerRequest, flowProperties);
            String fromRefLatestCommit = fromRef.getLatestCommit();

            BugTracker bt = ScanUtils.getBugTracker(controllerRequest.getAssignee(), bugType, jiraProperties,
                    controllerRequest.getBug());

            FilterConfiguration filter = filterFactory.getFilter(controllerRequest, flowProperties);

            setExclusionProperties(cxProperties, controllerRequest);

            String gitUrl = getGitUrl(fromRefRepository);
            String gitAuthUrl = getGitAuthUrl(gitUrl);

            String repoSelfUrl = getRepoSelfUrl(toRefRepository.getProject().getKey(), toRefRepository.getSlug());

            String mergeEndpoint = repoSelfUrl.concat(MERGE_COMMENT);
            mergeEndpoint = mergeEndpoint.replace("{id}", pullRequest.getId().toString());

            String buildStatusEndpoint = properties.getUrl().concat(BUILD_API_PATH);
            buildStatusEndpoint = buildStatusEndpoint.replace("{commit}", fromRefLatestCommit);

            String blockerCommentUrl = repoSelfUrl.concat(BLOCKER_COMMENT);
            blockerCommentUrl = blockerCommentUrl.replace("{id}", pullRequest.getId().toString());

            String scanPreset = cxProperties.getScanPreset();
            if (!ScanUtils.empty(controllerRequest.getPreset())) {
                scanPreset = controllerRequest.getPreset();
            }

            ScanRequest request = ScanRequest.builder()
                    .application(app)
                    .product(p)
                    .project(controllerRequest.getProject())
                    .team(controllerRequest.getTeam())
                    .namespace(getNamespace(fromRefRepository))
                    .repoName(fromRefRepository.getName())
                    .repoUrl(gitUrl)
                    .repoUrlWithAuth(gitAuthUrl)
                    .repoType(ScanRequest.Repository.BITBUCKETSERVER)
                    .branch(currentBranch)
                    .mergeTargetBranch(targetBranch)
                    .mergeNoteUri(mergeEndpoint)
                    .refs(fromRef.getId())
                    .email(null)
                    .incremental(isScanIncremental(controllerRequest, cxProperties))
                    .scanPreset(scanPreset)
                    .excludeFolders(controllerRequest.getExcludeFolders())
                    .excludeFiles(controllerRequest.getExcludeFiles())
                    .bugTracker(bt)
                    .filter(filter)
                    .hash(fromRefLatestCommit)
                    .build();

            setBrowseUrl(fromRefRepository, request);
            fillRequestWithCommonAdditionalData(request, toRefRepository, body);
            checkForConfigAsCode(request);
            request.putAdditionalMetadata("buildStatusUrl", buildStatusEndpoint);
            request.putAdditionalMetadata("cxBaseUrl", cxProperties.getBaseUrl());
            request.putAdditionalMetadata("blocker-comment-url", blockerCommentUrl);
            request.setId(uid);

            // only initiate scan/automation if target branch is applicable
            if (helperService.isBranch2Scan(request, branches)) {
                flowService.initiateAutomation(request);
            }
        } catch (IllegalArgumentException e) {
            return getBadRequestMessage(e, controllerRequest, product);
        }
        return getSuccessMessage();
    }

    private void setBrowseUrl(Repository repo, ScanRequest targetRequest) {
        try {
            targetRequest.putAdditionalMetadata("BITBUCKET_BROWSE", repo.getLinks().getSelf().get(0).getHref());
        } catch (NullPointerException e) {
            log.warn("Not able to determine file url for browsing", e);
        }
    }

    /**
     * Receive Push event submitted from Bitbucket
     */
    @PostMapping(value = { "/{product}", "/" }, headers = PUSH)
    public ResponseEntity<EventResponse> pushRequest(@RequestBody String body,
            @PathVariable(value = "product", required = false) String product,
            @RequestHeader(value = SIGNATURE) String signature, ControllerRequest controllerRequest

    ) {
        String uid = helperService.getShortUid();
        MDC.put(FlowConstants.MAIN_MDC_ENTRY, uid);
        verifyHmacSignature(body, signature);
        controllerRequest = ensureNotNull(controllerRequest);

        ObjectMapper mapper = new ObjectMapper();
        PushEvent event;

        try {
            event = mapper.readValue(body, PushEvent.class);
        } catch (IOException e) {
            throw new MachinaRuntimeException(e);
        }

        try {
            Repository repository = event.getRepository();
            String app = repository.getName();
            if (!ScanUtils.empty(controllerRequest.getApplication())) {
                app = controllerRequest.getApplication();
            }

            // set the default bug tracker as per yml
            setBugTracker(flowProperties, controllerRequest);
            BugTracker.Type bugType = ScanUtils.getBugTypeEnum(controllerRequest.getBug(),
                    flowProperties.getBugTrackerImpl());

            Optional.ofNullable(controllerRequest.getAppOnly()).ifPresent(flowProperties::setTrackApplicationOnly);

            if (ScanUtils.empty(product)) {
                product = ScanRequest.Product.CX.getProduct();
            }
            ScanRequest.Product p = ScanRequest.Product.valueOf(product.toUpperCase(Locale.ROOT));
            String currentBranch = ScanUtils.getBranchFromRef(event.getChanges().get(INDEX_FROM_CHANGES).getRefId());
            List<String> branches = getBranches(controllerRequest, flowProperties);
            String latestCommit = event.getChanges().get(INDEX_FROM_CHANGES).getToHash();

            BugTracker bt = ScanUtils.getBugTracker(controllerRequest.getAssignee(), bugType, jiraProperties,
                    controllerRequest.getBug());
            FilterConfiguration filter = filterFactory.getFilter(controllerRequest, flowProperties);

            setExclusionProperties(cxProperties, controllerRequest);

            List<String> emails = new ArrayList<>();
            emails.add(event.getActor().getEmailAddress());

            String gitUrl = getGitUrl(repository);
            String gitAuthUrl = getGitAuthUrl(gitUrl);

            String scanPreset = cxProperties.getScanPreset();
            if (!ScanUtils.empty(controllerRequest.getPreset())) {
                scanPreset = controllerRequest.getPreset();
            }

            ScanRequest request = ScanRequest.builder()
                    .application(app)
                    .product(p)
                    .project(controllerRequest.getProject())
                    .team(controllerRequest.getTeam())
                    .namespace(getNamespace(repository))
                    .repoName(repository.getName())
                    .repoUrl(gitUrl)
                    .repoUrlWithAuth(gitAuthUrl)
                    .repoType(ScanRequest.Repository.BITBUCKETSERVER)
                    .branch(currentBranch)
                    .refs(event.getChanges().get(0).getRefId())
                    .email(emails)
                    .incremental(isScanIncremental(controllerRequest, cxProperties))
                    .scanPreset(scanPreset)
                    .excludeFolders(controllerRequest.getExcludeFolders())
                    .excludeFiles(controllerRequest.getExcludeFiles())
                    .bugTracker(bt)
                    .filter(filter)
                    .hash(latestCommit)
                    .build();

            setBrowseUrl(repository, request);
            fillRequestWithCommonAdditionalData(request, repository, body);
            checkForConfigAsCode(request);
            request.setId(uid);
            // only initiate scan/automation if target branch is applicable
            if (helperService.isBranch2Scan(request, branches)) {
                flowService.initiateAutomation(request);
            }
        } catch (IllegalArgumentException e) {
            return getBadRequestMessage(e, controllerRequest, product);
        }
        return getSuccessMessage();
    }

    /**
     * Validates the received body using the BB hook secret.
     */
    private void verifyHmacSignature(String message, String signature) {
        byte[] sig = hmac.doFinal(message.getBytes(CHARSET));
        String computedSignature = "sha256=" + DatatypeConverter.printHexBinary(sig);
        if (!computedSignature.equalsIgnoreCase(signature)) {
            throw new InvalidTokenException();
        }
        log.info("Signature verified");
    }

    private String getEncodedAccessToken() {
        final String CREDENTIAL_SEPARATOR = ":";
        String[] basicAuthCredentials = properties.getToken().split(CREDENTIAL_SEPARATOR);
        String accessToken = basicAuthCredentials[1];

        String encodedTokenString = ScanUtils.getStringWithEncodedCharacter(accessToken);

        return basicAuthCredentials[0].concat(CREDENTIAL_SEPARATOR).concat(encodedTokenString);
    }

    private String getNamespace(Repository repo) {
        return repo.getProject().getKey().replace(" ", "_");
    }

    private String getGitAuthUrl(String gitUrl) {
        String gitAuthUrl = gitUrl.replace(Constants.HTTPS,
                Constants.HTTPS.concat(getEncodedAccessToken()).concat("@"));
        return gitAuthUrl.replace(Constants.HTTP, Constants.HTTP.concat(getEncodedAccessToken()).concat("@"));
    }

    private String getGitUrl(Repository repository) {
        return properties.getUrl().concat("/scm/").concat(repository.getProject().getKey().concat("/"))
                .concat(repository.getSlug()).concat(".git");
    }

    private void checkForConfigAsCode(ScanRequest request) {
        CxConfig cxConfig = bitbucketService.getCxConfigOverride(request);
        configOverrider.overrideScanRequestProperties(cxConfig, request);
    }

    private void fillRequestWithCommonAdditionalData(ScanRequest request, Repository_ repository, String hookPayload) {
        fillRequestWithCommonAdditionalData (request, repository.getProject().getKey(), repository.getSlug(), hookPayload);
    }

    private void fillRequestWithCommonAdditionalData(ScanRequest request, Repository repository, String hookPayload) {
        fillRequestWithCommonAdditionalData (request, repository.getProject().getKey(), repository.getSlug(), hookPayload);
    }

    private void fillRequestWithCommonAdditionalData(ScanRequest request, String projectKey, String slug, String hookPayload) {
        String repoSelfUrl = getRepoSelfUrl(projectKey, slug);
        request.putAdditionalMetadata(BitBucketService.REPO_SELF_URL, repoSelfUrl);
        request.putAdditionalMetadata(HTMLHelper.WEB_HOOK_PAYLOAD, hookPayload);
    }


    String getRepoSelfUrl(String projectKey, String repoSlug) {
        String repoSelfUrl = properties.getUrl().concat(properties.getApiPath()).concat(PROJECT_REPO_PATH);
        repoSelfUrl = repoSelfUrl.replace("{project}", projectKey);
        repoSelfUrl = repoSelfUrl.replace("{repo}", repoSlug);
        return repoSelfUrl;
    }
}
