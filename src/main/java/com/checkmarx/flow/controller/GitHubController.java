package com.checkmarx.flow.controller;

import com.checkmarx.flow.config.CxProperties;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.*;
import com.checkmarx.flow.dto.github.*;
import com.checkmarx.flow.exception.InvalidTokenException;
import com.checkmarx.flow.exception.MachinaRuntimeException;
import com.checkmarx.flow.service.FlowService;
import com.checkmarx.flow.utils.ScanUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
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
import java.util.List;
import java.util.Locale;

/**
 * Class used to manage Controller for GitHub WebHooks
 */
@RestController
@RequestMapping(value = "/")
public class GitHubController {

    private static final String HTTP = "http://";
    private static final String HTTPS = "https://";
    private static final String SIGNATURE = "X-Hub-Signature";
    private static final String EVENT = "X-GitHub-Event";
    private static final String PING = EVENT + "=ping";
    private static final String PULL = EVENT + "=pull_request";
    private static final String PUSH = EVENT + "=push";
    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final Charset CHARSET = StandardCharsets.UTF_8;
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(GitHubController.class);
    private final GitHubProperties properties;
    private final FlowProperties flowProperties;
    private final CxProperties cxProperties;
    private final JiraProperties jiraProperties;
    private final FlowService flowService;
    private Mac hmac;

    @ConstructorProperties({"properties", "flowProperties", "cxProperties", "jiraProperties", "flowService"})
    public GitHubController(GitHubProperties properties, FlowProperties flowProperties, CxProperties cxProperties, JiraProperties jiraProperties, FlowService flowService) {
        this.properties = properties;
        this.flowProperties = flowProperties;
        this.cxProperties = cxProperties;
        this.jiraProperties = jiraProperties;
        this.flowService = flowService;
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
        log.info("Processing GitHub PULL request");
        PullEvent event;
        ObjectMapper mapper = new ObjectMapper();
        MachinaOverride o = ScanUtils.getMachinaOverride(override);

        try {
            event = mapper.readValue(body, PullEvent.class);
        } catch (IOException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            throw new MachinaRuntimeException();
        }
        //verify message signature
        verifyHmacSignature(body, signature);

        try {
            String action = event.getAction();
            if(!action.equalsIgnoreCase("opened") &&
                    !action.equalsIgnoreCase("reopened")){
                log.info("Pull requested not processed.  Status was not opened ({})", action);
                return ResponseEntity.status(HttpStatus.OK).body(EventResponse.builder()
                        .message("No processing occurred for updates to Pull Request")
                        .success(true)
                        .build());
            }
            Repository repository = event.getRepository();
            String app = repository.getName();
            if(!ScanUtils.empty(application)){
                app = application;
            }

            BugTracker.Type bugType = BugTracker.Type.GITHUBPULL;
            if(!ScanUtils.empty(bug)){
                bugType = ScanUtils.getBugTypeEnum(bug, flowProperties.getBugTrackerImpl());
            }

            if(appOnlyTracking != null){
                flowProperties.setTrackApplicationOnly(appOnlyTracking);
            }

            if(ScanUtils.empty(product)){
                product = ScanRequest.Product.CX.getProduct();
            }
            ScanRequest.Product p = ScanRequest.Product.valueOf(product.toUpperCase(Locale.ROOT));
            PullRequest pullRequest = event.getPullRequest();
            String currentBranch = pullRequest.getHead().getRef();
            String targetBranch = pullRequest.getBase().getRef();
            List<String> branches = new ArrayList<>();
            List<Filter> filters;
            if(!ScanUtils.empty(branch)){
                branches.addAll(branch);
            }
            else if(!ScanUtils.empty(flowProperties.getBranches())){
                branches.addAll(flowProperties.getBranches());
            }

            BugTracker bt = ScanUtils.getBugTracker(assignee, bugType, jiraProperties, bug);
            /*Determine filters, if any*/
            if(!ScanUtils.empty(severity) || !ScanUtils.empty(cwe) || !ScanUtils.empty(category) || !ScanUtils.empty(status)){
                filters = ScanUtils.getFilters(severity, cwe, category, status);
            }
            else{
                filters = ScanUtils.getFilters(flowProperties.getFilterSeverity(), flowProperties.getFilterCwe(),
                        flowProperties.getFilterCategory(), flowProperties.getFilterStatus());
            }

            //build request object
            String gitUrl = repository.getCloneUrl();
            String token = properties.getToken();
            log.info("Using url: {}", gitUrl);
            String gitAuthUrl = gitUrl.replace(HTTPS, HTTPS.concat(token).concat("@"));
            gitAuthUrl = gitAuthUrl.replace(HTTP, HTTP.concat(token).concat("@"));

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
                    .namespace(repository.getOwner().getLogin().replaceAll(" ","_"))
                    .repoName(repository.getName())
                    .repoUrl(repository.getCloneUrl())
                    .repoUrlWithAuth(gitAuthUrl)
                    .repoType(ScanRequest.Repository.GITHUB)
                    .branch(currentBranch)
                    .refs("refs/heads/".concat(currentBranch))
                    .mergeNoteUri(event.getPullRequest().getIssueUrl().concat("/comments"))
                    .mergeTargetBranch(targetBranch)
                    .email(null)
                    .incremental(inc)
                    .scanPreset(scanPreset)
                    .excludeFolders(excludeFolders)
                    .excludeFiles(excludeFiles)
                    .bugTracker(bt)
                    .filters(filters)
                    .build();

            request = ScanUtils.overrideMap(request, o);
            request.putAdditionalMetadata("statuses_url", event.getPullRequest().getStatusesUrl());
            //only initiate scan/automation if target branch is applicable
            if(branches.isEmpty() || branches.contains(targetBranch)) {
                flowService.initiateAutomation(request);
            }


        }catch (IllegalArgumentException e){
            log.error("Error submitting Scan Request. Product option incorrect {}", product);
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(EventResponse.builder()
                    .message("Error submitting Scan Request.  Product or Bugtracker option incorrect ".concat(product))
                    .success(false)
                    .build());
        }

        return ResponseEntity.status(HttpStatus.OK).body(EventResponse.builder()
                .message("Scan Request Successfully Submitted")
                .success(true)
                .build());

    }

