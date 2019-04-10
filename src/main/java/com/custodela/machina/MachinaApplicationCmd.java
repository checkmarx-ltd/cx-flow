package com.custodela.machina;

import com.custodela.machina.config.*;
import com.custodela.machina.dto.BugTracker;
import com.custodela.machina.dto.Filter;
import com.custodela.machina.dto.MachinaOverride;
import com.custodela.machina.dto.ScanRequest;
import com.custodela.machina.service.MachinaService;
import com.custodela.machina.utils.ScanUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import java.beans.ConstructorProperties;
import java.io.File;
import java.io.IOException;
import java.util.List;
import static java.lang.System.exit;

@EnableAsync
//@SpringBootApplication
public class MachinaApplicationCmd implements ApplicationRunner {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(MachinaApplicationCmd.class);
    private final MachinaProperties machinaProperties;
    private final CxProperties cxProperties;
    private final JiraProperties jiraProperties;
    private final GitHubProperties gitHubProperties;
    private final GitLabProperties gitLabProperties;
    private final MachinaService machinaService;

    @ConstructorProperties({"machinaProperties", "cxProperties", "jiraProperties", "gitHubProperties", "gitLabProperties", "machinaService"})
    public MachinaApplicationCmd(MachinaProperties machinaProperties, CxProperties cxProperties, JiraProperties jiraProperties, GitHubProperties gitHubProperties, GitLabProperties gitLabProperties, MachinaService machinaService) {
        this.machinaProperties = machinaProperties;
        this.cxProperties = cxProperties;
        this.jiraProperties = jiraProperties;
        this.gitHubProperties = gitHubProperties;
        this.gitLabProperties = gitLabProperties;
        this.machinaService = machinaService;
    }

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(MachinaApplicationCmd.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }

