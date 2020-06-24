package com.checkmarx.flow.controller;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ControllerRequest;
import com.checkmarx.flow.dto.EventResponse;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.github.*;
import com.checkmarx.flow.exception.InvalidTokenException;
import com.checkmarx.flow.exception.MachinaRuntimeException;
import com.checkmarx.flow.service.*;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.dto.CxConfig;
import com.checkmarx.sdk.dto.filtering.FilterConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
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
import java.util.*;

/**
 * Class used to manage Controller for GitHub WebHooks
 */
@RestController
@RequestMapping(value = "/")
@RequiredArgsConstructor
public class GitHubController {

    private static final String SIGNATURE = "X-Hub-Signature";
    private static final String EVENT = "X-GitHub-Event";
    private static final String PING = EVENT + "=ping";
    private static final String PULL = EVENT + "=pull_request";
    private static final String PUSH = EVENT + "=push";
    private static final String DELETE = EVENT + "=delete";
    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final Charset CHARSET = StandardCharsets.UTF_8;
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(GitHubController.class);
    private final GitHubProperties properties;
    private final FlowProperties flowProperties;
    private final CxProperties cxProperties;
    private final JiraProperties jiraProperties;
    private final FlowService flowService;
    private final HelperService helperService;
    private final GitHubService gitHubService;
    private final SastScanner sastScanner;
    private final FilterFactory filterFactory;
    private Mac hmac;

    @PostConstruct
    public void init() throws NoSuchAlgorithmException, InvalidKeyException {
        // initialize HMAC with SHA1 algorithm and secret
        if(properties != null && !ScanUtils.empty(properties.getWebhookToken())) {
            SecretKeySpec secret = new SecretKeySpec(properties.getWebhookToken().getBytes(CHARSET), HMAC_ALGORITHM);
            hmac = Mac.getInstance(HMAC_ALGORITHM);
            hmac.init(secret);
        }
    }

    /**
     * Ping Request event submitted (JSON)
     */
    @PostMapping(value={"/{product}", "/"}, headers = PING)
    public String pingRequest(
            @RequestBody String body,
            @PathVariable(value = "product", required = false) String product,
            @RequestHeader(value = SIGNATURE) String signature){
        log.info("Processing GitHub PING request");
        verifyHmacSignature(body, signature);

        return "ok";
    }

