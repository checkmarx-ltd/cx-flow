package com.checkmarx.flow.controller;

import com.checkmarx.flow.config.BitBucketProperties;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.EventResponse;
import com.checkmarx.flow.dto.FlowOverride;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.bitbucketserver.*;
import com.checkmarx.flow.exception.InvalidTokenException;
import com.checkmarx.flow.exception.MachinaRuntimeException;
import com.checkmarx.flow.service.FilterFactory;
import com.checkmarx.flow.service.FlowService;
import com.checkmarx.flow.service.HelperService;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.dto.filtering.FilterConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.beans.ConstructorProperties;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;


@RestController
@RequestMapping(value = "/" )
public class BitbucketServerController {

    private static final String SIGNATURE = "X-Hub-Signature";
    private static final String EVENT = "X-Event-Key";
    private static final String PING = EVENT + "=diagnostics:ping";
    private static final String PUSH = EVENT + "=repo:refs_changed";
    private static final String MERGE = EVENT + "=pr:opened";
    private static final String MERGED = EVENT + "=pr:merged";
    private static final String PR_SOURCE_BRANCH_UPDATED = EVENT + "=pr:from_ref_updated";
    private static final String HMAC_ALGORITHM = "HMACSha256";
    private static final String MERGE_COMMENT = "/projects/{project}/repos/{repo}/pull-requests/{id}/comments";
    private static final String BLOCKER_COMMENT = "/projects/{project}/repos/{repo}/pull-requests/{id}/blocker-comments";
    private static final String BUILD_API_PATH = "/rest/build-status/latest/commits/{commit}";
    private static final Charset CHARSET = StandardCharsets.UTF_8;
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BitbucketServerController.class);

    private final FlowProperties flowProperties;
    private final BitBucketProperties properties;
    private final CxProperties cxProperties;
    private final JiraProperties jiraProperties;
    private final FlowService flowService;
    private final HelperService helperService;
    private Mac hmac;

    @ConstructorProperties({"flowProperties", "properties", "cxProperties", "jiraProperties", "flowService", "helperService"})
    public BitbucketServerController(FlowProperties flowProperties, BitBucketProperties properties, CxProperties cxProperties,
                                     JiraProperties jiraProperties, FlowService flowService, HelperService helperService) {
        this.flowProperties = flowProperties;
        this.properties = properties;
        this.cxProperties = cxProperties;
        this.jiraProperties = jiraProperties;
        this.flowService = flowService;
        this.helperService = helperService;
    }

    @PostConstruct
    public void init() throws NoSuchAlgorithmException, InvalidKeyException {
        // initialize HMAC with SHA1 algorithm and secret
        if(!ScanUtils.empty(properties.getWebhookToken())) {
            SecretKeySpec secret = new SecretKeySpec(properties.getWebhookToken().getBytes(CHARSET), HMAC_ALGORITHM);
            hmac = Mac.getInstance(HMAC_ALGORITHM);
            hmac.init(secret);
        }
    }

    @PostMapping(value = {"/{product}", "/"}, headers = PING)
    public String pingEvent(
            @PathVariable(value = "product", required = false) String product){
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
            @RequestParam(value = "application", required = false) String application,
            @RequestParam(value = "branch", required = false) List<String> branch,
            @RequestParam(value = "severity", required = false) List<String> severity,
            @RequestParam(value = "cwe", required = false) List<String> cwe,
            @RequestParam(value = "category", required = false) List<String> category,
            @RequestParam(value = "project", required = false) String project,
            @RequestParam(value = "team", required = false) String team,
            @RequestParam(value = "status", required = false) List<String> status,
            @RequestParam(value = "assignee", required = false) String assignee,
            @RequestParam(value = "preset", required = false) String preset,
            @RequestParam(value = "incremental", required = false) Boolean incremental,
            @RequestParam(value = "exclude-files", required = false) List<String> excludeFiles,
            @RequestParam(value = "exclude-folders", required = false) List<String> excludeFolders,
            @RequestParam(value = "override", required = false) String override,
            @RequestParam(value = "bug", required = false) String bug,
            @RequestParam(value = "app-only", required = false) Boolean appOnlyTracking
    ){
        return doMergeEvent(body,
                product,
                signature,
                application,
                branch,
                severity,
                cwe,
                category,
                project,
                team,
                status,
                assignee,
                preset,
                incremental,
                excludeFiles,
                excludeFolders,
                override,
                bug,
                appOnlyTracking);
    }

    /**
     * Push Request event webhook submitted.
     */
    @PostMapping(value = {"/{product}", "/"}, headers = MERGED)
    public ResponseEntity<EventResponse> mergedRequest(
            @RequestBody String body,
            @PathVariable(value = "product", required = false) String product,
            @RequestHeader(value = SIGNATURE) String signature,
            @RequestParam(value = "application", required = false) String application,
            @RequestParam(value = "branch", required = false) List<String> branch,
            @RequestParam(value = "severity", required = false) List<String> severity,
            @RequestParam(value = "cwe", required = false) List<String> cwe,
            @RequestParam(value = "category", required = false) List<String> category,
            @RequestParam(value = "project", required = false) String project,
            @RequestParam(value = "team", required = false) String team,
            @RequestParam(value = "status", required = false) List<String> status,
            @RequestParam(value = "assignee", required = false) String assignee,
            @RequestParam(value = "preset", required = false) String preset,
            @RequestParam(value = "incremental", required = false) Boolean incremental,
            @RequestParam(value = "exclude-files", required = false) List<String> excludeFiles,
            @RequestParam(value = "exclude-folders", required = false) List<String> excludeFolders,
            @RequestParam(value = "override", required = false) String override,
            @RequestParam(value = "bug", required = false) String bug,
            @RequestParam(value = "app-only", required = false) Boolean appOnlyTracking
    ){
        return doMergeEvent(body,
                product,
                signature,
                application,
                branch,
                severity,
                cwe,
                category,
                project,
                team,
                status,
                assignee,
                preset,
                incremental,
                excludeFiles,
                excludeFolders,
                override,
                bug,
                appOnlyTracking);
    }

    /**
     * PR Source Branch Updated Request event webhook submitted.
     */
    @PostMapping(value = {"/{product}", "/"}, headers = PR_SOURCE_BRANCH_UPDATED)
    public ResponseEntity<EventResponse> prSourceBranchUpdateRequest(
            @RequestBody String body,
            @PathVariable(value = "product", required = false) String product,
            @RequestHeader(value = SIGNATURE) String signature,
            @RequestParam(value = "application", required = false) String application,
            @RequestParam(value = "branch", required = false) List<String> branch,
            @RequestParam(value = "severity", required = false) List<String> severity,
            @RequestParam(value = "cwe", required = false) List<String> cwe,
            @RequestParam(value = "category", required = false) List<String> category,
            @RequestParam(value = "project", required = false) String project,
            @RequestParam(value = "team", required = false) String team,
            @RequestParam(value = "status", required = false) List<String> status,
            @RequestParam(value = "assignee", required = false) String assignee,
            @RequestParam(value = "preset", required = false) String preset,
            @RequestParam(value = "incremental", required = false) Boolean incremental,
            @RequestParam(value = "exclude-files", required = false) List<String> excludeFiles,
            @RequestParam(value = "exclude-folders", required = false) List<String> excludeFolders,
            @RequestParam(value = "override", required = false) String override,
            @RequestParam(value = "bug", required = false) String bug,
            @RequestParam(value = "app-only", required = false) Boolean appOnlyTracking
    ){
        return doMergeEvent(body,
                product,
                signature,
                application,
                branch,
                severity,
                cwe,
                category,
                project,
                team,
                status,
                assignee,
                preset,
                incremental,
                excludeFiles,
                excludeFolders,
                override,
                bug,
                appOnlyTracking);
    }

    private ResponseEntity<EventResponse> doMergeEvent(@RequestBody String body, @PathVariable(value = "product", required = false) String product, @RequestHeader(SIGNATURE) String signature, @RequestParam(value = "application", required = false) String application, @RequestParam(value = "branch", required = false) List<String> branch, @RequestParam(value = "severity", required = false) List<String> severity, @RequestParam(value = "cwe", required = false) List<String> cwe, @RequestParam(value = "category", required = false) List<String> category, @RequestParam(value = "project", required = false) String project, @RequestParam(value = "team", required = false) String team, @RequestParam(value = "status", required = false) List<String> status, @RequestParam(value = "assignee", required = false) String assignee, @RequestParam(value = "preset", required = false) String preset, @RequestParam(value = "incremental", required = false) Boolean incremental, @RequestParam(value = "exclude-files", required = false) List<String> excludeFiles, @RequestParam(value = "exclude-folders", required = false) List<String> excludeFolders, @RequestParam(value = "override", required = false) String override, @RequestParam(value = "bug", required = false) String bug, @RequestParam(value = "app-only", required = false) Boolean appOnlyTracking) {
        String uid = helperService.getShortUid();
        MDC.put("cx", uid);
        verifyHmacSignature(body, signature);

        FlowOverride o = ScanUtils.getMachinaOverride(override);
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
            if (!ScanUtils.empty(application)) {
                app = application;
            }

            BugTracker.Type bugType = BugTracker.Type.BITBUCKETSERVERPULL;
            if (!ScanUtils.empty(bug)) {
                bugType = ScanUtils.getBugTypeEnum(bug, flowProperties.getBugTrackerImpl());
            }
            Optional.ofNullable(appOnlyTracking).ifPresent(flowProperties::setTrackApplicationOnly);

            if (ScanUtils.empty(product)) {
                product = ScanRequest.Product.CX.getProduct();
            }
            ScanRequest.Product p = ScanRequest.Product.valueOf(product.toUpperCase(Locale.ROOT));
            String currentBranch = fromRef.getDisplayId();
            String targetBranch = toRef.getDisplayId();
            List<String> branches = new ArrayList<>();

            if (!ScanUtils.empty(branch)) {
                branches.addAll(branch);
            } else if (!ScanUtils.empty(flowProperties.getBranches())) {
                branches.addAll(flowProperties.getBranches());
            }

            BugTracker bt = ScanUtils.getBugTracker(assignee, bugType, jiraProperties, bug);

            FilterConfiguration filter = FilterFactory.getFilter(severity, cwe, category, status, flowProperties);

            if (excludeFiles == null && !ScanUtils.empty(cxProperties.getExcludeFiles())) {
                excludeFiles = Arrays.asList(cxProperties.getExcludeFiles().split(","));
            }
            if (excludeFolders == null && !ScanUtils.empty(cxProperties.getExcludeFolders())) {
                excludeFolders = Arrays.asList(cxProperties.getExcludeFolders().split(","));
            }

            String projectKey = fromRefRepository.getProject().getKey();
            String gitUrl = properties.getUrl().concat("/scm/")
                    .concat(projectKey.concat("/"))
                    .concat(fromRefRepository.getSlug()).concat(".git");

            String gitAuthUrl = gitUrl.replace(Constants.HTTPS, Constants.HTTPS.concat(getEncodedAccessToken()).concat("@"));
            gitAuthUrl = gitAuthUrl.replace(Constants.HTTP, Constants.HTTP.concat(getEncodedAccessToken()).concat("@"));

            String mergeEndpoint = properties.getUrl().concat(properties.getApiPath()).concat(MERGE_COMMENT);
            mergeEndpoint = mergeEndpoint.replace("{project}", toRefRepository.getProject().getKey());
            mergeEndpoint = mergeEndpoint.replace("{repo}", toRefRepository.getSlug());
            mergeEndpoint = mergeEndpoint.replace("{id}", pullRequest.getId().toString());

            String buildStatusEndpoint = properties.getUrl().concat(BUILD_API_PATH);
            buildStatusEndpoint = buildStatusEndpoint.replace("{commit}", fromRef.getLatestCommit());

            String blockerCommentUrl = properties.getUrl().concat(BLOCKER_COMMENT);
            blockerCommentUrl = blockerCommentUrl.replace("{project}", toRefRepository.getProject().getKey());
            blockerCommentUrl = blockerCommentUrl.replace("{repo}", toRefRepository.getSlug());
            blockerCommentUrl = blockerCommentUrl.replace("{id}", pullRequest.getId().toString());


            String scanPreset = cxProperties.getScanPreset();
            if (!ScanUtils.empty(preset)) {
                scanPreset = preset;
            }
            boolean inc = cxProperties.getIncremental();
            if (incremental != null) {
                inc = incremental;
            }

            ScanRequest request = ScanRequest.builder()
                    .application(app)
                    .product(p)
                    .project(project)
                    .team(team)
                    .namespace(projectKey.replace(" ", "_"))
                    .repoName(fromRefRepository.getName())
                    .repoUrl(gitUrl)
                    .repoUrlWithAuth(gitAuthUrl)
                    .repoType(ScanRequest.Repository.BITBUCKETSERVER)
                    .branch(currentBranch)
                    .mergeTargetBranch(targetBranch)
                    .mergeNoteUri(mergeEndpoint)
                    .refs(fromRef.getId())
                    .email(null)
                    .incremental(inc)
                    .scanPreset(scanPreset)
                    .excludeFolders(excludeFolders)
                    .excludeFiles(excludeFiles)
                    .bugTracker(bt)
                    .filter(filter)
                    .build();

            request = ScanUtils.overrideMap(request, o);
            request.putAdditionalMetadata(ScanUtils.WEB_HOOK_PAYLOAD, body);
            request.putAdditionalMetadata("buildStatusUrl", buildStatusEndpoint);
            request.putAdditionalMetadata("cxBaseUrl", cxProperties.getBaseUrl());
            request.putAdditionalMetadata("blocker-comment-url", blockerCommentUrl);
            request.setId(uid);
            try {
                request.putAdditionalMetadata("BITBUCKET_BROWSE", fromRefRepository.getLinks().getSelf().get(0).getHref());
            } catch (NullPointerException e) {
                log.warn("Not able to determine file url for browsing", e);
            }
            //only initiate scan/automation if target branch is applicable
            if (helperService.isBranch2Scan(request, branches)) {
                flowService.initiateAutomation(request);
            }


        } catch (IllegalArgumentException e) {
            String errorMessage = "Error submitting Scan Request.  Product or Bugtracker option incorrect ".concat(product != null ? product : "").concat(" | ").concat(bug != null ? bug : "");
            log.error(errorMessage,e );
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(EventResponse.builder()
                    .message(errorMessage)
                    .success(false)
                    .build());
        }
        return ResponseEntity.status(HttpStatus.OK).body(EventResponse.builder()
                .message("Scan Request Successfully Submitted")
                .success(true)
                .build());
    }




    /**
     * Receive Push event submitted from Bitbucket
     */
    @PostMapping(value = {"/{product}", "/"}, headers = PUSH)
    public ResponseEntity<EventResponse> pushRequest(
            @RequestBody String body,
            @PathVariable(value = "product", required = false) String product,
            @RequestHeader(value = SIGNATURE) String signature,
            @RequestParam(value = "application", required = false) String application,
            @RequestParam(value = "branch", required = false) List<String> branch,
            @RequestParam(value = "severity", required = false) List<String> severity,
            @RequestParam(value = "cwe", required = false) List<String> cwe,
            @RequestParam(value = "category", required = false) List<String> category,
            @RequestParam(value = "project", required = false) String project,
            @RequestParam(value = "team", required = false) String team,
            @RequestParam(value = "status", required = false) List<String> status,
            @RequestParam(value = "assignee", required = false) String assignee,
            @RequestParam(value = "preset", required = false) String preset,
            @RequestParam(value = "incremental", required = false) Boolean incremental,
            @RequestParam(value = "exclude-files", required = false) List<String> excludeFiles,
            @RequestParam(value = "exclude-folders", required = false) List<String> excludeFolders,
            @RequestParam(value = "override", required = false) String override,
            @RequestParam(value = "bug", required = false) String bug,
            @RequestParam(value = "app-only", required = false) Boolean appOnlyTracking

    ){
        String uid = helperService.getShortUid();
        MDC.put("cx", uid);
        verifyHmacSignature(body, signature);

        FlowOverride o = ScanUtils.getMachinaOverride(override);
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
            if(!ScanUtils.empty(application)){
                app = application;
            }

            //set the default bug tracker as per yml
            BugTracker.Type bugType;
            if (ScanUtils.empty(bug)) {
                bug =  flowProperties.getBugTracker();
            }
            bugType = ScanUtils.getBugTypeEnum(bug, flowProperties.getBugTrackerImpl());

            Optional.ofNullable(appOnlyTracking).ifPresent(flowProperties::setTrackApplicationOnly);

            if(ScanUtils.empty(product)){
                product = ScanRequest.Product.CX.getProduct();
            }
            ScanRequest.Product p = ScanRequest.Product.valueOf(product.toUpperCase(Locale.ROOT));
            String currentBranch = ScanUtils.getBranchFromRef(event.getChanges().get(0).getRefId());
            List<String> branches = new ArrayList<>();

            if(!ScanUtils.empty(branch)){
                branches.addAll(branch);
            }
            else if(!ScanUtils.empty(flowProperties.getBranches())){
                branches.addAll(flowProperties.getBranches());
            }

            BugTracker bt = ScanUtils.getBugTracker(assignee, bugType, jiraProperties, bug);
            FilterConfiguration filter = FilterFactory.getFilter(severity, cwe, category, status, flowProperties);

            if(excludeFiles == null && !ScanUtils.empty(cxProperties.getExcludeFiles())){
                excludeFiles = Arrays.asList(cxProperties.getExcludeFiles().split(","));
            }
            if(excludeFolders == null && !ScanUtils.empty(cxProperties.getExcludeFolders())){
                excludeFolders = Arrays.asList(cxProperties.getExcludeFolders().split(","));
            }

            List<String> emails = new ArrayList<>();

            emails.add(event.getActor().getEmailAddress());

            String projectKey = repository.getProject().getKey();
            String gitUrl = properties.getUrl().concat("/scm/")
                    .concat(projectKey.concat("/"))
                    .concat(repository.getSlug()).concat(".git");

            String gitAuthUrl = gitUrl.replace(Constants.HTTPS, Constants.HTTPS.concat(getEncodedAccessToken()).concat("@"));
            gitAuthUrl = gitAuthUrl.replace(Constants.HTTP, Constants.HTTP.concat(getEncodedAccessToken()).concat("@"));

            String scanPreset = cxProperties.getScanPreset();
            if(!ScanUtils.empty(preset)){
                scanPreset = preset;
            }
            boolean inc = cxProperties.getIncremental();
            if(incremental != null){
                inc = incremental;
            }

            ScanRequest request = ScanRequest.builder()
                    .application(app)
                    .product(p)
                    .project(project)
                    .team(team)
                    .namespace(projectKey.replace(" ","_"))
                    .repoName(repository.getName())
                    .repoUrl(gitUrl)
                    .repoUrlWithAuth(gitAuthUrl)
                    .repoType(ScanRequest.Repository.BITBUCKETSERVER)
                    .branch(currentBranch)
                    .refs(event.getChanges().get(0).getRefId())
                    .email(emails)
                    .incremental(inc)
                    .scanPreset(scanPreset)
                    .excludeFolders(excludeFolders)
                    .excludeFiles(excludeFiles)
                    .bugTracker(bt)
                    .filter(filter)
                    .build();
            try {
                request.putAdditionalMetadata("BITBUCKET_BROWSE", repository.getLinks().getSelf().get(0).getHref());
            }catch (NullPointerException e){
                log.warn("Not able to determine file url for browsing", e);
            }
            request = ScanUtils.overrideMap(request, o);
            request.putAdditionalMetadata(ScanUtils.WEB_HOOK_PAYLOAD, body);
            request.setId(uid);
            //only initiate scan/automation if target branch is applicable
            if(helperService.isBranch2Scan(request, branches)){
                flowService.initiateAutomation(request);
            }


        }catch (IllegalArgumentException e){
            String errorMessage = "Error submitting Scan Request.  Product or Bugtracker option incorrect ".concat(product != null ? product : "").concat(" | ").concat(bug != null ? bug : "");
            log.error(errorMessage, e);
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(EventResponse.builder()
                    .message(errorMessage)
                    .success(false)
                    .build());
        }
        return ResponseEntity.status(HttpStatus.OK).body(EventResponse.builder()
                .message("Scan Request Successfully Submitted")
                .success(true)
                .build());
    }

    /** Validates the received body using the BB hook secret. */
    private void verifyHmacSignature(String message, String signature) {
        byte[] sig = hmac.doFinal(message.getBytes(CHARSET));
        String computedSignature = "sha256=" + DatatypeConverter.printHexBinary(sig);
        if (!computedSignature.equalsIgnoreCase(signature)) {
            throw new InvalidTokenException();
        }
        log.info("Signature verified");
    }

    private String getEncodedAccessToken() {
        String[] basicAuthCredentials = properties.getToken().split(":");
        String accessToken = basicAuthCredentials[1];

        String encodedTokenString =  ScanUtils.getStringWithEncodedCharacter(accessToken);

         return basicAuthCredentials[0].concat(":").concat(encodedTokenString);

    }

}