    /**
     * Push Request event submitted (JSON), along with the Product (cx for example)
     */
    @PostMapping(value = {"/{product}", "/"}, headers = PUSH)
    public ResponseEntity<EventResponse> pushRequest(
            @RequestBody String body,
            @RequestHeader(value = SIGNATURE) String signature,
            @PathVariable(value = "product", required = false) String product,
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
        log.info("Processing GitHub PUSH request");
        PushEvent event;
        ObjectMapper mapper = new ObjectMapper();
        MachinaOverride o = ScanUtils.getMachinaOverride(override);

        try {
            event = mapper.readValue(body, PushEvent.class);
        } catch (IOException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            throw new MachinaRuntimeException();
        }
        //verify message signature
        verifyHmacSignature(body, signature);

        try {
            String app = event.getRepository().getName();
            if(!ScanUtils.empty(application)){
                app = application;
            }

            //set the default bug tracker as per yml
            BugTracker.Type bugType;
            if (ScanUtils.empty(bug)) {
                bug =  flowProperties.getBugTracker();
            }
            bugType = ScanUtils.getBugTypeEnum(bug, flowProperties.getBugTrackerImpl());

            if(appOnlyTracking != null){
                flowProperties.setTrackApplicationOnly(appOnlyTracking);
            }
            if(ScanUtils.empty(product)){
                product = ScanRequest.Product.CX.getProduct();
            }
            ScanRequest.Product p = ScanRequest.Product.valueOf(product.toUpperCase(Locale.ROOT));

            //determine branch (without refs)
            String currentBranch = event.getRef().split("/")[2];
            List<String> branches = new ArrayList<>();
            List<Filter> filters;
            if(!ScanUtils.empty(branch)){
                branches.addAll(branch);
            }
            else if(!ScanUtils.empty(flowProperties.getBranches())){
                branches.addAll(flowProperties.getBranches());
            }

            BugTracker bt = ScanUtils.getBugTracker(assignee, bugType, jiraProperties, bug);
            /*Determine filters, if any*/
            if(!ScanUtils.empty(severity) || !ScanUtils.empty(cwe) || !ScanUtils.empty(category) || !ScanUtils.empty(status)){
                filters = ScanUtils.getFilters(severity, cwe, category, status);
            }
            else{
                filters = ScanUtils.getFilters(flowProperties.getFilterSeverity(), flowProperties.getFilterCwe(),
                        flowProperties.getFilterCategory(), flowProperties.getFilterStatus());
            }
            /*Determine emails*/
            List<String> emails = new ArrayList<>();
            for(Commit c: event.getCommits()){
                if (c.getAuthor() != null && !ScanUtils.empty(c.getAuthor().getEmail())){
                    emails.add(c.getAuthor().getEmail());
                }
            }
            emails.add(event.getPusher().getEmail());

            //build request object
            Repository repository = event.getRepository();
            String gitUrl = repository.getCloneUrl();
            log.debug("Using url: {}", gitUrl);
            String gitAuthUrl = gitUrl.replace(HTTPS, HTTPS.concat(properties.getToken()).concat("@"));
            gitAuthUrl = gitAuthUrl.replace(HTTP, HTTP.concat(properties.getToken()).concat("@"));

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
                    .namespace(repository.getOwner().getName().replaceAll(" ","_"))
                    .repoName(repository.getName())
                    .repoUrl(repository.getCloneUrl())
                    .repoUrlWithAuth(gitAuthUrl)
                    .repoType(ScanRequest.Repository.GITHUB)
                    .branch(currentBranch)
                    .refs(event.getRef())
                    .email(emails)
                    .incremental(inc)
                    .scanPreset(scanPreset)
                    .excludeFolders(excludeFolders)
                    .excludeFiles(excludeFiles)
                    .bugTracker(bt)
                    .filters(filters)
                    .build();

            //if an override blob/file is provided, substitute these values
            request = ScanUtils.overrideMap(request, o);

            //only initiate scan/automation if branch is applicable
            if(branches.isEmpty() || branches.contains(currentBranch)) {
                flowService.initiateAutomation(request);
            }

        }catch (IllegalArgumentException e){
            log.error("Error submitting Scan Request. Product option incorrect {}", product);
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);

           return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(EventResponse.builder()
                    .message("Error submitting Scan Request.  Product or Bugtracker option incorrect ".concat(product))
                    .success(false)
                    .build());
        }

        return ResponseEntity.status(HttpStatus.OK).body(EventResponse.builder()
                .message("Scan Request Successfully Submitted")
                .success(true)
                .build());

    }

    /** Validates the received body using the Github hook secret. */
    private void verifyHmacSignature(String message, String signature) {
        byte[] sig = hmac.doFinal(message.getBytes(CHARSET));
        String computedSignature = "sha1=" + DatatypeConverter.printHexBinary(sig);
        if (!computedSignature.equalsIgnoreCase(signature)) {
            throw new InvalidTokenException();
        }
        log.info("Signature verified");
    }

}