    /**
     * Pull Request event submitted (JSON)
     */
    @PostMapping(value={"/{product}","/"}, headers = PULL)
    public ResponseEntity<EventResponse> pullRequest(
            @RequestBody String body,
            @RequestHeader(value = SIGNATURE) String signature,
            @PathVariable(value = "product", required = false) String product,
            ControllerRequest controllerRequest
    ){
        String uid = helperService.getShortUid();
        MDC.put("cx", uid);
        log.info("Processing GitHub PULL request");
        PullEvent event;
        ObjectMapper mapper = new ObjectMapper();
        controllerRequest = Optional.ofNullable(controllerRequest)
                .orElseGet(() -> ControllerRequest.builder().build());

        try {
            event = mapper.readValue(body, PullEvent.class);
        } catch (IOException e) {
            throw new MachinaRuntimeException(e);
        }
        //verify message signature
        verifyHmacSignature(body, signature);

        try {
            String action = event.getAction();
            if(!action.equalsIgnoreCase("opened") &&
                    !action.equalsIgnoreCase("reopened") &&
                    !action.equalsIgnoreCase("synchronize")){
                log.info("Pull requested not processed.  Status was not opened ({})", action);
                return ResponseEntity.status(HttpStatus.OK).body(EventResponse.builder()
                        .message("No processing occurred for updates to Pull Request")
                        .success(true)
                        .build());
            }
            Repository repository = event.getRepository();
            String app = repository.getName();
            if(!ScanUtils.empty(controllerRequest.getApplication())){
                app = controllerRequest.getApplication();
            }

            // By default, when a pull request is opened, use the current source control provider as a bug tracker
            // (GitHub in this case). Bug tracker from the config is not used, because we only want to notify the user
            // that their code has some issues. I.e. we don't want to open real issues in the "official" bug tracker yet.
            BugTracker.Type bugType = BugTracker.Type.GITHUBPULL;

            // However, if the bug tracker is overridden in the query string, use the override value.
            if (!ScanUtils.empty(controllerRequest.getBug())) {
                bugType = ScanUtils.getBugTypeEnum(controllerRequest.getBug(), flowProperties.getBugTrackerImpl());
            }

            if (controllerRequest.getAppOnly() != null) {
                flowProperties.setTrackApplicationOnly(controllerRequest.getAppOnly());
            }

            if(ScanUtils.empty(product)){
                product = ScanRequest.Product.CX.getProduct();
            }
            ScanRequest.Product p = ScanRequest.Product.valueOf(product.toUpperCase(Locale.ROOT));
            PullRequest pullRequest = event.getPullRequest();
            String currentBranch = pullRequest.getHead().getRef();
            String targetBranch = pullRequest.getBase().getRef();
            List<String> branches = getBranches(controllerRequest.getBranch());
            BugTracker bt = ScanUtils.getBugTracker(controllerRequest.getAssignee(), bugType, jiraProperties, controllerRequest.getBug());
            FilterConfiguration filter = filterFactory.getFilter(controllerRequest.getSeverity(),
                    controllerRequest.getCwe(),
                    controllerRequest.getCategory(),
                    controllerRequest.getStatus(),
                    null,
                    flowProperties);

            setExclusionProperties(controllerRequest);
            //build request object
            String gitUrl = repository.getCloneUrl();
            String token = properties.getToken();
            log.info("Using url: {}", gitUrl);
            String gitAuthUrl = gitUrl.replace(Constants.HTTPS, Constants.HTTPS.concat(token).concat("@"));
            gitAuthUrl = gitAuthUrl.replace(Constants.HTTP, Constants.HTTP.concat(token).concat("@"));

            String scanPreset = cxProperties.getScanPreset();

            ScanRequest request = ScanRequest.builder()
                    .application(app)
                    .product(p)
                    .project(controllerRequest.getProject())
                    .team(controllerRequest.getTeam())
                    .namespace(repository.getOwner().getLogin().replace(" ","_"))
                    .repoName(repository.getName())
                    .repoUrl(repository.getCloneUrl())
                    .repoUrlWithAuth(gitAuthUrl)
                    .repoType(ScanRequest.Repository.GITHUB)
                    .branch(currentBranch)
                    .refs(Constants.CX_BRANCH_PREFIX.concat(currentBranch))
                    .mergeNoteUri(event.getPullRequest().getIssueUrl().concat("/comments"))
                    .mergeTargetBranch(targetBranch)
                    .email(null)
                    .incremental(isScanIncremental(controllerRequest))
                    .scanPreset(scanPreset)
                    .excludeFolders(controllerRequest.getExcludeFolders())
                    .excludeFiles(controllerRequest.getExcludeFiles())
                    .bugTracker(bt)
                    .filter(filter)
                    .build();

            overrideScanPreset(controllerRequest, request);

            /*Check for Config as code (cx.config) and override*/
            CxConfig cxConfig =  gitHubService.getCxConfigOverride(request);
            request = ScanUtils.overrideCxConfig(request, cxConfig, flowProperties);

            request.putAdditionalMetadata(ScanUtils.WEB_HOOK_PAYLOAD, body);
            request.putAdditionalMetadata("statuses_url", event.getPullRequest().getStatusesUrl());
            request.setId(uid);
            //only initiate scan/automation if target branch is applicable
            if(helperService.isBranch2Scan(request, branches)){
                flowService.initiateAutomation(request);
            }
        } catch (IllegalArgumentException e) {
            return getBadRequestMessage(e, controllerRequest, product);
        }

        return getSuccessResponse();
    }

