package com.checkmarx.flow.controller;

import com.checkmarx.flow.config.ADOProperties;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.dto.*;
import com.checkmarx.flow.dto.ScanRequest.Product;
import com.checkmarx.flow.dto.ScanRequest.ScanRequestBuilder;
import com.checkmarx.flow.dto.azure.AdoDetailsRequest;
import com.checkmarx.flow.dto.azure.PullEvent;
import com.checkmarx.flow.dto.azure.Repository;
import com.checkmarx.flow.dto.azure.Resource;
import com.checkmarx.flow.exception.InvalidTokenException;
import com.checkmarx.flow.service.FilterFactory;
import com.checkmarx.flow.service.FlowService;
import com.checkmarx.flow.service.HelperService;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.dto.filtering.FilterConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/")
@RequiredArgsConstructor
@Slf4j
public class TfsController extends AdoControllerBase {
    private static final String PULL_EVENT = "git.pullrequest.created";
    private static final String AUTHORIZATION = "authorization";
    public static final String ACTION_PULL = "pull";
    public static final String ACTION_PUSH = "push";

    private final ADOProperties properties;
    private final FlowProperties flowProperties;
    private final CxProperties cxProperties;
    private final JiraProperties jiraProperties;
    private final FlowService flowService;
    private final HelperService helperService;
    private final FilterFactory filterFactory;

    @PostMapping(value = {"/{product}/tfs/pull", "/tfs/pull", "/{product}/tfs/push", "/tfs/push"})
    public ResponseEntity<EventResponse> pullPushRequest(
            HttpServletRequest httpRequest,
            @RequestBody PullEvent body,
            @RequestHeader(value = AUTHORIZATION) String auth,
            @PathVariable(value = "product", required = false) String product,
            ControllerRequest controllerRequest,
            AdoDetailsRequest adoDetailsRequest
    ) {
        String action = getAction(httpRequest);

        String uid = helperService.getShortUid();
        MDC.put("cx", uid);
        if (log.isInfoEnabled()) {
            log.info(String.format("Processing TFS %s request", action));
        }
        validateBasicAuth(auth);
        Resource resource = body.getResource();
        controllerRequest = ensureNotNull(controllerRequest);
        adoDetailsRequest = ensureDetailsNotNull(adoDetailsRequest);

        if (ACTION_PULL.equals(action) && !body.getEventType().equals(PULL_EVENT)) {
            log.info("Pull requested not processed.  Event was not 'opened' ({})", body.getEventType());
            return ResponseEntity.accepted().body(EventResponse.builder()
                    .message("No processing occurred for updates to Pull Request")
                    .success(true)
                    .build());
        }

        FlowOverride o = ScanUtils.getMachinaOverride(Optional.ofNullable(controllerRequest.getOverride())
                .orElse(null));

        Repository repository = resource.getRepository();
        String app = repository.getName();
        if (app.startsWith(properties.getTestRepository())) {
            log.info("Handling TFS Test Event");
            return ResponseEntity.ok(EventResponse.builder()
                    .message("Test Event").success(true).build());
        }

        Optional.ofNullable(controllerRequest.getAppOnly())
                .ifPresent(flowProperties::setTrackApplicationOnly);

        FilterConfiguration filter = filterFactory.getFilter(controllerRequest, flowProperties);

        setExclusionProperties(cxProperties, controllerRequest);

        ScanRequestBuilder requestBuilder = ScanRequest.builder()
                .application(Optional.ofNullable(controllerRequest.getApplication()).orElse(app))
                .product(getProductForName(product))
                .project(Optional.ofNullable(controllerRequest.getProject()).orElse(null))
                .team(Optional.ofNullable(controllerRequest.getTeam()).orElse(null))
                .namespace(repository.getProject().getName().replace(" ", "_"))
                .repoName(repository.getName())
                .repoType(ScanRequest.Repository.ADO)
                .incremental(isScanIncremental(controllerRequest, cxProperties))
                .scanPreset(Optional.ofNullable(controllerRequest.getPreset()).orElse(cxProperties.getScanPreset()))
                .excludeFolders(controllerRequest.getExcludeFolders())
                .excludeFiles(controllerRequest.getExcludeFiles())
                .filter(filter);

        if (ACTION_PULL.equals(action)) {
            BugTracker.Type bugType = Optional.ofNullable(controllerRequest.getBug())
                    .map(theBug -> ScanUtils.getBugTypeEnum(theBug, flowProperties.getBugTrackerImpl()))
                    .orElse(BugTracker.Type.ADOPULL);

            Optional.ofNullable(controllerRequest.getAppOnly())
                    .ifPresent(flowProperties::setTrackApplicationOnly);

            BugTracker bugTracker = ScanUtils.getBugTracker(
                    Optional.ofNullable(controllerRequest.getAssignee()).orElse(null),
                    bugType,
                    jiraProperties,
                    Optional.ofNullable(controllerRequest.getBug()).orElse(null));

            requestBuilder
                    .refs(resource.getSourceRefName())
                    .repoUrl(repository.getWebUrl())
                    .repoUrlWithAuth(addTokenToUrl(repository.getWebUrl(), properties.getToken()))
                    .mergeNoteUri(resource.getUrl().concat("/threads"))
                    .branch(ScanUtils.getBranchFromRef(resource.getSourceRefName()))
                    .mergeTargetBranch(ScanUtils.getBranchFromRef(resource.getTargetRefName()))
                    .email(null)
                    .bugTracker(bugTracker);
        } else if (ACTION_PUSH.equals(action)) {
            String bug = Optional.ofNullable(controllerRequest.getBug())
                    .orElse(flowProperties.getBugTracker());

            BugTracker.Type bugType = ScanUtils.getBugTypeEnum(bug, flowProperties.getBugTrackerImpl());

            BugTracker bugTracker = ScanUtils.getBugTracker(
                    Optional.ofNullable(controllerRequest.getAssignee()).orElse(null),
                    bugType,
                    jiraProperties,
                    Optional.ofNullable(controllerRequest.getBug()).orElse(null));

            requestBuilder
                    .refs(resource.getRefUpdates().get(0).getName())
                    .repoUrl(repository.getRemoteUrl())
                    .repoUrlWithAuth(addTokenToUrl(repository.getRemoteUrl(), properties.getToken()))
                    .branch(ScanUtils.getBranchFromRef(resource.getRefUpdates().get(0).getName()))
                    .defaultBranch(repository.getDefaultBranch())
                    .email(determineEmails(resource))
                    .bugTracker(bugTracker);
        }
        ScanRequest request = requestBuilder.build();

        request = ScanUtils.overrideMap(request, o);
        if (ACTION_PULL.equals(action)) {
            request.putAdditionalMetadata("statuses_url", resource.getUrl().concat("/statuses"));
        }
        addMetadataToScanRequest(adoDetailsRequest, request);
        request.putAdditionalMetadata(ScanUtils.WEB_HOOK_PAYLOAD, body.toString());
        request.setId(uid);
        //only initiate scan/automation if target branch is applicable
        List<String> branches = new ArrayList<>();

        Optional<List<String>> branch = Optional.ofNullable(controllerRequest.getBranch());
        if (branch.isPresent()) {
            branches.addAll(branch.get());
        } else if (CollectionUtils.isNotEmpty(flowProperties.getBranches())) {
            branches.addAll(flowProperties.getBranches());
        }

        if (helperService.isBranch2Scan(request, branches)) {
            flowService.initiateAutomation(request);
        }
        return ResponseEntity.accepted().body(EventResponse.builder()
                .message("Scan Request Successfully Submitted")
                .success(true)
                .build());
    }

