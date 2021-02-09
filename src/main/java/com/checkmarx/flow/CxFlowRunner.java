package com.checkmarx.flow;

import com.checkmarx.flow.config.*;
import com.checkmarx.flow.constants.FlowConstants;
import com.checkmarx.flow.dto.*;
import com.checkmarx.flow.exception.*;
import com.checkmarx.flow.service.*;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.CxPropertiesBase;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.filtering.FilterConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.MDC;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.info.BuildProperties;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static com.checkmarx.flow.exception.ExitThrowable.exit;

@Component
@RequiredArgsConstructor
@Slf4j
public class CxFlowRunner implements ApplicationRunner {

    /**
     * Command line option that causes CxFlow to throw an exception instead of exiting.
     * Introduced for testing to prevent sudden test interruption and allow to evaluate exit codes.
     */
    public static final String THROW_INSTEAD_OF_EXIT_OPTION = "blocksysexit";

    public static final String PARSE_OPTION = "parse";
    public static final String BATCH_OPTION = "batch";

    private final FlowProperties flowProperties;
    private final CxScannerService cxScannerService;
    private final JiraProperties jiraProperties;
    private final GitHubProperties gitHubProperties;
    private final GitLabProperties gitLabProperties;
    private final ADOProperties adoProperties;
    private final HelperService helperService;
    private final List<ThreadPoolTaskExecutor> executors;
    private final ResultsService resultsService;
    private final OsaScannerService osaScannerService;
    private final FilterFactory filterFactory;
    private final ConfigurationOverrider configOverrider;
    private final BuildProperties buildProperties;
    private final List<VulnerabilityScanner> scanners;
    private final ThresholdValidator thresholdValidator;
    private static final String ERROR_BREAK_MSG = String.format("Exiting with Error code %d because some of the checks weren't passed", ExitCode.BUILD_INTERRUPTED.getValue());

    @PostConstruct
    private void logVersion() {
        log.info("=======BUILD INFO=======");
        log.info("Version: {}-{}", buildProperties.getName(), buildProperties.getVersion());
        log.info("Time: {}", buildProperties.getTime().toString());
        log.info("=======================");
    }

    @Override
    public void run(ApplicationArguments args) throws InvocationTargetException {

        if (!args.getOptionNames().isEmpty()) {
            try {
                if (args.containsOption("web")) {
                    log.debug("Running web mode");
                } else {
                    log.debug("Running cmd mode. Parameters: {}", String.join(" ", args.getSourceArgs()));
                    commandLineRunner(args);
                }
            } catch (ExitThrowable ee) {
                log.info("Finished with exit code: {}", ee.getExitCode());
                if (args.containsOption(THROW_INSTEAD_OF_EXIT_OPTION)) {
                    throw new InvocationTargetException(ee);
                }
                System.exit(ee.getExitCode());
            } finally {
                if (executors != null && (args.containsOption("scan") || args.containsOption(PARSE_OPTION) || args.containsOption(BATCH_OPTION))) {
                    executors.forEach(ThreadPoolTaskExecutor::shutdown);
                }
            }
        }
    }

