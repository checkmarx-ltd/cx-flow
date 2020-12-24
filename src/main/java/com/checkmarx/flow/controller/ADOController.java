package com.checkmarx.flow.controller;

import com.checkmarx.flow.config.ADOProperties;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.config.ScmConfigOverrider;
import com.checkmarx.flow.constants.FlowConstants;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ControllerRequest;
import com.checkmarx.flow.dto.EventResponse;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.azure.*;
import com.checkmarx.flow.exception.InvalidTokenException;
import com.checkmarx.flow.service.*;
import com.checkmarx.flow.utils.HTMLHelper;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.dto.CxConfig;
import com.checkmarx.sdk.dto.filtering.FilterConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Handles Azure DevOps (ADO) webhook requests.
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/")
public class ADOController extends AdoControllerBase {
    private static final String HTTP = "http://";
    private static final String HTTPS = "https://";
    private static final List<String> PULL_EVENT = Arrays.asList("git.pullrequest.created", "git.pullrequest.updated");
    private static final String BRANCH_DELETED_REF = StringUtils.repeat('0', 40);
    private static final String AUTHORIZATION = "authorization";
    private static final int NAMESPACE_INDEX = 3;
    private static final String EMPTY_STRING = "";
    private final ADOProperties properties;
    private final FlowProperties flowProperties;
    private final JiraProperties jiraProperties;
    private final FlowService flowService;
    private final HelperService helperService;
    private final FilterFactory filterFactory;
    private final ConfigurationOverrider configOverrider;
    private final ADOService adoService;
    private final ScmConfigOverrider scmConfigOverrider;
    private final GitAuthUrlGenerator gitAuthUrlGenerator;


