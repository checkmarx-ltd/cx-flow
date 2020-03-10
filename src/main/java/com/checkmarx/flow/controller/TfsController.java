package com.checkmarx.flow.controller;

import java.beans.ConstructorProperties;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import com.checkmarx.flow.config.ADOProperties;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.EventResponse;
import com.checkmarx.flow.dto.FlowOverride;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.ScanRequest.Product;
import com.checkmarx.flow.dto.ScanRequest.ScanRequestBuilder;
import com.checkmarx.flow.dto.azure.PullEvent;
import com.checkmarx.flow.dto.azure.Resource;
import com.checkmarx.flow.exception.InvalidTokenException;
import com.checkmarx.flow.service.FlowService;
import com.checkmarx.flow.service.HelperService;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.dto.Filter;
import com.checkmarx.flow.dto.azure.Repository;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/")
public class TfsController {
    private static final String PULL_EVENT = "git.pullrequest.created";
    private static final String AUTHORIZATION = "authorization";
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(TfsController.class);

    private final ADOProperties properties;
    private final FlowProperties flowProperties;
    private final CxProperties cxProperties;
    private final JiraProperties jiraProperties;
    private final FlowService flowService;
    private final HelperService helperService;

    @ConstructorProperties({"properties", "flowProperties", "cxProperties", "jiraProperties", "flowService", "helperService"})
    public TfsController(ADOProperties properties, FlowProperties flowProperties, CxProperties cxProperties,
            JiraProperties jiraProperties, FlowService flowService, HelperService helperService) {
        this.properties = properties;
        this.flowProperties = flowProperties;
        this.cxProperties = cxProperties;
        this.jiraProperties = jiraProperties;
        this.flowService = flowService;
        this.helperService = helperService;
    }

