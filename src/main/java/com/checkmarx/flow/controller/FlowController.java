package com.checkmarx.flow.controller;

import com.checkmarx.flow.config.CxProperties;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.dto.*;
import com.checkmarx.flow.exception.InvalidTokenException;
import com.checkmarx.flow.service.FlowService;
import com.checkmarx.flow.service.HelperService;
import com.checkmarx.flow.utils.Constants;
import com.checkmarx.flow.utils.ScanUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.beans.ConstructorProperties;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;


/**
 * REST endpoint for Cx-Flow. Currently supports fetching the latest scan results given a Checkmarx Project Name
 * and a fully qualified Checkmarx Team Path (ex. CxServer\EMEA\Marketing)
 */
@RestController
@RequestMapping(value = "/")
public class FlowController {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(FlowController.class);

    // Simple (shared) API token
    private static final String TOKEN_HEADER = "x-cx-token";

    private final FlowProperties properties;
    private final CxProperties cxProperties;
    private final FlowService scanService;
    private final HelperService helperService;
    private final JiraProperties jiraProperties;

    @ConstructorProperties({"properties", "cxProperties", "scanService", "helperService", "jiraProperties"})
    public FlowController(FlowProperties properties, CxProperties cxProperties, FlowService scanService,
                          HelperService helperService, JiraProperties jiraProperties) {
        this.properties = properties;
        this.cxProperties = cxProperties;
        this.scanService = scanService;
        this.helperService = helperService;
        this.jiraProperties = jiraProperties;
    }

    @RequestMapping(value = "/scanresults", method = RequestMethod.GET, produces = "application/json")
    public ScanResults latestScanResults(
            // Mandatory parameters
            @RequestParam(value = "project", required = true) String project,
            @RequestHeader(value = TOKEN_HEADER) String token,
            // Optional parameters
            @RequestParam(value = "team", required = false) String team,
            @RequestParam(value = "application", required = false) String application,
            @RequestParam(value = "severity", required = false) List<String> severity,
            @RequestParam(value = "cwe", required = false) List<String> cwe,
            @RequestParam(value = "category", required = false) List<String> category,
            @RequestParam(value = "status", required = false) List<String> status,
            @RequestParam(value = "assignee", required = false) String assignee,
            @RequestParam(value = "override", required = false) String override,
            @RequestParam(value = "bug", required = false) String bug) {

        String uid = helperService.getShortUid();
        MDC.put("cx", uid);
        // Validate shared API token from header
        validateToken(token);

        // Create bug tracker
        BugTracker bugTracker = getBugTracker(assignee, bug);

        // Create filters if available
        List<Filter> filters = getFilters(severity, cwe, category, status);

        // Create the scan request
        ScanRequest scanRequest = ScanRequest.builder()
                // By default, use project as application, unless overridden
                .application(ScanUtils.empty(application) ? project : application)
                .product(ScanRequest.Product.CX) // Default product: CX
                .project(project)
                .team(team)
                .bugTracker(bugTracker)
                .filters(filters)
                .build();
        scanRequest.setId(uid);
        // If an override blob/file is provided, substitute these values
        if (!ScanUtils.empty(override)) {
            MachinaOverride ovr = ScanUtils.getMachinaOverride(override);
            scanRequest = ScanUtils.overrideMap(scanRequest, ovr);
        }

        // Fetch the Checkmarx Scan Results based on given ScanRequest.
        // The cxProject parameter is null because the required project metadata
        // is already contained in the scanRequest parameter.
        ScanResults scanResults = scanService.cxGetResults(scanRequest, null).join();
        log.debug("ScanResults {}", scanResults);

        return scanResults;
    }