    /**
     * Pull Request event submitted (JSON)
     */
    @PostMapping(value = {"/{product}/ado/pull", "/ado/pull"})
    public ResponseEntity<EventResponse> pullRequest(
            @RequestBody PullEvent body,
            @RequestHeader(value = AUTHORIZATION) String auth,
            @PathVariable(value = "product", required = false) String product,
            ControllerRequest controllerRequest,
            AdoDetailsRequest adoDetailsRequest
    ) {
        String uid = helperService.getShortUid();
        MDC.put(FlowConstants.MAIN_MDC_ENTRY, uid);
        log.info("Processing Azure PULL request");
        Action action = Action.PULL;
        controllerRequest = ensureNotNull(controllerRequest);
        validateBasicAuth(auth, controllerRequest);
        adoDetailsRequest = ensureDetailsNotNull(adoDetailsRequest);
        ResourceContainers resourceContainers = body.getResourceContainers();

        if (!PULL_EVENT.contains(body.getEventType()) || !body.getResource().getStatus().equals("active")) {
            log.info("Pull requested not processed.  Event was not opened ({})", body.getEventType());
            return ResponseEntity.status(HttpStatus.OK).body(EventResponse.builder()
                    .message("No processing occurred for updates to Pull Request")
                    .success(true)
                    .build());
        }

        try {
            Resource resource = body.getResource();
            Repository repository = resource.getRepository();
            String pullUrl = resource.getUrl();
            String app = repository.getName();

            if (repository.getName().startsWith(properties.getTestRepository())) {
                log.info("Handling ADO Test Event");
                return ResponseEntity.status(HttpStatus.OK).body(EventResponse.builder()
                        .message("Test Event").success(true).build());
            }

            if (StringUtils.isNotEmpty(controllerRequest.getApplication())) {
                app = controllerRequest.getApplication();
            }

            BugTracker.Type bugType = BugTracker.Type.ADOPULL;
            if (StringUtils.isNotEmpty(controllerRequest.getBug())) {
                bugType = ScanUtils.getBugTypeEnum(controllerRequest.getBug(), flowProperties.getBugTrackerImpl());
            }

            if (controllerRequest.getAppOnly() != null) {
                flowProperties.setTrackApplicationOnly(controllerRequest.getAppOnly());
            }

            initAdoSpecificParams(adoDetailsRequest);

            if (StringUtils.isEmpty(product)) {
                product = ScanRequest.Product.CX.getProduct();
            }
            ScanRequest.Product p = ScanRequest.Product.valueOf(product.toUpperCase(Locale.ROOT));

            String ref = resource.getSourceRefName();
            String currentBranch = ScanUtils.getBranchFromRef(ref);
            String targetBranch = ScanUtils.getBranchFromRef(resource.getTargetRefName());

            List<String> branches = getBranches(controllerRequest, flowProperties);

            BugTracker bt = ScanUtils.getBugTracker(controllerRequest.getAssignee(), bugType, jiraProperties, controllerRequest.getBug());

            FilterConfiguration filter = filterFactory.getFilter(controllerRequest, flowProperties);

            //build request object
            String gitUrl = repository.getWebUrl();
            String token = scmConfigOverrider.determineConfigToken(properties, controllerRequest.getScmInstance());
            log.info("Using url: {}", gitUrl);
            String gitAuthUrl = gitAuthUrlGenerator.addCredToUrl(ScanRequest.Repository.ADO, gitUrl, token);

            ScanRequest request = ScanRequest.builder()
                    .application(app)
                    .product(p)
                    .project(controllerRequest.getProject())
                    .team(controllerRequest.getTeam())
                    .namespace(determineNamespace(resourceContainers))
                    .repoName(repository.getName())
                    .repoUrl(gitUrl)
                    .repoUrlWithAuth(gitAuthUrl)
                    .repoType(ScanRequest.Repository.ADO)
                    .branch(currentBranch)
                    .refs(ref)
                    .mergeNoteUri(pullUrl.concat("/threads"))
                    .mergeTargetBranch(targetBranch)
                    .email(null)
                    .scanPreset(controllerRequest.getPreset())
                    .incremental(controllerRequest.getIncremental())
                    .excludeFolders(controllerRequest.getExcludeFolders())
                    .excludeFiles(controllerRequest.getExcludeFiles())
                    .bugTracker(bt)
                    .filter(filter)
                    .organizationId(determineNamespace(resourceContainers))
                    .gitUrl(gitUrl)
                    .build();

            setScmInstance(controllerRequest, request);
            request.putAdditionalMetadata(ADOService.PROJECT_SELF_URL, getTheProjectURL(body.getResourceContainers()));
            fillRequestWithAdditionalData(request, repository, body.toString());
            checkForConfigAsCode(request, getConfigBranch(request, resource, action));
            request.putAdditionalMetadata("statuses_url", pullUrl.concat("/statuses"));
            addMetadataToScanRequest(adoDetailsRequest, request);
            request.setId(uid);
            //only initiate scan/automation if target branch is applicable
            if (helperService.isBranch2Scan(request, branches)) {
                flowService.initiateAutomation(request);
            }

        } catch (IllegalArgumentException e) {
            return getBadRequestMessage(e, controllerRequest, product);
        }

        return getSuccessMessage();
    }



