package com.checkmarx.flow.controller;

import com.checkmarx.flow.config.*;
import com.checkmarx.flow.constants.FlowConstants;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ControllerRequest;
import com.checkmarx.flow.dto.EventResponse;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.github.*;
import com.checkmarx.flow.exception.InvalidTokenException;
import com.checkmarx.flow.exception.MachinaRuntimeException;
import com.checkmarx.flow.service.*;
import com.checkmarx.flow.utils.HTMLHelper;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.dto.sast.CxConfig;
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
public class GitHubController extends WebhookController {

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
    private final JiraProperties jiraProperties;
    private final FlowService flowService;
    private final HelperService helperService;
    private final GitHubService gitHubService;
    private final GitHubAppAuthService gitHubAppAuthService;
    private final FilterFactory filterFactory;
    private final ConfigurationOverrider configOverrider;
    private final ScmConfigOverrider scmConfigOverrider;
    private final GitAuthUrlGenerator gitAuthUrlGenerator;

    private Mac hmac;

    @PostConstruct
    public void init() throws NoSuchAlgorithmException, InvalidKeyException {
        // initialize HMAC with SHA1 algorithm and secret
        setHmacToken(properties.getWebhookToken());
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
        verifyHmacSignature(body, signature, null);

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
        MDC.put(FlowConstants.MAIN_MDC_ENTRY, uid);
        log.info("Processing GitHub PULL request");
        PullEvent event;
        ObjectMapper mapper = new ObjectMapper();
        Integer installationId = null;
        controllerRequest = ensureNotNull(controllerRequest);

        try {
            event = mapper.readValue(body, PullEvent.class);
        } catch (IOException e) {
            throw new MachinaRuntimeException(e);
        }

        gitHubService.initConfigProviderOnPullEvent(uid, event);

        //verify message signature
        verifyHmacSignature(body, signature, controllerRequest);

        try {
            String action = event.getAction();
            // synchronize - happens when user pushes code into a branch for which a pull request exists
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
            List<String> branches = getBranches(controllerRequest, flowProperties);
            BugTracker bt = ScanUtils.getBugTracker(controllerRequest.getAssignee(), bugType, jiraProperties, controllerRequest.getBug());
            FilterConfiguration filter = filterFactory.getFilter(controllerRequest, flowProperties);
            Map<FindingSeverity,Integer> thresholdMap = getThresholds(controllerRequest);
            
            //build request object
            String gitUrl = Optional.ofNullable(pullRequest.getHead().getRepo())
                    .map(Repo::getCloneUrl)
                    .orElse(repository.getCloneUrl());

            String token;
            String gitAuthUrl;
            log.info("Using url: {}", gitUrl);

            if(event.getInstallation() != null && event.getInstallation().getId() != null){
                installationId = event.getInstallation().getId();
                token = gitHubAppAuthService.getInstallationToken(installationId);
                token = FlowConstants.GITHUB_APP_CLONE_USER.concat(":").concat(token);
            }
            else{
                token = scmConfigOverrider.determineConfigToken(properties, controllerRequest.getScmInstance());
            }
            gitAuthUrl = gitAuthUrlGenerator.addCredToUrl(ScanRequest.Repository.GITHUB, gitUrl, token);

            ScanRequest request = ScanRequest.builder()
                    .application(app)
                    .product(p)
                    .project(controllerRequest.getProject())
                    .team(controllerRequest.getTeam())
                    .namespace(pullRequest.getHead().getRepo().getOwner().getLogin().replace(" ","_"))
                    .repoName(repository.getName())
                    .repoUrl(repository.getCloneUrl())
                    .repoUrlWithAuth(gitAuthUrl)
                    .repoType(ScanRequest.Repository.GITHUB)
                    .branch(currentBranch)
                    .defaultBranch(repository.getDefaultBranch())
                    .refs(Constants.CX_BRANCH_PREFIX.concat(currentBranch))
                    .mergeNoteUri(pullRequest.getIssueUrl().concat("/comments"))
                    .mergeTargetBranch(targetBranch)
                    .email(null)
                    .scanPreset(controllerRequest.getPreset())
                    .incremental(controllerRequest.getIncremental())
                    .excludeFolders(controllerRequest.getExcludeFolders())
                    .excludeFiles(controllerRequest.getExcludeFiles())
                    .bugTracker(bt)
                    .filter(filter)
                    .thresholds(thresholdMap)
                    .organizationId(getOrganizationid(repository))
                    .gitUrl(gitUrl)
                    .hash(pullRequest.getHead().getSha())
                    .build();

            setScmInstance(controllerRequest, request);

            //Check if an installation Id is provided and store it for later use
            if(installationId != null){
                request.putAdditionalMetadata(
                        FlowConstants.GITHUB_APP_INSTALLATION_ID, installationId.toString()
                );
            }
            /*Check for Config as code (cx.config) and override*/
            log.debug(repository.getId()+" :: Calling  getCxConfigOverride function : "+System.currentTimeMillis());
            CxConfig cxConfig =  gitHubService.getCxConfigOverride(request);
            log.debug(repository.getId()+" :: Calling  overrideScanRequestProperties function : "+System.currentTimeMillis());
            request = configOverrider.overrideScanRequestProperties(cxConfig, request);
            log.debug(repository.getId()+" :: Calling  putAdditionalMetadata function : "+System.currentTimeMillis());

            request.putAdditionalMetadata(HTMLHelper.WEB_HOOK_PAYLOAD, body);
            request.putAdditionalMetadata("statuses_url", pullRequest.getStatusesUrl());
            request.setId(uid);
            //only initiate scan/automation if target branch is applicable
            if(helperService.isBranch2Scan(request, branches)){
                log.debug(repository.getId()+" :: Calling  isBranch2Scan function End : "+System.currentTimeMillis());
                log.debug(repository.getId()+" :: Free Memory : "+Runtime.getRuntime().freeMemory());
                log.debug(repository.getId()+" :: Total Numbers of processors : "+Runtime.getRuntime().availableProcessors());
                long startTime=System.currentTimeMillis();
                log.debug(repository.getId()+" :: Start Time : "+startTime);
                flowService.initiateAutomation(request);
                long endTime=System.currentTimeMillis();
                log.debug(repository.getId()+" :: End Time  : "+endTime);
                log.debug(repository.getId()+" :: Total Time Taken  : "+(endTime-startTime));
            }
        } catch (IllegalArgumentException e) {
            return getBadRequestMessage(e, controllerRequest, product);
        }

        return getSuccessMessage();
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
        MDC.put(FlowConstants.MAIN_MDC_ENTRY, uid);
        log.info("Processing GitHub PUSH request");
        PushEvent event;
        Integer installationId = null;
        ObjectMapper mapper = new ObjectMapper();
        controllerRequest = ensureNotNull(controllerRequest);

        try {
            event = mapper.readValue(body, PushEvent.class);
        } catch (NullPointerException | IOException | IllegalArgumentException e) {
            throw new MachinaRuntimeException(e);
        }
        // Delete event is triggering a push event that needs to be ignored
        if(event.getDeleted() != null && event.getDeleted()){
            log.info("Push event is associated with a Delete branch event...ignoring request");
            return getSuccessMessage();
        }
        
        gitHubService.initConfigProviderOnPushEvent(uid, event);

        if (flowProperties == null ) {
            log.error("Properties have null values");
            throw new MachinaRuntimeException();
        }

        //verify message signature
        verifyHmacSignature(body, signature, controllerRequest);

        try {
            String app = event.getRepository().getName();
            if(!ScanUtils.empty(controllerRequest.getApplication())){
                app = controllerRequest.getApplication();
            }

            // If user has pushed their changes into an important branch (e.g. master) and the code has some issues,
            // use the bug tracker from the config. As a result, "real" issues will be opened in the bug tracker and
            // not just notifications for the user. The "push" case also includes merging a pull request.
            // See the comment for the pullRequest method for further details.
            // However, if the bug tracker is overridden in the query string, use the override value.
            setBugTracker(flowProperties, controllerRequest);
            BugTracker.Type bugType = ScanUtils.getBugTypeEnum(controllerRequest.getBug(), flowProperties.getBugTrackerImpl());

            if(controllerRequest.getAppOnly() != null){
                flowProperties.setTrackApplicationOnly(controllerRequest.getAppOnly());
            }
            if(ScanUtils.empty(product)){
                product = ScanRequest.Product.CX.getProduct();
            }
            ScanRequest.Product p = ScanRequest.Product.valueOf(product.toUpperCase(Locale.ROOT));

            //determine branch (without refs)
            String currentBranch = ScanUtils.getBranchFromRef(event.getRef());
            List<String> branches = getBranches(controllerRequest, flowProperties);

            BugTracker bt = ScanUtils.getBugTracker(controllerRequest.getAssignee(), bugType, jiraProperties, controllerRequest.getBug());
            FilterConfiguration filter = filterFactory.getFilter(controllerRequest, flowProperties);

            Map<FindingSeverity,Integer> thresholdMap = getThresholds(controllerRequest);

            //build request object
            Repository repository = event.getRepository();
            String gitUrl = repository.getCloneUrl();
            String token;
            String gitAuthUrl;
            log.info("Using url: {}", gitUrl);

            if(event.getInstallation() != null && event.getInstallation().getId() != null){
                installationId = event.getInstallation().getId();
                token = gitHubAppAuthService.getInstallationToken(installationId);
                token = FlowConstants.GITHUB_APP_CLONE_USER.concat(":").concat(token);
            }
            else{
                token = scmConfigOverrider.determineConfigToken(properties, controllerRequest.getScmInstance());
                if(ScanUtils.empty(token)){
                    log.error("No token was provided for Github");
                    throw new MachinaRuntimeException();
                }
            }
            gitAuthUrl = gitAuthUrlGenerator.addCredToUrl(ScanRequest.Repository.GITHUB, gitUrl, token);

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
                    .scanPreset(controllerRequest.getPreset())
                    .incremental(controllerRequest.getIncremental())
                    .excludeFolders(controllerRequest.getExcludeFolders())
                    .excludeFiles(controllerRequest.getExcludeFiles())
                    .bugTracker(bt)
                    .filter(filter)
                    .thresholds(thresholdMap)
                    .organizationId(getOrganizationid(repository))
                    .gitUrl(gitUrl)
                    .hash(event.getAfter())
                    .build();

            setScmInstance(controllerRequest, request);

            //Check if an installation Id is provided and store it for later use
            if(installationId != null){
                request.putAdditionalMetadata(
                        FlowConstants.GITHUB_APP_INSTALLATION_ID, installationId.toString()
                );
            }

            /*Check for Config as code (cx.config) and override*/
            log.debug(repository.getId()+" :: Calling  getCxConfigOverride function : "+System.currentTimeMillis());
            CxConfig cxConfig =  gitHubService.getCxConfigOverride(request);
            log.debug(repository.getId()+" :: Calling  overrideScanRequestProperties function : "+System.currentTimeMillis());
            request = configOverrider.overrideScanRequestProperties(cxConfig, request);
            log.debug(repository.getId()+" :: Calling  putAdditionalMetadata function : "+System.currentTimeMillis());

            request.putAdditionalMetadata(HTMLHelper.WEB_HOOK_PAYLOAD, body);
            request.setId(uid);

            //only initiate scan/automation if branch is applicable
            if(helperService.isBranch2Scan(request, branches)){
                log.debug(repository.getId()+" :: Calling  isBranch2Scan function End : "+System.currentTimeMillis());
                log.debug(repository.getId()+" :: Free Memory : "+Runtime.getRuntime().freeMemory());
                log.debug(repository.getId()+" :: Total Numbers of processors : "+Runtime.getRuntime().availableProcessors());
                long startTime=System.currentTimeMillis();
                log.debug(repository.getId()+" :: Start Time : "+startTime);
                flowService.initiateAutomation(request);
                long endTime=System.currentTimeMillis();
                log.debug(repository.getId()+" :: End Time  : "+endTime);
                log.debug(repository.getId()+" :: Total Time Taken  : "+(endTime-startTime));
            }

        }
        catch (IllegalArgumentException e){
            return getBadRequestMessage(e, controllerRequest, product);
        }

        return getSuccessMessage();
    }

