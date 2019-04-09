package com.custodela.machina.controller;

import com.custodela.machina.config.CxProperties;
import com.custodela.machina.config.GitHubProperties;
import com.custodela.machina.config.JiraProperties;
import com.custodela.machina.config.MachinaProperties;
import com.custodela.machina.dto.*;
import com.custodela.machina.dto.github.Commit;
import com.custodela.machina.dto.github.PullEvent;
import com.custodela.machina.dto.github.PushEvent;
import com.custodela.machina.exception.InvalidTokenException;
import com.custodela.machina.exception.MachinaRuntimeException;
import com.custodela.machina.service.MachinaService;
import com.custodela.machina.utils.ScanUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
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

/**
 * Class used to manage Controller for GitHub WebHooks
 */
@RestController
@RequestMapping(value = "/")
public class GitHubController {

    private static final String SIGNATURE = "X-Hub-Signature";
    private static final String EVENT = "X-GitHub-Event";
    private static final String PING = EVENT + "=ping";
    private static final String PULL = EVENT + "=pull_request";
    private static final String PUSH = EVENT + "=push";
    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final Charset CHARSET = StandardCharsets.UTF_8;
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(GitHubController.class);
    private final GitHubProperties properties;
    private final MachinaProperties machinaProperties;
    private final CxProperties cxProperties;
    private final JiraProperties jiraProperties;
    private final MachinaService machinaService;
    private Mac hmac;