    /**
     * Push Request event submitted (JSON), along with the Product (cx for example)
     */
    @PostMapping(value = {"/{product}", "/"}, headers = PUSH)
    public ResponseEntity<EventResponse> pushRequest(
            @RequestBody String body,
            @RequestHeader(value = SIGNATURE) String signature,
            @PathVariable(value = "product", required = false) String product,
            ControllerRequest controllerRequest) {
        String uid = helperService.getShortUid();
        MDC.put("cx", uid);
        log.info("Processing GitHub PUSH request");
        PushEvent event;
        ObjectMapper mapper = new ObjectMapper();
        controllerRequest = Optional.ofNullable(controllerRequest)
                .orElseGet(() -> ControllerRequest.builder().build());

        try {
            event = mapper.readValue(body, PushEvent.class);
        } catch (NullPointerException | IOException | IllegalArgumentException e) {
            throw new MachinaRuntimeException(e);
        }

        if (flowProperties == null || cxProperties == null) {
            log.error("Properties have null values");
            throw new MachinaRuntimeException();
        }
        //verify message signature
        verifyHmacSignature(body, signature);

        try {
            String app = event.getRepository().getName();
            if(!ScanUtils.empty(controllerRequest.getApplication())){
                app = controllerRequest.getApplication();
            }

            // If user has pushed their changes into an important branch (e.g. master) and the code has some issues,
            // use the bug tracker from the config. As a result, "real" issues will be opened in the bug tracker and
            // not just notifications for the user. The "push" case also includes merging a pull request.
            // See the comment for the pullRequest method for further details.
            BugTracker.Type bugType;
            // However, if the bug tracker is overridden in the query string, use the override value.
            if (ScanUtils.empty(controllerRequest.getBug())) {
                controllerRequest.setBug(flowProperties.getBugTracker());
            }
            bugType = ScanUtils.getBugTypeEnum(controllerRequest.getBug(), flowProperties.getBugTrackerImpl());

            if(controllerRequest.getAppOnly() != null){
                flowProperties.setTrackApplicationOnly(controllerRequest.getAppOnly());
            }
            if(ScanUtils.empty(product)){
                product = ScanRequest.Product.CX.getProduct();
            }
            ScanRequest.Product p = ScanRequest.Product.valueOf(product.toUpperCase(Locale.ROOT));

            //determine branch (without refs)
            String currentBranch = ScanUtils.getBranchFromRef(event.getRef());
            List<String> branches = getBranches(controllerRequest.getBranch());

            BugTracker bt = ScanUtils.getBugTracker(controllerRequest.getAssignee(), bugType, jiraProperties, controllerRequest.getBug());
            FilterConfiguration filter = filterFactory.getFilter(controllerRequest.getSeverity(), controllerRequest.getCwe(), controllerRequest.getCategory(), controllerRequest.getStatus(), null, flowProperties);

            setExclusionProperties(controllerRequest);

            //build request object
            Repository repository = event.getRepository();
            String gitUrl = repository.getCloneUrl();
            log.debug("Using url: {}", gitUrl);
            String token = properties.getToken();
            if(ScanUtils.empty(token)){
                log.error("No token was provided for Github");
                throw new MachinaRuntimeException();
            }
            String gitAuthUrl = gitUrl.replace(Constants.HTTPS, Constants.HTTPS.concat(token).concat("@"));
            gitAuthUrl = gitAuthUrl.replace(Constants.HTTP, Constants.HTTP.concat(token).concat("@"));

            String scanPreset = cxProperties.getScanPreset();

            ScanRequest request = ScanRequest.builder()
                    .application(app)
                    .product(p)
                    .project(controllerRequest.getProject())
                    .team(controllerRequest.getTeam())
                    .namespace(repository.getOwner().getName().replace(" ","_"))
                    .repoName(repository.getName())
                    .repoUrl(repository.getCloneUrl())
                    .repoUrlWithAuth(gitAuthUrl)
                    .repoType(ScanRequest.Repository.GITHUB)
                    .branch(currentBranch)
                    .defaultBranch(repository.getDefaultBranch())
                    .refs(event.getRef())
                    .email(determineEmails(event))
                    .incremental(isScanIncremental(controllerRequest))
                    .scanPreset(scanPreset)
                    .excludeFolders(controllerRequest.getExcludeFolders())
                    .excludeFiles(controllerRequest.getExcludeFiles())
                    .bugTracker(bt)
                    .filter(filter)
                    .build();

            overrideScanPreset(controllerRequest, request);

            /*Check for Config as code (cx.config) and override*/
            CxConfig cxConfig =  gitHubService.getCxConfigOverride(request);
            request = ScanUtils.overrideCxConfig(request, cxConfig, flowProperties);

            request.putAdditionalMetadata(ScanUtils.WEB_HOOK_PAYLOAD, body);
            request.setId(uid);

            //only initiate scan/automation if branch is applicable
            if(helperService.isBranch2Scan(request, branches)){
                flowService.initiateAutomation(request);
            }

        }
        catch (IllegalArgumentException e){
            return getBadRequestMessage(e, controllerRequest, product);
        }
        
        return getSuccessResponse();
    }