    private String getOrganizationid(Repository repository) {
        // E.g. "cxflowtestuser/VB_3845" ==> "cxflowtestuser"
        return StringUtils.substringBefore(repository.getFullName(), "/");
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
        MDC.put(FlowConstants.MAIN_MDC_ENTRY, uid);
        log.info("Processing GitHub DELETE Branch request");
        DeleteEvent event;
        ObjectMapper mapper = new ObjectMapper();

        try {
            event = mapper.readValue(body, DeleteEvent.class);
        } catch (NullPointerException | IOException | IllegalArgumentException e) {
            throw new MachinaRuntimeException(e);
        }

        if(flowProperties == null ){
            log.error("Properties have null values");
            throw new MachinaRuntimeException();
        }
        //verify message signature
        verifyHmacSignature(body, signature, null);

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
                .defaultBranch(repository.getDefaultBranch())
                .refs(event.getRef())
                .build();

        request.setScanPresetOverride(false);

        CxConfig cxConfig = gitHubService.getCxConfigOverride(request);
        request = configOverrider.overrideScanRequestProperties(cxConfig, request);

        //Check if an installation Id is provided and store it for later use
        if(event.getInstallation() != null && event.getInstallation().getId() != null){
            request.putAdditionalMetadata(
                    FlowConstants.GITHUB_APP_INSTALLATION_ID, event.getInstallation().getId().toString()
            );
        }
        //deletes a project which is not in the middle of a scan, otherwise it will not be deleted
        flowService.deleteProject(request);

        final String MESSAGE = "Branch deletion event was handled successfully.";
        log.info(MESSAGE);

        return ResponseEntity.status(HttpStatus.OK).body(EventResponse.builder()
                .message(MESSAGE)
                .success(true)
                .build());
    }


    /** Validates the received body using the Github hook secret. */
    public void verifyHmacSignature(String message, String signature, ControllerRequest controllerRequest) {
        if(hmac == null) {
            log.error("Hmac was not initialized. Trying to initialize...");
            try {
                init();
            } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                log.error(e.getMessage(), e);
            }
        }
        try {
            setHmacToken(scmConfigOverrider.determineConfigWebhookToken(properties, controllerRequest));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error(e.getMessage());
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

    private void setHmacToken(String webhookToken) throws NoSuchAlgorithmException, InvalidKeyException {
        if(properties != null && !ScanUtils.empty(webhookToken)) {
            SecretKeySpec secret = new SecretKeySpec(webhookToken.getBytes(CHARSET), HMAC_ALGORITHM);
            hmac = Mac.getInstance(HMAC_ALGORITHM);
            hmac.init(secret);
        }
    }
}