    private void commandLineRunner(ApplicationArguments args) throws ExitThrowable {
        String bugTracker;
        String application;
        String namespace;
        String repoName;
        String repoUrl;
        String branch;
        String mergeId;
        String mergeNoteUri = null;
        String projectId;
        String assignee;
        List<String> emails;
        String file;
        String libFile;
        String preset;
        String team;
        String cxProject;
        String altProject;
        String altFields;
        String config;
        List<String> severity;
        List<String> cwe;
        List<String> category;
        List<String> status;
        List<String> excludeFiles;
        List<String> excludeFolders;
        ScanRequest.Repository repoType = ScanRequest.Repository.NA;
        boolean osa;
        boolean force;
        FlowOverride flowOverride = null;
        ObjectMapper mapper = new ObjectMapper();
        String uid = helperService.getShortUid();
        MDC.put(FlowConstants.MAIN_MDC_ENTRY, uid);

        if (args.containsOption("branch-create")) {
            exit(ExitCode.SUCCESS);
        }
        if (args.containsOption("branch-delete")) {
            exit(ExitCode.SUCCESS);
        }

        if (!args.containsOption("scan") && !args.containsOption(PARSE_OPTION) && !args.containsOption(BATCH_OPTION) && !args.containsOption("project")) {
            log.error("--scan | --parse | --batch | --project option must be specified");
            exit(1);
        }

        //override with config
        if (args.containsOption("config")) {
            config = args.getOptionValues("config").get(0);
            try {
                flowOverride = mapper.readValue(new File(config), FlowOverride.class);
            } catch (IOException e) {
                log.error("Error reading config file, ignoring...", e);
            }
        }

        /*Collect command line options (String)*/
        bugTracker = getOptionValues(args, "bug-tracker");
        file = getOptionValues(args, "f");
        libFile = getOptionValues(args, "lib-file");
        repoName = getOptionValues(args, "repo-name");
        repoUrl = getOptionValues(args, "repo-url");
        branch = getOptionValues(args, "branch");
        namespace = getOptionValues(args, "namespace");
        projectId = getOptionValues(args, "project-id");
        team = getOptionValues(args, "cx-team");
        altProject = getOptionValues(args, "alt-project");
        altFields = getOptionValues(args, "alt-fields");
        cxProject = getOptionValues(args, "cx-project");
        application = getOptionValues(args, "app");
        assignee = getOptionValues(args, "assignee");
        mergeId = getOptionValues(args, "merge-id");
        preset = getOptionValues(args, "preset");
        osa = args.getOptionValues("osa") != null;
        force = args.getOptionValues("forcescan") != null;
        /*Collect command line options (List of Strings)*/
        emails = args.getOptionValues("emails");
        severity = args.getOptionValues("severity");
        category = args.getOptionValues("category");
        cwe = args.getOptionValues("cwe");
        status = args.getOptionValues("status");
        excludeFiles = args.getOptionValues("exclude-files");
        excludeFolders = args.getOptionValues("exclude-folders");
        boolean usingBitBucketCloud = args.containsOption("bb");
        boolean usingBitBucketServer = args.containsOption("bbs");
        CxPropertiesBase cxProperties = cxScannerService.getProperties();
        
        if (((ScanUtils.empty(namespace) && ScanUtils.empty(repoName) && ScanUtils.empty(branch)) &&
                ScanUtils.empty(application)) && !args.containsOption(BATCH_OPTION)) {
            log.error("Namespace/Repo/Branch or Application (app) must be provided");
            exit(1);
        }

        ControllerRequest controllerRequest = new ControllerRequest(severity, cwe, category, status);
        FilterConfiguration filter = filterFactory.getFilter(controllerRequest, flowProperties);

        //Adding default file/folder exclusions from properties if they are not provided as an override
        if (excludeFiles == null && !ScanUtils.empty(cxProperties.getExcludeFiles())) {
            excludeFiles = Arrays.asList(cxProperties.getExcludeFiles().split(","));
        }
        if (excludeFolders == null && !ScanUtils.empty(cxProperties.getExcludeFolders())) {
            excludeFolders = Arrays.asList(cxProperties.getExcludeFolders().split(","));
        }
        if (ScanUtils.empty(bugTracker)) {
            bugTracker = flowProperties.getBugTracker();
        }
        BugTracker.Type bugType = getBugTrackerType(bugTracker);
        ScanRequest.Product product;
        if (osa) {
            if (libFile == null) {
                log.error("Both vulnerabilities file (f) and libraries file (lib-file) must be provided for OSA");
                exit(1);
            }
            product = ScanRequest.Product.CXOSA;
        } else {
            product = ScanRequest.Product.CX;
        }

        if (ScanUtils.empty(preset)) {
            preset = cxProperties.getScanPreset();
        }

        BugTracker bt = null;
        String gitAuthUrl = null;
        switch (bugType) {
            case WAIT:
            case wait:
                log.info("No bug tracker will be used...waiting for scan to complete");
                bugType = BugTracker.Type.WAIT;
                bt = BugTracker.builder()
                        .type(bugType)
                        .build();
                break;
            case NONE:
                log.info("No bug tracker will be used");
                bugType = BugTracker.Type.NONE;
                bt = BugTracker.builder()
                        .type(bugType)
                        .build();
                break;
            case JIRA:
                bt = jiraPropertiesToBugTracker()
                        .type(bugType)
                        .assignee(assignee)
                        .build();
                break;
            case ADOPULL:
            case adopull:
                bugType = BugTracker.Type.ADOPULL;
                bt = BugTracker.builder()
                        .type(bugType)
                        .build();
                repoType = ScanRequest.Repository.ADO;

                if (ScanUtils.empty(namespace) || ScanUtils.empty(repoName) || ScanUtils.empty(mergeId)) {
                    log.error("Namespace/Repo/MergeId must be provided for ADOPULL bug tracking");
                    exit(1);
                }
                mergeNoteUri = adoProperties.getMergeNoteUri(namespace, repoName, mergeId);
                break;
            case GITHUBPULL:
            case githubpull:
                bugType = BugTracker.Type.GITHUBPULL;
                bt = BugTracker.builder()
                        .type(bugType)
                        .build();
                repoType = ScanRequest.Repository.GITHUB;

                if (ScanUtils.empty(namespace) || ScanUtils.empty(repoName) || ScanUtils.empty(mergeId)) {
                    log.error("--namespace, --repo and --merge-id must be provided for GITHUBPULL bug tracking");
                    exit(1);
                }
                mergeNoteUri = gitHubProperties.getMergeNoteUri(namespace, repoName, mergeId);
                repoUrl = getNonEmptyRepoUrl(namespace, repoName, repoUrl, gitHubProperties.getGitUri(namespace, repoName));
                break;
            case GITLABMERGE:
            case gitlabmerge:
                bugType = BugTracker.Type.GITLABMERGE;
                bt = BugTracker.builder()
                        .type(bugType)
                        .build();
                repoType = ScanRequest.Repository.GITLAB;

                if (ScanUtils.empty(projectId) || ScanUtils.empty(mergeId)) {
                    log.error("--project-id and --merge-id must be provided for GITLABMERGE bug tracking");
                    exit(1);
                }
                mergeNoteUri = gitLabProperties.getMergeNoteUri(projectId, mergeId);

                if (!ScanUtils.empty(namespace) && !ScanUtils.empty(repoName)){
                    repoUrl = getNonEmptyRepoUrl(namespace, repoName, repoUrl, gitLabProperties.getGitUri(namespace, repoName));
                }
                break;
            case BITBUCKETPULL:
            case bitbucketserverpull:
                log.info("BitBucket Pull not currently supported from command line");
                exit(1);
                break;
            case EMAIL:
                break;
            case CUSTOM:
                log.info("Using custom bean implementation  for bug tracking");
                bt = BugTracker.builder()
                        .type(bugType)
                        .customBean(bugTracker)
                        .build();
                break;
            default:
                log.warn("No supported bug tracking type provided");
        }

        ScanRequest request = ScanRequest.builder()
                .application(application)
                .product(product)
                .namespace(namespace)
                .team(team)
                .project(cxProject)
                .repoName(repoName)
                .mergeNoteUri(mergeNoteUri)
                .repoUrl(repoUrl)
                .repoUrlWithAuth(gitAuthUrl)
                .repoType(repoType)
                .branch(branch)
                .refs(null)
                .email(emails)
                .incremental(cxProperties.getIncremental())
                .scanPreset(preset)
                .excludeFolders(excludeFolders)
                .excludeFiles(excludeFiles)
                .bugTracker(bt)
                .filter(filter)
                .altProject(altProject)
                .altFields(altFields)
                .forceScan(force)
                .build();

        request = configOverrider.overrideScanRequestProperties(flowOverride, request);
        /*Determine if BitBucket Cloud/Server is being used - this will determine formatting of URL that links to file/line in repository */
        request.setId(uid);
        if (usingBitBucketCloud) {
            request.setRepoType(ScanRequest.Repository.BITBUCKETSERVER);
            //TODO create browse code url
        } else if (usingBitBucketServer) {
            request.setRepoType(ScanRequest.Repository.BITBUCKETSERVER);
            repoUrl = getBitBuckerServerBrowseUrl(repoUrl);
            request.putAdditionalMetadata("BITBUCKET_BROWSE", repoUrl);
        }

        try {
            if (args.containsOption(PARSE_OPTION)) {
                File f = new File(file);
                if (!f.exists()) {
                    log.error("Result File not found {}", file);
                    exit(ExitCode.ARGUMENT_NOT_PROVIDED);
                }
                if (osa) { //grab the libs file if OSA results
                    File libs = new File(libFile);
                    if (!libs.exists()) {
                        log.error("Library File not found {}", file);
                        exit(ExitCode.ARGUMENT_NOT_PROVIDED);
                    }
                    cxOsaParse(request, f, libs);
                } else { //SAST
                    if (args.containsOption("offline")) {
                        cxProperties.setOffline(true);
                    }
                    log.info("Processing Checkmarx result file {}", file);

                    cxParse(request, f);
                }
            } else if (args.containsOption(BATCH_OPTION)) {
                log.info("Executing batch process");
                cxBatch(request);
            } else if (args.containsOption("project")) {
                if (ScanUtils.empty(cxProject)) {
                    log.error("cx-project must be provided when --project option is used");
                    exit(ExitCode.ARGUMENT_NOT_PROVIDED);
                }
                publishLatestScanResults(request);
            } else if (args.containsOption("scan")) {
                log.info("Executing scan process");
                //GitHub Scan with Git Clone
                if (args.containsOption("github")) {
                    repoUrl = getNonEmptyRepoUrl(namespace, repoName, repoUrl, gitHubProperties.getGitUri(namespace, repoName));
                    String token = gitHubProperties.getToken();
                    gitAuthUrl = repoUrl.replace(Constants.HTTPS, Constants.HTTPS.concat(token).concat("@"));
                    gitAuthUrl = gitAuthUrl.replace(Constants.HTTP, Constants.HTTP.concat(token).concat("@"));

                    scanRemoteRepo(request, repoUrl, gitAuthUrl, branch, ScanRequest.Repository.GITHUB);
                } //GitLab Scan with Git Clone
                else if (args.containsOption("gitlab") && !ScanUtils.anyEmpty(namespace, repoName)) {
                    repoUrl = getNonEmptyRepoUrl(namespace, repoName, repoUrl, gitLabProperties.getGitUri(namespace, repoName));
                    String token = gitLabProperties.getToken();
                    gitAuthUrl = repoUrl.replace(Constants.HTTPS, Constants.HTTPS_OAUTH2.concat(token).concat("@"));
                    gitAuthUrl = gitAuthUrl.replace(Constants.HTTP, Constants.HTTP_OAUTH2.concat(token).concat("@"));
                    scanRemoteRepo(request, repoUrl, gitAuthUrl, branch, ScanRequest.Repository.GITLAB);
                } else if (args.containsOption("bitbucket") && containsRepoArgs(namespace, repoName, branch)) {
                    log.warn("Bitbucket git clone scan not implemented");
                } else if (args.containsOption("ado") && containsRepoArgs(namespace, repoName, branch)) {
                    log.warn("Azure DevOps git clone scan not implemented");
                } else if (file != null) {
                    scanLocalPath(request, file);
                } else {
                    log.error("No valid option was provided for driving scan");
                }
            }
        } catch (Exception e) {
            log.error("An error occurred while processing request", e);
            exit(ExitCode.BUILD_INTERRUPTED);
        }
        log.info("Completed Successfully");
        exit(ExitCode.SUCCESS);
    }

