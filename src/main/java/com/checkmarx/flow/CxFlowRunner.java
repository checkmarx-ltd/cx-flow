package com.checkmarx.flow;

import com.checkmarx.flow.config.*;
import com.checkmarx.flow.dto.*;
import com.checkmarx.flow.service.FlowService;
import com.checkmarx.flow.utils.ScanUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import java.beans.ConstructorProperties;
import java.io.File;
import java.io.IOException;
import java.util.List;
import static java.lang.System.exit;

@Component
public class CxFlowRunner implements ApplicationRunner {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(CxFlowRunner.class);
    private final FlowProperties flowProperties;
    private final CxProperties cxProperties;
    private final JiraProperties jiraProperties;
    private final GitHubProperties gitHubProperties;
    private final GitLabProperties gitLabProperties;
    private final FlowService flowService;

    @ConstructorProperties({"flowProperties", "cxProperties", "jiraProperties", "gitHubProperties",
            "gitLabProperties", "flowService"})
    public CxFlowRunner(FlowProperties flowProperties,
                        CxProperties cxProperties, JiraProperties jiraProperties,
                        GitHubProperties gitHubProperties, GitLabProperties gitLabProperties,
                        FlowService flowService) {
        this.flowProperties = flowProperties;
        this.cxProperties = cxProperties;
        this.jiraProperties = jiraProperties;
        this.gitHubProperties = gitHubProperties;
        this.gitLabProperties = gitLabProperties;
        this.flowService = flowService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if(args.containsOption("web") || args.getOptionNames().isEmpty()){
            log.debug("running web mode");
        }
        else{
            log.debug("Running cmd mode");
            commandLineRunner(args);
        }
    }

    private void commandLineRunner(ApplicationArguments args) {
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
        String preset = null;
        String team;
        String cxProject;
        String config;
        List<String> severity;
        List<String> cwe;
        List<String> category;
        List<String> status;
        List<String> excludeFiles;
        List<String> excludeFolders;
        ScanRequest.Repository repoType = ScanRequest.Repository.NA;
        boolean osa;
        MachinaOverride o = null;
        ObjectMapper mapper = new ObjectMapper();

        if(args.containsOption("branch-create")){

            exit(0);
        }
        if(args.containsOption("branch-delete")){

            exit(0);
        }
        if(!args.containsOption("scan") && !args.containsOption("parse") && !args.containsOption("batch") && !args.containsOption("project")){
            log.error("--scan | --parse | --batch | --project option must be specified");
            exit(1);
        }

        //override with config
        if (args.containsOption("config")) {
            config = args.getOptionValues("config").get(0);
            try {
                o = mapper.readValue(new File(config), MachinaOverride.class);
            } catch (IOException e) {
                log.error("Error reading config file, ignoring...");
                log.error(ExceptionUtils.getStackTrace(e));
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
                ScanUtils.empty(application)) && !args.containsOption("batch")) {
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

        //set the default bug tracker as per yml
        BugTracker.Type bugType = null;
        if (ScanUtils.empty(bugTracker)) {
            bugTracker =  flowProperties.getBugTracker();
        }
        try {
            bugType = ScanUtils.getBugTypeEnum(bugTracker, flowProperties.getBugTrackerImpl());
        }catch (IllegalArgumentException e){
            log.error("No valid bug tracker was provided");
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
        String gitUrlAuth = null;
        switch (bugType){
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
                .repoUrlWithAuth(gitUrlAuth)
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
                .build();

        request = ScanUtils.overrideMap(request, o);
        /*Determine if BitBucket Cloud/Server is being used - this will determine formatting of URL that links to file/line in repository */
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
            if(args.containsOption("parse")){

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
            else if(args.containsOption("batch")){
                log.info("Executing batch process");
                cxBatch(request);
            }
            else if(args.containsOption("project")){
                if(ScanUtils.empty(team) || ScanUtils.empty(cxProject)){
                    log.error("team and cx-project must be provided when --project option is used");
                    exit(2);
                }
                cxResults(request);
            }
            else if(args.containsOption("scan")){
                log.info("Executing scan process");
                cxScan(request, file);
            }
        }catch (Exception e){
            log.error("An error occurred while processing request");
            log.error(ExceptionUtils.getStackTrace(e));
            exit(10);
        }
        log.info("Completed Successfully");
        exit(0);
    }

    private String getOptionValues(ApplicationArguments arg, String option){
        if(arg != null && option != null) {
            List<String> values = arg.getOptionValues(option);
            return ScanUtils.empty(values) ? null : values.get(0);
        } else {
            return  null;
        }
    }

    private void cxScan(ScanRequest request, String path){
        flowService.cxFullScan(request, path);
    }
    private void cxOsaParse(ScanRequest request, File file, File libs){
        flowService.cxOsaParseResults(request, file, libs);
    }
    private void cxParse(ScanRequest request, File file){
        flowService.cxParseResults(request, file);
    }
    private void cxBatch(ScanRequest request){
        flowService.cxBatch(request);
    }
    private void cxResults(ScanRequest request){
        ScanResults results = flowService.cxGetResults(request, null).join();
        if(flowProperties.isBreakBuild() && results !=null && results.getXIssues()!=null && !results.getXIssues().isEmpty()){
            log.error("Exiting with Error code 10 due to issues present");
            exit(10);
        }
    }
}