package com.checkmarx.flow.controller;

import com.checkmarx.flow.config.*;
import com.checkmarx.flow.dto.*;
import com.checkmarx.flow.dto.azure.*;
import com.checkmarx.flow.exception.InvalidTokenException;
import com.checkmarx.flow.service.FlowService;
import com.checkmarx.flow.utils.ScanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.beans.ConstructorProperties;
import java.util.*;

/**
 * Class used to manage Controller for GitHub WebHooks
 */
@RestController
@RequestMapping(value = "/")
public class ADOController {

    private static final String HTTP = "http://";
    private static final String HTTPS = "https://";
    private static final String PULL_EVENT = "git.pullrequest.created";
    private static final String AUTHORIZATION = "authorization";
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ADOController.class);
    private final ADOProperties properties;
    private final FlowProperties flowProperties;
    private final CxProperties cxProperties;
    private final JiraProperties jiraProperties;
    private final FlowService flowService;

    @ConstructorProperties({"properties", "flowProperties", "cxProperties", "jiraProperties", "flowService"})
    public ADOController(ADOProperties properties, FlowProperties flowProperties, CxProperties cxProperties, JiraProperties jiraProperties, FlowService flowService) {
        this.properties = properties;
        this.flowProperties = flowProperties;
        this.cxProperties = cxProperties;
        this.jiraProperties = jiraProperties;
        this.flowService = flowService;
    }

    /**
     * Pull Request event submitted (JSON)
     */
    @PostMapping(value={"/{product}/ado/pull","/ado/pull"})
    public ResponseEntity<EventResponse> pullRequest(
            @RequestBody PullEvent body,
            @RequestHeader(value = AUTHORIZATION) String auth,
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
        log.info("Processing Azure PULL request");
        validateBasicAuth(auth);

        if(!body.getEventType().equals(PULL_EVENT)){
            log.info("Pull requested not processed.  Event was not opened ({})", body.getEventType());
            return ResponseEntity.status(HttpStatus.OK).body(EventResponse.builder()
                    .message("No processing occurred for updates to Pull Request")
                    .success(true)
                    .build());
        }

        MachinaOverride o = ScanUtils.getMachinaOverride(override);

        try {
            Resource resource = body.getResource();
            Repository repository = resource.getRepository();
            String pullUrl = resource.getUrl();
            String app = repository.getName();

            if(!ScanUtils.empty(application)){
                app = application;
            }

            BugTracker.Type bugType = BugTracker.Type.ADOPULL;
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

            String ref = resource.getSourceRefName();
            int index = StringUtils.ordinalIndexOf(ref, "/", 2);
            String currentBranch = ref.substring(index+1);
            String targetBranch = resource.getTargetRefName();
            int index2 = StringUtils.ordinalIndexOf(targetBranch, "/", 2);
            targetBranch = targetBranch.substring(index2+1);
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
            String gitUrl = repository.getWebUrl();
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
                    .namespace(repository.getProject().getName().replaceAll(" ","_"))
                    .repoName(repository.getName())
                    .repoUrl(gitUrl)
                    .repoUrlWithAuth(gitAuthUrl)
                    .repoType(ScanRequest.Repository.ADO)
                    .branch(currentBranch)
                    .refs(ref)
                    .mergeNoteUri(pullUrl.concat("/threads"))
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
            request.putAdditionalMetadata("statuses_url", pullUrl.concat("/statuses"));
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
    @PostMapping(value={"/{product}/ado/push","/ado/push"})
    public ResponseEntity<EventResponse> pushRequest(
            @RequestBody PushEvent body,
            @RequestHeader(value = AUTHORIZATION) String auth,
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
        log.info("Processing Azure PULL request");
        validateBasicAuth(auth);

        MachinaOverride o = ScanUtils.getMachinaOverride(override);

        try {
            Resource resource = body.getResource();
            Repository repository = resource.getRepository();
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

            //determine branch (without refs)
            String ref = resource.getRefUpdates().get(0).getName();
            int index = StringUtils.ordinalIndexOf(ref, "/", 2);
            String currentBranch = ref.substring(index+1);

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
            for(Commit c: resource.getCommits()){
                if (c.getAuthor() != null && !ScanUtils.empty(c.getAuthor().getEmail())){
                    emails.add(c.getAuthor().getEmail());
                }
            }
            emails.add(resource.getPushedBy().getUniqueName());

            //build request object
            String gitUrl = repository.getRemoteUrl();
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
                    .namespace(repository.getProject().getName().replaceAll(" ","_"))
                    .repoName(repository.getName())
                    .repoUrl(gitUrl)
                    .repoUrlWithAuth(gitAuthUrl)
                    .repoType(ScanRequest.Repository.ADO)
                    .branch(currentBranch)
                    .refs(ref)
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

    /** Validates the base64 / basic auth received in the request. */
    private void validateBasicAuth(String token){
        String auth = "Basic ".concat(Base64.getEncoder().encodeToString(properties.getWebhookToken().getBytes()));
        if(!auth.equals(token)){
            throw new InvalidTokenException();
        }
    }
}
