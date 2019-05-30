package com.checkmarx.flow.controller;

import com.checkmarx.flow.config.*;
import com.checkmarx.flow.dto.*;
import com.checkmarx.flow.dto.gitlab.Commit;
import com.checkmarx.flow.dto.gitlab.MergeEvent;
import com.checkmarx.flow.dto.gitlab.PushEvent;
import com.checkmarx.flow.exception.InvalidTokenException;
import com.checkmarx.flow.service.GitLabService;
import com.checkmarx.flow.service.FlowService;
import com.checkmarx.flow.utils.ScanUtils;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.beans.ConstructorProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


@RestController
@RequestMapping(value = "/")
public class GitLabController {

    private static final String TOKEN_HEADER = "X-Gitlab-Token";
    private static final String EVENT = "X-Gitlab-Event";
    private static final String PUSH = EVENT + "=Push Hook";
    private static final String MERGE = EVENT + "=Merge Request Hook";
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(GitLabController.class);
    private final FlowService flowService;
    private final GitLabProperties properties;
    private final CxProperties cxProperties;
    private final JiraProperties jiraProperties;
    private final FlowProperties flowProperties;

    @ConstructorProperties({"flowService", "properties", "cxProperties", "jiraProperties", "flowProperties"})
    public GitLabController(FlowService flowService, GitLabProperties properties, CxProperties cxProperties, JiraProperties jiraProperties, FlowProperties flowProperties) {
        this.flowService = flowService;
        this.properties = properties;
        this.cxProperties = cxProperties;
        this.jiraProperties = jiraProperties;
        this.flowProperties = flowProperties;
    }


    @GetMapping(value = "/test")
    public String getTest() {
        log.info("Build Info");
        return "IT WORKS";
    }