    /**
     * Push Request event submitted (JSON), along with the Product (cx for example)
     */
    @PostMapping(value = {"/{product}/ado/push", "/ado/push"})
    public ResponseEntity<EventResponse> pushRequest(
            @RequestBody PushEvent body,
            @RequestHeader(value = AUTHORIZATION) String auth,
            @PathVariable(value = "product", required = false) String product,
            ControllerRequest controllerRequest,
            AdoDetailsRequest adoDetailsRequest
    ) {
        //TODO handle different state (Active/Closed)
        String uid = helperService.getShortUid();
        MDC.put(FlowConstants.MAIN_MDC_ENTRY, uid);
        log.info("Processing Azure Push request");
        Action action = Action.PUSH;

        controllerRequest = ensureNotNull(controllerRequest);
        validateBasicAuth(auth, controllerRequest);
        adoDetailsRequest = ensureDetailsNotNull(adoDetailsRequest);
        ResourceContainers resourceContainers = body.getResourceContainers();

        try {
            Resource resource = body.getResource();
            Repository repository = resource.getRepository();
            String app = repository.getName();
            if (repository.getName().startsWith(properties.getTestRepository())) {
                log.info("Handling ADO Test Event");
                return ResponseEntity.status(HttpStatus.OK).body(EventResponse.builder()
                        .message("Test Event").success(true).build());
            }
            if (StringUtils.isNotEmpty(controllerRequest.getApplication())) {
                app = controllerRequest.getApplication();
            }

            //set the default bug tracker as per yml
            setBugTracker(flowProperties, controllerRequest);
            BugTracker.Type bugType = ScanUtils.getBugTypeEnum(controllerRequest.getBug(), flowProperties.getBugTrackerImpl());

            initAdoSpecificParams(adoDetailsRequest);

            if (controllerRequest.getAppOnly() != null) {
                flowProperties.setTrackApplicationOnly(controllerRequest.getAppOnly());
            }
            if (StringUtils.isEmpty(product)) {
                product = ScanRequest.Product.CX.getProduct();
            }
            ScanRequest.Product p = ScanRequest.Product.valueOf(product.toUpperCase(Locale.ROOT));

            //determine branch (without refs)
            String ref = resource.getRefUpdates().get(0).getName();
            String currentBranch = ScanUtils.getBranchFromRef(ref);

            List<String> branches = getBranches(controllerRequest, flowProperties);

            BugTracker bt = ScanUtils.getBugTracker(controllerRequest.getAssignee(), bugType, jiraProperties, controllerRequest.getBug());

            FilterConfiguration filter = filterFactory.getFilter(controllerRequest, flowProperties);

            List<String> emails = determineEmails(resource);

            //build request object
            String gitUrl = repository.getRemoteUrl();
            log.debug("Using url: {}", gitUrl);
            String configToken = scmConfigOverrider.determineConfigToken(properties, controllerRequest.getScmInstance());
            String gitAuthUrl = gitAuthUrlGenerator.addCredToUrl(ScanRequest.Repository.ADO, gitUrl, configToken);
            String defaultBranch = ScanUtils.getBranchFromRef(Optional.ofNullable(repository.getDefaultBranch()).orElse(ref));

            ScanRequest request = ScanRequest.builder()
                    .application(app)
                    .product(p)
                    .project(controllerRequest.getProject())
                    .team(controllerRequest.getTeam())
                    .namespace(determineNamespace(resourceContainers))
                    .altProject(determineAzureProject(repository))
                    .repoName(repository.getName())
                    .repoUrl(gitUrl)
                    .repoUrlWithAuth(gitAuthUrl)
                    .repoType(ScanRequest.Repository.ADO)
                    .branch(currentBranch)
                    .defaultBranch(defaultBranch)
                    .refs(ref)
                    .email(emails)
                    .scanPreset(controllerRequest.getPreset())
                    .incremental(controllerRequest.getIncremental())
                    .excludeFolders(controllerRequest.getExcludeFolders())
                    .excludeFiles(controllerRequest.getExcludeFiles())
                    .bugTracker(bt)
                    .filter(filter)
                    .organizationId(determineNamespace(resourceContainers))
                    .gitUrl(gitUrl)
                    .build();

            setScmInstance(controllerRequest, request);
            request.putAdditionalMetadata(ADOService.PROJECT_SELF_URL, getTheProjectURL(body.getResourceContainers()));
            addMetadataToScanRequest(adoDetailsRequest, request);
            fillRequestWithAdditionalData(request,repository, body.toString());
            //if an override blob/file is provided, substitute these values
            checkForConfigAsCode(request, getConfigBranch(request, resource, action));
            request.setId(uid);
            //only initiate scan/automation if target branch is applicable
            if (helperService.isBranch2Scan(request, branches)) {
                flowService.initiateAutomation(request);
            }
            else if(isDeleteBranchEvent(resource) && properties.getDeleteCxProject()){
                flowService.deleteProject(request);
            }

        } catch (IllegalArgumentException e) {
            return getBadRequestMessage(e, controllerRequest, product);
        }

        return getSuccessMessage();
    }