    @Override
    public void run(ApplicationArguments arg) {
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

        if(arg.containsOption("branch-create")){

            exit(0);
        }
        if(arg.containsOption("branch-delete")){

            exit(0);
        }
        if(!arg.containsOption("scan") && !arg.containsOption("parse") && !arg.containsOption("batch") && !arg.containsOption("project")){
            log.error("--scan | --parse | --batch | --project option must be specified");
            exit(1);
        }

        //override with config
        if (arg.containsOption("config")) {
            config = arg.getOptionValues("config").get(0);
            try {
                o = mapper.readValue(new File(config), MachinaOverride.class);
            } catch (IOException e) {
                e.printStackTrace();
                log.error("Error reading config file, ignoring...");
            }
        }

        /*Collect command line options (String)*/
        bugTracker = ((ScanUtils.empty(arg.getOptionValues("bug-tracker")))) ? null : arg.getOptionValues("bug-tracker").get(0);
        file = ((ScanUtils.empty(arg.getOptionValues("f")))) ? null : arg.getOptionValues("f").get(0);
        libFile = ((ScanUtils.empty(arg.getOptionValues("lib-file")))) ? null : arg.getOptionValues("lib-file").get(0);
        repoName = ((ScanUtils.empty(arg.getOptionValues("repo-name")))) ? null : arg.getOptionValues("repo-name").get(0);
        repoUrl = ((ScanUtils.empty(arg.getOptionValues("repo-url")))) ? null : arg.getOptionValues("repo-url").get(0);
        branch = ((ScanUtils.empty(arg.getOptionValues("branch")))) ? null : arg.getOptionValues("branch").get(0);
        namespace = ((ScanUtils.empty(arg.getOptionValues("namespace")))) ? null : arg.getOptionValues("namespace").get(0);
        team = ((ScanUtils.empty(arg.getOptionValues("cx-team")))) ? null : arg.getOptionValues("cx-team").get(0);
        cxProject = ((ScanUtils.empty(arg.getOptionValues("cx-project")))) ? null : arg.getOptionValues("cx-project").get(0);
        application = ((ScanUtils.empty(arg.getOptionValues("app")))) ? null : arg.getOptionValues("app").get(0);
        assignee = ((ScanUtils.empty(arg.getOptionValues("assignee")))) ? null : arg.getOptionValues("assignee").get(0);
        mergeId = ((ScanUtils.empty(arg.getOptionValues("merge-id")))) ? null : arg.getOptionValues("merge-id").get(0);
        preset = ((ScanUtils.empty(arg.getOptionValues("preset")))) ? null : arg.getOptionValues("preset").get(0);
        osa = arg.getOptionValues("osa") != null;
        /*Collect command line options (List of Strings)*/
        emails = arg.getOptionValues("emails");
        severity = arg.getOptionValues("severity");
        category = arg.getOptionValues("category");
        cwe = arg.getOptionValues("cwe");
        status = arg.getOptionValues("status");
        excludeFiles = arg.getOptionValues("exclude-files");
        excludeFolders = arg.getOptionValues("exclude-folders");
        boolean bb = arg.containsOption("bb"); //BitBucket Cloud
        boolean bbs = arg.containsOption("bbs"); //BitBucket Server

        if(((ScanUtils.empty(namespace) && ScanUtils.empty(repoName) && ScanUtils.empty(branch)) &&
                ScanUtils.empty(application)) && !arg.containsOption("batch")) {
            log.error("Namespace/Repo/Branch or Application (app) must be provided");
            exit(1);
        }

        /*Determine filters, if any*/
        List<Filter> filters;
        if(!ScanUtils.empty(severity) || !ScanUtils.empty(cwe) || !ScanUtils.empty(category) || !ScanUtils.empty(status) ){
            filters = ScanUtils.getFilters(severity, cwe, category, status);
        }
        else{
            filters = ScanUtils.getFilters(machinaProperties.getFilterSeverity(), machinaProperties.getFilterCwe(),
                    machinaProperties.getFilterCategory(), machinaProperties.getFilterStatus());
        }

        //set the default bug tracker as per yml
        BugTracker.Type bugType = null;
        if (ScanUtils.empty(bugTracker)) {
            bugTracker =  machinaProperties.getBugTracker();
        }
        try {
            bugType = ScanUtils.getBugTypeEnum(bugTracker, machinaProperties.getBugTrackerImpl());
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
        }
        else {
            p = ScanRequest.Product.CX;
        }

        if(ScanUtils.empty(preset)){
            preset = cxProperties.getScanPreset();
        }

        BugTracker bt = null;
        String gitUrlAuth = null;
        switch (bugType){
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
            /*case GITLAB:
                gitUrlAuth = repoUrl.replace("https://", "https://oauth2:".concat(gitLabProperties.getToken()).concat("@"));
                bt = BugTracker.builder()
                        .type(bugType)
                        .build();
                break;
            case GITHUB:
                gitUrlAuth = repoUrl.replace("https://", "https://".concat(gitHubProperties.getToken()).concat("@"));
                bt = BugTracker.builder()
                        .type(bugType)
                        .build();
                break;
            case BITBUCKET:
                log.warn("Bitbucket is not supported at this time");
                break;*/
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
            repoUrl = repoUrl.replaceAll("\\/scm\\/", "/projects/");
            repoUrl = repoUrl.replaceAll("\\/[\\w-]+.git$", "/repos$0");
            repoUrl = repoUrl.replaceAll(".git$", "");
            repoUrl = repoUrl.concat("/browse");
            request.putAdditionalMetadata("BITBUCKET_BROWSE", repoUrl);
        }

        try {
            if(arg.containsOption("parse")){

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
                }
                else{ //SAST
                    if(arg.containsOption("offline")){
                        cxProperties.setOffline(true);
                    }
                    log.info("Processing Checkmarx result file {}", file);

                    cxParse(request, f);
                }

            }
            else if(arg.containsOption("batch")){
                log.info("Executing batch process");
                cxBatch(request);
            }
            else if(arg.containsOption("project")){
                if(ScanUtils.empty(team) || ScanUtils.empty(cxProject)){
                    log.error("team and cx-project must be provided when --project option is used");
                    exit(2);
                }
                cxResults(request);
            }
            else if(arg.containsOption("scan")){
                log.info("Executing scan process");
                cxScan(request, file);
            }
        }catch (Exception e){
            log.error("An error occurred while processing request");
            e.printStackTrace();
            exit(10);
        }
        log.info("Completed Successfully");
        exit(0);
    }

    public void cxScan(ScanRequest request, String path){
        machinaService.cxFullScan(request, path);
    }
    private void cxOsaParse(ScanRequest request, File file, File libs){
        machinaService.cxOsaParseResults(request, file, libs);
    }
    private void cxParse(ScanRequest request, File file){
        machinaService.cxParseResults(request, file);
    }
    private void cxBatch(ScanRequest request){
        machinaService.cxBatch(request);
    }
    private void cxResults(ScanRequest request){
        machinaService.cxGetResults(request, null);
    }
}