    private String getBitBuckerServerBrowseUrl(String repoUrl) {
        if (repoUrl != null) {
            repoUrl = repoUrl.replace("/scm/", "/projects/");
            repoUrl = repoUrl.replaceAll("/[\\w-]+.git$", "/repos$0");
            repoUrl = repoUrl.replaceAll(".git$", "");
            repoUrl = repoUrl.concat("/browse");
        }
        return repoUrl;
    }

    private BugTracker.BugTrackerBuilder jiraPropertiesToBugTracker() {
        return BugTracker.builder()
                .projectKey(jiraProperties.getProject())
                .issueType(jiraProperties.getIssueType())
                .priorities(jiraProperties.getPriorities())
                .closeTransitionField(jiraProperties.getCloseTransitionField())
                .closeTransitionValue(jiraProperties.getCloseTransitionValue())
                .closedStatus(jiraProperties.getClosedStatus())
                .closeTransition(jiraProperties.getCloseTransition())
                .openStatus(jiraProperties.getOpenStatus())
                .openTransition(jiraProperties.getOpenTransition())
                .fields(jiraProperties.getFields());
    }

    private BugTracker.Type getBugTrackerType(String bugTracker) throws ExitThrowable {
        //set the default bug tracker as per yml
        BugTracker.Type bugTypeEnum;
        try {
            bugTypeEnum = ScanUtils.getBugTypeEnum(bugTracker, flowProperties.getBugTrackerImpl());
        } catch (IllegalArgumentException e) {
            log.error("No valid bug tracker was provided", e);
            bugTypeEnum = BugTracker.Type.NONE;
            exit(1);
        }
        return bugTypeEnum;
    }