    private List<String> determineEmails(PushEvent event) {
        /*Determine emails*/
        List<String> emails = new ArrayList<>();
        for (Commit c : event.getCommits()) {
            if (c.getAuthor() != null && !ScanUtils.empty(c.getAuthor().getEmail())) {
                emails.add(c.getAuthor().getEmail());
            }
        }
        emails.add(event.getPusher().getEmail());
        if(event.getRepository().getOwner() != null &&
                event.getRepository().getOwner().getEmail() != null &&
                !ScanUtils.empty(event.getRepository().getOwner().getEmail().toString())){
            emails.add(event.getRepository().getOwner().getEmail().toString());
        }
        return emails;
    }

    /**
     * Delete Request event submitted (JSON), along with the Product (cx for example)
     */
    @PostMapping(value = {"/{product}", "/"}, headers = DELETE)
    public ResponseEntity<EventResponse> deleteBranchRequest(
            @RequestBody String body,
            @RequestHeader(value = SIGNATURE) String signature,
            @PathVariable(value = "product", required = false) String product,
            @RequestParam(value = "application", required = false) String application,
            @RequestParam(value = "project", required = false) String project,
            @RequestParam(value = "team", required = false) String team
    ){
        String uid = helperService.getShortUid();
        MDC.put("cx", uid);
        log.info("Processing GitHub DELETE Branch request");
        DeleteEvent event;
        ObjectMapper mapper = new ObjectMapper();

        try {
            event = mapper.readValue(body, DeleteEvent.class);
        } catch (NullPointerException | IOException | IllegalArgumentException e) {
            throw new MachinaRuntimeException(e);
        }

        if(flowProperties == null || cxProperties == null){
            log.error("Properties have null values");
            throw new MachinaRuntimeException();
        }
        //verify message signature
        verifyHmacSignature(body, signature);

        if(!event.getRefType().equalsIgnoreCase("branch")){
            log.error("Nothing to do for delete tag");
            return ResponseEntity.status(HttpStatus.OK).body(EventResponse.builder()
                    .message("Nothing to do for delete tag")
                    .success(true)
                    .build());
        }

        String app = event.getRepository().getName();
        if (!ScanUtils.empty(application)) {
            app = application;
        }

        if (ScanUtils.empty(product)) {
            product = ScanRequest.Product.CX.getProduct();
        }
        ScanRequest.Product p = ScanRequest.Product.valueOf(product.toUpperCase(Locale.ROOT));
        String currentBranch = ScanUtils.getBranchFromRef(event.getRef());
        Repository repository = event.getRepository();

        String namespace;
        if (StringUtils.isBlank(repository.getOwner().getName())) {
            namespace = repository.getOwner().getLogin();
        } else {
            namespace = repository.getOwner().getName().replace(" ", "_");
        }

        flowProperties.setAutoProfile(true);

        ScanRequest request = ScanRequest.builder()
                .application(app)
                .product(p)
                .project(project)
                .team(team)
                .namespace(namespace)
                .repoName(repository.getName())
                .repoUrl(repository.getCloneUrl())
                .repoType(ScanRequest.Repository.NA)
                .branch(currentBranch)
                .refs(event.getRef())
                .build();

        request.setScanPresetOverride(false);

        //deletes a project which is not in the middle of a scan, otherwise it will not be deleted
        sastScanner.deleteProject(request);

        log.info("Process of delete branch has finished successfully");

        return ResponseEntity.status(HttpStatus.OK).body(EventResponse.builder()
                .message("Delete Branch Successfully finished")
                .success(true)
                .build());

    }