    private List<String> determineEmails(Resource resource) {
        List<String> emails = new ArrayList<>();
        if (resource.getCommits() != null) {
            emails = resource.getCommits()
                    .stream()
                    .filter(c -> c.getAuthor() != null && StringUtils.isNotEmpty(c.getAuthor().getEmail()))
                    .map(c -> c.getAuthor().getEmail()).collect(Collectors.toList());
            emails.add(resource.getPushedBy().getUniqueName());
        }
        return emails;
    }

    private String getAction(HttpServletRequest request) {
        String pathInfo = request.getRequestURI();
        return pathInfo.substring(pathInfo.length() - 4);
    }

    private Product getProductForName(String product) {
        return Optional.ofNullable(product).map(pr -> ScanRequest.Product.valueOf(pr.toUpperCase(Locale.ROOT)))
                .orElse(ScanRequest.Product.CX);
    }

    /*
     * $1 is http or https
     * $2 is the remainder of the url.
     */
    private String addTokenToUrl(String url, String token) {
        return url.replaceAll("^(https?://)(.+$)", "$1" + token + "@" + "$2");
    }

    private void validateBasicAuth(String token) {
        String auth = "Basic ".concat(Base64.getEncoder().encodeToString(properties.getWebhookToken().getBytes()));
        if (!auth.equals(token)) {
            throw new InvalidTokenException();
        }
    }
}