    private String getNonEmptyRepoUrl(String namespace, String repoName, String repoUrl, String gitUri) throws ExitThrowable {
        if (Strings.isNullOrEmpty(repoUrl)) {
            if (!ScanUtils.anyEmpty(namespace, repoName)) {
                repoUrl = gitUri;
            } else {
                log.error("Unable to determine git url for scanning, exiting...");
                exit(ExitCode.ARGUMENT_NOT_PROVIDED);
                throw new IllegalArgumentException("repo url is null");
            }
        }
        return repoUrl;
    }

    private boolean containsRepoArgs(String namespace, String repoName, String branch) {
        return (!ScanUtils.empty(namespace) &&
                !ScanUtils.empty(repoName) &&
                !ScanUtils.empty(branch));
    }

    private String getOptionValues(ApplicationArguments arg, String option) {
        if (arg != null && option != null) {
            List<String> values = arg.getOptionValues(option);
            return ScanUtils.empty(values) ? null : values.get(0);
        } else {
            return null;
        }
    }

    private void scanRemoteRepo(ScanRequest request, String gitUrl, String gitAuthUrl, String branch, ScanRequest.Repository repoType) throws ExitThrowable {
        log.info("Initiating scan using Checkmarx git clone");
        request.setRepoType(repoType);
        log.info("Git url: {}", gitUrl);

        request.setBranch(branch);
        request.setRepoUrl(gitUrl);
        request.setRepoUrlWithAuth(gitAuthUrl);
        request.setRefs(Constants.CX_BRANCH_PREFIX.concat(branch));

        ScanResults scanResults = runOnActiveScanners(scanner -> scanner.scanCli(request, "Scan-git-clone"));
        processResults(request, scanResults);
    }

