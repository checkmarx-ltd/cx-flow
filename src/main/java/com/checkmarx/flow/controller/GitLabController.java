package com.checkmarx.flow.controller;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitLabProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ControllerRequest;
import com.checkmarx.flow.dto.EventResponse;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.gitlab.*;
import com.checkmarx.flow.exception.InvalidTokenException;
import com.checkmarx.flow.service.FilterFactory;
import com.checkmarx.flow.service.FlowService;
import com.checkmarx.flow.service.GitLabService;
import com.checkmarx.flow.service.HelperService;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.dto.CxConfig;
import com.checkmarx.sdk.dto.filtering.FilterConfiguration;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


@RestController
@RequestMapping(value = "/")
@RequiredArgsConstructor
public class GitLabController extends WebhookController {

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
    private final FilterFactory filterFactory;

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
            ControllerRequest controllerRequest
    ){
        String uid = helperService.getShortUid();
        MDC.put("cx", uid);
        log.info("Processing GitLab MERGE request");
        validateGitLabRequest(token);
        controllerRequest = ensureNotNull(controllerRequest);

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
            if(StringUtils.isNotEmpty(controllerRequest.getApplication())){
                app = controllerRequest.getApplication();
            }

            BugTracker.Type bugType = BugTracker.Type.GITLABMERGE;
            if (StringUtils.isNotEmpty(controllerRequest.getBug())) {
                bugType = ScanUtils.getBugTypeEnum(controllerRequest.getBug(), flowProperties.getBugTrackerImpl());
            }

            if(controllerRequest.getAppOnly() != null){
                flowProperties.setTrackApplicationOnly(controllerRequest.getAppOnly());
            }

            if(ScanUtils.empty(product)){
                product = ScanRequest.Product.CX.getProduct();
            }
            ScanRequest.Product p = ScanRequest.Product.valueOf(product.toUpperCase(Locale.ROOT));
            String currentBranch = objectAttributes.getSourceBranch();
            String targetBranch = objectAttributes.getTargetBranch();
            String defaultBranch = objectAttributes.getTarget().getDefaultBranch();

            List<String> branches = getBranches(controllerRequest, flowProperties);

            BugTracker bt = ScanUtils.getBugTracker(controllerRequest.getAssignee(), bugType, jiraProperties, controllerRequest.getBug());

            FilterConfiguration filter = filterFactory.getFilter(controllerRequest.getSeverity(), controllerRequest.getCwe(), controllerRequest.getCategory(), controllerRequest.getStatus(), null, flowProperties);

            setExclusionProperties(cxProperties, controllerRequest);

            Project proj = body.getProject();
            String mergeEndpoint = properties.getApiUrl().concat(GitLabService.MERGE_NOTE_PATH);
            mergeEndpoint = mergeEndpoint.replace("{id}", proj.getId().toString());
            mergeEndpoint = mergeEndpoint.replace("{iid}", objectAttributes.getIid().toString());
            String gitUrl = proj.getGitHttpUrl();
            log.info("Using url: {}", gitUrl);
            String gitAuthUrl = gitUrl.replace(Constants.HTTPS, Constants.HTTPS_OAUTH2.concat(properties.getToken()).concat("@"));
            gitAuthUrl = gitAuthUrl.replace(Constants.HTTP, Constants.HTTP_OAUTH2.concat(properties.getToken()).concat("@"));
            String scanPreset = cxProperties.getScanPreset();
            if(StringUtils.isNotEmpty(controllerRequest.getPreset())){
                scanPreset = controllerRequest.getPreset();
            }

            ScanRequest request = ScanRequest.builder()
                    .id(String.valueOf(proj.getId()))
                    .application(app)
                    .product(p)
                    .project(controllerRequest.getProject())
                    .team(controllerRequest.getTeam())
                    .namespace(proj.getNamespace().replace(" ","_"))
                    .repoName(proj.getName())
                    .repoUrl(proj.getGitHttpUrl())
                    .repoUrlWithAuth(gitAuthUrl)
                    .repoType(ScanRequest.Repository.GITLAB)
                    .branch(currentBranch)
                    .defaultBranch(defaultBranch)
                    .mergeTargetBranch(targetBranch)
                    .mergeNoteUri(mergeEndpoint)
                    .refs(Constants.CX_BRANCH_PREFIX.concat(currentBranch))
                    .email(null)
                    .incremental(isScanIncremental(controllerRequest, cxProperties))
                    .scanPreset(scanPreset)
                    .excludeFolders(controllerRequest.getExcludeFolders())
                    .excludeFiles(controllerRequest.getExcludeFiles())
                    .bugTracker(bt)
                    .filter(filter)
                    .build();

            overrideScanPreset(controllerRequest, request);

            /*Check for Config as code (cx.config) and override*/
            CxConfig cxConfig =  gitLabService.getCxConfigOverride(request);
            request = ScanUtils.overrideCxConfig(request, cxConfig, flowProperties);

            request.putAdditionalMetadata(ScanUtils.WEB_HOOK_PAYLOAD, body.toString());
            request.putAdditionalMetadata("merge_id",objectAttributes.getIid().toString());
            request.putAdditionalMetadata("merge_title", objectAttributes.getTitle());
            if(proj.getId() != null) {
                request.setRepoProjectId(proj.getId());
            }
            request.setId(uid);
            if(helperService.isBranch2Scan(request, branches)){
                flowService.initiateAutomation(request);
            }

        } catch (IllegalArgumentException e) {
            return getBadRequestMessage(e, controllerRequest, product);
        }
        return getSuccessMessage();
    }

    /**
     * Push Request event webhook submitted.
     */
    @PostMapping(value = {"/{product}","/"}, headers = PUSH)
    public ResponseEntity<EventResponse> pushRequest(
            @RequestBody PushEvent body,
            @RequestHeader(value = TOKEN_HEADER) String token,
            @PathVariable(value = "product", required = false) String product,
            ControllerRequest controllerRequest
    ){
        String uid = helperService.getShortUid();
        MDC.put("cx", uid);
        validateGitLabRequest(token);
        controllerRequest = ensureNotNull(controllerRequest);

        String commitEndpoint = null;
        try {
            String app = body.getRepository().getName();
            if(StringUtils.isNotEmpty(controllerRequest.getApplication())){
                app = controllerRequest.getApplication();
            }

            //set the default bug tracker as per yml
            setBugTracker(flowProperties, controllerRequest);
            BugTracker.Type bugType = ScanUtils.getBugTypeEnum(controllerRequest.getBug(), flowProperties.getBugTrackerImpl());

            if(controllerRequest.getAppOnly() != null){
                flowProperties.setTrackApplicationOnly(controllerRequest.getAppOnly());
            }
            if(ScanUtils.empty(product)){
                product = ScanRequest.Product.CX.getProduct();
            }
            ScanRequest.Product p = ScanRequest.Product.valueOf(product.toUpperCase(Locale.ROOT));
            //extract branch from ref (refs/heads/master -> master)
            String currentBranch = ScanUtils.getBranchFromRef(body.getRef());
            List<String> branches = new ArrayList<>();

            if(CollectionUtils.isNotEmpty(controllerRequest.getBranch())){
                branches.addAll(controllerRequest.getBranch());
            }
            else if(CollectionUtils.isNotEmpty(flowProperties.getBranches())){
                branches.addAll(flowProperties.getBranches());
            }

            BugTracker bt = ScanUtils.getBugTracker(controllerRequest.getAssignee(), bugType, jiraProperties, controllerRequest.getBug());
            FilterConfiguration filter = filterFactory.getFilter(controllerRequest.getSeverity(), controllerRequest.getCwe(), controllerRequest.getCategory(), controllerRequest.getStatus(), null, flowProperties);

            setExclusionProperties(cxProperties, controllerRequest);

            Project proj = body.getProject();
            /*Determine emails*/
            List<String> emails = new ArrayList<>();
            for(Commit c: body.getCommits()){
                Author author = c.getAuthor();
                if (author != null && StringUtils.isNotEmpty(author.getEmail())){
                    emails.add(author.getEmail());
                }
                if(StringUtils.isNotEmpty(c.getUrl()) && bugType.equals(BugTracker.Type.GITLABCOMMIT)) {
                    commitEndpoint = properties.getApiUrl().concat(GitLabService.COMMIT_PATH);
                    commitEndpoint = commitEndpoint.replace("{id}", proj.getId().toString());
                    commitEndpoint = commitEndpoint.replace("{sha}", c.getId());
                }
            }

            if(StringUtils.isNotEmpty(body.getUserEmail())) {
                emails.add(body.getUserEmail());
            }
            String gitUrl = proj.getGitHttpUrl();
            log.debug("Using url: {}", gitUrl);
            String gitAuthUrl = gitUrl.replace(Constants.HTTPS, Constants.HTTPS_OAUTH2.concat(properties.getToken()).concat("@"));
            gitAuthUrl = gitAuthUrl.replace(Constants.HTTP, Constants.HTTP_OAUTH2.concat(properties.getToken()).concat("@"));

            String scanPreset = cxProperties.getScanPreset();
            if(StringUtils.isNotEmpty(controllerRequest.getPreset())){
                scanPreset = controllerRequest.getPreset();
            }

            ScanRequest request = ScanRequest.builder()
                    .id(String.valueOf(body.getProjectId()))
                    .application(app)
                    .product(p)
                    .project(controllerRequest.getProject())
                    .team(controllerRequest.getTeam())
                    .namespace(proj.getNamespace().replace(" ","_"))
                    .repoName(proj.getName())
                    .repoUrl(proj.getGitHttpUrl())
                    .repoUrlWithAuth(gitAuthUrl)
                    .repoType(ScanRequest.Repository.GITLAB)
                    .branch(currentBranch)
                    .mergeNoteUri(commitEndpoint)
                    .refs(body.getRef())
                    .email(emails)
                    .incremental(isScanIncremental(controllerRequest, cxProperties))
                    .scanPreset(scanPreset)
                    .excludeFolders(controllerRequest.getExcludeFolders())
                    .excludeFiles(controllerRequest.getExcludeFiles())
                    .bugTracker(bt)
                    .filter(filter)
                    .build();

            if(StringUtils.isNotEmpty(controllerRequest.getPreset())){
                request.setScanPreset(controllerRequest.getPreset());
                request.setScanPresetOverride(true);
            }

            /*Check for Config as code (cx.config) and override*/
            CxConfig cxConfig =  gitLabService.getCxConfigOverride(request);
            request = ScanUtils.overrideCxConfig(request, cxConfig, flowProperties);

            request.putAdditionalMetadata(ScanUtils.WEB_HOOK_PAYLOAD, body.toString());
            request.setId(uid);
            if(proj.getId() != null) {
                request.setRepoProjectId(proj.getId());
            }
            if(helperService.isBranch2Scan(request, branches)){
                flowService.initiateAutomation(request);
            }
        } catch (IllegalArgumentException e) {
            return getBadRequestMessage(e, controllerRequest, product);
        }
        return getSuccessMessage();
    }

    private void validateGitLabRequest(String token){
        log.info("Validating GitLab request token");
        if(!properties.getWebhookToken().equals(token)){
            log.error("GitLab request token validation failed");
            throw new InvalidTokenException();
        }
        log.info("Validation successful");
    }

    /**
     * Check if the merge event is being driven by updates to 'work item in progress' status.
     */
    private boolean isWIP(MergeEvent event){
        /*Merge has been marked WIP, ignoring*/
        Boolean inProgress = event.getObjectAttributes().getWorkInProgress();
        if (Boolean.TRUE.equals(inProgress)) {
            return true;
        }
        Changes changes = event.getChanges();
        if(!properties.isBlockMerge()){ //skip looking for WIP changes
            return false;
        }
        /*Merge has been changed from WIP to not-WIP, ignoring*/
        else return changes != null && changes.getTitle() != null && changes.getTitle().getPrevious() != null &&
                changes.getTitle().getPrevious().startsWith("WIP:CX|") &&
                !changes.getTitle().getCurrent().startsWith("WIP:");
    }
}