    @PostMapping(value = { "/{product}/tfs/pull", "/tfs/pull" , "/{product}/tfs/push","/tfs/push" })
    public ResponseEntity<EventResponse> pullPushRequest(
            HttpServletRequest httpRequest,
            @RequestBody PullEvent body,
            @RequestHeader(value = AUTHORIZATION) String auth, 
            @PathVariable Optional<String> product,
            @RequestParam Optional<String> application, 
            @RequestParam Optional<List<String>> branch,
            @RequestParam Optional<List<String>> severity, 
            @RequestParam Optional<List<String>> cwe,
            @RequestParam Optional<List<String>> category, 
            @RequestParam Optional<String> project,
            @RequestParam Optional<String> team, 
            @RequestParam Optional<List<String>> status,
            @RequestParam Optional<String> assignee, 
            @RequestParam Optional<String> preset,
            @RequestParam Optional<Boolean> incremental,
            @RequestParam(value = "exclude-files") Optional<List<String>> excludeFiles,
            @RequestParam(value = "exclude-folders") Optional<List<String>> excludeFolders,
            @RequestParam Optional<String> override, 
            @RequestParam Optional<String> bug,
            @RequestParam(value = "ado-issue") Optional<String> adoIssueType,
            @RequestParam(value = "ado-body") Optional<String> adoIssueBody,
            @RequestParam(value = "ado-opened") Optional<String> adoOpenedState,
            @RequestParam(value = "ado-closed") Optional<String> adoClosedState,
            @RequestParam(value = "app-only") Optional<Boolean> appOnlyTracking
    ) {
        String action = getAction(httpRequest);
        
        String uid = helperService.getShortUid();
        MDC.put("cx", uid);
        if (log.isInfoEnabled()) {
            log.info(String.format("Processing TFS %s request", action));
        }
        validateBasicAuth(auth);
        Resource resource = body.getResource();

        if("pull".equals(action) && !body.getEventType().equals(PULL_EVENT)){
            log.info("Pull requested not processed.  Event was not opened ({})", body.getEventType());
            return ResponseEntity.accepted().body(EventResponse.builder()
                    .message("No processing occurred for updates to Pull Request")
                    .success(true)
                    .build());
        }

        FlowOverride o = ScanUtils.getMachinaOverride(override.orElse(null));

        Repository repository = resource.getRepository();
        String app = repository.getName();
        if(app.startsWith(properties.getTestRepository())){
            log.info("Handling TFS Test Event");
            return ResponseEntity.ok(EventResponse.builder()
                    .message("Test Event").success(true).build());
        }

        appOnlyTracking.ifPresent(flowProperties::setTrackApplicationOnly);

        List<Filter> filters;
        if(severity.isPresent() || cwe.isPresent() || category.isPresent() || status.isPresent()){
            filters = ScanUtils.getFilters(
                severity.orElse(Collections.emptyList()), 
                cwe.orElse(Collections.emptyList()), 
                category.orElse(Collections.emptyList()), 
                status.orElse(Collections.emptyList()));
        } else {
            filters = ScanUtils.getFilters(flowProperties.getFilterSeverity(), flowProperties.getFilterCwe(),
                    flowProperties.getFilterCategory(), flowProperties.getFilterStatus());
        }

        ScanRequestBuilder requestBuilder = ScanRequest.builder()
                .application(application.orElse(app))
                .product(getProductForName(product))
                .project(project.orElse(null))
                .team(team.orElse(null))
                .namespace(repository.getProject().getName().replace(" ","_"))
                .repoName(repository.getName())
                .repoType(ScanRequest.Repository.ADO)
                .incremental(incremental.orElse(cxProperties.getIncremental()))
                .scanPreset(preset.orElse(cxProperties.getScanPreset()))
                .excludeFolders(createExludeList(excludeFolders , cxProperties.getExcludeFolders()))
                .excludeFiles(createExludeList(excludeFiles , cxProperties.getExcludeFiles()))
                .filters(filters);
        if("pull".equals(action)) {
            BugTracker.Type bugType = 
            bug.map(theBug -> ScanUtils.getBugTypeEnum(theBug, flowProperties.getBugTrackerImpl()))
            .orElse(BugTracker.Type.ADOPULL);
        
            appOnlyTracking.ifPresent(flowProperties::setTrackApplicationOnly);
            
            requestBuilder
                .refs(resource.getSourceRefName())
                .repoUrl(repository.getWebUrl())
                .repoUrlWithAuth(addTokenToUrl(repository.getWebUrl() , properties.getToken()))
                .mergeNoteUri(resource.getUrl().concat("/threads"))
                .branch(ScanUtils.getBranchFromRef(resource.getSourceRefName()))
                .mergeTargetBranch(ScanUtils.getBranchFromRef(resource.getTargetRefName()))
                .email(null)
                .bugTracker(ScanUtils.getBugTracker(
                    assignee.orElse(null), 
                        bugType, 
                        jiraProperties, 
                        bug.orElse(null)));
        } else if ("push".equals(action)) {
            BugTracker.Type bugType = ScanUtils.getBugTypeEnum(
                bug.orElse(flowProperties.getBugTracker()), 
                flowProperties.getBugTrackerImpl());


            requestBuilder
                .refs(resource.getRefUpdates().get(0).getName())
                .repoUrl(repository.getRemoteUrl())
                .repoUrlWithAuth(addTokenToUrl(repository.getRemoteUrl() , properties.getToken()))
                .branch(ScanUtils.getBranchFromRef(resource.getRefUpdates().get(0).getName()))
                .email(determineEmails(resource))
                .bugTracker(ScanUtils.getBugTracker(
                    assignee.orElse(null), 
                        bugType, 
                        jiraProperties, 
                        bug.orElse(null)));
        }
        ScanRequest request = requestBuilder.build();

        request = ScanUtils.overrideMap(request, o);
        if ("pull".equals(action)) {     
            request.putAdditionalMetadata("statuses_url", resource.getUrl().concat("/statuses"));
        }
        //String baseUrl = body.getResourceContainers().getAccount().getBaseUrl();
        String baseUrl = body.getResourceContainers().getCollection().getBaseUrl();
        request.putAdditionalMetadata(Constants.ADO_BASE_URL_KEY,baseUrl);
        request.putAdditionalMetadata(Constants.ADO_ISSUE_KEY, adoIssueType.orElse(properties.getIssueType()));
        request.putAdditionalMetadata(Constants.ADO_ISSUE_BODY_KEY, adoIssueBody.orElse(properties.getIssueBody()));
        request.putAdditionalMetadata(Constants.ADO_OPENED_STATE_KEY, adoOpenedState.orElse(properties.getOpenStatus()));
        request.putAdditionalMetadata(Constants.ADO_CLOSED_STATE_KEY, adoClosedState.orElse(properties.getClosedStatus()));
        request.setId(uid);
        //only initiate scan/automation if target branch is applicable
        List<String> branches = new ArrayList<>();
        branch.ifPresentOrElse(
            branches::addAll,
            () -> {
                if(!ScanUtils.empty(flowProperties.getBranches())) {
                    branches.addAll(flowProperties.getBranches());
                }
            }
        );
        
        if(helperService.isBranch2Scan(request, branches)){
            flowService.initiateAutomation(request);
        }
        return ResponseEntity.accepted().body(EventResponse.builder()
                .message("Scan Request Successfully Submitted")
                .success(true)
                .build());
    }

    private List<String> determineEmails(Resource resource) {
        List<String> emails = new ArrayList<>();
        if(resource.getCommits() != null) {
            emails = new ArrayList<>(resource.getCommits()
                    .stream()
                    .filter(c -> c.getAuthor() != null && !ScanUtils.empty(c.getAuthor().getEmail()))
                    .map(c -> c.getAuthor().getEmail())
                    .collect(Collectors.toList()));
            emails.add(resource.getPushedBy().getUniqueName());
        }
        return emails;
    }

    private String getAction(HttpServletRequest request) {
        String pathInfo = request.getRequestURI();
        return pathInfo.substring(pathInfo.length()-4);
    }

    private Product getProductForName(Optional<String> product) {
        return product.map(pr -> ScanRequest.Product.valueOf(pr.toUpperCase(Locale.ROOT)))
            .orElse(ScanRequest.Product.CX);
    }

    /*
     * $1 is http or https
     * $2 is the remainder of the url.
     */
    private String addTokenToUrl(String url , String token) {
        return url.replaceAll("^(https?:\\/\\/)(.+$)", "$1"+ token +"@"+"$2");
    }

    private List<String> createExludeList(Optional<List<String>> list , String defaultString) {
        return list.orElse(
                defaultString == null ?
                        Collections.EMPTY_LIST
                        : Arrays.asList(StringUtils.split(defaultString, ","))
        );
    }

    private void validateBasicAuth(String token){
        String auth = "Basic ".concat(Base64.getEncoder().encodeToString(properties.getWebhookToken().getBytes()));
        if(!auth.equals(token)){
            throw new InvalidTokenException();
        }
    }
}
