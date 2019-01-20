package com.custodela.machina.utils;

import com.custodela.machina.config.JiraProperties;
import com.custodela.machina.config.MachinaProperties;
import com.custodela.machina.dto.*;
import com.custodela.machina.exception.MachinaRuntimeException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ScanUtils {

    public static final String RUNNING = "running";
    public static final String CRLF = "\r\n";
    private static final String ISSUE_BODY = "**%s** issue exists @ **%s** in branch **%s**";
    private static final String ISSUE_KEY = "%s %s @ %s [%s]";
    public static final String JIRA_ISSUE_KEY = "%s%s @ %s [%s]";
    public static final String JIRA_ISSUE_KEY_2 = "%s%s @ %s";
    public static final String JIRA_ISSUE_BODY = "*%s* issue exists @ *%s* in branch *%s*";
    public static final String JIRA_ISSUE_BODY_2 = "*%s* issue exists @ *%s*";

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ScanUtils.class);


    /**
     * Function used to determine if file extension of full filename is preset in list
     *
     * @param list
     * @param value - extension of file, or full filename
     * @return
     */
    public static boolean fileListContains(List<String> list, String value){
        for(String s: list){
            if(s.endsWith(value)){
                return true;
            }
        }
        return false;
    }

    /**
     * Create List of filters based on String lists of severity, cwe, category
     * @param severity
     * @param cwe
     * @param category
     * @return
     */
    public static List<Filter> getFilters(List<String> severity, List<String> cwe, List<String> category, List<String> status) {
        List<Filter> severityList = new ArrayList<>();
        List<Filter> categoryList = new ArrayList<>();
        List<Filter> cweList = new ArrayList<>();
        List<Filter> statusList = new ArrayList<>();
        List<Filter> filters = new ArrayList<>();
        if(severity != null) {
            for (String s : severity) {
                severityList.add(Filter.builder()
                        .type(Filter.Type.SEVERITY)
                        .value(s)
                        .build());
            }
        }
        if(cwe != null) {
            for (String c : cwe) {
                cweList.add(Filter.builder()
                        .type(Filter.Type.CWE)
                        .value(c)
                        .build());
            }
        }
        if(category != null) {
            for (String c : category) {
                categoryList.add(Filter.builder()
                        .type(Filter.Type.TYPE)
                        .value(c)
                        .build());
            }
        }
        if(status != null) {
            for (String s : status) {
                statusList.add(Filter.builder()
                        .type(Filter.Type.STATUS)
                        .value(s)
                        .build());
            }
        }
        filters.addAll(severityList);
        filters.addAll(cweList);
        filters.addAll(categoryList);
        filters.addAll(statusList);
        return filters;
    }

    /**
     * Check if string is empty or null
     * @param str
     * @return
     */
    public static boolean empty(String str) {
        if (str == null) {
            return true;
        } else if (str.isEmpty()) {
            return true;
        }
        return false;
    }

    /**
     * Check if list is empty or null
     * @param list
     * @return
     */
    public static boolean empty(List list) {
        if (list == null) {
            return true;
        } else return list.isEmpty();
    }

    public static boolean emptyObj(Object object) {
        if (object == null) {
            return true;
        } else if (object instanceof List) {
            return ScanUtils.empty((List)object);
        }
        else if (object instanceof String) {
            return ScanUtils.empty((String)object);
        }
        return false;
    }

    /**
     * Override scan request details as per file/blob (MachinaOverride)
     *
     * @param request
     * @param override
     * @return
     */
    public static ScanRequest overrideMap(ScanRequest request, MachinaOverride override){
        if(override == null){
            return request;
        }
        BugTracker bt = request.getBugTracker();
        /*Override only applicable to Simple JIRA bug*/
        if(request.getBugTracker().getType().equals(BugTracker.Type.JIRA)) {
            if(override.getJira()!=null) {
                MachinaOverride.Jira jira = override.getJira();
                if(!ScanUtils.empty(jira.getAssignee())) {
                    bt.setAssignee(jira.getAssignee());
                }//if empty value override with null
                if(jira.getAssignee() != null && jira.getAssignee().isEmpty()) {
                    bt.setAssignee(null);
                }
                if(!ScanUtils.empty(jira.getProject())) {
                    bt.setProjectKey(jira.getProject());
                }
                if(!ScanUtils.empty(jira.getIssueType())) {
                    bt.setIssueType(jira.getIssueType());
                }
                if(!ScanUtils.empty(jira.getOpenedStatus())) {
                    bt.setOpenStatus(jira.getOpenedStatus());
                }
                if(!ScanUtils.empty(jira.getClosedStatus())) {
                    bt.setClosedStatus(jira.getClosedStatus());
                }
                if(!ScanUtils.empty(jira.getOpenTransition())) {
                    bt.setOpenTransition(jira.getOpenTransition());
                }
                if(!ScanUtils.empty(jira.getCloseTransition())) {
                    bt.setCloseTransition(jira.getCloseTransition());
                }
                if(!ScanUtils.empty(jira.getCloseTransitionField())) {
                    bt.setCloseTransitionField(jira.getCloseTransitionField());
                }
                if(!ScanUtils.empty(jira.getCloseTransitionValue())) {
                    bt.setCloseTransitionValue(jira.getCloseTransitionValue());
                }
                if(jira.getFields()!=null) { //if empty, assume no fields
                    bt.setFields(jira.getFields());
                }
                if(jira.getPriorities() != null && !jira.getPriorities().isEmpty()) {
                    bt.setPriorities(jira.getPriorities());
                }
            }
        }
        request.setBugTracker(bt);

        if(!ScanUtils.empty(override.getApplication())) {
            request.setApplication(override.getApplication());
        }

        if(!ScanUtils.empty(override.getBranches())) {
            request.setActiveBranches(override.getBranches());
        }

        if(override.getIncremental() != null) {
            request.setIncremental(override.getIncremental());
        }

        if(!ScanUtils.empty(override.getScanPreset())) {
            request.setScanPreset(override.getScanPreset());
        }
        if(override.getExcludeFolders() != null) {
            request.setExcludeFolders(Arrays.asList(override.getExcludeFolders().split(",")));
        }
        if(override.getExcludeFiles() != null) {
            request.setExcludeFiles(Arrays.asList(override.getExcludeFiles().split(",")));
        }

        if(override.getEmails() != null) {
            if (override.getEmails().isEmpty()) {
                request.setEmail(null);
            } else {
                request.setEmail(override.getEmails());
            }
        }

        if(override.getFilters() != null && (!ScanUtils.empty(override.getFilters().getSeverity()) || !ScanUtils.empty(override.getFilters().getCwe()) ||
                !ScanUtils.empty(override.getFilters().getCategory()) || !ScanUtils.empty(override.getFilters().getStatus()))) {
            List<Filter> filters = ScanUtils.getFilters(override.getFilters().getSeverity(), override.getFilters().getCwe(),
                    override.getFilters().getCategory(), override.getFilters().getStatus());
            request.setFilters(filters);
        }
        else if (override.getFilters() != null){
            request.setFilters(null);
        }

        return request;
    }

    /**
     * Build BugTracker object (referenced within ScanRequest)
     * @param assignee
     * @param bugType
     * @param jiraProperties
     * @return
     */
    public static BugTracker getBugTracker(@RequestParam(value = "assignee", required = false) String assignee, BugTracker.Type bugType, JiraProperties jiraProperties) {
        BugTracker bt;
        if(bugType.equals(BugTracker.Type.JIRA)) {
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
        }
        else {
            bt = BugTracker.builder()
                    .type(bugType)
                    .build();
        }
        return bt;
    }


    public static void zipDirectory(String sourceDirectoryPath, String zipPath) throws IOException {
        Path zipFilePath = Files.createFile(Paths.get(zipPath));

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(zipFilePath))) {
            Path sourceDirPath = Paths.get(sourceDirectoryPath);

            Files.walk(sourceDirPath).filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(sourceDirPath.relativize(path).toString());
                        try {
                            zipOutputStream.putNextEntry(zipEntry);
                            zipOutputStream.write(Files.readAllBytes(path));
                            zipOutputStream.closeEntry();
                        } catch (Exception e) {
                            System.err.println(e);
                        }
                    });
        }
    }

    public static String getMDBody(ScanResults.XIssue issue, String branch, String fileUrl, MachinaProperties machinaProperties) {
        StringBuilder body = new StringBuilder();
        body.append(String.format(ISSUE_BODY, issue.getVulnerability(), issue.getFilename(), branch)).append(CRLF).append(CRLF);
        if(!ScanUtils.empty(issue.getDescription())) {
            body.append("*").append(issue.getDescription().trim()).append("*").append(CRLF).append(CRLF);
        }
        if(!ScanUtils.empty(issue.getSeverity())) {
            body.append("Severity: ").append(issue.getSeverity()).append(CRLF);
        }
        if(!ScanUtils.empty(issue.getCwe())) {
            body.append("CWE:").append(issue.getCwe()).append(CRLF);
            if(!empty(machinaProperties.getMitreUrl())) {
                body.append("[Vulnerability details and guidance](").append(String.format(machinaProperties.getMitreUrl(), issue.getCwe())).append(")").append(CRLF);
            }
        }
        if(!ScanUtils.empty(machinaProperties.getWikiUrl())) {
            body.append("[Internal Guidance](").append(machinaProperties.getWikiUrl()).append(")").append(CRLF);
        }
        if(issue.getDetails() != null && !issue.getDetails().isEmpty()) {
            body.append("Lines: ");
            for (Map.Entry<Integer, String> entry : issue.getDetails().entrySet()) {
                if (entry.getKey() != null) {  //[<line>](<url>)
                    body.append("[").append(entry.getKey()).append("](").append(fileUrl).append("#L").append(entry.getKey()).append(") ");
                }
            }
            body.append(CRLF).append(CRLF);

            for (Map.Entry<Integer, String> entry : issue.getDetails().entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    body.append("---").append(CRLF);
                    body.append("[Code (Line #").append(entry.getKey()).append("):](").append(fileUrl).append("#L").append(entry.getKey()).append(")").append(CRLF);
                    body.append("```").append(CRLF);
                    body.append(entry.getValue()).append(CRLF);
                    body.append("```").append(CRLF);
                }
            }
            body.append("---").append(CRLF);
        }
        if(issue.getOsaDetails()!=null){
            for(ScanResults.OsaDetails o: issue.getOsaDetails()){
                body.append(CRLF);
                if(!ScanUtils.empty(o.getCve())) {
                    body.append("*").append(o.getCve()).append("*").append(CRLF);
                }
                body.append("```");
                if(!ScanUtils.empty(o.getSeverity())) {
                    body.append("Severity: ").append(o.getSeverity()).append(CRLF);
                }
                if(!ScanUtils.empty(o.getVersion())) {
                    body.append("Version: ").append(o.getVersion()).append(CRLF);
                }
                if(!ScanUtils.empty(o.getDescription())) {
                    body.append("Description: ").append(o.getDescription()).append(CRLF);
                }
                if(!ScanUtils.empty(o.getRecommendation())){
                    body.append("Recommendation: ").append(o.getRecommendation()).append(CRLF);
                }
                if(!ScanUtils.empty(o.getUrl())) {
                    body.append("URL: ").append(o.getUrl());
                }
                body.append("```");
                body.append(CRLF);
            }
        }
        return body.toString();
    }

    public static String getMergeCommentMD(ScanRequest request, ScanResults results, MachinaProperties machinaProperties) {
        StringBuilder body = new StringBuilder();
        body.append("#### Checkmarx scan completed with the following findings").append(CRLF);
        body.append("|Lines|Severity|Category|File|Link|").append(CRLF);
        body.append("---|---|---|---|---").append(CRLF);

        Map<String, ScanResults.XIssue> xMap;
        xMap = getXIssueMap(results.getXIssues(), request);
        log.info("Creating Merge/Pull Request Markdown comment");

        for (Map.Entry<String, ScanResults.XIssue> xIssue : xMap.entrySet()) {
            try {
                ScanResults.XIssue currentIssue = xIssue.getValue();
                String fileUrl = ScanUtils.getFileUrl(request, currentIssue.getFilename());
                for (Map.Entry<Integer, String> entry : currentIssue.getDetails().entrySet()) {
                    if (entry.getKey() != null) {  //[<line>](<url>)
                        if(request.getRepoType().equals(ScanRequest.Repository.BITBUCKET)){
                            body.append("[").append(entry.getKey()).append("](").append(fileUrl).append("#lines-").append(entry.getKey()).append(") ");
                        }
                        else{
                            body.append("[").append(entry.getKey()).append("](").append(fileUrl).append("#L").append(entry.getKey()).append(") ");
                        }
                    }
                }
                body.append("|");
                body.append(currentIssue.getSeverity()).append("|");
                body.append(currentIssue.getVulnerability()).append("|");
                body.append(currentIssue.getFilename()).append("|");
                body.append("[link](").append(currentIssue.getLink()).append(")");
                body.append(CRLF);
            //body.append("```").append(currentIssue.getDescription()).append("```").append(CRLF); Description is too long
            } catch (HttpClientErrorException e) {
                log.error("Error occurred while processing issue with key {} {}", xIssue.getKey(), e);
            }
        }

        return body.toString();
    }

    public static String getFileUrl(ScanRequest request, String filename) {
        if(request.getProduct().equals(ScanRequest.Product.CXOSA)){
            return null;
        }
        if(!ScanUtils.empty(request.getRepoUrl()) && !ScanUtils.empty(request.getBranch())) {
            String repoUrl = request.getRepoUrl().replace(".git", "/");
            if (request.getRepoType().equals(ScanRequest.Repository.BITBUCKET)) {
                return repoUrl.concat("src/").concat(request.getBranch()).concat("/").concat(filename);
            } else {
                return repoUrl.concat("/blob/").concat(request.getBranch()).concat("/").concat(filename);
            }
        }
        return null;
    }

    public static Map<String, ScanResults.XIssue> getXIssueMap(List<ScanResults.XIssue> issues, ScanRequest request) {
        Map<String, ScanResults.XIssue> xMap = new HashMap<>();
        for (ScanResults.XIssue issue : issues) {
            String key = String.format(ISSUE_KEY, request.getProduct().getProduct(), issue.getVulnerability(), issue.getFilename(), request.getBranch());
            xMap.put(key, issue);
        }
        return xMap;
    }

    /**
     * TODO  BB has a different format
     *
     * @param request
     * @param filename
     * @return
     */
    public static String getBBFileUrl(ScanRequest request, String filename) {
        String repoUrl = request.getRepoUrl().replace(".git", "/");
        return repoUrl.concat("/blob/").concat(request.getBranch()).concat("/").concat(filename);
    }

    public static MachinaOverride getMachinaOverride(@RequestParam(value = "override", required = false) String override) {
        MachinaOverride o = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            //if override is provided, check if chars are more than 20 in length, implying base64 encoded json
            if(!ScanUtils.empty(override)){
                if(override.length() > 20){
                    String oJson = new String(Base64.getDecoder().decode(override));
                    o = mapper.readValue(oJson, MachinaOverride.class);
                    log.info("Overriding attributes with Base64 encoded String");
                }
                else{
                    //TODO download file
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new MachinaRuntimeException();
        }
        return o;
    }

    /**
     * Creates a map of GitLab Issues
     *
     * @param issues
     * @return
     */
    public static Map<String, ? extends RepoIssue> getRepoIssueMap(List<? extends RepoIssue> issues, String prefix) {
        Map<String, RepoIssue> map = new HashMap<>();
        for (RepoIssue issue : issues) {
            if (issue.getTitle().startsWith(prefix)) {
                map.put(issue.getTitle(), issue);
            }
        }
        return map;
    }


}
