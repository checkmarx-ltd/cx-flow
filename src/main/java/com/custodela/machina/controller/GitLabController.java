package com.custodela.machina.controller;

import com.custodela.machina.config.CxProperties;
import com.custodela.machina.config.GitLabProperties;
import com.custodela.machina.config.JiraProperties;
import com.custodela.machina.config.MachinaProperties;
import com.custodela.machina.dto.*;
import com.custodela.machina.dto.gitlab.Commit;
import com.custodela.machina.dto.gitlab.MergeEvent;
import com.custodela.machina.dto.gitlab.PushEvent;
import com.custodela.machina.exception.InvalidTokenException;
import com.custodela.machina.exception.MachinaRuntimeException;
import com.custodela.machina.service.GitLabService;
import com.custodela.machina.service.MachinaService;
import com.custodela.machina.utils.ScanUtils;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.beans.ConstructorProperties;
import java.util.ArrayList;
import java.util.List;


@RestController
@RequestMapping(value = "/")
public class GitLabController {

    private static final String TOKEN_HEADER = "X-Gitlab-Token";
    private static final String EVENT = "X-Gitlab-Event";
    private static final String PUSH = EVENT + "=Push Hook";
    private static final String MERGE = EVENT + "=Merge Request Hook";
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(GitLabController.class);
    private final MachinaService machinaService;
    private final GitLabProperties properties;
    private final CxProperties cxProperties;
    private final JiraProperties jiraProperties;
    private final MachinaProperties machinaProperties;

    @ConstructorProperties({"machinaService", "properties", "cxProperties", "jiraProperties", "machinaProperties"})
    public GitLabController(MachinaService machinaService, GitLabProperties properties, CxProperties cxProperties, JiraProperties jiraProperties, MachinaProperties machinaProperties) {
        this.machinaService = machinaService;
        this.properties = properties;
        this.cxProperties = cxProperties;
        this.jiraProperties = jiraProperties;
        this.machinaProperties = machinaProperties;
    }


    @GetMapping(value = "/test")
    public String getTest() {
        log.info("Build Info");
        return "IT WORKS";
    }

