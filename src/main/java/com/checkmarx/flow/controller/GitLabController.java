package com.checkmarx.flow.controller;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitLabProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.EventResponse;
import com.checkmarx.flow.dto.FlowOverride;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.gitlab.*;
import com.checkmarx.flow.exception.InvalidTokenException;
import com.checkmarx.flow.service.FlowService;
import com.checkmarx.flow.service.GitLabService;
import com.checkmarx.flow.service.HelperService;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.dto.CxConfig;
import com.checkmarx.sdk.dto.Filter;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
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
    private final HelperService helperService;
    private final GitLabProperties properties;
    private final CxProperties cxProperties;
    private final JiraProperties jiraProperties;
    private final FlowProperties flowProperties;
    private final GitLabService gitLabService;

    public GitLabController(FlowService flowService,
                            HelperService helperService,
                            GitLabProperties properties,
                            CxProperties cxProperties,
                            JiraProperties jiraProperties,
                            FlowProperties flowProperties,
                            GitLabService gitLabService) {
        this.flowService = flowService;
        this.helperService = helperService;
        this.properties = properties;
        this.cxProperties = cxProperties;
        this.jiraProperties = jiraProperties;
        this.flowProperties = flowProperties;
        this.gitLabService = gitLabService;
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
        String uid = helperService.getShortUid();
        MDC.put("cx", uid);
        log.info("Processing GitLab MERGE request");
        validateGitLabRequest(token);
        FlowOverride o = ScanUtils.getMachinaOverride(override);

        try {
            ObjectAttributes objectAttributes = body.getObjectAttributes();
            if(!objectAttributes.getState().equalsIgnoreCase("opened") ||
                    isWIP(body)){
                log.info("Merge requested not processed.  Status was not opened , or was WIP ({})", objectAttributes.getState());

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
            String currentBranch = objectAttributes.getSourceBranch();
            String targetBranch = objectAttributes.getTargetBranch();
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

            if(excludeFiles == null && !ScanUtils.empty(cxProperties.getExcludeFiles())){
                excludeFiles = Arrays.asList(cxProperties.getExcludeFiles().split(","));
            }
            if(excludeFolders == null && !ScanUtils.empty(cxProperties.getExcludeFolders())){
                excludeFolders = Arrays.asList(cxProperties.getExcludeFolders().split(","));
            }

            Project proj = body.getProject();
            String mergeEndpoint = properties.getApiUrl().concat(GitLabService.MERGE_NOTE_PATH);
            mergeEndpoint = mergeEndpoint.replace("{id}", proj.getId().toString());
            mergeEndpoint = mergeEndpoint.replace("{iid}", objectAttributes.getIid().toString());
            String gitUrl = proj.getGitHttpUrl();
            log.info("Using url: {}", gitUrl);
            String gitAuthUrl = gitUrl.replace(Constants.HTTPS, Constants.HTTPS_OAUTH2.concat(properties.getToken()).concat("@"));
            gitAuthUrl = gitAuthUrl.replace(Constants.HTTP, Constants.HTTP_OAUTH2.concat(properties.getToken()).concat("@"));
            String scanPreset = cxProperties.getScanPreset();
            if(!ScanUtils.empty(preset)){
                scanPreset = preset;
            }
            boolean inc = cxProperties.getIncremental();
            if(incremental != null){
                inc = incremental;
            }
            ScanRequest request = ScanRequest.builder()
                    .id(proj.getId())
                    .application(app)
                    .product(p)
                    .project(project)
                    .team(team)
                    .namespace(proj.getNamespace().replaceAll(" ","_"))
                    .repoName(proj.getName())
                    .repoUrl(proj.getGitHttpUrl())
                    .repoUrlWithAuth(gitAuthUrl)
                    .repoType(ScanRequest.Repository.GITLAB)
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

            if(!ScanUtils.empty(preset)){
                request.setScanPreset(preset);
                request.setScanPresetOverride(true);
            }

            /*Check for Config as code (cx.config) and override*/
            CxConfig cxConfig =  gitLabService.getCxConfigOverride(request);
            request = ScanUtils.overrideCxConfig(request, cxConfig, flowProperties, jiraProperties);

            request.putAdditionalMetadata("merge_id",objectAttributes.getIid().toString());
            request.putAdditionalMetadata("merge_title", objectAttributes.getTitle());
            if(proj.getId() != null) {
                request.setRepoProjectId(proj.getId());
            }
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
        String uid = helperService.getShortUid();
        MDC.put("cx", uid);
        validateGitLabRequest(token);

        FlowOverride o = ScanUtils.getMachinaOverride(override);
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
            String currentBranch = ScanUtils.getBranchFromRef(body.getRef());
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

            if(excludeFiles == null && !ScanUtils.empty(cxProperties.getExcludeFiles())){
                excludeFiles = Arrays.asList(cxProperties.getExcludeFiles().split(","));
            }
            if(excludeFolders == null && !ScanUtils.empty(cxProperties.getExcludeFolders())){
                excludeFolders = Arrays.asList(cxProperties.getExcludeFolders().split(","));
            }

            Project proj = body.getProject();
            /*Determine emails*/
            List<String> emails = new ArrayList<>();
            for(Commit c: body.getCommits()){
                Author author = c.getAuthor();
                if (author != null && !ScanUtils.empty(author.getEmail())){
                    emails.add(author.getEmail());
                }
                if(!ScanUtils.empty(c.getUrl()) && bugType.equals(BugTracker.Type.GITLABCOMMIT)) {
                    commitEndpoint = properties.getApiUrl().concat(GitLabService.COMMIT_PATH);
                    commitEndpoint = commitEndpoint.replace("{id}", proj.getId().toString());
                    commitEndpoint = commitEndpoint.replace("{sha}", c.getId());
                }
            }

            if(!ScanUtils.empty(body.getUserEmail())) {
                emails.add(body.getUserEmail());
            }
            String gitUrl = proj.getGitHttpUrl();
            log.debug("Using url: {}", gitUrl);
            String gitAuthUrl = gitUrl.replace(Constants.HTTPS, Constants.HTTPS_OAUTH2.concat(properties.getToken()).concat("@"));
            gitAuthUrl = gitAuthUrl.replace(Constants.HTTP, Constants.HTTP_OAUTH2.concat(properties.getToken()).concat("@"));

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
                    .namespace(proj.getNamespace().replaceAll(" ","_"))
                    .repoName(proj.getName())
                    .repoUrl(proj.getGitHttpUrl())
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

            if(!ScanUtils.empty(preset)){
                request.setScanPreset(preset);
                request.setScanPresetOverride(true);
            }

            /*Check for Config as code (cx.config) and override*/
            CxConfig cxConfig =  gitLabService.getCxConfigOverride(request);
            request = ScanUtils.overrideCxConfig(request, cxConfig, flowProperties, jiraProperties);

            request.setId(uid);
            if(proj.getId() != null) {
                request.setRepoProjectId(proj.getId());
            }
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
        Changes changes = event.getChanges();
        if(!properties.isBlockMerge()){ //skip looking for WIP changes
            return false;
        }
        /*Merge has been changed from WIP to not-WIP, ignoring*/
        else if(changes != null && changes.getTitle() != null && changes.getTitle().getPrevious() != null &&
                changes.getTitle().getPrevious().startsWith("WIP:CX|") &&
                    !changes.getTitle().getCurrent().startsWith("WIP:")){
            return true;
        }
        return false;
    }
}

