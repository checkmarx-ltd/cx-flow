package com.checkmarx.flow.controller;

import com.checkmarx.flow.config.properties.FlowProperties;
import com.checkmarx.flow.config.properties.GitLabProperties;
import com.checkmarx.flow.config.properties.JiraProperties;
import com.checkmarx.flow.config.ScmConfigOverrider;
import com.checkmarx.flow.constants.FlowConstants;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ControllerRequest;
import com.checkmarx.flow.dto.EventResponse;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.gitlab.*;
import com.checkmarx.flow.exception.InvalidTokenException;
import com.checkmarx.flow.service.*;
import com.checkmarx.flow.utils.HTMLHelper;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.Constants;

import com.checkmarx.sdk.dto.sast.CxConfig;
import com.checkmarx.sdk.dto.filtering.FilterConfiguration;
import lombok.RequiredArgsConstructor;
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
    private final JiraProperties jiraProperties;
    private final FlowProperties flowProperties;
    private final GitLabService gitLabService;
    private final FilterFactory filterFactory;
    private final ConfigurationOverrider configOverrider;
    private final ScmConfigOverrider scmConfigOverrider;
    private final GitAuthUrlGenerator gitAuthUrlGenerator;

    @GetMapping(value = "/test")
    public String getTest() {
        log.debug("Build Info");
        return "IT WORKS";
    }

    /**
     * Merge Request event webhook submitted.
     */
    @PostMapping(value = {"/{product}", "/"}, headers = MERGE)
    public ResponseEntity<EventResponse> mergeRequest(
            @RequestBody MergeEvent body,
            @RequestHeader(value = TOKEN_HEADER) String token,
            @PathVariable(value = "product", required = false) String product,
            ControllerRequest controllerRequest
    ) {
        String uid = helperService.getShortUid();
        MDC.put(FlowConstants.MAIN_MDC_ENTRY, uid);
        log.info("Processing GitLab MERGE request");
        controllerRequest = ensureNotNull(controllerRequest);
        validateGitLabRequest(token, controllerRequest);

        try {
            ObjectAttributes objectAttributes = body.getObjectAttributes();
            if (!objectAttributes.getState().equalsIgnoreCase("opened") ||
                    isWIP(body)) {
                log.info("Merge requested not processed.  Status was not opened , or was WIP ({})", objectAttributes.getState());

                return ResponseEntity.status(HttpStatus.OK).body(EventResponse.builder()
                        .message("No processing occurred for updates to Merge Request")
                        .success(true)
                        .build());
            }
            String app = body.getRepository().getName();
            if (StringUtils.isNotEmpty(controllerRequest.getApplication())) {
                app = controllerRequest.getApplication();
            }

            BugTracker.Type bugType = BugTracker.Type.GITLABMERGE;
            if (StringUtils.isNotEmpty(controllerRequest.getBug())) {
                bugType = ScanUtils.getBugTypeEnum(controllerRequest.getBug(), flowProperties.getBugTrackerImpl());
            }

            if (controllerRequest.getAppOnly() != null) {
                flowProperties.setTrackApplicationOnly(controllerRequest.getAppOnly());
            }

            if (ScanUtils.empty(product)) {
                product = ScanRequest.Product.CX.getProduct();
            }
            ScanRequest.Product p = ScanRequest.Product.valueOf(product.toUpperCase(Locale.ROOT));
            String currentBranch = objectAttributes.getSourceBranch();
            String targetBranch = objectAttributes.getTargetBranch();
            String defaultBranch = objectAttributes.getTarget().getDefaultBranch();

            List<String> branches = getBranches(controllerRequest, flowProperties);

            BugTracker bt = ScanUtils.getBugTracker(controllerRequest.getAssignee(), bugType, jiraProperties, controllerRequest.getBug());

            FilterConfiguration filter = filterFactory.getFilter(controllerRequest, flowProperties);

            Project proj = body.getProject();
            String gitUrl = proj.getGitHttpUrl();

            log.info("Using url: {}", gitUrl);
            String configToken = scmConfigOverrider.determineConfigToken(properties, controllerRequest.getScmInstance());
            String gitAuthUrl = gitAuthUrlGenerator.addCredToUrl(ScanRequest.Repository.GITLAB, gitUrl, configToken);

            ScanRequest request = ScanRequest.builder()
                    .id(String.valueOf(proj.getId()))
                    .application(app)
                    .product(p)
                    .project(controllerRequest.getProject())
                    .team(controllerRequest.getTeam())
                    .namespace(proj.getNamespace().replace(" ", "_"))
                    .repoName(proj.getName())
                    .repoUrl(proj.getGitHttpUrl())
                    .repoUrlWithAuth(gitAuthUrl)
                    .repoType(ScanRequest.Repository.GITLAB)
                    .branch(currentBranch)
                    .defaultBranch(defaultBranch)
                    .mergeTargetBranch(targetBranch)
                    .refs(Constants.CX_BRANCH_PREFIX.concat(currentBranch))
                    .email(null)
                    .incremental(controllerRequest.getIncremental())
                    .scanPreset(controllerRequest.getPreset())
                    .excludeFolders(controllerRequest.getExcludeFolders())
                    .excludeFiles(controllerRequest.getExcludeFiles())
                    .bugTracker(bt)
                    .filter(filter)
                    .organizationId(getOrganizationId(proj))
                    .gitUrl(gitUrl)
                    .hash(objectAttributes.getLastCommit().getId())
                    .build();

            setMergeEndPointUri(objectAttributes, proj, request);

            setScmInstance(controllerRequest, request);

            if (proj.getId() != null) {
                request.setRepoProjectId(proj.getId());
            }

            /*Check for Config as code (cx.config) and override*/
            CxConfig cxConfig = gitLabService.getCxConfigOverride(request);
            request = configOverrider.overrideScanRequestProperties(cxConfig, request);

            request.putAdditionalMetadata(HTMLHelper.WEB_HOOK_PAYLOAD, body.toString());
            request.putAdditionalMetadata(FlowConstants.MERGE_ID, objectAttributes.getIid().toString());
            request.putAdditionalMetadata(FlowConstants.MERGE_TITLE, objectAttributes.getTitle());

            request.setId(uid);
            if (helperService.isBranch2Scan(request, branches)) {
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
    @PostMapping(value = {"/{product}", "/"}, headers = PUSH)
    public ResponseEntity<EventResponse> pushRequest(
            @RequestBody PushEvent body,
            @RequestHeader(value = TOKEN_HEADER) String token,
            @PathVariable(value = "product", required = false) String product,
            ControllerRequest controllerRequest
    ) {
        String uid = helperService.getShortUid();
        MDC.put(FlowConstants.MAIN_MDC_ENTRY, uid);
        controllerRequest = ensureNotNull(controllerRequest);
        validateGitLabRequest(token, controllerRequest);

        try {
            String app;
            if (body != null && body.getRepository() != null) {
                app = body.getRepository().getName();
            } else {
                throw new IllegalArgumentException("Request body or request repository cannot be null");
            }

            if (StringUtils.isNotEmpty(controllerRequest.getApplication())) {
                app = controllerRequest.getApplication();
            }

            //set the default bug tracker as per yml
            setBugTracker(flowProperties, controllerRequest);
            BugTracker.Type bugType = ScanUtils.getBugTypeEnum(controllerRequest.getBug(), flowProperties.getBugTrackerImpl());

            if (controllerRequest.getAppOnly() != null) {
                flowProperties.setTrackApplicationOnly(controllerRequest.getAppOnly());
            }
            if (ScanUtils.empty(product)) {
                product = ScanRequest.Product.CX.getProduct();
            }
            ScanRequest.Product scanRequestProduct = ScanRequest.Product.valueOf(product.toUpperCase(Locale.ROOT));
            //extract branch from ref (refs/heads/master -> master)
            String currentBranch = ScanUtils.getBranchFromRef(body.getRef());
            List<String> branches = getBranches(controllerRequest, flowProperties);

            BugTracker bugTracker = ScanUtils.getBugTracker(controllerRequest.getAssignee(), bugType, jiraProperties, controllerRequest.getBug());
            FilterConfiguration filter = filterFactory.getFilter(controllerRequest, flowProperties);

            Project project = body.getProject();

            String gitUrl = project.getGitHttpUrl();
            log.debug("Using url: {}", gitUrl);
            String configToken = scmConfigOverrider.determineConfigToken(properties, controllerRequest.getScmInstance());
            String gitAuthUrl = gitAuthUrlGenerator.addCredToUrl(ScanRequest.Repository.GITLAB, gitUrl, configToken);

            ScanRequest request = ScanRequest.builder()
                    .id(String.valueOf(body.getProjectId()))
                    .application(app)
                    .product(scanRequestProduct)
                    .project(controllerRequest.getProject())
                    .team(controllerRequest.getTeam())
                    .namespace(project.getNamespace().replace(" ", "_"))
                    .repoName(project.getName())
                    .repoUrl(project.getGitHttpUrl())
                    .repoUrlWithAuth(gitAuthUrl)
                    .repoType(ScanRequest.Repository.GITLAB)
                    .branch(currentBranch)
                    .refs(body.getRef())
                    .incremental(controllerRequest.getIncremental())
                    .scanPreset(controllerRequest.getPreset())
                    .excludeFolders(controllerRequest.getExcludeFolders())
                    .excludeFiles(controllerRequest.getExcludeFiles())
                    .bugTracker(bugTracker)
                    .filter(filter)
                    .organizationId(getOrganizationId(project))
                    .gitUrl(gitUrl)
                    .hash(body.getAfter())
                    .build();

            /*Determine emails*/
            List<String> emails = new ArrayList<>();
            String commitEndpoint = null;
            commitEndpoint = parseCommits(body, bugType, project, request, emails, commitEndpoint);
            setUserEmail(body, request, emails);

            request.setMergeNoteUri(commitEndpoint);
            request.setEmail(emails);

            setScmInstance(controllerRequest, request);

            if (StringUtils.isNotEmpty(controllerRequest.getPreset())) {
                request.setScanPreset(controllerRequest.getPreset());
                request.setScanPresetOverride(true);
            }

            if (project.getId() != null) {
                request.setRepoProjectId(project.getId());
            }

            /*Check for Config as code (cx.config) and override*/
            CxConfig cxConfig = gitLabService.getCxConfigOverride(request);
            request = configOverrider.overrideScanRequestProperties(cxConfig, request);

            request.putAdditionalMetadata(HTMLHelper.WEB_HOOK_PAYLOAD, body.toString());
            request.setId(uid);

            if (helperService.isBranch2Scan(request, branches)) {
                flowService.initiateAutomation(request);
            }
        } catch (IllegalArgumentException e) {
            return getBadRequestMessage(e, controllerRequest, product);
        }
        return getSuccessMessage();
    }

    private String getOrganizationId(Project proj) {
        // Cannot use the 'namespace' field here, because it's for display only and won't work in GitLab API calls.
        // pathWithNamespace may look like the following, depending on project location:
        //      my-username/personal-project
        //      my-group/sample-project
        //      my-group/my-subgroup/sample-project
        return StringUtils.substringBefore(proj.getPathWithNamespace(), "/");
    }

    private String parseCommits(@RequestBody PushEvent body, BugTracker.Type bugType, Project project, ScanRequest request, List<String> emails, String commitEndpoint) {
        for (Commit c : body.getCommits()) {
            Author author = c.getAuthor();
            if (author != null && StringUtils.isNotEmpty(author.getEmail())) {
                emails.add(author.getEmail());
            }

            if (StringUtils.isNotEmpty(c.getUrl()) && bugType.equals(BugTracker.Type.GITLABCOMMIT)) {
                commitEndpoint = scmConfigOverrider.determineConfigApiUrl(properties, request).concat(GitLabService.COMMIT_PATH);
                commitEndpoint = commitEndpoint.replace("{id}", project.getId().toString());
                commitEndpoint = commitEndpoint.replace("{sha}", c.getId());
            }
        }

        return commitEndpoint;
    }

    private void setUserEmail(@RequestBody PushEvent body, ScanRequest request, List<String> emails) {
        if (StringUtils.isNotEmpty(body.getUserEmail())) {
            String pusherEmail = body.getUserEmail();
            request.setPusherEmail(pusherEmail);
            emails.add(pusherEmail);
        }
    }

    private void setMergeEndPointUri(ObjectAttributes objectAttributes, Project proj, ScanRequest request) {
        String mergeEndpoint = scmConfigOverrider.determineConfigApiUrl(properties, request).concat(GitLabService.MERGE_NOTES_PATH);
        mergeEndpoint = mergeEndpoint.replace("{id}", proj.getId().toString());
        mergeEndpoint = mergeEndpoint.replace("{iid}", objectAttributes.getIid().toString());
        request.setMergeNoteUri(mergeEndpoint);
    }

    private void validateGitLabRequest(String token, ControllerRequest controllerRequest) {
        log.info("Validating GitLab request token");
        if (!scmConfigOverrider.determineConfigWebhookToken(properties, controllerRequest).equals(token)) {
            log.error("GitLab request token validation failed");
            throw new InvalidTokenException();
        }
        log.info("Validation successful");
    }

    /**
     * Check if the merge event is being driven by updates to 'work item in progress' status.
     */
    private boolean isWIP(MergeEvent event) {
        /*Merge has been marked WIP, ignoring*/
        Boolean inProgress = event.getObjectAttributes().getWorkInProgress();
        if (Boolean.TRUE.equals(inProgress)) {
            return true;
        }
        Changes changes = event.getChanges();
        if (!properties.isBlockMerge()) { //skip looking for WIP changes
            return false;
        }
        /*Merge has been changed from WIP to not-WIP, ignoring*/
        else return changes != null && changes.getTitle() != null && changes.getTitle().getPrevious() != null &&
                changes.getTitle().getPrevious().startsWith("WIP:CX|") &&
                !changes.getTitle().getCurrent().startsWith("WIP:");
    }
}