    private List<String> getBranches(List<String> branchesFromQuery) {
        List<String> result = new ArrayList<>();
        if (!ScanUtils.empty(branchesFromQuery)) {
            result.addAll(branchesFromQuery);
        } else if (!ScanUtils.empty(flowProperties.getBranches())) {
            result.addAll(flowProperties.getBranches());
        }
        return result;
    }

    private ResponseEntity<EventResponse> getSuccessResponse() {
        return ResponseEntity.status(HttpStatus.OK).body(EventResponse.builder()
                .message("Scan Request Successfully Submitted")
                .success(true)
                .build());
    }

    private ResponseEntity<EventResponse> getBadRequestMessage(IllegalArgumentException cause, ControllerRequest controllerRequest, String product) {
        String errorMessage = String.format("Error submitting Scan Request. Product or Bugtracker option incorrect %s | %s",
                StringUtils.defaultIfEmpty(product, ""),
                StringUtils.defaultIfEmpty(controllerRequest.getBug(), ""));

        log.error(errorMessage, cause);
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(EventResponse.builder()
                .message(errorMessage)
                .success(false)
                .build());
    }

    /** Validates the received body using the Github hook secret. */
    public void verifyHmacSignature(String message, String signature) {
        if(hmac == null) {
            log.error("Hmac was not initialized. Trying to initialize...");
            try {
                init();
            } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                log.error(e.getMessage(), e);
            }
        }
        if(hmac != null) {
            if(message != null) {
                byte[] sig = hmac.doFinal(message.getBytes(CHARSET));
                String computedSignature = "sha1=" + DatatypeConverter.printHexBinary(sig);
                if (!computedSignature.equalsIgnoreCase(signature)) {
                    log.error("Message was not signed with signature provided.");
                    throw new InvalidTokenException("Invalid Credentials: Make sure webhook token is correct");
                }
                log.info("Signature verified");
            } else{
                log.error("Signature cannot be verified because message is null.");
                throw new InvalidTokenException();
            }
        } else {
            log.error("Unable to initialize Hmac. Signature cannot be verified.");
            throw new InvalidTokenException();
        }
    }

    private void setExclusionProperties(ControllerRequest target) {
        if (target.getExcludeFiles() == null && !ScanUtils.empty(cxProperties.getExcludeFiles())) {
            target.setExcludeFiles(Arrays.asList(cxProperties.getExcludeFiles().split(",")));
        }
        if (target.getExcludeFolders() == null && !ScanUtils.empty(cxProperties.getExcludeFolders())) {
            target.setExcludeFolders(Arrays.asList(cxProperties.getExcludeFolders().split(",")));
        }
    }

    private boolean isScanIncremental(ControllerRequest request) {
        return Optional.ofNullable(request.getIncremental())
                .orElse(cxProperties.getIncremental());
    }

    private void overrideScanPreset(ControllerRequest controllerRequest, ScanRequest scanRequest) {
        if (!ScanUtils.empty(controllerRequest.getPreset())) {
            scanRequest.setScanPreset(controllerRequest.getPreset());
            scanRequest.setScanPresetOverride(true);
        }
    }
}
