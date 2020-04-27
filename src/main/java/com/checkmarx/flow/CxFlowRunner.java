package com.checkmarx.flow;

import com.checkmarx.flow.config.*;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ExitCode;
import com.checkmarx.flow.dto.FlowOverride;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.ExitThrowable;
import com.checkmarx.flow.service.FlowService;
import com.checkmarx.flow.service.HelperService;
import com.checkmarx.flow.service.ResultsService;
import com.checkmarx.flow.service.SastScannerService;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.dto.Filter;
import com.checkmarx.sdk.dto.ScanResults;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

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
    private final CxProperties cxProperties;
    private final JiraProperties jiraProperties;
    private final GitHubProperties gitHubProperties;
    private final GitLabProperties gitLabProperties;
    private final ADOProperties adoProperties;
    private final HelperService helperService;
    private final FlowService flowService;
    private final SastScannerService sastScannerService;
    private final List<ThreadPoolTaskExecutor> executors;
    private final ResultsService resultsService;

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
        FlowOverride o = null;
        ObjectMapper mapper = new ObjectMapper();
        String uid = helperService.getShortUid();
        MDC.put("cx", uid);

        if(args.containsOption("branch-create")){
            exit(ExitCode.SUCCESS);
        }
        if(args.containsOption("branch-delete")){
            exit(ExitCode.SUCCESS);
        }

        if(!args.containsOption("scan") && !args.containsOption(PARSE_OPTION) && !args.containsOption(BATCH_OPTION) && !args.containsOption("project")){
            log.error("--scan | --parse | --batch | --project option must be specified");
            exit(1);
        }

        //override with config
        if (args.containsOption("config")) {
            config = args.getOptionValues("config").get(0);
            try {
                o = mapper.readValue(new File(config), FlowOverride.class);
            } catch (IOException e) {
                log.error("Error reading config file, ignoring...", e);
            }
        }

        /*Collect command line options (String)*/
        bugTracker = getOptionValues(args, "bug-tracker");
        file = getOptionValues(args,"f");
        libFile = getOptionValues(args,"lib-file");
        repoName = getOptionValues(args,"repo-name");
        repoUrl = getOptionValues(args,"repo-url");
        branch = getOptionValues(args,"branch");
        namespace = getOptionValues(args,"namespace");
        team = getOptionValues(args,"cx-team");
		altProject = getOptionValues(args,"alt-project");
        altFields = getOptionValues(args,"alt-fields");
        cxProject = getOptionValues(args,"cx-project");
        application = getOptionValues(args,"app");
        assignee = getOptionValues(args,"assignee");
        mergeId = getOptionValues(args,"merge-id");
        preset = getOptionValues(args,"preset");
        osa = args.getOptionValues("osa") != null;
        /*Collect command line options (List of Strings)*/
        emails = args.getOptionValues("emails");
        severity = args.getOptionValues("severity");
        category = args.getOptionValues("category");
        cwe = args.getOptionValues("cwe");
        status = args.getOptionValues("status");
        excludeFiles = args.getOptionValues("exclude-files");
        excludeFolders = args.getOptionValues("exclude-folders");
        boolean bb = args.containsOption("bb"); //BitBucket Cloud
        boolean bbs = args.containsOption("bbs"); //BitBucket Server

        if(((ScanUtils.empty(namespace) && ScanUtils.empty(repoName) && ScanUtils.empty(branch)) &&
                ScanUtils.empty(application)) && !args.containsOption(BATCH_OPTION)) {
            log.error("Namespace/Repo/Branch or Application (app) must be provided");
            exit(1);
        }

        /*Determine filters, if any*/
        List<Filter> filters;
        if(!ScanUtils.empty(severity) || !ScanUtils.empty(cwe) || !ScanUtils.empty(category) || !ScanUtils.empty(status) ){
            filters = ScanUtils.getFilters(severity, cwe, category, status);
        }
        else{
            filters = ScanUtils.getFilters(flowProperties);
        }
        //Adding default file/folder exclusions from properties if they are not provided as an override
        if(excludeFiles == null && !ScanUtils.empty(cxProperties.getExcludeFiles())){
            excludeFiles = Arrays.asList(cxProperties.getExcludeFiles().split(","));
        }
        if(excludeFolders == null && !ScanUtils.empty(cxProperties.getExcludeFolders())){
            excludeFolders = Arrays.asList(cxProperties.getExcludeFolders().split(","));
        }
        //set the default bug tracker as per yml
        BugTracker.Type bugType = null;
        if (ScanUtils.empty(bugTracker)) {
            bugTracker =  flowProperties.getBugTracker();
        }
        try {
            bugType = ScanUtils.getBugTypeEnum(bugTracker, flowProperties.getBugTrackerImpl());
        }catch (IllegalArgumentException e){
            log.error("No valid bug tracker was provided", e);
            exit(1);
        }
        ScanRequest.Product p;
        if(osa){
            if(libFile == null){
                log.error("Both vulnerabilities file (f) and libraries file (lib-file) must be provided for OSA");
                exit(1);
            }
            p = ScanRequest.Product.CXOSA;
        } else {
            p = ScanRequest.Product.CX;
        }

        if(ScanUtils.empty(preset)){
            preset = cxProperties.getScanPreset();
        }

        BugTracker bt = null;
        String gitAuthUrl = null;
        switch (bugType){
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
                bt = BugTracker.builder()
                        .type(bugType)
                        .projectKey(jiraProperties.getProject())
                        .issueType(jiraProperties.getIssueType())
                        .assignee(assignee)
                        .priorities(jiraProperties.getPriorities())
                        .closeTransitionField(jiraProperties.getCloseTransitionField())
                        .closeTransitionValue(jiraProperties.getCloseTransitionValue())
                        .closedStatus(jiraProperties.getClosedStatus())
                        .closeTransition(jiraProperties.getCloseTransition())
                        .openStatus(jiraProperties.getOpenStatus())
                        .openTransition(jiraProperties.getOpenTransition())
                        .fields(jiraProperties.getFields())
                        .build();
                break;
            case ADOPULL:
            case adopull:
                bugType = BugTracker.Type.ADOPULL;
                bt = BugTracker.builder()
                        .type(bugType)
                        .build();
                repoType = ScanRequest.Repository.ADO;

                if(ScanUtils.empty(namespace) ||ScanUtils.empty(repoName)||ScanUtils.empty(mergeId)){
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

                if(ScanUtils.empty(namespace) ||ScanUtils.empty(repoName)||ScanUtils.empty(mergeId)){
                    log.error("Namespace/Repo/MergeId must be provided for GITHUBPULL bug tracking");
                    exit(1);
                }
                mergeNoteUri = gitHubProperties.getMergeNoteUri(namespace, repoName, mergeId);
                break;
            case GITLABMERGE:
            case gitlabmerge:
                log.info("GitLab Merge not currently supported from command line");
                exit(1);
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
                .product(p)
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
                .filters(filters)
				.altProject(altProject)
                .altFields(altFields)
                .build();

        request = ScanUtils.overrideMap(request, o);
        /*Determine if BitBucket Cloud/Server is being used - this will determine formatting of URL that links to file/line in repository */
        request.setId(uid);
        if(bb){
            request.setRepoType(ScanRequest.Repository.BITBUCKETSERVER);
            //TODO create browse code url
        }
        else if(bbs){
            request.setRepoType(ScanRequest.Repository.BITBUCKETSERVER);
            if(repoUrl != null) {
                repoUrl = repoUrl.replaceAll("\\/scm\\/", "/projects/");
                repoUrl = repoUrl.replaceAll("\\/[\\w-]+.git$", "/repos$0");
                repoUrl = repoUrl.replaceAll(".git$", "");
                repoUrl = repoUrl.concat("/browse");
            }
            request.putAdditionalMetadata("BITBUCKET_BROWSE", repoUrl);
        }

        try {
            if(args.containsOption(PARSE_OPTION)){

                File f = new File(file);
                if(!f.exists()){
                    log.error("Result File not found {}", file);
                    exit(2);
                }
                if(osa){ //grab the libs file if OSA results
                    File libs = new File(libFile);
                    if(!libs.exists()){
                        log.error("Library File not found {}", file);
                        exit(2);
                    }
                    cxOsaParse(request, f, libs);
                } else { //SAST
                    if(args.containsOption("offline")){
                        cxProperties.setOffline(true);
                    }
                    log.info("Processing Checkmarx result file {}", file);

                    cxParse(request, f);
                }
            }
            else if(args.containsOption(BATCH_OPTION)){
                log.info("Executing batch process");
                cxBatch(request);
            }
            else if(args.containsOption("project")){
                if(ScanUtils.empty(cxProject)){
                    log.error("cx-project must be provided when --project option is used");
                    exit(2);
                }
                cxResults(request);
            }
            else if(args.containsOption("scan")){
                log.info("Executing scan process");
                //GitHub Scan with Git Clone
                if(args.containsOption("github")){
                    if(ScanUtils.empty(repoUrl) && !ScanUtils.anyEmpty(namespace, repoName)) {
                        repoUrl = gitHubProperties.getGitUri(namespace, repoName);
                    } else if(ScanUtils.empty(repoUrl)){
                        log.error("Unable to determine git url for scanning, exiting...");
                        exit(2);
                    }
                    String token = gitHubProperties.getToken();
                    gitAuthUrl = repoUrl.replace(Constants.HTTPS, Constants.HTTPS.concat(token).concat("@"));
                    gitAuthUrl = gitAuthUrl.replace(Constants.HTTP, Constants.HTTP.concat(token).concat("@"));

                    cxScan(request, repoUrl, gitAuthUrl, branch, ScanRequest.Repository.GITHUB);
                } //GitLab Scan with Git Clone
                else if(args.containsOption("gitlab") &&  !ScanUtils.anyEmpty(namespace, repoName)){
                    if(ScanUtils.empty(repoUrl) && !ScanUtils.anyEmpty(namespace, repoName)) {
                        repoUrl = gitLabProperties.getGitUri(namespace, repoName);
                    } else if(ScanUtils.empty(repoUrl)){
                        log.error("Unable to determine git url for scanning, exiting...");
                        exit(2);
                    }
                    String token = gitLabProperties.getToken();
                    gitAuthUrl = repoUrl.replace(Constants.HTTPS, Constants.HTTPS_OAUTH2.concat(token).concat("@"));
                    gitAuthUrl = gitAuthUrl.replace(Constants.HTTP, Constants.HTTP_OAUTH2.concat(token).concat("@"));
                    cxScan(request, repoUrl, gitAuthUrl, branch, ScanRequest.Repository.GITLAB);
                }
                else if(args.containsOption("bitbucket") && containsRepoArgs(namespace, repoName, branch)){
                    log.warn("Bitbucket git clone scan not implemented");
                }
                else if(args.containsOption("ado") && containsRepoArgs(namespace, repoName, branch)){
                    log.warn("Azure DevOps git clone scan not implemented");
                }
                else if(file != null) {
                        cxScan(request, file);
                    }
                else{
                        log.error("No valid option was provided for driving scan");
                }
            }
        }catch (Exception e){
            log.error("An error occurred while processing request", e);
            exit(10);
        }
        log.info("Completed Successfully");
        exit(ExitCode.SUCCESS);
    }

    private boolean containsRepoArgs(String namespace, String repoName, String branch){
        return (!ScanUtils.empty(namespace) &&
                !ScanUtils.empty(repoName) &&
                !ScanUtils.empty(branch));
    }

    private String getOptionValues(ApplicationArguments arg, String option){
        if(arg != null && option != null) {
            List<String> values = arg.getOptionValues(option);
            return ScanUtils.empty(values) ? null : values.get(0);
        } else {
            return  null;
        }
    }

    private void cxScan(ScanRequest request, String gitUrl, String gitAuthUrl, String branch, ScanRequest.Repository repoType) throws ExitThrowable {
        log.info("Initiating scan using Checkmarx git clone");
        request.setRepoType(repoType);
        log.info("Git url: {}", gitUrl);
        request.setBranch(branch);
        request.setRepoUrl(gitUrl);
        request.setRepoUrlWithAuth(gitAuthUrl);
        request.setRefs(Constants.CX_BRANCH_PREFIX.concat(branch));
        sastScannerService.cxFullScan(request);
    }
    private void cxScan(ScanRequest request, String path) throws ExitThrowable {
        if(ScanUtils.empty(request.getProject())){
            log.error("Please provide --cx-project to define the project in Checkmarx");
            exit(2);
        }
        sastScannerService.cxFullScan(request, path);
    }
    private void cxOsaParse(ScanRequest request, File file, File libs) throws ExitThrowable {
        flowService.cxOsaParseResults(request, file, libs);
    }
    private void cxParse(ScanRequest request, File file) throws ExitThrowable {
        sastScannerService.cxParseResults(request, file);
    }
    private void cxBatch(ScanRequest request) throws ExitThrowable {
        flowService.cxBatch(request);
    }
    private void cxResults(ScanRequest request) throws ExitThrowable {
        ScanResults results = resultsService.cxGetResults(request, null).join();
        if(flowProperties.isBreakBuild() && results !=null && results.getXIssues()!=null && !results.getXIssues().isEmpty()){
            log.error("Exiting with Error code 10 due to issues present");
            exit(10);
        }
    }
}