    private boolean isDeleteBranchEvent(Resource resource){
        if (resource.getRefUpdates().size() == 1){
            String newBranchRef = resource.getRefUpdates().get(0).getNewObjectId();

            if (newBranchRef.equals(BRANCH_DELETED_REF)){
                log.info("new-branch ref is empty - detect ADO DELETE event");
                return true;
            }
            return false;
        }

        int refCount = resource.getRefUpdates().size();
        log.warn("unexpected number of refUpdates in push event: {}", refCount);

        return false;
    }
    private List<String> determineEmails(Resource resource) {
        List<String> emails = new ArrayList<>();
        if (resource.getCommits() != null) {
            for (Commit c : resource.getCommits()) {
                if (c.getAuthor() != null && StringUtils.isNotEmpty(c.getAuthor().getEmail())) {
                    emails.add(c.getAuthor().getEmail());
                }
            }
            emails.add(resource.getPushedBy().getUniqueName());
        }
        return emails;
    }

    private String determineNamespace(ResourceContainers resourceContainers) {
        String namespace = EMPTY_STRING;
        try {
            log.debug("Trying to extract namespace from request body");
            String projectUrl = resourceContainers.getProject().getBaseUrl();
            namespace = projectUrl.split("/")[NAMESPACE_INDEX];
        }
        catch (Exception e){
            log.warn("can't find namespace in body resource containers: {}", e.getMessage());
        }

        log.info("using namespace: {}", namespace);
        return namespace;
    }

    private String determineAzureProject(Repository repository) {
        String azureProject = repository.getProject().getName();

        log.info("using azure project: {}", azureProject);
        return azureProject;
    }

    private String getConfigBranch(ScanRequest request, Resource resource, Action action){
        String branch = request.getBranch();
        try{

            if (isDeleteBranchEvent(resource) && action.equals(Action.PUSH)){
                branch = request.getDefaultBranch();
                log.debug("branch to read config-as-code: {}", branch);
            }
        }
        catch (Exception ex){
            log.info("failed to get branch for config as code. using default");
        }
        return branch;
    }
    /**
     * Validates the base64 / basic auth received in the request.
     */
    private void validateBasicAuth(String token, ControllerRequest controllerRequest) {
        String auth = "Basic ".concat(Base64.getEncoder().encodeToString(scmConfigOverrider.determineConfigWebhookToken(properties, controllerRequest).getBytes()));
        if (!auth.equals(token)) {
            throw new InvalidTokenException();
        }
    }

    private void initAdoSpecificParams(AdoDetailsRequest request) {
        if (StringUtils.isEmpty(request.getAdoIssue())) {
            request.setAdoIssue(properties.getIssueType());
        }
        if (StringUtils.isEmpty(request.getAdoBody())) {
            request.setAdoBody(properties.getIssueBody());
        }
        if (StringUtils.isEmpty(request.getAdoOpened())) {
            request.setAdoOpened(properties.getOpenStatus());
        }
        if (StringUtils.isEmpty(request.getAdoClosed())) {
            request.setAdoClosed(properties.getClosedStatus());
        }
    }

    private void checkForConfigAsCode(ScanRequest request, String branch) {
        CxConfig cxConfig = adoService.getCxConfigOverride(request, branch);
        configOverrider.overrideScanRequestProperties(cxConfig, request);
    }

    private void fillRequestWithAdditionalData(ScanRequest request, Repository repository, String hookPayload) {
        request.putAdditionalMetadata(ADOService.REPO_ID, repository.getId());
        request.putAdditionalMetadata(ADOService.REPO_SELF_URL, repository.getUrl());
        request.putAdditionalMetadata(HTMLHelper.WEB_HOOK_PAYLOAD, hookPayload);
    }

    private String getTheProjectURL(ResourceContainers resourceContainers) {
        String projectId = resourceContainers.getProject().getId();
        String baseUrl = resourceContainers.getProject().getBaseUrl();
        return baseUrl.concat(projectId);
    }
    private enum Action {
        PULL,
        PUSH
    }
}