    private void scanLocalPath(ScanRequest request, String path) throws ExitThrowable {
        if (ScanUtils.empty(request.getProject())) {
            log.error("Please provide --cx-project to define the project in Checkmarx");
            exit(ExitCode.ARGUMENT_NOT_PROVIDED);
        }
        ScanResults scanResults = runOnActiveScanners(scanner -> scanner.scanCli(request, "cxFullScan", new File(path)));
        processResults(request, scanResults);
    }

    private void cxOsaParse(ScanRequest request, File file, File libs) throws ExitThrowable {
        osaScannerService.cxOsaParseResults(request, file, libs);
    }

    private void cxParse(ScanRequest request, File file) throws ExitThrowable {
        runOnActiveScanners(scanner -> scanner.scanCli(request, "cxParse", file));
    }

    private void cxBatch(ScanRequest request) throws ExitThrowable {
        runOnActiveScanners(scanner -> scanner.scanCli(request, "cxBatch"));
    }

    private void publishLatestScanResults(ScanRequest request) throws ExitThrowable {
        ScanResults scanResults = runOnActiveScanners(scanner -> scanner.getLatestScanResults(request));
        processResults(request, scanResults);
    }

    private void processResults(ScanRequest request, ScanResults results) throws ExitThrowable {
        try {
            resultsService.processResults(request, results, null);
            if (checkIfBreakBuild(request, results)) {
                log.error(ERROR_BREAK_MSG);
                exit(ExitCode.BUILD_INTERRUPTED);
            }

        } catch (MachinaException e) {
            log.error("An error has occurred.", ExceptionUtils.getRootCause(e));
        }
    }

    private boolean checkIfBreakBuild(ScanRequest request, ScanResults results){
        boolean breakBuildResult = false;

        if(thresholdValidator.isThresholdsConfigurationExist(request)){
            if(thresholdValidator.thresholdsExceeded(request, results)){
                log.info("Fail build because some of the checks weren't passed");
                breakBuildResult = true;
            }
        } else if(flowProperties.isBreakBuild() && resultsService.filteredSastIssuesPresent(results)){
            log.info("Build failed because some issues were found");
            breakBuildResult = true;
        }
        else {
            log.info("Build succeeded. all checks passed");
        }

        return breakBuildResult;
    }

    private ScanResults runOnActiveScanners(Function<? super VulnerabilityScanner, ScanResults> action) throws ExitThrowable {
        try {
            ScanResults[] scanResultslist = scanners.stream()
                    .filter(VulnerabilityScanner::isEnabled)
                    .map(action)
                    .toArray(ScanResults[]::new);
            return resultsService.joinResults(scanResultslist);
        } catch (MachinaRuntimeException e) {
            throw (ExitThrowable) (e.getCause());
        }
    }
}
