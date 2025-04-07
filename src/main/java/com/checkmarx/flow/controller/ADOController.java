package com.checkmarx.flow.controller;

import com.checkmarx.flow.config.*;
import com.checkmarx.flow.constants.FlowConstants;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ControllerRequest;
import com.checkmarx.flow.dto.EventResponse;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.azure.*;
import com.checkmarx.flow.exception.InvalidTokenException;
import com.checkmarx.flow.exception.MachinaRuntimeException;
import com.checkmarx.flow.service.*;
import com.checkmarx.flow.utils.ADOUtils;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.dto.filtering.FilterConfiguration;
import com.checkmarx.sdk.dto.sast.CxConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
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
    private static final String COMMENT = "ms.vss-code.git-pullrequest-comment-event";
    private static final String PRCREATED_EVENT = "git.pullrequest.created";
    private static final String PRUPDATED_EVENT = "git.pullrequest.updated";
    private static final List<String> PULL_EVENT = Arrays.asList("git.pullrequest.created", "git.pullrequest.updated");
    private static final String BRANCH_DELETED_REF = StringUtils.repeat('0', 40);
    private static final String AUTHORIZATION = "authorization";
    private static final String EMPTY_STRING = "";
    private final ADOProperties properties;
    private final FlowProperties flowProperties;
    private final JiraProperties jiraProperties;
    private final FlowService flowService;
    private final HelperService helperService;
    private final FilterFactory filterFactory;
    private  final ADOCommentService adoCommentService;
    private final ADOConfigService adoConfigService;
    private final ScmConfigOverrider scmConfigOverrider;
    private final GitAuthUrlGenerator gitAuthUrlGenerator;


    /**
     * Pull Request event submitted (JSON)
     */
    @PostMapping(value = {"/{product}/ado/pull", "/ado/pull"})
    public ResponseEntity<EventResponse> pullRequest(
            @RequestBody String body,
            @RequestHeader(value = AUTHORIZATION) String auth,
            @PathVariable(value = "product", required = false) String product,
            ControllerRequest controllerRequest,
            AdoDetailsRequest adoDetailsRequest
    ) {

        PullEvent event = null;
        String eventType;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNodebody = mapper.readTree(body);
            eventType = jsonNodebody.get("eventType").asText();
            if (PULL_EVENT.contains(eventType)) {
                event = mapper.convertValue(jsonNodebody, PRCreatedEvent.class);
            } else if (eventType.contains(COMMENT)) {
                event = mapper.convertValue(jsonNodebody, PRCommentEvent.class);
            }
        } catch (IOException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            throw new MachinaRuntimeException(e);
        }

        String uid = helperService.getShortUid();
        MDC.put(FlowConstants.MAIN_MDC_ENTRY, uid);
        log.info("Processing Azure event on Pull request {}",event.getEventType());
        Action action = Action.PULL;
        controllerRequest = ensureNotNull(controllerRequest);
        validateBasicAuth(auth, controllerRequest);
        adoDetailsRequest = ensureDetailsNotNull(adoDetailsRequest);
        ResourceContainers resourceContainers = event.getResourceContainers();

        return switch (eventType) {
            case (PRCREATED_EVENT), (PRUPDATED_EVENT) ->
                    processPullRequestCreation((PRCreatedEvent) event, controllerRequest, adoDetailsRequest, product, resourceContainers, body, action, uid);
            case (COMMENT) -> processPRComment((PRCommentEvent) event, properties,adoDetailsRequest,controllerRequest, product,resourceContainers,body,action,uid);
            default -> ResponseEntity.status(HttpStatus.OK).body(EventResponse.builder()
                    .message("No processing occurred for updates to Pull Request")
                    .success(true)
                    .build());
        };
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

            adoConfigService.initAdoSpecificParams(adoDetailsRequest);

            if (controllerRequest.getAppOnly() != null) {
                flowProperties.setTrackApplicationOnly(controllerRequest.getAppOnly());
            }
            if (StringUtils.isEmpty(product)) {
                product = ScanRequest.Product.CX.getProduct();
            }

            if (controllerRequest.getCommentmsgid() != null) {
                properties.setCommentStatusWhook(controllerRequest.getCommentmsgid());
            } else {
                properties.setCommentStatusWhook(-1);
            }

            ScanRequest.Product p = ScanRequest.Product.valueOf(product.toUpperCase(Locale.ROOT));

            //determine branch (without refs)
            String ref = resource.getRefUpdates().get(0).getName();
            String currentBranch = ScanUtils.getBranchFromRef(ref);

            List<String> branches = getBranches(controllerRequest, flowProperties);

            BugTracker bt = ScanUtils.getBugTracker(controllerRequest.getAssignee(), bugType, jiraProperties, controllerRequest.getBug());

            FilterConfiguration filter = filterFactory.getFilter(controllerRequest, flowProperties);

            Map<FindingSeverity, Integer> thresholdMap = getThresholds(controllerRequest);

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
                    .namespace(adoConfigService.determineNamespace(resourceContainers))
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
                    .thresholds(thresholdMap)
                    .organizationId(adoConfigService.determineNamespace(resourceContainers))
                    .gitUrl(gitUrl)
                    .build();
            if (body.getResource().getCommits() != null) {
                request.setLatestCommitterEmail(body.getResource().getCommits().get(0).getAuthor().getEmail());
            }
            setScmInstance(controllerRequest, request);
            request.putAdditionalMetadata(ADOService.PROJECT_SELF_URL, getTheProjectURL(body.getResourceContainers()));
            addMetadataToScanRequest(adoDetailsRequest, request);
            adoConfigService.fillRequestWithAdditionalData(request, repository, body.toString());
            //if an override blob/file is provided, substitute these values
            adoConfigService.checkForConfigAsCode(request, adoConfigService.getConfigBranch(request, resource, action));
            request.setId(uid);
            //only initiate scan/automation if target branch is applicable
            if (helperService.isBranch2Scan(request, branches)) {
                log.debug("{} :: Calling  isBranch2Scan function End :  {}", request.getProject(),System.currentTimeMillis());
                log.debug("{} :: Free Memory :  {}",request.getProject(),Runtime.getRuntime().freeMemory());
                log.debug("{}  :: Total Numbers of processors :  {}",request.getProject(),Runtime.getRuntime().availableProcessors());
                long startTime=System.currentTimeMillis();
                log.debug("{}  :: Start Time :  {}", request.getProject(),startTime);
                flowService.initiateAutomation(request);
                long endTime=System.currentTimeMillis();
                log.debug("{} :: End Time  :  {}",request.getProject(),endTime);
                log.debug("{} :: Total Time Taken  :  {}", request.getProject(),(endTime-startTime));
            }
            else if(adoConfigService.isDeleteBranchEvent(resource) && properties.getDeleteCxProject()){
                flowService.deleteProject(request);
            }

        } catch (IllegalArgumentException e) {
            return getBadRequestMessage(e, controllerRequest, product);
        }

        return getSuccessMessage();
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

    private String determineAzureProject(Repository repository) {
        String azureProject = repository.getProject().getName();

        log.info("using azure project: {}", azureProject);
        return azureProject;
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

    /**
     * Validates the base64 / basic auth received in the request.
     */


    private String getTheProjectURL(ResourceContainers resourceContainers) {
        String projectId = resourceContainers.getProject().getId();
        String baseUrl = resourceContainers.getProject().getBaseUrl();
        return baseUrl.concat(projectId);
    }

// handles  the comment on the PR  for @cxflow command
    public ResponseEntity<EventResponse> processPRComment(PRCommentEvent event, ADOProperties properties,AdoDetailsRequest adoDetailsRequest, ControllerRequest controllerRequest, String product,ResourceContainers resourceContainers,String body,Action action,String uid) {
        log.info("Processing Pull Request Comment Event");
        try {
            Repository repository= event.getResource().getPullRequest().getRepository();
            if (repository.getName().startsWith(properties.getTestRepository())) {
                log.info("Handling ADO PR  Test Event");
                return ResponseEntity.status(HttpStatus.OK).body(EventResponse.builder()
                        .message("Test Event").success(true).build());
            }

            String commentBody = event.getResource().getComment().getContent();
            String baseUrl = event.getResourceContainers().getAccount().getBaseUrl();
            String projectName = event.getResource().getPullRequest().getRepository().getProject().getName();
            String repositoryId = event.getResource().getPullRequest().getRepository().getId();
            Integer pullRequestId = event.getResource().getPullRequest().getPullRequestId();
            String threadRegex="(?<=threads/)(\\d+)";
            String threadURL=event.getResource().getComment().getLinks().getThreads().getHref();
            String threadIdString = ADOUtils.extractRegex(threadRegex, "threadId not found in the url" + threadURL,threadURL);
            Integer threadId = Integer.parseInt(threadIdString);
            Map<FindingSeverity, Integer> thresholdMap = getThresholds(controllerRequest);
            List<String> branches = getBranches(controllerRequest, flowProperties);

            if(commentBody.toLowerCase().contains("@cxflow")){
                adoCommentService.adoPRCommentHandler(event,properties, commentBody, baseUrl, projectName, repositoryId, pullRequestId, threadId,thresholdMap,branches,controllerRequest,product,resourceContainers,body,action,uid ,adoDetailsRequest);
            }
        } catch (IllegalArgumentException e) {
            return getBadRequestMessage(e, controllerRequest, product);
        }

        return getSuccessMessage();
    }

    public ResponseEntity<EventResponse> processPullRequestCreation(PRCreatedEvent event, ControllerRequest controllerRequest, AdoDetailsRequest adoDetailsRequest, String product, ResourceContainers resourceContainers, String body, Action action, String uid) {
        try {
            Resource resource = event.getResource();
            Repository repository = resource.getRepository();
            String pullUrl = resource.getUrl();
            log.debug("Pull request URL: {}", pullUrl);
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

            if (controllerRequest.getCommentmsgid() != null) {
                properties.setCommentStatusWhook(controllerRequest.getCommentmsgid());
            } else {
                properties.setCommentStatusWhook(-1);
            }
            List<String> branches = getBranches(controllerRequest, flowProperties);

            adoConfigService.initAdoSpecificParams(adoDetailsRequest);

            if (StringUtils.isEmpty(product)) {
                product = ScanRequest.Product.CX.getProduct();
            }
            ScanRequest.Product p = ScanRequest.Product.valueOf(product.toUpperCase(Locale.ROOT));

            String ref = resource.getSourceRefName();
            String currentBranch = ScanUtils.getBranchFromRef(ref);
            String targetBranch = ScanUtils.getBranchFromRef(resource.getTargetRefName());


            BugTracker bt = ScanUtils.getBugTracker(controllerRequest.getAssignee(), bugType, jiraProperties, controllerRequest.getBug());

            FilterConfiguration filter = filterFactory.getFilter(controllerRequest, flowProperties);

            Map<FindingSeverity, Integer> thresholdMap = getThresholds(controllerRequest);

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
                    .namespace(adoConfigService.determineNamespace(resourceContainers))
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
                    .thresholds(thresholdMap)
                    .organizationId(adoConfigService.determineNamespace(resourceContainers))
                    .gitUrl(gitUrl)
                    .build();

            setScmInstance(controllerRequest, request);
            request.putAdditionalMetadata(ADOService.PROJECT_SELF_URL, getTheProjectURL(event.getResourceContainers()));
            adoConfigService.fillRequestWithAdditionalData(request, repository, body);
            adoConfigService.checkForConfigAsCode(request, adoConfigService.getConfigBranch(request, resource, action));
            request.putAdditionalMetadata("statuses_url", pullUrl.concat("/statuses"));
            addMetadataToScanRequest(adoDetailsRequest, request);
            request.setId(uid);
            //only initiate scan/automation if target branch is applicable
            if (helperService.isBranch2Scan(request, branches)) {
                log.debug("{} :: Calling  isBranch2Scan function End :  {}", request.getProject(),System.currentTimeMillis());
                log.debug("{} :: Free Memory :  {}",request.getProject(),Runtime.getRuntime().freeMemory());
                log.debug("{}  :: Total Numbers of processors :  {}",request.getProject(),Runtime.getRuntime().availableProcessors());
                long startTime = System.currentTimeMillis();
                log.debug("{}  :: Start Time :  {}", request.getProject(),startTime);
                flowService.initiateAutomation(request);
                long endTime = System.currentTimeMillis();
                log.debug("{} :: End Time  :  {}",request.getProject(),endTime);
                log.debug("{} :: Total Time Taken  :  {}", request.getProject(),(endTime-startTime));
            }

        } catch (IllegalArgumentException e) {
            return getBadRequestMessage(e, controllerRequest, product);
        }

        return getSuccessMessage();
    }

    public enum Action {
        PULL,
        PUSH
    }
}
