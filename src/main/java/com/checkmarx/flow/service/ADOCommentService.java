package com.checkmarx.flow.service;

import com.checkmarx.flow.config.*;
import com.checkmarx.flow.controller.ADOController;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ControllerRequest;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.azure.*;
import com.checkmarx.flow.utils.CommonUtils;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.CxPropertiesBase;
import com.checkmarx.sdk.dto.filtering.FilterConfiguration;
import com.checkmarx.sdk.service.CxService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ADOCommentService {

    private final ADOProperties properties;

    private final FlowService flowService;
    private final FlowProperties flowProperties;
    private final CxPropertiesBase cxProperties;
    private final ScmConfigOverrider scmConfigOverrider;
    private final ADOConfigService adoConfigService;

    @Autowired
    GitAuthUrlGenerator gitAuthUrlGenerator;

    @Autowired
    JiraProperties jiraProperties;

    @Autowired
    HelperService helperService;


    @Autowired
    FilterFactory filterFactory;

    @Autowired
    @Qualifier("cxService")
    private CxService cxService;


    public ADOCommentService(ADOProperties properties, FlowService flowService,
                             FlowProperties flowProperties, ScmConfigOverrider scmConfigOverrider, CxScannerService cxScannerService, ADOConfigService adoConfigService) {
        this.properties = properties;
        this.flowService = flowService;
        this.flowProperties = flowProperties;
        this.cxProperties = cxScannerService.getProperties();
        this.scmConfigOverrider = scmConfigOverrider;
        this.adoConfigService = adoConfigService;
    }

    private boolean isScanCommand(String command) {
        return command.equalsIgnoreCase("cancel") || command.equalsIgnoreCase("status");
    }

    private Optional<Integer> extractScanId(String command, String comment) {
        log.info("Extracting ScanId from comment: {}", command);
        Pattern pattern;
        pattern = command.equalsIgnoreCase("cancel") ? Pattern.compile("@cxflow cancel (\\d+)") : Pattern.compile("@cxflow status (\\d+)");
        Matcher matcher = pattern.matcher(comment);
        return matcher.find() ? Optional.of(Integer.parseInt(matcher.group(1))) : Optional.empty();
    }

    private URI buildPullRequestCommentsURL(String baseUrl, String projectName, String repositoryId, Integer pullRequestId, Integer threadId) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl).pathSegment(projectName, "_apis", "git", "repositories", repositoryId, "pullRequests", String.valueOf(pullRequestId), "threads", String.valueOf(threadId), "comments");
        return builder.queryParam("api-version", "7.1").build().toUri();
    }


    private void processPRCommentCommand(PRCommentEvent event, ADOProperties properties, String command, String baseUrl,
                                         String projectName, String repositoryId, Integer pullRequestId, Integer threadId, Optional<Integer> scanID, Map<FindingSeverity, Integer> thresholdMap, List<String> branches, ControllerRequest controllerRequest, String product,
                                         ResourceContainers resourceContainers, String body, ADOController.Action action, String uid, AdoDetailsRequest adoDetailsRequest, String userName) {
        switch (command) {
            case "hi":
                postComment(properties, " Hi " + userName + "," + "\n How can CX-Flow help you? \n" + "- Get the status of the current scan by posting the command: <b>@CXFlow</b> status scanID\n" + "- Perform a new scan by posting the command: <b>@CXFlow</b> rescan\n" + "- Cancel a running scan by posting the command: <b>@CXFlow</b> cancel scanID", baseUrl, projectName, repositoryId, pullRequestId, threadId);
                log.info("Finished processing for PR comment :@Cxflow hi");
                break;

            case "status":
                if (scanID.isPresent()) {
                    postComment(properties, "- Scan with scanID " + scanID.get() + " is in: " + "<b>" + cxService.getScanStatusName(scanID.get()) + "</b>" + " state", baseUrl, projectName, repositoryId, pullRequestId, threadId);
                    log.info("Finished processing for PR comment :@Cxflow Scan status");
                }
                break;

            case "cancel":
                if (scanID.isPresent()) {
                    if (!cxService.getScanStatusName(scanID.get()).equalsIgnoreCase("Finished")) {
                        cxService.cancelScan(scanID.get());
                        postComment(properties, "- Scan cancelled with ScanID:" + scanID.get(), baseUrl, projectName, repositoryId, pullRequestId, threadId);
                    } else {
                        postComment(properties, "- Cannot cancel already finished Scan with ScanID: " + scanID.get(), baseUrl, projectName, repositoryId, pullRequestId, threadId);
                    }

                    log.info("Finished processing for PR comment :@CxFlow cancel");
                }
                break;

            case "rescan":
                postComment(properties, "- Rescan initiated.", baseUrl, projectName, repositoryId, pullRequestId, threadId);
                String rescanStatus = triggerRescan(event, controllerRequest, adoDetailsRequest, product, resourceContainers, body, action, uid, thresholdMap, branches, threadId);
                log.info("Finished processing for PR comment :@CxFlow {}", command);
                log.info("Status for rescan: {} ", rescanStatus);
                break;

            default:
                unsupportedCommand(properties, baseUrl, projectName, repositoryId, pullRequestId, threadId, userName);
                log.info("Received Unsupported command for CxFlow");
        }
    }

    private void unsupportedCommand(ADOProperties properties, String baseUrl, String projectName, String repositoryId, Integer pullRequestId, Integer threadId, String username) {
        postComment(properties, "I'm afraid I can't do that " + username, baseUrl, projectName, repositoryId, pullRequestId, threadId);
    }

    private void postComment(ADOProperties properties, String content, String baseUrl, String projectName, String repositoryId, Integer pullRequestId, Integer threadId) {
        try {
            log.info("Posting the Comment");
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.valueOf(javax.ws.rs.core.MediaType.APPLICATION_JSON));
            headers.setBearerAuth(properties.getToken());

            Map<String, String> body = new HashMap<>();
            body.put("content", content);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            URI uriToPost = buildPullRequestCommentsURL(baseUrl, projectName, repositoryId, pullRequestId, threadId);
            restTemplate.postForEntity(uriToPost, request, String.class);

        } catch (HttpClientErrorException e) {
            log.error("Error occurred while posting comment on PR {}", e.getStatusCode());
            log.debug(ExceptionUtils.getStackTrace(e));
        }

    }

    private void processNoScanIdCommand(ADOProperties properties, String command, String baseUrl, String projectName, String repositoryId, Integer pullRequestId, Integer threadId) {
        log.info("Processing No ScanId provided Command");
        postComment(properties, "Please provide Scan ID", baseUrl, projectName, repositoryId, pullRequestId, threadId);
    }

    public void adoPRCommentHandler(PRCommentEvent event, ADOProperties properties, String comment, String baseUrl, String projectName, String repositoryId, Integer pullRequestId, Integer threadId, Map<FindingSeverity, Integer> thresholdMap, List<String> branches, ControllerRequest controllerRequest, String product, ResourceContainers resourceContainers, String body, ADOController.Action action, String uid, AdoDetailsRequest adoDetailsRequest) {
        log.info("Parsing the PR comment");
        String userName = event.getResource().getComment().getAuthor().getDisplayName();
        String command = CommonUtils.parseCommand(comment);
        Optional<Integer> scanID = isScanCommand(command) ? extractScanId(command, comment) : Optional.empty();

        if (scanID.isEmpty() && isScanCommand(command)) {
            processNoScanIdCommand(properties, command, baseUrl, projectName, repositoryId, pullRequestId, threadId);
        } else {
            processPRCommentCommand(event, properties, command, baseUrl, projectName, repositoryId, pullRequestId, threadId, scanID, thresholdMap, branches, controllerRequest, product,
                    resourceContainers, body, action, uid, adoDetailsRequest, userName);
        }
    }

    private String triggerRescan(PRCommentEvent event, ControllerRequest controllerRequest, AdoDetailsRequest adoDetailsRequest, String product,
                                 ResourceContainers resourceContainers, String body, ADOController.Action action,
                                 String uid, Map<FindingSeverity, Integer> thresholdMap, List<String> branches, Integer threadId) {
        try {
            ResourceComment resource = event.getResource();
            Repository repository = resource.getPullRequest().getRepository();
            String pullUrl = resource.getPullRequest().getUrl();
            String app = repository.getName();

            if (repository.getName().startsWith(properties.getTestRepository())) {
                log.info("Handling ADO Test Event");
                return "Test Event";
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

            adoConfigService.initAdoSpecificParams(adoDetailsRequest);

            if (StringUtils.isEmpty(product)) {
                product = ScanRequest.Product.CX.getProduct();
            }
            ScanRequest.Product p = ScanRequest.Product.valueOf(product.toUpperCase(Locale.ROOT));

            String ref = resource.getPullRequest().getSourceRefName();
            String currentBranch = ScanUtils.getBranchFromRef(ref);
            String targetBranch = ScanUtils.getBranchFromRef(resource.getPullRequest().getTargetRefName());


            BugTracker bt = ScanUtils.getBugTracker(controllerRequest.getAssignee(), bugType, jiraProperties, controllerRequest.getBug());

            FilterConfiguration filter = filterFactory.getFilter(controllerRequest, flowProperties);


            //build request object
            String gitUrl = repository.getWebUrl();
            String token = scmConfigOverrider.determineConfigToken(properties, controllerRequest.getScmInstance());
            log.info("Using url: {}", gitUrl);
            String gitAuthUrl = gitAuthUrlGenerator.addCredToUrl(ScanRequest.Repository.ADO, gitUrl, token);

            ScanRequest request = ScanRequest.builder().application(app).product(p).project(controllerRequest.getProject()).team(controllerRequest.getTeam()).namespace(adoConfigService.determineNamespace(resourceContainers)).repoName(repository.getName()).repoUrl(gitUrl).repoUrlWithAuth(gitAuthUrl).repoType(ScanRequest.Repository.ADO).branch(currentBranch).refs(ref).mergeNoteUri(pullUrl.concat("/threads")).mergeTargetBranch(targetBranch).email(null).scanPreset(controllerRequest.getPreset()).incremental(controllerRequest.getIncremental()).excludeFolders(controllerRequest.getExcludeFolders()).excludeFiles(controllerRequest.getExcludeFiles()).bugTracker(bt).filter(filter).thresholds(thresholdMap).organizationId(adoConfigService.determineNamespace(resourceContainers)).gitUrl(gitUrl).build();

            //setScmInstance
            Optional.ofNullable(controllerRequest.getScmInstance()).ifPresent(request::setScmInstance);

            request.putAdditionalMetadata(ADOService.PROJECT_SELF_URL, getProjectURL(event.getResourceContainers()));
            adoConfigService.fillRequestWithAdditionalData(request, repository, body.toString());
            //checkForConfigAsCode(request, getConfigBranch(request, resource, action));
            request.putAdditionalMetadata("statuses_url", pullUrl.concat("/statuses"));
            request.putAdditionalMetadata(Constants.ADO_ISSUE_KEY, adoDetailsRequest.getAdoIssue());
            request.putAdditionalMetadata(Constants.ADO_ISSUE_BODY_KEY, adoDetailsRequest.getAdoBody());
            request.putAdditionalMetadata(Constants.ADO_OPENED_STATE_KEY, adoDetailsRequest.getAdoOpened());
            request.putAdditionalMetadata(Constants.ADO_CLOSED_STATE_KEY, adoDetailsRequest.getAdoClosed());
            request.setId(uid);
            //only initiate scan/automation if target branch is applicable
            if (helperService.isBranch2Scan(request, branches)) {
                log.debug(request.getProject() + " :: Calling  isBranch2Scan function End : " + System.currentTimeMillis());
                log.debug(request.getProject() + " :: Free Memory : " + Runtime.getRuntime().freeMemory());
                log.debug(request.getProject() + " :: Total Numbers of processors : " + Runtime.getRuntime().availableProcessors());
                long startTime = System.currentTimeMillis();
                log.debug(request.getProject() + " :: Start Time : " + startTime);
                flowService.initiateAutomation(request);
                long endTime = System.currentTimeMillis();
                log.debug(request.getProject() + " :: End Time  : " + endTime);
                log.debug(request.getProject() + " :: Total Time Taken  : " + (endTime - startTime));
            } else {
                return ScanState.FAILED.getState();
            }

        } catch (IllegalArgumentException e) {
            log.error("\"Error submitting Scan Request. Product: " + product + "or Bugtracker option incorrect:" + controllerRequest.getBug(), e);
            return ScanState.FAILED.getState();
        }
        return ScanState.SUCCESS.getState();
    }


    private String getProjectURL(ResourceContainers resourceContainers) {
        String projectId = resourceContainers.getProject().getId();
        String baseUrl = resourceContainers.getProject().getBaseUrl();
        return baseUrl.concat(projectId);
    }

    private enum ScanState {
        SUCCESS("SUCESS"),
        FAILED("FAILED");
        private final String state;

        ScanState(String state) {
            this.state = state;
        }
        public String getState() {
            return state;
        }
    }
}
