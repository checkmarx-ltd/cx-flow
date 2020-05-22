package com.checkmarx.flow.utils;

import com.checkmarx.flow.config.FindingSeverity;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.config.RepoProperties;
import com.checkmarx.flow.dto.*;
import com.checkmarx.flow.exception.MachinaRuntimeException;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.dto.CxConfig;
import com.checkmarx.sdk.dto.Filter;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.cx.CxScanSummary;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.springframework.web.bind.annotation.RequestParam;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Map.Entry.comparingByKey;

public class ScanUtils {

    public static final String RUNNING = "running";
    public static final String CRLF = "\r\n";
    public static final String ISSUE_BODY = "**%s** issue exists @ **%s** in branch **%s**";
    public static final String ISSUE_BODY_TEXT = "%s issue exists @ %s in branch %s";
    public static final String ISSUE_KEY = "%s %s @ %s [%s]";
    public static final String ISSUE_KEY_2 = "%s %s @ %s";
    public static final String JIRA_ISSUE_KEY = "%s%s @ %s [%s]%s";
    public static final String JIRA_ISSUE_KEY_2 = "%s%s @ %s%s";
    public static final String JIRA_ISSUE_BODY = "*%s* issue exists @ *%s* in branch *%s*";
    public static final String JIRA_ISSUE_BODY_2 = "*%s* issue exists @ *%s*";
    public static final String WEB_HOOK_PAYLOAD = "web-hook-payload";

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
     * @param flowProperties
     * @return
     */
    public static List<Filter> getFilters(FlowProperties flowProperties) {
        return getFilters(flowProperties.getFilterSeverity(), flowProperties.getFilterCwe(), flowProperties.getFilterCategory(), flowProperties.getFilterStatus());
    }

    /**
     * Create List of filters based on String lists of severity, cwe, category
     * @param severity
     * @param cwe
     * @param category
     * @return
     */
    public static List<Filter> getFilters(List<String> severity, List<String> cwe, List<String> category, List<String> status) {
        List<Filter> filters = new ArrayList<>();
        filters.addAll(getListByFilterType(severity, Filter.Type.SEVERITY));
        filters.addAll(getListByFilterType(cwe, Filter.Type.CWE));
        filters.addAll(getListByFilterType(category, Filter.Type.TYPE));
        filters.addAll(getListByFilterType(status, Filter.Type.STATUS));
        return filters;
    }

    private static List<Filter> getListByFilterType(List<String> stringFilters, Filter.Type type){
        List<Filter> filterList = new ArrayList<>();
        if(stringFilters != null) {
            for (String s : stringFilters) {
                filterList.add(Filter.builder()
                        .type(type)
                        .value(s)
                        .build());
            }
        }
        return filterList;
    }