    /**
     * Merge Request event webhook submitted.
     */
    @PostMapping(value = "/{product}", headers = MERGE)
    public ResponseEntity<EventResponse> mergeRequest(
            @RequestBody MergeEvent body,
            @RequestHeader(value = TOKEN_HEADER) String token,
            @PathVariable("product") String product,
            @RequestParam(value = "application", required = false) String application,
            @RequestParam(value = "branch", required = false) List<String> branch,
            @RequestParam(value = "severity", required = false) List<String> severity,
            @RequestParam(value = "cwe", required = false) List<String> cwe,
            @RequestParam(value = "category", required = false) List<String> category,
            @RequestParam(value = "status", required = false) List<String> status,
            @RequestParam(value = "assignee", required = false) String assignee,
            @RequestParam(value = "exclude-files", required = false) List<String> excludeFiles,
            @RequestParam(value = "exclude-folders", required = false) List<String> excludeFolders,
            @RequestParam(value = "override", required = false) String override,
            @RequestParam(value = "bug", required = false) String bug

    ){

        log.info("Processing GitLab MERGE request");
        validateGitLabRequest(token);
        MachinaOverride o = ScanUtils.getMachinaOverride(override);

        try {
            if(!body.getObjectAttributes().getState().equalsIgnoreCase("opened")){
                log.info("Merge requested not processed.  Status was not opened ({})", body.getObjectAttributes().getState());
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
                bugType = BugTracker.Type.valueOf(bug.toUpperCase());
            }
            ScanRequest.Product p = ScanRequest.Product.valueOf(product.toUpperCase());
            String currentBranch = body.getObjectAttributes().getSourceBranch();
            String targetBranch = body.getObjectAttributes().getTargetBranch();
            List<String> branches = new ArrayList<>();
            List<Filter> filters;

            if(!ScanUtils.empty(branch)){
                branches.addAll(branch);
            }
            else if(!ScanUtils.empty(machinaProperties.getBranches())){
                branches.addAll(machinaProperties.getBranches());
            }

            BugTracker bt = ScanUtils.getBugTracker(assignee, bugType, jiraProperties);

            /*Determine filters, if any*/
            if(!ScanUtils.empty(severity) || !ScanUtils.empty(cwe) || !ScanUtils.empty(category) || !ScanUtils.empty(status)){
                filters = ScanUtils.getFilters(severity, cwe, category, status);
            }
            else{
                filters = ScanUtils.getFilters(machinaProperties.getFilterSeverity(), machinaProperties.getFilterCwe(),
                        machinaProperties.getFilterCategory(), machinaProperties.getFilterStatus());
            }

            String mergeEndpoint = properties.getApiUrl().concat(GitLabService.MERGE_PATH);
            mergeEndpoint = mergeEndpoint.replace("{id}", body.getProject().getId().toString());
            mergeEndpoint = mergeEndpoint.replace("{iid}", body.getObjectAttributes().getIid().toString());
            String gitUrl = body.getProject().getGitHttpUrl();
            log.info("Using url: {}", gitUrl);
            String gitAuthUrl = gitUrl.replace("https://", "https://oauth2:".concat(properties.getToken()).concat("@"));
            gitAuthUrl = gitAuthUrl.replace("http://", "http://oauth2:".concat(properties.getToken()).concat("@"));
            ScanRequest request = ScanRequest.builder()
                    .id(body.getProject().getId())
                    .application(app)
                    .product(p)
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
                    .incremental(cxProperties.getIcremental()) //todo handle incremental
                    .scanPreset(cxProperties.getScanPreset())
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
     * Push Request event webhook submitted.
     */
    @PostMapping(value = "/{product}", headers = PUSH)
    public ResponseEntity<EventResponse> pushRequest(
            @RequestBody PushEvent body,
            @RequestHeader(value = TOKEN_HEADER) String token,
            @PathVariable("product") String product,
            @RequestParam(value = "application", required = false) String application,
            @RequestParam(value = "branch", required = false) List<String> branch,
            @RequestParam(value = "severity", required = false) List<String> severity,
            @RequestParam(value = "cwe", required = false) List<String> cwe,
            @RequestParam(value = "category", required = false) List<String> category,
            @RequestParam(value = "status", required = false) List<String> status,
            @RequestParam(value = "assignee", required = false) String assignee,
            @RequestParam(value = "exclude-files", required = false) List<String> excludeFiles,
            @RequestParam(value = "exclude-folders", required = false) List<String> excludeFolders,
            @RequestParam(value = "override", required = false) String override,
            @RequestParam(value = "bug", required = false) String bug
        ){
        validateGitLabRequest(token);

        MachinaOverride o = ScanUtils.getMachinaOverride(override);
        String commitEndpoint = null;
        try {
            String app = body.getRepository().getName();
            if(!ScanUtils.empty(application)){
                app = application;
            }
            BugTracker.Type bugType = BugTracker.Type.valueOf(machinaProperties.getBugTracker().toUpperCase());
            if(!ScanUtils.empty(bug)){
                bugType = BugTracker.Type.valueOf(bug.toUpperCase());
            }
            ScanRequest.Product p = ScanRequest.Product.valueOf(product.toUpperCase());
            //extract branch from ref (refs/heads/master -> master)
            String currentBranch = body.getRef().split("/")[2];
            List<String> branches = new ArrayList<>();
            List<Filter> filters;

            if(!ScanUtils.empty(branch)){
                branches.addAll(branch);
            }
            else if(!ScanUtils.empty(machinaProperties.getBranches())){
                branches.addAll(machinaProperties.getBranches());
            }

            BugTracker bt = ScanUtils.getBugTracker(assignee, bugType, jiraProperties);
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
            for(Commit c: body.getCommits()){
                if (c.getAuthor() != null && !ScanUtils.empty(c.getAuthor().getEmail())){
                    emails.add(c.getAuthor().getEmail());
                }
                if(!ScanUtils.empty(c.getUrl())){
                    if(bugType.equals(BugTracker.Type.GITLABCOMMIT)) {
                        commitEndpoint = properties.getApiUrl().concat(GitLabService.COMMIT_PATH);
                        commitEndpoint = commitEndpoint.replace("{id}", body.getProject().getId().toString());
                        commitEndpoint = commitEndpoint.replace("{sha}", c.getId());
                    }
                }
            }

            if(!ScanUtils.empty(body.getUserEmail())) {
                emails.add(body.getUserEmail());
            }
            String gitUrl = body.getProject().getGitHttpUrl();
            log.info("Using url: {}", gitUrl);
            String gitAuthUrl = gitUrl.replace("https://", "https://oauth2:".concat(properties.getToken()).concat("@"));
            gitAuthUrl = gitAuthUrl.replace("http://", "http://oauth2:".concat(properties.getToken()).concat("@"));
            ScanRequest request = ScanRequest.builder()
                    .id(body.getProjectId())
                    .application(app)
                    .product(p)
                    .namespace(body.getProject().getNamespace().replaceAll(" ","_"))
                    .repoName(body.getProject().getName())
                    .repoUrl(body.getProject().getGitHttpUrl())
                    .repoUrlWithAuth(gitAuthUrl)
                    .repoType(ScanRequest.Repository.GITLAB)
                    .branch(currentBranch)
                    .mergeNoteUri(commitEndpoint)
                    .refs(body.getRef())
                    .email(emails)
                    .incremental(cxProperties.getIcremental())
                    .scanPreset(cxProperties.getScanPreset())
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

}