    @ConstructorProperties({"properties", "machinaProperties", "cxProperties", "jiraProperties", "machinaService"})
    public GitHubController(GitHubProperties properties, MachinaProperties machinaProperties, CxProperties cxProperties, JiraProperties jiraProperties, MachinaService machinaService) {
        this.properties = properties;
        this.machinaProperties = machinaProperties;
        this.cxProperties = cxProperties;
        this.jiraProperties = jiraProperties;
        this.machinaService = machinaService;
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
    @PostMapping(value="/{product}", headers = PING)
    public String pingRequest(
            @RequestBody String body,
            @PathVariable("product") String product,
            @RequestHeader(value = SIGNATURE) String signature){
        log.info("Processing GitHub PING request");
        verifyHmacSignature(body, signature);

        return "ok";
    }

    /**
     * Pull Request event submitted (JSON)
     */
    @PostMapping(value="/{product}", headers = PULL)
    public ResponseEntity<EventResponse> pullRequest(
            @RequestBody String body,
            @RequestHeader(value = SIGNATURE) String signature,
            @PathVariable("product") String product,
            @RequestParam(value = "application", required = false) String application,
            @RequestParam(value = "branch", required = false) List<String> branch,
            @RequestParam(value = "severity", required = false) List<String> severity,
            @RequestParam(value = "cwe", required = false) List<String> cwe,
            @RequestParam(value = "category", required = false) List<String> category,
            @RequestParam(value = "status", required = false) List<String> status,
            @RequestParam(value = "assignee", required = false) String assignee,
            @RequestParam(value = "preset", required = false) String preset,
            @RequestParam(value = "incremental", required = false) Boolean incremental,
            @RequestParam(value = "exclude-files", required = false) List<String> excludeFiles,
            @RequestParam(value = "exclude-folders", required = false) List<String> excludeFolders,
            @RequestParam(value = "override", required = false) String override,
            @RequestParam(value = "bug", required = false) String bug
    ){
        log.info("Processing GitHub PULL request");
        PullEvent event;
        ObjectMapper mapper = new ObjectMapper();
        MachinaOverride o = ScanUtils.getMachinaOverride(override);

        try {
            event = mapper.readValue(body, PullEvent.class);
        } catch (IOException e) {
            e.printStackTrace();
            throw new MachinaRuntimeException();
        }
        //verify message signature
        verifyHmacSignature(body, signature);

        try {
            if(!event.getAction().equalsIgnoreCase("opened")){
                log.info("Pull requested not processed.  Status was not opened ({})", event.getAction());
                return ResponseEntity.status(HttpStatus.OK).body(EventResponse.builder()
                        .message("No processing occurred for updates to Pull Request")
                        .success(true)
                        .build());
            }
            String app = event.getRepository().getName();
            if(!ScanUtils.empty(application)){
                app = application;
            }

            BugTracker.Type bugType = BugTracker.Type.GITHUBPULL;
            if(!ScanUtils.empty(bug)){
                bugType = ScanUtils.getBugTypeEnum(bug, machinaProperties.getBugTrackerImpl());
            }

            ScanRequest.Product p = ScanRequest.Product.valueOf(product.toUpperCase());
            String currentBranch = event.getPullRequest().getHead().getRef();
            String targetBranch = event.getPullRequest().getBase().getRef();
            List<String> branches = new ArrayList<>();
            List<Filter> filters;
            if(!ScanUtils.empty(branch)){
                branches.addAll(branch);
            }
            else if(!ScanUtils.empty(machinaProperties.getBranches())){
                branches.addAll(machinaProperties.getBranches());
            }

            BugTracker bt = ScanUtils.getBugTracker(assignee, bugType, jiraProperties, bug);
            /*Determine filters, if any*/
            if(!ScanUtils.empty(severity) || !ScanUtils.empty(cwe) || !ScanUtils.empty(category) || !ScanUtils.empty(status)){
                filters = ScanUtils.getFilters(severity, cwe, category, status);
            }
            else{
                filters = ScanUtils.getFilters(machinaProperties.getFilterSeverity(), machinaProperties.getFilterCwe(),
                        machinaProperties.getFilterCategory(), machinaProperties.getFilterStatus());
            }

            //build request object
            String gitUrl = event.getRepository().getCloneUrl();
            log.info("Using url: {}", gitUrl);
            String gitAuthUrl = gitUrl.replace("https://", "https://".concat(properties.getToken()).concat("@"));
            gitAuthUrl = gitAuthUrl.replace("http://", "http://".concat(properties.getToken()).concat("@"));

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
                    .namespace(event.getRepository().getOwner().getLogin().replaceAll(" ","_"))
                    .repoName(event.getRepository().getName())
                    .repoUrl(event.getRepository().getCloneUrl())
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

            //only initiate scan/automation if target branch is applicable
            if(branches.isEmpty() || branches.contains(targetBranch)) {
                machinaService.initiateAutomation(request);
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
    @PostMapping(value = "/{product}", headers = PUSH)
    public ResponseEntity<EventResponse> pushRequest(
            @RequestBody String body,
            @RequestHeader(value = SIGNATURE) String signature,
            @PathVariable("product") String product,
            @RequestParam(value = "application", required = false) String application,
            @RequestParam(value = "branch", required = false) List<String> branch,
            @RequestParam(value = "severity", required = false) List<String> severity,
            @RequestParam(value = "cwe", required = false) List<String> cwe,
            @RequestParam(value = "category", required = false) List<String> category,
            @RequestParam(value = "status", required = false) List<String> status,
            @RequestParam(value = "assignee", required = false) String assignee,
            @RequestParam(value = "preset", required = false) String preset,
            @RequestParam(value = "incremental", required = false) Boolean incremental,
            @RequestParam(value = "exclude-files", required = false) List<String> excludeFiles,
            @RequestParam(value = "exclude-folders", required = false) List<String> excludeFolders,
            @RequestParam(value = "override", required = false) String override,
            @RequestParam(value = "bug", required = false) String bug
    ){
        log.info("Processing GitHub PUSH request");
        PushEvent event;
        ObjectMapper mapper = new ObjectMapper();
        MachinaOverride o = ScanUtils.getMachinaOverride(override);

        try {
            event = mapper.readValue(body, PushEvent.class);
        } catch (IOException e) {
            e.printStackTrace();
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
                bug =  machinaProperties.getBugTracker();
            }
            bugType = ScanUtils.getBugTypeEnum(bug, machinaProperties.getBugTrackerImpl());

            ScanRequest.Product p = ScanRequest.Product.valueOf(product.toUpperCase());

            //determine branch (without refs)
            String currentBranch = event.getRef().split("/")[2];
            List<String> branches = new ArrayList<>();
            List<Filter> filters;
            if(!ScanUtils.empty(branch)){
                branches.addAll(branch);
            }
            else if(!ScanUtils.empty(machinaProperties.getBranches())){
                branches.addAll(machinaProperties.getBranches());
            }

            BugTracker bt = ScanUtils.getBugTracker(assignee, bugType, jiraProperties, bug);
            /*Determine filters, if any*/
            if(!ScanUtils.empty(severity) || !ScanUtils.empty(cwe) || !ScanUtils.empty(category) || !ScanUtils.empty(status)){
                filters = ScanUtils.getFilters(severity, cwe, category, status);
            }
            else{
                filters = ScanUtils.getFilters(machinaProperties.getFilterSeverity(), machinaProperties.getFilterCwe(),
                        machinaProperties.getFilterCategory(), machinaProperties.getFilterStatus());
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
            String gitUrl = event.getRepository().getCloneUrl();
            log.debug("Using url: {}", gitUrl);
            String gitAuthUrl = gitUrl.replace("https://", "https://".concat(properties.getToken()).concat("@"));
            gitAuthUrl = gitAuthUrl.replace("http://", "http://".concat(properties.getToken()).concat("@"));

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
                    .namespace(event.getRepository().getOwner().getName().replaceAll(" ","_"))
                    .repoName(event.getRepository().getName())
                    .repoUrl(event.getRepository().getCloneUrl())
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
                machinaService.initiateAutomation(request);
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
