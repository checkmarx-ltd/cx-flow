package com.checkmarx.flow.controller;

import com.checkmarx.flow.config.BitBucketProperties;
import com.checkmarx.flow.config.CxProperties;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.dto.*;
import com.checkmarx.flow.dto.bitbucket.*;
import com.checkmarx.flow.exception.InvalidTokenException;
import com.checkmarx.flow.service.FlowService;
import com.checkmarx.flow.service.HelperService;
import com.checkmarx.flow.utils.Constants;
import com.checkmarx.flow.utils.ScanUtils;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.beans.ConstructorProperties;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;


@RestController
@RequestMapping(value = "/" )
public class BitbucketCloudController {

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

    @ConstructorProperties({"flowProperties", "properties", "cxProperties", "jiraProperties", "flowService", "helperService"})
    public BitbucketCloudController(FlowProperties flowProperties, BitBucketProperties properties, CxProperties cxProperties,
                                    JiraProperties jiraProperties, FlowService flowService, HelperService helperService) {
        this.flowProperties = flowProperties;
        this.properties = properties;
        this.cxProperties = cxProperties;
        this.jiraProperties = jiraProperties;
        this.flowService = flowService;
        this.helperService = helperService;
    }

    /**
     * Push Request event webhook submitted.
     */
    @PostMapping(value = {"/{product}", "/"}, headers = MERGE)
    public ResponseEntity<EventResponse> pushRequest(
            @RequestBody MergeEvent body,
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
            @RequestParam(value = "app-only", required = false) Boolean appOnlyTracking,
            @RequestParam(value = "token") String token

    ){
        String uid = helperService.getShortUid();
        MDC.put("cx", uid);
        validateBitBucketRequest(token);
        log.info("Processing BitBucket MERGE request");
        MachinaOverride o = ScanUtils.getMachinaOverride(override);

        try {
            Repository repository = body.getRepository();
            String app = repository.getName();
            if(!ScanUtils.empty(application)){
                app = application;
            }

            BugTracker.Type bugType = BugTracker.Type.BITBUCKETPULL;
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
            Pullrequest pullRequest = body.getPullrequest();
            String currentBranch = pullRequest.getSource().getBranch().getName();
            String targetBranch = pullRequest.getDestination().getBranch().getName();
            List<String> branches = new ArrayList<>();
            List<Filter> filters;

            if(!ScanUtils.empty(branch)){
                branches.addAll(branch);
            }
            else if(!ScanUtils.empty(flowProperties.getBranches())){
                branches.addAll(flowProperties.getBranches());
            }

            BugTracker bt = ScanUtils.getBugTracker(assignee, bugType, jiraProperties, bug);

            if(!ScanUtils.empty(severity) || !ScanUtils.empty(cwe) || !ScanUtils.empty(category) || !ScanUtils.empty(status)){
                filters = ScanUtils.getFilters(severity, cwe, category, status);
            }
            else{
                filters = ScanUtils.getFilters(flowProperties);
            }

            if(excludeFiles == null){
                excludeFiles = Arrays.asList(cxProperties.getExcludeFiles().split(","));
            }
            if(excludeFolders == null){
                excludeFolders = Arrays.asList(cxProperties.getExcludeFolders().split(","));
            }

            String gitUrl = repository.getLinks().getHtml().getHref().concat(".git");
            String mergeEndpoint = pullRequest.getLinks().getComments().getHref();

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
                    .namespace(repository.getOwner().getUsername().replaceAll(" ","_"))
                    .repoName(repository.getName())
                    .repoUrl(gitUrl)
                    .repoUrlWithAuth(gitUrl.replace(Constants.HTTPS, Constants.HTTPS.concat(properties.getToken()).concat("@")))
                    .repoType(ScanRequest.Repository.BITBUCKET)
                    .branch(currentBranch)
                    .mergeTargetBranch(targetBranch)
                    .mergeNoteUri(mergeEndpoint)
                    .refs(Constants.CX_BRANCH_PREFIX.concat(currentBranch))
                    .email(null)
                    .incremental(inc)
                    .scanPreset(scanPreset)
                    .excludeFolders(excludeFolders)
                    .excludeFiles(excludeFiles)
                    .bugTracker(bt)
                    .filters(filters)
                    .build();

            request = ScanUtils.overrideMap(request, o);
            request.setId(uid);

            if(helperService.isBranch2Scan(request, branches)){
                flowService.initiateAutomation(request);
            }

        }catch (IllegalArgumentException e){
            String errorMessage = "Error submitting Scan Request.  Product or Bugtracker option incorrect ".concat(product != null ? product : "").concat(" | ").concat(bug != null ? bug : "");
            log.error(errorMessage);
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
            @RequestBody PushEvent body,
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
            @RequestParam(value = "app-only", required = false) Boolean appOnlyTracking,
            @RequestParam(value = "token") String token

    ){
        String uid = helperService.getShortUid();
        MDC.put("cx", uid);
        validateBitBucketRequest(token);

        MachinaOverride o = ScanUtils.getMachinaOverride(override);

        try {
            Repository repository = body.getRepository();
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

            if(appOnlyTracking != null){
                flowProperties.setTrackApplicationOnly(appOnlyTracking);
            }

            if(ScanUtils.empty(product)){
                product = ScanRequest.Product.CX.getProduct();
            }
            ScanRequest.Product p = ScanRequest.Product.valueOf(product.toUpperCase(Locale.ROOT));
            List<Change> changeList =  body.getPush().getChanges();
            String currentBranch = changeList.get(0).getNew().getName();
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
                filters = ScanUtils.getFilters(flowProperties);
            }

            if(excludeFiles == null){
                excludeFiles = Arrays.asList(cxProperties.getExcludeFiles().split(","));
            }
            if(excludeFolders == null){
                excludeFolders = Arrays.asList(cxProperties.getExcludeFolders().split(","));
            }

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
                    .namespace(repository.getOwner().getUsername().replaceAll(" ","_"))
                    .repoName(repository.getName())
                    .repoUrl(gitUrl)
                    .repoUrlWithAuth(gitUrl.replace(Constants.HTTPS, Constants.HTTPS.concat(properties.getToken()).concat("@")))
                    .repoType(ScanRequest.Repository.BITBUCKET)
                    .branch(currentBranch)
                    .refs(Constants.CX_BRANCH_PREFIX.concat(currentBranch))
                    .email(emails)
                    .incremental(inc)
                    .scanPreset(scanPreset)
                    .excludeFolders(excludeFolders)
                    .excludeFiles(excludeFiles)
                    .bugTracker(bt)
                    .filters(filters)
                    .build();

            request = ScanUtils.overrideMap(request, o);
            request.setId(uid);

            if(helperService.isBranch2Scan(request, branches)){
                flowService.initiateAutomation(request);
            }


        }catch (IllegalArgumentException e){
            String errorMessage = "Error submitting Scan Request.  Product or Bugtracker option incorrect ".concat(product != null ? product : "").concat(" | ").concat(bug != null ? bug : "");
            log.error(errorMessage);
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
     * Token/Credential validation
     * @param token
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