    /**
     * Merge Request event webhook submitted.
     */
    @PostMapping(value = {"/{product}","/"}, headers = MERGE)
    public ResponseEntity<EventResponse> mergeRequest(
            @RequestBody MergeEvent body,
            @RequestHeader(value = TOKEN_HEADER) String token,
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

        log.info("Processing GitLab MERGE request");
        validateGitLabRequest(token);
        MachinaOverride o = ScanUtils.getMachinaOverride(override);

        try {
            if(!body.getObjectAttributes().getState().equalsIgnoreCase("opened") ||
                    isWIP(body)){
                log.info("Merge requested not processed.  Status was not opened , or was WIP ({})", body.getObjectAttributes().getState());

                return ResponseEntity.status(HttpStatus.OK).body(EventResponse.builder()
                        .message("No processing occurred for updates to Merge Request")
                        .success(true)
                        .build());
            }
            String app = body.getRepository().getName();
            if(!ScanUtils.empty(application)){
                app = application;
            }

            BugTracker.Type bugType = BugTracker.Type.GITLABMERGE;
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
            String currentBranch = body.getObjectAttributes().getSourceBranch();
            String targetBranch = body.getObjectAttributes().getTargetBranch();
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

            String mergeEndpoint = properties.getApiUrl().concat(GitLabService.MERGE_NOTE_PATH);
            mergeEndpoint = mergeEndpoint.replace("{id}", body.getProject().getId().toString());
            mergeEndpoint = mergeEndpoint.replace("{iid}", body.getObjectAttributes().getIid().toString());
            String gitUrl = body.getProject().getGitHttpUrl();
            log.info("Using url: {}", gitUrl);
            String gitAuthUrl = gitUrl.replace("https://", "https://oauth2:".concat(properties.getToken()).concat("@"));
            gitAuthUrl = gitAuthUrl.replace("http://", "http://oauth2:".concat(properties.getToken()).concat("@"));
            String scanPreset = cxProperties.getScanPreset();
            if(!ScanUtils.empty(preset)){
                scanPreset = preset;
            }
            boolean inc = cxProperties.getIncremental();
            if(incremental != null){
                inc = incremental;
            }
            ScanRequest request = ScanRequest.builder()
                    .id(body.getProject().getId())
                    .application(app)
                    .product(p)
                    .project(project)
                    .team(team)
                    .namespace(body.getProject().getNamespace().replaceAll(" ","_"))
                    .repoName(body.getProject().getName())
                    .repoUrl(body.getProject().getGitHttpUrl())
                    .repoUrlWithAuth(gitAuthUrl)
                    .repoType(ScanRequest.Repository.GITLAB)
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
            request.putAdditionalMetadata("merge_id",body.getObjectAttributes().getIid().toString());
            request.putAdditionalMetadata("merge_title", body.getObjectAttributes().getTitle());

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
     * Push Request event webhook submitted.
     */
    @PostMapping(value = {"/{product}","/"}, headers = PUSH)
    public ResponseEntity<EventResponse> pushRequest(
            @RequestBody PushEvent body,
            @RequestHeader(value = TOKEN_HEADER) String token,
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
        validateGitLabRequest(token);

        MachinaOverride o = ScanUtils.getMachinaOverride(override);
        String commitEndpoint = null;
        try {
            String app = body.getRepository().getName();
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
            //extract branch from ref (refs/heads/master -> master)
            String currentBranch = body.getRef().split("/")[2];
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
            for(Commit c: body.getCommits()){
                if (c.getAuthor() != null && !ScanUtils.empty(c.getAuthor().getEmail())){
                    emails.add(c.getAuthor().getEmail());
                }
                if(!ScanUtils.empty(c.getUrl()) && bugType.equals(BugTracker.Type.GITLABCOMMIT)) {
                    commitEndpoint = properties.getApiUrl().concat(GitLabService.COMMIT_PATH);
                    commitEndpoint = commitEndpoint.replace("{id}", body.getProject().getId().toString());
                    commitEndpoint = commitEndpoint.replace("{sha}", c.getId());
                }
            }

            if(!ScanUtils.empty(body.getUserEmail())) {
                emails.add(body.getUserEmail());
            }
            String gitUrl = body.getProject().getGitHttpUrl();
            log.debug("Using url: {}", gitUrl);
            String gitAuthUrl = gitUrl.replace("https://", "https://oauth2:".concat(properties.getToken()).concat("@"));
            gitAuthUrl = gitAuthUrl.replace("http://", "http://oauth2:".concat(properties.getToken()).concat("@"));

            String scanPreset = cxProperties.getScanPreset();
            if(!ScanUtils.empty(preset)){
                scanPreset = preset;
            }
            boolean inc = cxProperties.getIncremental();
            if(incremental != null){
                inc = incremental;
            }

            ScanRequest request = ScanRequest.builder()
                    .id(body.getProjectId())
                    .application(app)
                    .product(p)
                    .project(project)
                    .team(team)
                    .namespace(body.getProject().getNamespace().replaceAll(" ","_"))
                    .repoName(body.getProject().getName())
                    .repoUrl(body.getProject().getGitHttpUrl())
                    .repoUrlWithAuth(gitAuthUrl)
                    .repoType(ScanRequest.Repository.GITLAB)
                    .branch(currentBranch)
                    .mergeNoteUri(commitEndpoint)
                    .refs(body.getRef())
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
                flowService.initiateAutomation(request);
            }

        }catch (IllegalArgumentException e){
            log.error("Error submitting Scan Request. Product option or BugTracker not valid {} | {}", product, bug);
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(EventResponse.builder()
                    .message("Error submitting Scan Request.  Product or Bugtracker option incorrect ".concat(product != null ? product : "").concat(" | ").concat(bug != null ? bug : ""))
                    .success(false)
                    .build());
        }
        return ResponseEntity.status(HttpStatus.OK).body(EventResponse.builder()
                .message("Scan Request Successfully Submitted")
                .success(true)
                .build());
    }

    /**
     * @param token
     */
    private void validateGitLabRequest(String token){
        log.info("Validating GitLab request token");
        if(!properties.getWebhookToken().equals(token)){
            log.error("GitLab request token validation failed");
            throw new InvalidTokenException();
        }
        log.info("Validation successful");
    }

    /**
     * Check if the merge event is being driven by updates to WIP status.
     * @param event
     * @return
     */
    private boolean isWIP(MergeEvent event){
        /*Merge has been marked WIP, ignoring*/
        if(event.getObjectAttributes().getWorkInProgress()){
            return true;
        }

        if(!properties.isBlockMerge()){ //skip looking for WIP changes
            return false;
        }
        /*Merge has been changed from WIP to not-WIP, ignoring*/
        else if(event.getChanges() != null && event.getChanges().getTitle() != null && event.getChanges().getTitle().getPrevious() != null &&
                    event.getChanges().getTitle().getPrevious().startsWith("WIP:CX|") &&
                    !event.getChanges().getTitle().getCurrent().startsWith("WIP:")){
            return true;
        }
        return false;
    }
}

