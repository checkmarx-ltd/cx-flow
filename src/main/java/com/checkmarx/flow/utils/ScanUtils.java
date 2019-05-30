package com.checkmarx.flow.utils;

import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.*;
import com.checkmarx.flow.exception.MachinaRuntimeException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ScanUtils {

    public static final String RUNNING = "running";
    public static final String CRLF = "\r\n";
    public static final String ISSUE_BODY = "**%s** issue exists @ **%s** in branch **%s**";
    public static final String ISSUE_KEY = "%s %s @ %s [%s]";
    public static final String ISSUE_KEY_2 = "%s %s @ %s";
    public static final String JIRA_ISSUE_KEY = "%s%s @ %s [%s]";
    public static final String JIRA_ISSUE_KEY_2 = "%s%s @ %s";
    public static final String JIRA_ISSUE_BODY = "*%s* issue exists @ *%s* in branch *%s*";
    public static final String JIRA_ISSUE_BODY_2 = "*%s* issue exists @ *%s*";

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ScanUtils.class);

    public ScanUtils() {
    }

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
        return str == null || str.isEmpty();
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
        if(request.getBugTracker().getType().equals(BugTracker.Type.JIRA) && override.getJira()!=null) {
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

        List<String> emails = override.getEmails();
        if(emails != null) {
            if (emails.isEmpty()) {
                request.setEmail(null);
            } else {
                request.setEmail(emails);
            }
        }
        MachinaOverride.Filters filtersObj = override.getFilters();

        if(filtersObj != null && (!ScanUtils.empty(filtersObj.getSeverity()) || !ScanUtils.empty(filtersObj.getCwe()) ||
                !ScanUtils.empty(filtersObj.getCategory()) || !ScanUtils.empty(filtersObj.getStatus()))) {
            List<Filter> filters = ScanUtils.getFilters(filtersObj.getSeverity(), filtersObj.getCwe(),
                    filtersObj.getCategory(), filtersObj.getStatus());
            request.setFilters(filters);
        }
        else if (filtersObj != null){
            request.setFilters(null);
        }

        return request;
    }

    public static BugTracker getBugTracker(@RequestParam(value = "assignee", required = false) String assignee,
                                           BugTracker.Type bugType, JiraProperties jiraProperties, String bugTracker) {
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
        else if(bugType.equals(BugTracker.Type.CUSTOM)){
            bt = BugTracker.builder()
                    .type(bugType)
                    .customBean(bugTracker)
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
                            log.error(ExceptionUtils.getStackTrace(e));
                        }
                    });
        }
    }

    public static String getMDBody(ScanResults.XIssue issue, String branch, String fileUrl, FlowProperties flowProperties) {
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
            if(!empty(flowProperties.getMitreUrl())) {
                body.append("[Vulnerability details and guidance](").append(String.format(flowProperties.getMitreUrl(), issue.getCwe())).append(")").append(CRLF);
            }
        }
        if(!ScanUtils.empty(flowProperties.getWikiUrl())) {
            body.append("[Internal Guidance](").append(flowProperties.getWikiUrl()).append(")").append(CRLF);
        }
        if(!ScanUtils.empty(issue.getLink())){
            body.append("[Checkmarx](").append(issue.getLink()).append(")").append(CRLF);
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

    public static String getMergeCommentMD(ScanRequest request, ScanResults results, FlowProperties flowProperties) {
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
                        else if(request.getRepoType().equals(ScanRequest.Repository.BITBUCKETSERVER)){
                            body.append("[").append(entry.getKey()).append("](").append(fileUrl).append("#").append(entry.getKey()).append(") ");
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
                body.append("[Checkmarx](").append(currentIssue.getLink()).append(")");
                body.append(CRLF);
            //body.append("```").append(currentIssue.getDescription()).append("```").append(CRLF); Description is too long
            } catch (HttpClientErrorException e) {
                log.error("Error occurred while processing issue with key {} {}", xIssue.getKey(), e);
            }
        }

        return body.toString();
    }

    public static String getFileUrl(ScanRequest request, String filename) {
        if(request.getProduct().equals(ScanRequest.Product.CXOSA) || filename == null || filename.isEmpty()){
            return null;
        }
        if(!ScanUtils.empty(request.getRepoUrl()) && !ScanUtils.empty(request.getBranch())) {
            String repoUrl = request.getRepoUrl().replace(".git", "/");
            if (request.getRepoType().equals(ScanRequest.Repository.BITBUCKETSERVER)) {
                String url = request.getAdditionalMetadata("BITBUCKET_BROWSE");
                if(url != null && !url.isEmpty()){
                    if(!ScanUtils.empty(request.getBranch())) {
                        return url.concat("/").concat(filename).concat("?at=").concat(request.getBranch());
                    }
                    else{
                        return url.concat("/").concat(filename);
                    }
                }
            }
            else if (request.getRepoType().equals(ScanRequest.Repository.BITBUCKET)) {
                return repoUrl.concat("src/").concat(request.getBranch()).concat("/").concat(filename);
            }
            else {
                if(!ScanUtils.empty(request.getBranch())) {
                    return repoUrl.concat("blob/").concat(request.getBranch()).concat("/").concat(filename);
                }
                else{ //default to master branch
                    return repoUrl.concat("blob/").concat("master/").concat(filename);
                }
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
            log.error(ExceptionUtils.getStackTrace(e));
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

    /**
     *  Parse cx custom field which is csv for custom field mapping in jira:
     *  type, name, jira-field-name, jira-field-value, jira-field-type (separated by ; for multiple)
     * @param cxFields
     * @return List of Fields
     */
    public static List<Field> getCustomFieldsFromCx(@NotNull String cxFields){
        List<Field> fields = new ArrayList<>();
        String[] entries = cxFields.split(";");
        for(String e: entries){
            String[] jira = e.split(",");
            if(jira.length >= 4) { //must be 4 or 5 values
                Field field = new Field();
                field.setType(jira[0].trim());
                field.setName(jira[1].trim());
                field.setJiraFieldName(jira[2].trim());
                field.setJiraFieldType(jira[3].trim());
                if(jira.length >= 5) {
                    field.setJiraDefaultValue(jira[4].trim());
                }
                fields.add(field);
            }
        }
        return fields;
    }

    public static BugTracker.Type getBugTypeEnum(String bug, List<String> bugTrackerImpl) throws IllegalArgumentException{

        BugTracker.Type bugType = EnumUtils.getEnum(BugTracker.Type.class, bug);
        if(bugType == null){ //Try uppercase
            bugType = EnumUtils.getEnum(BugTracker.Type.class, bug.toUpperCase(Locale.ROOT));
            if(bugType == null){
                log.debug("Determine if custom bean is being used");
                if(bugTrackerImpl == null){
                    log.debug("bug tracker {} not found within available options {}", bug, bugTrackerImpl);
                    throw new IllegalArgumentException("Custom bug tracker not found in list of available implementations");
                }
                return BugTracker.Type.CUSTOM;
            }
        }
        return bugType;
    }

    public static String getFilename(ScanRequest request, String format){
        String filename;
        filename = format;

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd.HHmmss");
        String dt = now.format(formatter);
        filename = filename.replace("[TIME]", dt);
        log.debug(dt);
        log.debug(filename);

        if(!empty(request.getTeam())){
            String team = request.getTeam();
            team = team.replaceAll("\\\\","_");
            filename = filename.replace("[TEAM]", team);
        }
        if(!empty(request.getApplication())) {
            filename = filename.replace("[APP]", request.getApplication());
            log.debug(request.getApplication());
            log.debug(filename);
        }
        if(!empty(request.getProject())) {
            filename = filename.replace("[PROJECT]", request.getProject());
            log.debug(request.getProject());
            log.debug(filename);
        }
        if(!empty(request.getNamespace())) {
            filename = filename.replace("[NAMESPACE]", request.getNamespace());
            log.debug(request.getNamespace());
            log.debug(filename);
        }
        if(!empty(request.getRepoName())) {
            filename = filename.replace("[REPO]", request.getRepoName());
            log.debug(request.getRepoName());
            log.debug(filename);
        }
        if(!empty(request.getBranch())) {
            filename = filename.replace("[BRANCH]", request.getBranch());
            log.debug(request.getBranch());
            log.debug(filename);
        }
        return filename;
    }

    public static String cleanStringUTF8(String dirty){
        return new String(dirty.getBytes(), 0, dirty.length(), UTF_8);
    }

}
