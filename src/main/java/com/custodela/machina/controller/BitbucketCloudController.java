package com.custodela.machina.controller;

import com.custodela.machina.config.BitBucketProperties;
import com.custodela.machina.config.CxProperties;
import com.custodela.machina.config.JiraProperties;
import com.custodela.machina.config.MachinaProperties;
import com.custodela.machina.dto.*;
import com.custodela.machina.dto.bitbucket.Change;
import com.custodela.machina.dto.bitbucket.Commit;
import com.custodela.machina.dto.bitbucket.MergeEvent;
import com.custodela.machina.dto.bitbucket.PushEvent;
import com.custodela.machina.exception.InvalidTokenException;
import com.custodela.machina.service.MachinaService;
import com.custodela.machina.utils.ScanUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.beans.ConstructorProperties;
import java.util.ArrayList;
import java.util.List;


@RestController
@RequestMapping(value = "/" )
public class BitbucketCloudController {

    private static final String EVENT = "X-Event-Key";
    private static final String PUSH = EVENT + "=repo:push";
    private static final String MERGE = EVENT + "=pullrequest:created";
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BitbucketCloudController.class);

    private final MachinaProperties machinaProperties;
    private final BitBucketProperties properties;
    private final CxProperties cxProperties;
    private final JiraProperties jiraProperties;
    private final MachinaService machinaService;

    @ConstructorProperties({"machinaProperties", "properties", "cxProperties", "jiraProperties", "machinaService"})
    public BitbucketCloudController(MachinaProperties machinaProperties, BitBucketProperties properties, CxProperties cxProperties, JiraProperties jiraProperties, MachinaService machinaService) {
        this.machinaProperties = machinaProperties;
        this.properties = properties;
        this.cxProperties = cxProperties;
        this.jiraProperties = jiraProperties;
        this.machinaService = machinaService;
    }

    /**
     * Push Request event webhook submitted.
     */
    @PostMapping(value = "/{product}", headers = MERGE)
    public ResponseEntity<EventResponse> pushRequest(
            @RequestBody MergeEvent body,
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
            @RequestParam(value = "bug", required = false) String bug,
            @RequestParam(value = "token") String token

    ){
        validateBitBucketRequest(token);
        log.info("Processing BitBucket MERGE request");
        MachinaOverride o = ScanUtils.getMachinaOverride(override);

        try {
            String app = body.getRepository().getName();
            if(!ScanUtils.empty(application)){
                app = application;
            }

            BugTracker.Type bugType = BugTracker.Type.BITBUCKETPULL;
            if(!ScanUtils.empty(bug)){
                bugType = ScanUtils.getBugTypeEnum(bug, machinaProperties.getBugTrackerImpl());
            }

            ScanRequest.Product p = ScanRequest.Product.valueOf(product.toUpperCase());
            String currentBranch = body.getPullrequest().getSource().getBranch().getName();
            String targetBranch = body.getPullrequest().getDestination().getBranch().getName();
            List<String> branches = new ArrayList<>();
            List<Filter> filters;

            if(!ScanUtils.empty(branch)){
                branches.addAll(branch);
            }
            else if(!ScanUtils.empty(machinaProperties.getBranches())){
                branches.addAll(machinaProperties.getBranches());
            }

            BugTracker bt = ScanUtils.getBugTracker(assignee, bugType, jiraProperties, bug);

            if(!ScanUtils.empty(severity) || !ScanUtils.empty(cwe) || !ScanUtils.empty(category) || !ScanUtils.empty(status)){
                filters = ScanUtils.getFilters(severity, cwe, category, status);
            }
            else{
                filters = ScanUtils.getFilters(machinaProperties.getFilterSeverity(), machinaProperties.getFilterCwe(),
                        machinaProperties.getFilterCategory(), machinaProperties.getFilterStatus());
            }

            String gitUrl = body.getRepository().getLinks().getHtml().getHref().concat(".git");
            String mergeEndpoint = body.getPullrequest().getLinks().getComments().getHref();

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
                    .namespace(body.getRepository().getOwner().getUsername().replaceAll(" ","_"))
                    .repoName(body.getRepository().getName())
                    .repoUrl(gitUrl)
                    .repoUrlWithAuth(gitUrl.replace("https://", "https://".concat(properties.getToken()).concat("@")))
                    .repoType(ScanRequest.Repository.BITBUCKET)
                    .branch(currentBranch)
                    .mergeTargetBranch(targetBranch)
                    .mergeNoteUri(mergeEndpoint)
                    .refs("refs/heads/".concat(currentBranch))
                    .email(null)
                    .incremental(inc)
                    .scanPreset(scanPreset)
                    .excludeFolders(excludeFolders)
                    .excludeFiles(excludeFiles)
                    .bugTracker(bt)
                    .filters(filters)
                    .build();

            request = ScanUtils.overrideMap(request, o);

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
     * Receive Push event submitted from Bitbucket
     */
    @PostMapping(value = "/{product}", headers = PUSH)
    public ResponseEntity<EventResponse> pushRequest(
            @RequestBody PushEvent body,
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
            @RequestParam(value = "bug", required = false) String bug,
            @RequestParam(value = "token") String token

    ){

        validateBitBucketRequest(token);

        MachinaOverride o = ScanUtils.getMachinaOverride(override);
        ObjectMapper mapper = new ObjectMapper();

        try {
            String app = body.getRepository().getName();
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
            String currentBranch = body.getPush().getChanges().get(0).getNew().getName();
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

            for(Change ch: body.getPush().getChanges()){
                for(Commit c: ch.getCommits()){
                    if(!ScanUtils.empty(c.getAuthor().getRaw())){
                        emails.add(c.getAuthor().getRaw());
                    }
                }
            }

            String gitUrl = body.getRepository().getLinks().getHtml().getHref().concat(".git");

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
                    .namespace(body.getRepository().getOwner().getUsername().replaceAll(" ","_"))
                    .repoName(body.getRepository().getName())
                    .repoUrl(gitUrl)
                    .repoUrlWithAuth(gitUrl.replace("https://", "https://".concat(properties.getToken()).concat("@")))
                    .repoType(ScanRequest.Repository.BITBUCKET)
                    .branch(currentBranch)
                    .refs("refs/heads/".concat(currentBranch))
                    .email(emails)
                    .incremental(inc)
                    .scanPreset(scanPreset)
                    .excludeFolders(excludeFolders)
                    .excludeFiles(excludeFiles)
                    .bugTracker(bt)
                    .filters(filters)
                    .build();

            request = ScanUtils.overrideMap(request, o);

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