    /**
     * Check if string is empty or null
     * @param str
     * @return
     */
    public static boolean empty(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static boolean anyEmpty(String ...str){
        for(String s : str)
            if (empty(s)) {
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
    public static ScanRequest overrideMap(ScanRequest request, FlowOverride override){
        if(override == null){
            return request;
        }
        BugTracker bt = request.getBugTracker();
        /*Override only applicable to Simple JIRA bug*/
        if(request.getBugTracker().getType().equals(BugTracker.Type.JIRA) && override.getJira()!=null) {
            overrideJiraBugProperties(override, bt);
        }
        request.setBugTracker(bt);

        if(!ScanUtils.empty(override.getApplication())) {
            request.setApplication(override.getApplication());
        }

        if(!ScanUtils.empty(override.getBranches())) {
            request.setActiveBranches(override.getBranches());
        }

        List<String> emails = override.getEmails();
        if(emails != null) {
            if (emails.isEmpty()) {
                request.setEmail(null);
            } else {
                request.setEmail(emails);
            }
        }
        FlowOverride.Filters filtersObj = override.getFilters();

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

    /**
     * Override scan request details as per file/blob (MachinaOverride)
     *
     * @param request
     * @param override
     * @return
     */
    public static ScanRequest overrideCxConfig(ScanRequest request, CxConfig override, FlowProperties flowProperties, JiraProperties jiraProperties){
        Map<String,String> overridePropertiesMap = new HashMap<>();

        if(override == null || request == null || !override.getActive()){
            return request;
        }
        if (!ScanUtils.empty(override.getProject())) {
            /*Replace ${repo} and ${branch}  with the actual reponame and branch - then strip out non-alphanumeric (-_ are allowed)*/
            String project = override.getProject();
            project = project.replaceAll("\\$\\{repo}", request.getRepoName());
            project = project.replaceAll("\\$\\{branch}", request.getBranch());
            project = project.replaceAll("[^a-zA-Z0-9-_.]+","-");
            request.setProject(project);
            overridePropertiesMap.put("project", project);
        }
        if (!ScanUtils.empty(override.getTeam())) {
            request.setTeam(override.getTeam());
            overridePropertiesMap.put("team", override.getTeam());
        }
        if(override.getSast() != null) {
            if (override.getSast().getIncremental() != null) {
                request.setIncremental(override.getSast().getIncremental());
                overridePropertiesMap.put("incremental", override.getSast().getIncremental().toString());
            }

            if (override.getSast().getForceScan() != null) {
                request.setForceScan(override.getSast().getForceScan());
                overridePropertiesMap.put("force scan", override.getSast().getForceScan().toString());
            }

            if (!ScanUtils.empty(override.getSast().getPreset())) {
                request.setScanPreset(override.getSast().getPreset());
                request.setScanPresetOverride(true);
                overridePropertiesMap.put("scan preset", override.getSast().getPreset());
            }
            if (override.getSast().getFolderExcludes() != null) {
                request.setExcludeFolders(Arrays.asList(override.getSast().getFolderExcludes().split(",")));
                overridePropertiesMap.put("exclude folders", override.getSast().getFolderExcludes());
            }
            if (override.getSast().getFileExcludes() != null) {
                request.setExcludeFiles(Arrays.asList(override.getSast().getFileExcludes().split(",")));
                overridePropertiesMap.put("exclude files", override.getSast().getFileExcludes());
            }
        }
        try {
            if (override.getAdditionalProperties() != null) {
                Object flow = override.getAdditionalProperties().get("cxFlow");
                ObjectMapper mapper = new ObjectMapper();
                FlowOverride flowOverride = mapper.convertValue(flow, FlowOverride.class);

                if (flowOverride != null) {
                    BugTracker bt = request.getBugTracker();
                    //initial bt as NONE
                    if(bt == null) {
                        bt = BugTracker.builder()
                                .type(BugTracker.Type.NONE)
                                .build();
                    }
                    String bug = flowOverride.getBugTracker();
                    if(bug != null && !bug.equalsIgnoreCase(bt.getType().toString())) {
                        BugTracker.Type bugType = ScanUtils.getBugTypeEnum(bug, flowProperties.getBugTrackerImpl());
                        if (bugType.equals(BugTracker.Type.CUSTOM)) {
                            bt = BugTracker.builder()
                                    .type(bugType)
                                    .customBean(bug)
                                    .build();
                        } else {
                            bt = BugTracker.builder()
                                    .type(bugType)
                                    .build();
                        }
                        overridePropertiesMap.put("bug tracker", flowOverride.getBugTracker());
                    }
                    /*Override only applicable to Simple JIRA bug*/
                    if (bt.getType().equals(BugTracker.Type.JIRA) && flowOverride.getJira() != null) {
                        overrideJiraBugProperties(flowOverride, bt);
                    }

                    request.setBugTracker(bt);

                    if (!ScanUtils.empty(flowOverride.getApplication())) {
                        request.setApplication(flowOverride.getApplication());
                        overridePropertiesMap.put("application", flowOverride.getApplication());
                    }

                    if (!ScanUtils.empty(flowOverride.getBranches())) {
                        request.setActiveBranches(flowOverride.getBranches());
                        overridePropertiesMap.put("active branches", flowOverride.getBranches().toArray().toString());
                    }

                    List<String> emails = flowOverride.getEmails();
                    if (emails != null) {
                        if (emails.isEmpty()) {
                            request.setEmail(null);
                        } else {
                            request.setEmail(emails);
                        }
                    }
                    FlowOverride.Filters filtersObj = flowOverride.getFilters();

                    if (filtersObj != null &&
                            (!ScanUtils.empty(filtersObj.getSeverity()) || !ScanUtils.empty(filtersObj.getCwe()) ||
                                    !ScanUtils.empty(filtersObj.getCategory()) || !ScanUtils.empty(filtersObj.getStatus()))) {
                        List<Filter> filters = ScanUtils.getFilters(filtersObj.getSeverity(), filtersObj.getCwe(),
                                filtersObj.getCategory(), filtersObj.getStatus());
                        request.setFilters(filters);
                        overridePropertiesMap.put("filters", filters.stream().map(Object::toString).collect(Collectors.joining(",")));
                    } else if (filtersObj != null) {
                        request.setFilters(null);
                        overridePropertiesMap.put("filters", "EMPTY");
                    }

                    FlowOverride.Thresholds thresholds = flowOverride.getThresholds();

                    if (thresholds != null &&
                            !(thresholds.getHigh()==null && thresholds.getMedium()==null &&
                                    thresholds.getLow()==null && thresholds.getInfo()==null)) {
                                
                            Map<FindingSeverity,Integer> thresholdsMap = ScanUtils.getThresholdsMap(thresholds);
                            if(!thresholdsMap.isEmpty()) {
                                flowProperties.setThresholds(thresholdsMap);
                            }

                        overridePropertiesMap.put("thresholds", convertMapToString(thresholdsMap));
                    } 
                }
            }

            String overridePropertiesString = convertMapToString(overridePropertiesMap);

            log.info("override configuration properties from config as code file. with values: {}", overridePropertiesString);

        }catch (IllegalArgumentException e){
            log.warn("Issue parsing CxConfig cxFlow element", e);
        }
        return request;
    }

    private static String convertMapToString(Map<?, ?> map) {
        String mapAsString = map.keySet().stream()
                .map(key -> key + "=" + map.get(key))
                .collect(Collectors.joining(", ", "{", "}"));
        return mapAsString;
    }

    private static void overrideJiraBugProperties(FlowOverride override, BugTracker bt) {
        FlowOverride.Jira jira = override.getJira();
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

    private static Map<FindingSeverity, Integer> getThresholdsMap(FlowOverride.Thresholds thresholds) {

        Map<FindingSeverity, Integer> map = new HashMap<>();
       if(thresholds.getHigh()!=null){
           map.put(FindingSeverity.HIGH, thresholds.getHigh());
       }
        if(thresholds.getMedium()!=null){
            map.put(FindingSeverity.MEDIUM, thresholds.getMedium());
        }
        if(thresholds.getLow()!=null){
            map.put(FindingSeverity.LOW, thresholds.getLow());
        }
        if(thresholds.getInfo()!=null){
            map.put(FindingSeverity.INFO, thresholds.getInfo());
        }
        
        return  map;
     }

    public static BugTracker getBugTracker(@RequestParam(value = "assignee", required = false) String assignee,
                                           BugTracker.Type bugType, JiraProperties jiraProperties, String bugTracker) {
        BugTracker bt = null;
        if(bugType != null) {
            if (bugType.equals(BugTracker.Type.JIRA)) {
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
            } else if (bugType.equals(BugTracker.Type.CUSTOM)) {
                bt = BugTracker.builder()
                        .type(bugType)
                        .customBean(bugTracker)
                        .build();
            } else {
                bt = BugTracker.builder()
                        .type(bugType)
                        .build();
            }
        }
        return bt;
    }

    public static String getMDBody(ScanResults.XIssue issue, String branch, String fileUrl, FlowProperties flowProperties) {
        StringBuilder body = new StringBuilder();
        body.append(String.format(ISSUE_BODY, issue.getVulnerability(), issue.getFilename(), branch)).append(CRLF).append(CRLF);
        if(!ScanUtils.empty(issue.getDescription())) {
            body.append("*").append(issue.getDescription().trim()).append("*").append(CRLF).append(CRLF);
        }
        if(!ScanUtils.empty(issue.getSeverity())) {
            body.append("Severity: ").append(issue.getSeverity()).append(CRLF).append(CRLF);
        }
        if(!ScanUtils.empty(issue.getCwe())) {
            body.append("CWE:").append(issue.getCwe()).append(CRLF).append(CRLF);
            if(!empty(flowProperties.getMitreUrl())) {
                body.append("[Vulnerability details and guidance](").append(String.format(flowProperties.getMitreUrl(), issue.getCwe())).append(")").append(CRLF).append(CRLF);
            }
        }
        if(!ScanUtils.empty(flowProperties.getWikiUrl())) {
            body.append("[Internal Guidance](").append(flowProperties.getWikiUrl()).append(")").append(CRLF).append(CRLF);
        }
        if(!ScanUtils.empty(issue.getLink())){
            body.append("[Checkmarx](").append(issue.getLink()).append(")").append(CRLF).append(CRLF);
        }
        if(issue.getDetails() != null && !issue.getDetails().isEmpty()) {
            Map<Integer, ScanResults.IssueDetails> trueIssues = issue.getDetails().entrySet().stream()
                    .filter(x -> x.getKey( ) != null && x.getValue() != null && !x.getValue().isFalsePositive())
                    .sorted(comparingByKey())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            Map<Integer, ScanResults.IssueDetails> fpIssues = issue.getDetails().entrySet().stream()
                    .filter(x -> x.getKey( ) != null && x.getValue() != null && x.getValue().isFalsePositive())
                    .sorted(comparingByKey())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            if(!trueIssues.isEmpty()) {
                body.append("Lines: ");
                for (Map.Entry<Integer, ScanResults.IssueDetails> entry : trueIssues.entrySet()) {
                    if (fileUrl != null) {  //[<line>](<url>)
                        body.append("[").append(entry.getKey()).append("](").append(fileUrl).append("#L").append(entry.getKey()).append(") ");
                    } else { //if the fileUrl is not provided, simply putting the line number (no link) - ADO for example
                        body.append(entry.getKey()).append(" ");
                    }
                }
                body.append(CRLF).append(CRLF);
            }
            if(flowProperties.isListFalsePositives() && !fpIssues.isEmpty()) {//List the false positives / not exploitable
                body.append(ScanUtils.CRLF);
                body.append("Lines Marked Not Exploitable: ");
                for (Map.Entry<Integer, ScanResults.IssueDetails> entry : fpIssues.entrySet()) {
                    if (fileUrl != null) {  //[<line>](<url>)
                        body.append("[").append(entry.getKey()).append("](").append(fileUrl).append("#L").append(entry.getKey()).append(") ");
                    } else { //if the fileUrl is not provided, simply putting the line number (no link) - ADO for example
                        body.append(entry.getKey()).append(" ");
                    }
                }
                body.append(CRLF).append(CRLF);
            }
            for (Map.Entry<Integer, ScanResults.IssueDetails> entry : trueIssues.entrySet()) {
                if (entry.getValue() != null && entry.getValue().getCodeSnippet() != null) {
                    body.append("---").append(CRLF);
                    body.append("[Code (Line #").append(entry.getKey()).append("):](").append(fileUrl).append("#L").append(entry.getKey()).append(")").append(CRLF);
                    body.append("```").append(CRLF);
                    body.append(entry.getValue().getCodeSnippet()).append(CRLF);
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

    public static String getMergeCommentMD(ScanRequest request, ScanResults results, FlowProperties flowProperties,
                                           RepoProperties properties) {
        CxScanSummary summary = results.getScanSummary();
        StringBuilder body = new StringBuilder();
        body.append("### Checkmarx scan completed").append(CRLF);
        body.append("[Full Scan Details](").append(results.getLink()).append(")").append(CRLF);
        if(properties.isCxSummary() && !request.getProduct().equals(ScanRequest.Product.CXOSA)){
            if(!ScanUtils.empty(properties.getCxSummaryHeader())) {
                body.append("#### ").append(properties.getCxSummaryHeader()).append(CRLF);
            }
            body.append("Severity|Count").append(CRLF);
            body.append("---|---").append(CRLF);
            body.append("High|").append(summary.getHighSeverity().toString()).append(CRLF);
            body.append("Medium|").append(summary.getMediumSeverity().toString()).append(CRLF);
            body.append("Low|").append(summary.getLowSeverity().toString()).append(CRLF);
            body.append("Informational|").append(summary.getInfoSeverity().toString()).append(CRLF).append(CRLF);
        }
        if(properties.isFlowSummary()){
            if(!ScanUtils.empty(properties.getFlowSummaryHeader())) {
                body.append("#### ").append(properties.getFlowSummaryHeader()).append(CRLF);
            }
            body.append("Severity|Count").append(CRLF);
            body.append("---|---").append(CRLF);
            Map<String, Integer> flow = (Map<String, Integer>) results.getAdditionalDetails().get(Constants.SUMMARY_KEY);
            if(flow != null) {
                for (Map.Entry<String, Integer> severity : flow.entrySet()) {
                    body.append(severity.getKey()).append("|").append(severity.getValue().toString()).append(CRLF);
                }
            }
            body.append(CRLF);
        }
        if(properties.isDetailed()) {
            if(!ScanUtils.empty(properties.getDetailHeader())) {
                body.append("#### ").append(properties.getDetailHeader()).append(CRLF);
            }
            body.append("|Lines|Severity|Category|File|Link|").append(CRLF);
            body.append("---|---|---|---|---").append(CRLF);

            Map<String, ScanResults.XIssue> xMap;
            xMap = getXIssueMap(results.getXIssues(), request);
            log.info("Creating Merge/Pull Request Markdown comment");

            Comparator<ScanResults.XIssue> issueComparator = Comparator
                    .comparing(ScanResults.XIssue::getSeverity)
                    .thenComparing(ScanResults.XIssue::getVulnerability);
            //SAST
            xMap.entrySet().stream()
                .filter(x -> x.getValue() != null && x.getValue().getDetails() != null)
                .sorted(Entry.comparingByValue(issueComparator))
                .forEach( xIssue -> {
                ScanResults.XIssue currentIssue = xIssue.getValue();
                String fileUrl = ScanUtils.getFileUrl(request, currentIssue.getFilename());
                currentIssue.getDetails().entrySet().stream()
                    .filter(x -> x.getKey( ) != null && x.getValue() != null && !x.getValue().isFalsePositive())
                    .sorted(Entry.comparingByKey())
                    .forEach( entry -> {
                    //[<line>](<url>)
                    //Azure DevOps direct repo line url is unknown at this time.
                    if (request.getRepoType().equals(ScanRequest.Repository.ADO)) {
                        body.append(entry.getKey()).append(" ");
                    } else {
                        body.append("[").append(entry.getKey()).append("](").append(fileUrl);
                        if (request.getRepoType().equals(ScanRequest.Repository.BITBUCKET)) {
                            body.append("#lines-").append(entry.getKey()).append(") ");
                        } else if (request.getRepoType().equals(ScanRequest.Repository.BITBUCKETSERVER)) {
                            body.append("#").append(entry.getKey()).append(") ");
                        } else {
                            body.append("#L").append(entry.getKey()).append(") ");
                        }
                    }
                });
                if(currentIssue.getDetails().entrySet().stream().anyMatch(x -> x.getKey() != null && x.getValue() != null && !x.getValue().isFalsePositive())) {
                    body.append("|");
                    body.append(currentIssue.getSeverity()).append("|");
                    body.append(currentIssue.getVulnerability()).append("|");
                    body.append(currentIssue.getFilename()).append("|");
                    body.append("[Checkmarx](").append(currentIssue.getLink()).append(")");
                    body.append(CRLF);
                }
            });

            if(results.getOsa() != null && results.getOsa()) {
                body.append(CRLF);
                body.append("|Library|Severity|CVE|").append(CRLF);
                body.append("---|---|---").append(CRLF);

                //OSA
                xMap.entrySet().stream()
                        .filter(x -> x.getValue() != null && x.getValue().getOsaDetails() != null)
                        .sorted(Entry.comparingByValue(issueComparator))
                        .forEach( xIssue -> {
                    ScanResults.XIssue currentIssue = xIssue.getValue();
                    body.append("|");
                    body.append(currentIssue.getFilename()).append("|");
                    body.append(currentIssue.getSeverity()).append("|");
                    for (ScanResults.OsaDetails o : currentIssue.getOsaDetails()) {
                        body.append("[").append(o.getCve()).append("](")
                                .append("https://cve.mitre.org/cgi-bin/cvename.cgi?name=").append(o.getCve()).append(") ");
                    }
                    body.append("|");
                    body.append(CRLF);
                //body.append("```").append(currentIssue.getDescription()).append("```").append(CRLF); Description is too long
                });
            }
        }
        return body.toString();
    }

    public static String getFileUrl(ScanRequest request, String filename) {
        if(request.getProduct().equals(ScanRequest.Product.CXOSA) || filename == null || filename.isEmpty()){
            return null;
        }

        String branch = request.getBranch();
        if(!ScanUtils.empty(request.getRepoUrl()) && !ScanUtils.empty(branch)) {
            String repoUrl = request.getRepoUrl().replace(".git", "/");
            if (request.getRepoType().equals(ScanRequest.Repository.BITBUCKETSERVER)) {
                String url = request.getAdditionalMetadata("BITBUCKET_BROWSE");
                if(url != null && !url.isEmpty()){
                    url = url.concat("/").concat(filename);
                    if(!ScanUtils.empty(branch)) {
                        return url.concat("?at=").concat(branch);
                    }
                    else{
                        return url;
                    }
                }
            }
            else if (request.getRepoType().equals(ScanRequest.Repository.BITBUCKET)) {
                return repoUrl.concat("src/").concat(branch).concat("/").concat(filename);
            }
            else if (request.getRepoType().equals(ScanRequest.Repository.ADO)) {
                return null;
            }
            else {
                repoUrl = repoUrl.concat("blob/");
                if(!ScanUtils.empty(branch)) {
                    return repoUrl.concat(branch).concat("/").concat(filename);
                }
                else{ //default to master branch
                    return repoUrl.concat("master/").concat(filename);
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
     * = Generates an HTML message describing the discovered issue.
     *
     * @param issue The issue to add the comment too
     * @param branch The repo branch name
     * @return string with the HTML message
     */
    public static String getHTMLBody(ScanResults.XIssue issue, ScanRequest request, FlowProperties flowProperties) {
        String branch = request.getBranch();
        StringBuilder body = new StringBuilder();
        body.append("<div>");
        body.append(String.format(ISSUE_BODY, issue.getVulnerability(), issue.getFilename(), branch)).append(CRLF);
        if(!ScanUtils.empty(issue.getDescription())) {
            body.append("<div><i>").append(issue.getDescription().trim()).append("</i></div>");
        }
        body.append(CRLF);
        if(!ScanUtils.empty(issue.getSeverity())) {
            body.append("<div><b>Severity:</b> ").append(issue.getSeverity()).append("</div>");
        }
        if(!ScanUtils.empty(issue.getCwe())) {
            body.append("<div><b>CWE:</b>").append(issue.getCwe()).append("</div>");
            if(!ScanUtils.empty(flowProperties.getMitreUrl())) {
                body.append("<div><a href=\'").append(
                        String.format(
                                flowProperties.getMitreUrl(),
                                issue.getCwe()
                        )
                ).append("\'>Vulnerability details and guidance</a></div>");
            }
        }
        if(!ScanUtils.empty(flowProperties.getWikiUrl())) {
            body.append("<div><a href=\'").append(flowProperties.getWikiUrl()).append("\'>Internal Guidance</a></div>");
        }
        if(!ScanUtils.empty(issue.getLink())){
            body.append("<div><a href=\'").append(issue.getLink()).append("\'>Checkmarx</a></div>");
        }
        if(issue.getDetails() != null && !issue.getDetails().isEmpty()) {
            Map<Integer, ScanResults.IssueDetails> trueIssues = issue.getDetails().entrySet().stream()
                    .filter(x -> x.getKey( ) != null && x.getValue() != null && !x.getValue().isFalsePositive())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            Map<Integer, ScanResults.IssueDetails> fpIssues = issue.getDetails().entrySet().stream()
                    .filter(x -> x.getKey( ) != null && x.getValue() != null && x.getValue().isFalsePositive())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            if(!trueIssues.isEmpty()) {
                body.append("<div><b>Lines: </b>");
                for (Map.Entry<Integer, ScanResults.IssueDetails> entry : trueIssues.entrySet()) {
                    body.append(entry.getKey()).append(" ");
                }
                body.append("</div>");
            }
            if(flowProperties.isListFalsePositives() && !fpIssues.isEmpty()) {//List the false positives / not exploitable
                body.append("<div><b>Lines Marked Not Exploitable: </b>");
                for (Map.Entry<Integer, ScanResults.IssueDetails> entry : fpIssues.entrySet()) {
                    body.append(entry.getKey()).append(" ");
                }
                body.append("</div>");
            }
            for (Map.Entry<Integer, ScanResults.IssueDetails> entry : trueIssues.entrySet()) {
                if (!ScanUtils.empty(entry.getValue().getCodeSnippet())) {
                    body.append("<hr/>");
                    body.append("<b>Line #").append(entry.getKey()).append("</b>");
                    body.append("<pre><code><div>");
                    String codeSnippet = entry.getValue().getCodeSnippet();
                    body.append(StringEscapeUtils.escapeHtml4(codeSnippet));
                    body.append("</div></code></pre><div>");
                }
            }
            body.append("<hr/>");
        }
        if(issue.getOsaDetails()!=null){
            for(ScanResults.OsaDetails o: issue.getOsaDetails()){
                body.append(CRLF);
                if(!ScanUtils.empty(o.getCve())) {
                    body.append("<b>").append(o.getCve()).append("</b>").append(CRLF);
                }
                body.append("<pre><code><div>");
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
                body.append("</div></code></pre><div>");
                body.append(CRLF);
            }
        }
        body.append("</div>");
        return body.toString();
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

    public static FlowOverride getMachinaOverride(@RequestParam(value = "override", required = false) String override) {
        FlowOverride o = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            //if override is provided, check if chars are more than 20 in length, implying base64 encoded json
            if(!ScanUtils.empty(override)){
                if(override.length() > 20){
                    String oJson = new String(Base64.getDecoder().decode(override));
                    o = mapper.readValue(oJson, FlowOverride.class);
                    log.info("Overriding attributes with Base64 encoded String");
                }
                else{
                    //TODO download file
                }
            }
        } catch (IOException e) {
            log.error("Error occurred", e);
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
        if(bugType == null && bug != null){ //Try uppercase
            bugType = EnumUtils.getEnum(BugTracker.Type.class, bug.toUpperCase(Locale.ROOT));
            if(bugType == null){
                log.debug("Determine if custom bean is being used");
                if(bugTrackerImpl == null || !bugTrackerImpl.contains(bug)){
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

        filename = getGenericFilename(filename, "[TEAM]", request.getTeam());
        filename = getGenericFilename(filename, "[APP]", request.getApplication());
        filename = getGenericFilename(filename, "[PROJECT]", request.getProject());
        filename = getGenericFilename(filename, "[NAMESPACE]", request.getNamespace());
        filename = getGenericFilename(filename, "[REPO]", request.getRepoName());
        filename = getGenericFilename(filename, "[BRANCH]", request.getBranch());

        return filename;
    }

    public static String getGenericFilename(String filename, String valueToReplace, String replacement){

        if(!empty(replacement)) {
            replacement = replacement.replaceAll("[^a-zA-Z0-9-_]+","_");

            filename = filename.replace(valueToReplace, replacement);
            log.debug(replacement);
            log.debug(filename);
        }
        return filename;
    }

    public static String cleanStringUTF8(String dirty){
        log.debug(""+dirty.length());
        return new String(dirty.getBytes(), 0, dirty.length(), UTF_8);
    }

    public static String cleanStringUTF8_2(String dirty){
        return new String(dirty.getBytes(), UTF_8);
    }

    public static void writeByte(String filename, byte[] bytes) {
        try (OutputStream os = new FileOutputStream(new File(filename))) {
            os.write(bytes);
        }
        catch (IOException e) {
            log.error("Error while writing file {}", filename, e);
        }
    }
    /**
     * Returns the protocol, host and port from given url.
     *
     * @param url url to process
     * @return  host with protocol and port
     */
    public static String getHostWithProtocol(String url) {
        String hostWithProtocol = null;
        try {
            URI uri = new URI(url);
            int port = uri.getPort();
            hostWithProtocol = uri.getScheme() + "//"  + uri.getHost() + (port > 0 ? ":" + port : "");
        } catch (URISyntaxException e) {
            log.debug("Could not parse given URL" + url, e);
        }
        return hostWithProtocol;
    }

    public static String getBranchFromRef(String ref){
        // refs/head/master (get 2nd position of /
        int index = StringUtils.ordinalIndexOf(ref, "/", 2);
        if(index < 0) return ref;
        return ref.substring(index+1);
    }

    /**
     * = Generates an Text message describing the discovered issue.
     *
     * @param issue The issue to add the comment too
     * @return string with the HTML message
     */
    public static String getTextBody(ScanResults.XIssue issue, ScanRequest request, FlowProperties flowProperties) {
        String branch = request.getBranch();
        StringBuilder body = new StringBuilder();
        body.append(String.format(ISSUE_BODY_TEXT, issue.getVulnerability(), issue.getFilename(), branch)).append(CRLF);
        if(!ScanUtils.empty(issue.getDescription())) {
            body.append(issue.getDescription().trim());
        }
        body.append(CRLF);
        if(!ScanUtils.empty(issue.getSeverity())) {
            body.append("Severity: ").append(issue.getSeverity()).append(CRLF);
        }
        if(!ScanUtils.empty(issue.getCwe())) {
            body.append("CWE: ").append(issue.getCwe()).append(CRLF);
            if(!ScanUtils.empty(flowProperties.getMitreUrl())) {
                body.append("Details - ")
                        .append(
                        String.format(
                                flowProperties.getMitreUrl(),
                                issue.getCwe()
                        )
                ).append(" - Vulnerability details and guidance").append(CRLF);
            }
        }
        if(!ScanUtils.empty(flowProperties.getWikiUrl())) {
            body.append("Details - ").append(flowProperties.getWikiUrl()).append(" - Internal Guidance ").append(CRLF);
        }
        if(!ScanUtils.empty(issue.getLink())){
            body.append("Details - ").append(issue.getLink()).append(" - Checkmarx").append(CRLF);
        }
        if(issue.getDetails() != null && !issue.getDetails().isEmpty()) {
            Map<Integer, ScanResults.IssueDetails> trueIssues = issue.getDetails().entrySet().stream()
                    .filter(x -> x.getKey( ) != null && x.getValue() != null && !x.getValue().isFalsePositive())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            Map<Integer, ScanResults.IssueDetails> fpIssues = issue.getDetails().entrySet().stream()
                    .filter(x -> x.getKey( ) != null && x.getValue() != null && x.getValue().isFalsePositive())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            if(!trueIssues.isEmpty()) {
                body.append("Lines: ");
                for (Map.Entry<Integer, ScanResults.IssueDetails> entry : trueIssues.entrySet()) {
                    body.append(entry.getKey()).append(" ");
                }
            }
            if(flowProperties.isListFalsePositives() && !fpIssues.isEmpty()) {//List the false positives / not exploitable
                body.append("Lines Marked Not Exploitable: ");
                for (Map.Entry<Integer, ScanResults.IssueDetails> entry : fpIssues.entrySet()) {
                    body.append(entry.getKey()).append(" ");
                }
            }
            for (Map.Entry<Integer, ScanResults.IssueDetails> entry : trueIssues.entrySet()) {
                if (!ScanUtils.empty(entry.getValue().getCodeSnippet())) {
                    body.append("Line # ").append(entry.getKey());
                    String codeSnippet = entry.getValue().getCodeSnippet();
                    body.append(StringEscapeUtils.escapeHtml4(codeSnippet)).append(CRLF);
                }
            }
        }
        if(issue.getOsaDetails()!=null){
            for(ScanResults.OsaDetails o: issue.getOsaDetails()){
                body.append(CRLF);
                if(!ScanUtils.empty(o.getCve())) {
                    body.append(o.getCve()).append(CRLF);
                }
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
                body.append(CRLF);
            }
        }
        return body.toString();
    }
}