    @PostMapping("/scan")
    public ResponseEntity<EventResponse> initiateScan(
            @RequestBody CxScanRequest scanRequest,
            @RequestHeader(value = TOKEN_HEADER) String token
    ){
        String uid = helperService.getShortUid();
        String errorMessage = "Error submitting Scan Request.";
        MDC.put("cx", uid);
        log.info("Processing Scan initiation request");

        validateToken(token);

        try {
            log.trace(scanRequest.toString());
            ScanRequest.Product product = ScanRequest.Product.CX;
            String project = scanRequest.getProject();
            String branch = scanRequest.getBranch();
            String application = scanRequest.getApplication();
            String team = scanRequest.getTeam();

            if(ScanUtils.empty(application)){
                application = project;
            }
            if(ScanUtils.empty(team)){
                team = cxProperties.getTeam();
            }
            properties.setTrackApplicationOnly(scanRequest.isApplicationOnly());

            if(ScanUtils.anyEmpty(project, branch, scanRequest.getGitUrl())){
                log.error("{}  The project | branch | git_url was not provided", errorMessage);
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(EventResponse.builder()
                        .message(errorMessage)
                        .success(false)
                        .build());
            }
            String scanPreset = cxProperties.getScanPreset();
            if(!ScanUtils.empty(scanRequest.getPreset())){
                scanPreset = scanRequest.getPreset();
            }
            boolean inc = cxProperties.getIncremental();
            if(scanRequest.isIncremental()){
                inc = true;
            }

            BugTracker.Type bugType;
            if(!ScanUtils.empty(scanRequest.getBug())){
                bugType = ScanUtils.getBugTypeEnum(scanRequest.getBug(), properties.getBugTrackerImpl());
            }
            else {
                bugType = ScanUtils.getBugTypeEnum(properties.getBugTracker(), properties.getBugTrackerImpl());
            }

            if(!ScanUtils.empty(scanRequest.getProduct())){
                product = ScanRequest.Product.valueOf(scanRequest.getProduct().toUpperCase(Locale.ROOT));
            }

            List<Filter> filters = getFilters(properties.getFilterSeverity(), properties.getFilterCwe(), properties.getFilterCategory(), properties.getFilterStatus());
            if(!ScanUtils.empty(scanRequest.getFilters())){
                filters = scanRequest.getFilters();
            }

            String bug = properties.getBugTracker();
            if(!ScanUtils.empty(scanRequest.getBug())){
                bug = scanRequest.getBug();
            }

            BugTracker bt = ScanUtils.getBugTracker(scanRequest.getAssignee(), bugType, jiraProperties, bug);

            List<String> excludeFiles = scanRequest.getExcludeFiles();
            List<String> excludeFolders = scanRequest.getExcludeFolders();
            if((excludeFiles == null) && !ScanUtils.empty(cxProperties.getExcludeFiles())){
                excludeFiles = Arrays.asList(cxProperties.getExcludeFiles().split(","));
            }
            if(excludeFolders == null && !ScanUtils.empty(cxProperties.getExcludeFolders())){
                excludeFolders = Arrays.asList(cxProperties.getExcludeFolders().split(","));
            }

            ScanRequest request = ScanRequest.builder()
                    .application(application)
                    .product(product)
                    .project(project)
                    .team(team)
                    .namespace(scanRequest.getNamespace())
                    .repoName(scanRequest.getRepoName())
                    .repoUrl(scanRequest.getGitUrl())
                    .repoUrlWithAuth(scanRequest.getGitUrl())
                    .repoType(ScanRequest.Repository.NA)
                    .branch(branch)
                    .refs(Constants.CX_BRANCH_PREFIX.concat(branch))
                    .email(null)
                    .incremental(inc)
                    .scanPreset(scanPreset)
                    .excludeFolders(excludeFolders)
                    .excludeFiles(excludeFiles)
                    .bugTracker(bt)
                    .filters(filters)
                    .build();
            request.setId(uid);

            if(!ScanUtils.empty(scanRequest.getResultUrl())){
                request.putAdditionalMetadata("result_url", scanRequest.getResultUrl());
            }

            scanService.initiateAutomation(request);

        }catch (Exception e){
            log.error("Error submitting Scan Request. {}", ExceptionUtils.getMessage(e));
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
     * Validates given token against the token value defined in the cx-flow section of the application yml.
     *
     * @param token token to validate
     */
    private void validateToken(String token) {
        log.info("Validating REST API token");
        if (!properties.getToken().equals(token)) {
            log.error("REST API token validation failed");
            throw new InvalidTokenException();
        }
        log.info("Validation successful");
    }

    /**
     * Creates a list of {@link Filter}s based on any given values. If no values are provided,
     * the filter values specified in the cx-flow section of the yml file will be used.
     *
     * @param severity list of severity values to use
     * @param cwe      list of CWE values to use
     * @param category list of vulnerability categories to use
     * @param status   list of status values to use
     * @return list of {@link Filter} objects
     */
    protected List<Filter> getFilters(List<String> severity, List<String> cwe, List<String> category, List<String> status) {
        List<Filter> filters;
        // If values are provided, use them
        if (!ScanUtils.empty(severity) || !ScanUtils.empty(cwe) || !ScanUtils.empty(category) || !ScanUtils.empty(status)) {
            filters = ScanUtils.getFilters(severity, cwe, category, status);
        }
        // otherwise, default to filters specified in the cx-flow section of the yml
        else {
            filters = ScanUtils.getFilters(properties.getFilterSeverity(), properties.getFilterCwe(),
                    properties.getFilterCategory(), properties.getFilterStatus());
        }
        return filters;
    }

    /**
     * Creates a {@link BugTracker} from given values. If values are not provided,
     * a default tracker of type {@link BugTracker.Type#NONE} will be returned.
     *
     * @param assignee assignee for bug tracking
     * @param bug  bug tracker type to use
     * @return a {@link BugTracker}
     */
    protected BugTracker getBugTracker(String assignee, String bug) {
        // Default bug tracker type : NONE
        BugTracker bugTracker = BugTracker.builder().type(BugTracker.Type.NONE).build();

        // If a bug tracker is explicitly provided, override the default
        if (!ScanUtils.empty(bug)) {
            BugTracker.Type bugTypeEnum = ScanUtils.getBugTypeEnum(bug, properties.getBugTrackerImpl());
            bugTracker = ScanUtils.getBugTracker(assignee, bugTypeEnum, jiraProperties, bug);
        }
        return bugTracker;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CxScanRequest {
        /*required*/
        @JsonProperty("git_url")
        public String gitUrl;
        @JsonProperty("branch")
        public String branch;
        @JsonProperty("application")
        public String application;
        @JsonProperty("cx_project")
        public String project;
        /*optional*/
        @JsonProperty("result_url")
        public String resultUrl;
        @JsonProperty("namespace")
        public String namespace;
        @JsonProperty("repo_name")
        public String repoName;
        @JsonProperty("cx_team")
        public String team;
        @JsonProperty("filters")
        public List<Filter> filters;
        @JsonProperty("cx_product")
        public String product;
        @JsonProperty("cx_preset")
        public String preset;
        @JsonProperty("cx_incremental")
        public boolean incremental;
        @JsonProperty("cx_configuration")
        public String configuration;
        @JsonProperty("cx_exclude_files")
        public List<String> excludeFiles;
        @JsonProperty("cx_exclude_folders")
        public List<String> excludeFolders;
        @JsonProperty("bug")
        public String bug;
        @JsonProperty("assignee")
        public String assignee;
        @JsonProperty("application_only")
        public boolean applicationOnly = false;

        public String getGitUrl() {
            return gitUrl;
        }

        public void setGitUrl(String gitUrl) {
            this.gitUrl = gitUrl;
        }

        public String getBranch() {
            return branch;
        }

        public void setBranch(String branch) {
            this.branch = branch;
        }

        public String getApplication() {
            return application;
        }

        public void setApplication(String application) {
            this.application = application;
        }

        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        public String getRepoName() {
            return repoName;
        }

        public void setRepoName(String repoName) {
            this.repoName = repoName;
        }

        public String getProject() {
            return project;
        }

        public void setProject(String project) {
            this.project = project;
        }

        public String getTeam() {
            return team;
        }

        public void setTeam(String team) {
            this.team = team;
        }

        public String getPreset() {
            return preset;
        }

        public void setPreset(String preset) {
            this.preset = preset;
        }

        public String getConfiguration() {
            return configuration;
        }

        public void setConfiguration(String configuration) {
            this.configuration = configuration;
        }

        public String getBug() {
            return bug;
        }

        public void setBug(String bug) {
            this.bug = bug;
        }

        public String getProduct() {
            return product;
        }

        public void setProduct(String product) {
            this.product = product;
        }

        public List<String> getExcludeFiles() {
            return excludeFiles;
        }

        public void setExcludeFiles(List<String> excludeFiles) {
            this.excludeFiles = excludeFiles;
        }

        public List<String> getExcludeFolders() {
            return excludeFolders;
        }

        public void setExcludeFolders(List<String> excludeFolders) {
            this.excludeFolders = excludeFolders;
        }

        public boolean isApplicationOnly() {
            return applicationOnly;
        }

        public String getAssignee() {
            return assignee;
        }

        public void setAssignee(String assignee) {
            this.assignee = assignee;
        }

        public void setApplicationOnly(boolean applicationOnly) {
            this.applicationOnly = applicationOnly;
        }

        public List<Filter> getFilters() {
            return filters;
        }

        public void setFilters(List<Filter> filters) {
            this.filters = filters;
        }

        public boolean isIncremental() {
            return incremental;
        }

        public void setIncremental(boolean incremental) {
            this.incremental = incremental;
        }

        public String getResultUrl() {
            return resultUrl;
        }

        public void setResultUrl(String resultUrl) {
            this.resultUrl = resultUrl;
        }

        @Override
        public String toString() {
            return "CxScanRequest{" +
                    "gitUrl='" + gitUrl + '\'' +
                    ", branch='" + branch + '\'' +
                    ", application='" + application + '\'' +
                    ", project='" + project + '\'' +
                    ", resultUrl='" + resultUrl + '\'' +
                    ", namespace='" + namespace + '\'' +
                    ", repoName='" + repoName + '\'' +
                    ", team='" + team + '\'' +
                    ", filters=" + filters +
                    ", product='" + product + '\'' +
                    ", preset='" + preset + '\'' +
                    ", incremental=" + incremental +
                    ", configuration='" + configuration + '\'' +
                    ", excludeFiles=" + excludeFiles +
                    ", excludeFolders=" + excludeFolders +
                    ", bug='" + bug + '\'' +
                    ", assignee='" + assignee + '\'' +
                    ", applicationOnly=" + applicationOnly +
                    '}';
        }
    }


}
