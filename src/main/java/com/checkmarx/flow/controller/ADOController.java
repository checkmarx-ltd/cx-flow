package com.checkmarx.flow.controller;

import com.checkmarx.flow.config.ADOProperties;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.dto.*;
import com.checkmarx.flow.dto.azure.*;
import com.checkmarx.flow.exception.InvalidTokenException;
import com.checkmarx.flow.service.ConfigurationOverrider;
import com.checkmarx.flow.service.FilterFactory;
import com.checkmarx.flow.service.FlowService;
import com.checkmarx.flow.service.HelperService;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.dto.filtering.FilterConfiguration;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Handles Azure DevOps (ADO) webhook requests.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/")
public class ADOController extends AdoControllerBase {
    private static final String HTTP = "http://";
    private static final String HTTPS = "https://";
    private static final List<String> PULL_EVENT = Arrays.asList("git.pullrequest.created", "git.pullrequest.updated");
    private static final String AUTHORIZATION = "authorization";
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ADOController.class);
    private final ADOProperties properties;
    private final FlowProperties flowProperties;
    private final CxProperties cxProperties;
    private final JiraProperties jiraProperties;
    private final FlowService flowService;
    private final HelperService helperService;
    private final FilterFactory filterFactory;
    private final ConfigurationOverrider configOverrider;

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
        MDC.put("cx", uid);
        log.info("Processing Azure PULL request");
        validateBasicAuth(auth);
        controllerRequest = ensureNotNull(controllerRequest);
        adoDetailsRequest = ensureDetailsNotNull(adoDetailsRequest);

        if (!PULL_EVENT.contains(body.getEventType()) || !body.getResource().getStatus().equals("active")) {
            log.info("Pull requested not processed.  Event was not opened ({})", body.getEventType());
            return ResponseEntity.status(HttpStatus.OK).body(EventResponse.builder()
                    .message("No processing occurred for updates to Pull Request")
                    .success(true)
                    .build());
        }

        FlowOverride o = ScanUtils.getMachinaOverride(controllerRequest.getOverride());

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

            setExclusionProperties(cxProperties, controllerRequest);

            //build request object
            String gitUrl = repository.getWebUrl();
            String token = properties.getToken();
            log.info("Using url: {}", gitUrl);
            String gitAuthUrl = gitUrl.replace(HTTPS, HTTPS.concat(token).concat("@"));
            gitAuthUrl = gitAuthUrl.replace(HTTP, HTTP.concat(token).concat("@"));

            String scanPreset = cxProperties.getScanPreset();
            if (StringUtils.isNotEmpty(controllerRequest.getPreset())) {
                scanPreset = controllerRequest.getPreset();
            }

            ScanRequest request = ScanRequest.builder()
                    .application(app)
                    .product(p)
                    .project(controllerRequest.getProject())
                    .team(controllerRequest.getTeam())
                    .namespace(determineNamespace(repository))
                    .repoName(repository.getName())
                    .repoUrl(gitUrl)
                    .repoUrlWithAuth(gitAuthUrl)
                    .repoType(ScanRequest.Repository.ADO)
                    .branch(currentBranch)
                    .refs(ref)
                    .mergeNoteUri(pullUrl.concat("/threads"))
                    .mergeTargetBranch(targetBranch)
                    .email(null)
                    .incremental(isScanIncremental(controllerRequest, cxProperties))
                    .scanPreset(scanPreset)
                    .excludeFolders(controllerRequest.getExcludeFolders())
                    .excludeFiles(controllerRequest.getExcludeFiles())
                    .bugTracker(bt)
                    .filter(filter)
                    .build();

            request = configOverrider.overrideScanRequestProperties(o, request);
            request.putAdditionalMetadata("statuses_url", pullUrl.concat("/statuses"));
            addMetadataToScanRequest(adoDetailsRequest, request);
            request.putAdditionalMetadata(ScanUtils.WEB_HOOK_PAYLOAD, body.toString());
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
        MDC.put("cx", uid);
        log.info("Processing Azure Push request");
        validateBasicAuth(auth);
        controllerRequest = ensureNotNull(controllerRequest);
        adoDetailsRequest = ensureDetailsNotNull(adoDetailsRequest);

        FlowOverride o = ScanUtils.getMachinaOverride(controllerRequest.getOverride());

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

            setExclusionProperties(cxProperties, controllerRequest);

            List<String> emails = determineEmails(resource);

            //build request object
            String gitUrl = repository.getRemoteUrl();
            log.debug("Using url: {}", gitUrl);
            String gitAuthUrl = gitUrl.replace(HTTPS, HTTPS.concat(properties.getToken()).concat("@"));
            gitAuthUrl = gitAuthUrl.replace(HTTP, HTTP.concat(properties.getToken()).concat("@"));

            String scanPreset = cxProperties.getScanPreset();
            if (StringUtils.isNotEmpty(controllerRequest.getPreset())) {
                scanPreset = controllerRequest.getPreset();
            }

            String defaultBranch = repository.getDefaultBranch();
            String[] branchPath = repository.getDefaultBranch().split("/");

            if (branchPath.length == 3) {
                defaultBranch = branchPath[2];
            }

            ScanRequest request = ScanRequest.builder()
                    .application(app)
                    .product(p)
                    .project(controllerRequest.getProject())
                    .team(controllerRequest.getTeam())
                    .namespace(determineNamespace(repository))
                    .repoName(repository.getName())
                    .repoUrl(gitUrl)
                    .repoUrlWithAuth(gitAuthUrl)
                    .repoType(ScanRequest.Repository.ADO)
                    .branch(currentBranch)
                    .defaultBranch(defaultBranch)
                    .refs(ref)
                    .email(emails)
                    .incremental(isScanIncremental(controllerRequest, cxProperties))
                    .scanPreset(scanPreset)
                    .excludeFolders(controllerRequest.getExcludeFolders())
                    .excludeFiles(controllerRequest.getExcludeFiles())
                    .bugTracker(bt)
                    .filter(filter)
                    .build();

            addMetadataToScanRequest(adoDetailsRequest, request);
            request.putAdditionalMetadata(ScanUtils.WEB_HOOK_PAYLOAD, body.toString());
            //if an override blob/file is provided, substitute these values
            request = configOverrider.overrideScanRequestProperties(o, request);

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

    private String determineNamespace(Repository repository) {
        String result = repository.getProject().getName().replace(" ", "_");
        log.debug("Using namespace based on repository.project.name: {}", result);
        return result;
    }

    /**
     * Validates the base64 / basic auth received in the request.
     */
    private void validateBasicAuth(String token) {
        String auth = "Basic ".concat(Base64.getEncoder().encodeToString(properties.getWebhookToken().getBytes()));
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
}
