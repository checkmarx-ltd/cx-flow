package com.checkmarx.flow.utils;

import com.checkmarx.flow.config.FindingSeverity;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.config.RepoProperties;
import com.checkmarx.flow.constants.SCATicketingConstants;
import com.checkmarx.flow.dto.*;
import com.checkmarx.flow.dto.BugTracker.BugTrackerBuilder;
import com.checkmarx.flow.exception.MachinaRuntimeException;
import com.checkmarx.flow.service.FilterFactory;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.dto.CxConfig;
import com.checkmarx.sdk.dto.Filter;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.cx.CxScanSummary;
import com.checkmarx.sdk.dto.filtering.FilterConfiguration;
import com.checkmarx.sdk.dto.sca.SCAResults;
import com.cx.restclient.sca.dto.report.Finding;
import com.cx.restclient.sca.dto.report.Package;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.constraints.NotNull;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Map.Entry.comparingByKey;

public class ScanUtils {

    private static final String DIV_A_HREF = "<div><a href=\'";
    public static final String RUNNING = "running";
    public static final String CRLF = "\r\n";
    public static final String MD_H3 = "###";
    public static final String MD_H4 = "####";
    public static final String ISSUE_BODY = "**%s** issue exists @ **%s** in branch **%s**";
    public static final String ISSUE_BODY_TEXT = "%s issue exists @ %s in branch %s";
    public static final String ISSUE_TITLE_KEY_WITH_BRANCH = "%s %s @ %s [%s]";
    public static final String ISSUE_TITLE_KEY = "%s %s @ %s";
    public static final String WEB_HOOK_PAYLOAD = "web-hook-payload";

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ScanUtils.class);
    public static final String VERSION = "Version: ";
    public static final String DESCRIPTION = "Description: ";
    public static final String RECOMMENDATION = "Recommendation: ";
    public static final String URL = "URL: ";
    public static final String DETAILS = "Details - ";
    public static final String SEVERITY = "Severity: ";
    public static final String DIV_CLOSING_TAG = "</div>";
    private static final String ITALIC_OPENING_DIV = "<div><i>";
    private static final String ITALIC_CLOSING_DIV = "</i></div>";
    private static final String LINE_BREAK = "<br>";
    private static final String NVD_URL_PREFIX = "https://nvd.nist.gov/vuln/detail/";

    private ScanUtils() {
        // this is to hide the public constractor
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

    public static List<ScanResults.XIssue> scaToXIssues(SCAResults scaResults) {
        List<ScanResults.XIssue> issueList = new ArrayList<>();

        List<Finding> findings = scaResults.getFindings();
        EnumSet.range(Filter.Severity.HIGH, Filter.Severity.LOW)
                .forEach(s -> {
                    List<Finding> findingsListBySeverity = getFindingsListBySeverity(findings, s);
                    Map<String, List<Finding>> packageMap = findingsListBySeverity.stream()
                            .collect(Collectors.groupingBy(f-> f.getId() + f.getPackageId()));
                    packageMap.forEach((k,v) -> {
                        ScanResults.XIssue issue = ScanResults.XIssue.builder()
                                .build();
                        issue.setScaDetails(getScaDetailsListBySeverity(scaResults, v));
                        issueList.add(issue);
                    });
                });
        return issueList;
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
    public static boolean empty(List<?> list) {
        return (list == null) || list.isEmpty();
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

        if (filtersObj != null) {
            FilterFactory filterFactory = new FilterFactory();
            FilterConfiguration filter = filterFactory.getFilter(filtersObj.getSeverity(),
                    filtersObj.getCwe(),
                    filtersObj.getCategory(),
                    filtersObj.getStatus(),
                    null,
                    null);
            request.setFilter(filter);
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
    public static ScanRequest overrideCxConfig(ScanRequest request, CxConfig override, FlowProperties flowProperties){
        Map<String,String> overridePropertiesMap = new HashMap<>();

        if(override == null || request == null || !override.getActive()){
            return request;
        }
        Optional.ofNullable(override.getProject())
        .filter(ne -> !empty(ne))
        .ifPresent(p -> {
            /*Replace ${repo} and ${branch}  with the actual reponame and branch - then strip out non-alphanumeric (-_ are allowed)*/
            String project = p.replace("${repo}", request.getRepoName())
            .replace("${branch}", request.getBranch())
            .replaceAll("[^a-zA-Z0-9-_.]+","-");
            request.setProject(project);
            overridePropertiesMap.put("project", project);
        });
        Optional.ofNullable(override.getTeam())
        .filter(ne -> !empty(ne))
        .ifPresent(t -> {
            request.setTeam(t);
            overridePropertiesMap.put("team", t);
        });
        Optional.ofNullable(override.getSast()).ifPresent(s -> {
            Optional.ofNullable(s.getIncremental()).ifPresent(si -> {
                request.setIncremental(si);
                overridePropertiesMap.put("incremental", si.toString());
            });

            Optional.ofNullable(s.getForceScan()).ifPresent(sf -> {
                request.setForceScan(sf);
                overridePropertiesMap.put("force scan", sf.toString());
            });

            Optional.ofNullable(s.getPreset()).ifPresent(sp -> {
                request.setScanPreset(sp);
                request.setScanPresetOverride(true);
                overridePropertiesMap.put("scan preset", sp);
            });
            Optional.ofNullable(s.getFolderExcludes()).ifPresent(sfe -> {
                request.setExcludeFolders(Arrays.asList(sfe.split(",")));
                overridePropertiesMap.put("exclude folders", sfe);
            });
            Optional.ofNullable(s.getFileExcludes()).ifPresent(sf -> {
                request.setExcludeFiles(Arrays.asList(sf.split(",")));
                overridePropertiesMap.put("exclude files", sf);
            });
        });

        try {
            Optional.ofNullable(override.getAdditionalProperties()).ifPresent(ap -> {
                Object flow = ap.get("cxFlow");
                ObjectMapper mapper = new ObjectMapper();
                FlowOverride flowOverride = mapper.convertValue(flow, FlowOverride.class);

                Optional.ofNullable(flowOverride).ifPresent(fo -> {
                    BugTracker bt = Optional.ofNullable(request.getBugTracker())
                        .orElse(BugTracker.builder()
                                .type(BugTracker.Type.NONE)
                                .build());

                    String bug = fo.getBugTracker();
                    if(bug != null && !bug.equalsIgnoreCase(bt.getType().toString())) {
                        BugTracker.Type bugType = ScanUtils.getBugTypeEnum(bug, flowProperties.getBugTrackerImpl());

                        BugTrackerBuilder builder = BugTracker.builder()
                            .type(bugType);
                        if (bugType.equals(BugTracker.Type.CUSTOM)) {
                            builder.customBean(bug);
                        }
                        bt = builder.build();
                        overridePropertiesMap.put("bug tracker", fo.getBugTracker());
                    }
                    /*Override only applicable to Simple JIRA bug*/
                    if (bt.getType().equals(BugTracker.Type.JIRA) && fo.getJira() != null) {
                        overrideJiraBugProperties(fo, bt);
                    }

                    request.setBugTracker(bt);

                    Optional.ofNullable(fo.getApplication())
                    .filter(ne -> !empty(ne))
                    .ifPresent(a -> {
                        request.setApplication(a);
                        overridePropertiesMap.put("application", a);
                    });

                    Optional.ofNullable(fo.getBranches())
                    .filter(ne -> !empty(ne))
                    .ifPresent(br -> {
                        request.setActiveBranches(br);
                        overridePropertiesMap.put("active branches", Arrays.toString(br.toArray()));
                    });

                    Optional.ofNullable(fo.getEmails())
                    .ifPresent(e -> request.setEmail(e.isEmpty() ? null : e));

                    Optional.ofNullable(fo.getFilters()).ifPresent(f -> {
                        FilterFactory filterFactory = new FilterFactory();
                        FilterConfiguration filter = filterFactory.getFilter(f.getSeverity(),
                                f.getCwe(),
                                f.getCategory(),
                                f.getStatus(),
                                null,
                                null);
                        request.setFilter(filter);

                        String filterDescr;
                        if (CollectionUtils.isNotEmpty(filter.getSimpleFilters())) {
                            filterDescr = filter.getSimpleFilters().stream().map(Object::toString).collect(Collectors.joining(","));
                        }
                        else {
                            filterDescr = "EMPTY";
                        }
                        overridePropertiesMap.put("filters", filterDescr);
                    });

                    FlowOverride.Thresholds thresholds = flowOverride.getThresholds();

                    Optional.ofNullable(flowOverride.getThresholds()).ifPresent(th -> {
                        if ( !(
                            th.getHigh()==null &&
                            th.getMedium()==null &&
                            th.getLow()==null &&
                            th.getInfo()==null
                            )) {
                                Map<FindingSeverity,Integer> thresholdsMap = ScanUtils.getThresholdsMap(th);
                                if(!thresholdsMap.isEmpty()) {
                                    flowProperties.setThresholds(thresholdsMap);
                                }

                            overridePropertiesMap.put("thresholds", convertMapToString(thresholdsMap));
                        }
                    });
                });
            });

            String overridePropertiesString = convertMapToString(overridePropertiesMap);

            log.info("override configuration properties from config as code file. with values: {}", overridePropertiesString);

        }catch (IllegalArgumentException e){
            log.warn("Issue parsing CxConfig cxFlow element", e);
        }
        return request;
    }

    private static String convertMapToString(Map<?, ?> map) {
        return map.keySet().stream()
                .map(key -> key + "=" + map.get(key))
                .collect(Collectors.joining(", ", "{", "}"));
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

        List<ScanResults.ScaDetails> scaDetails = issue.getScaDetails();
        if (!empty(scaDetails)) {
            setSCAMDBody(branch, body, scaDetails);

        } else {
            setSASTMDBody(issue, branch, fileUrl, flowProperties, body);
        }

        return body.toString();
    }

    public static String getMergeCommentMD(ScanRequest request, ScanResults results, FlowProperties flowProperties,
                                           RepoProperties properties) {
        StringBuilder body = new StringBuilder();

        if (Optional.ofNullable(results.getScanSummary()).isPresent()) {
            log.debug("Building merge comment MD for SAST scanner");

            CxScanSummary summary = results.getScanSummary();
            body.append(MD_H3).append(" Checkmarx SAST Scan Summary").append(CRLF);
            body.append("[Full Scan Details](").append(results.getLink()).append(")").append(CRLF);
            if (properties.isCxSummary() && !request.getProduct().equals(ScanRequest.Product.CXOSA)) {
                if (!ScanUtils.empty(properties.getCxSummaryHeader())) {
                    body.append(MD_H4).append(" ").append(properties.getCxSummaryHeader()).append(CRLF);
                }
                body.append("Severity|Count").append(CRLF);
                body.append("---|---").append(CRLF);
                body.append("High|").append(summary.getHighSeverity().toString()).append(CRLF);
                body.append("Medium|").append(summary.getMediumSeverity().toString()).append(CRLF);
                body.append("Low|").append(summary.getLowSeverity().toString()).append(CRLF);
                body.append("Informational|").append(summary.getInfoSeverity().toString()).append(CRLF).append(CRLF);
            }
            if (properties.isFlowSummary()) {
                if (!ScanUtils.empty(properties.getFlowSummaryHeader())) {
                    body.append(MD_H4).append(" ").append(properties.getFlowSummaryHeader()).append(CRLF);
                }
                body.append("Severity|Count").append(CRLF);
                body.append("---|---").append(CRLF);
                Map<String, Integer> flow = (Map<String, Integer>) results.getAdditionalDetails().get(Constants.SUMMARY_KEY);
                if (flow != null) {
                    for (Map.Entry<String, Integer> severity : flow.entrySet()) {
                        body.append(severity.getKey()).append("|").append(severity.getValue().toString()).append(CRLF);
                    }
                }
                body.append(CRLF);
            }
            if (properties.isDetailed()) {
                if (!ScanUtils.empty(properties.getDetailHeader())) {
                    body.append(MD_H4).append(" ").append(properties.getDetailHeader()).append(CRLF);
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
                        .forEach(xIssue -> {
                            ScanResults.XIssue currentIssue = xIssue.getValue();
                            String fileUrl = ScanUtils.getFileUrl(request, currentIssue.getFilename());
                            currentIssue.getDetails().entrySet().stream()
                                    .filter(x -> x.getKey() != null && x.getValue() != null && !x.getValue().isFalsePositive())
                                    .sorted(Entry.comparingByKey())
                                    .forEach(entry -> {
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
                            if (currentIssue.getDetails().entrySet().stream().anyMatch(x -> x.getKey() != null && x.getValue() != null && !x.getValue().isFalsePositive())) {
                                body.append("|");
                                body.append(currentIssue.getSeverity()).append("|");
                                body.append(currentIssue.getVulnerability()).append("|");
                                body.append(currentIssue.getFilename()).append("|");
                                body.append("[Checkmarx](").append(currentIssue.getLink()).append(")");
                                body.append(CRLF);
                            }
                        });

                if (results.getOsa() != null && results.getOsa()) {
                    log.debug("Building merge comment MD for OSA scanner");
                    body.append(CRLF);
                    body.append("|Library|Severity|CVE|").append(CRLF);
                    body.append("---|---|---").append(CRLF);

                    //OSA
                    xMap.entrySet().stream()
                            .filter(x -> x.getValue() != null && x.getValue().getOsaDetails() != null)
                            .sorted(Entry.comparingByValue(issueComparator))
                            .forEach(xIssue -> {
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
        }

        Optional.ofNullable(results.getScaResults()).ifPresent(r -> {
            log.debug("Building merge comment MD for SCA scanner");
            if (body.length() > 0) {
                body.append("***").append(CRLF);
            }

            body.append(MD_H3).append(" Checkmarx Dependency (CxSCA) Scan Summary").append(CRLF)
                    .append("[Full Scan Details](").append(r.getWebReportLink()).append(")  ").append(CRLF)
                    .append(MD_H4).append(" Summary  ").append(CRLF)
                    .append("| Total Packages Identified | ").append(r.getSummary().getTotalPackages()).append("| ").append(CRLF)
                    .append("-|-").append(CRLF);

            Arrays.asList("High", "Medium", "Low").forEach(v ->
                    body.append(v).append(" severity vulnerabilities | ")
                            .append(r.getSummary().getFindingCounts().get(Filter.Severity.valueOf(v.toUpperCase()))).append(" ").append(CRLF));
            body.append("Scan risk score | ").append(String.format("%.2f", r.getSummary().getRiskScore())).append(" |").append(CRLF).append(CRLF);

            body.append(MD_H4).append(" CxSCA vulnerability result overview").append(CRLF);
            List<String> headlines = Arrays.asList(
                    "Vulnerability ID",
                    "Package",
                    "Severity",
//                    "CWE / Category",
                    "CVSS score",
                    "Publish date",
                    "Current version",
                    "Recommended version",
                    "Link in CxSCA",
                    "Reference – NVD link"
            );
            headlines.forEach(h -> body.append("| ").append(h));
            body.append("|").append(CRLF);

            headlines.forEach(h -> body.append("|-"));
            body.append("|").append(CRLF);

            r.getFindings().stream()
                    .sorted(Comparator.comparingDouble(o -> -o.getScore()))
                    .sorted(Comparator.comparingInt(o -> -o.getSeverity().ordinal()))
                    .forEach(f -> {

                        Arrays.asList(
                                '`'+f.getId()+'`',
                                extractPackageNameFromFindings(r, f),
                                f.getSeverity().name(),
//                                "N\\A",
                                f.getScore(),
                                f.getPublishDate(),
                                extractPackageVersionFromFindings(r, f),
                                Optional.ofNullable(f.getRecommendations()).orElse(""),
                                " [Vulnerability Link](" + constructVulnerabilityUrl(r.getWebReportLink(), f) + ") | "
                        ).forEach(v -> body.append("| ").append(v));

                        if (!StringUtils.isEmpty(f.getCveName())) {
                            body.append("[").append(f.getCveName()).append("](https://nvd.nist.gov/vuln/detail/").append(f.getCveName()).append(")");
                        } else {
                            body.append("N\\A");
                        }
                        body.append("|" + CRLF);
                    });

        });
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
            String key = String.format(ISSUE_TITLE_KEY_WITH_BRANCH, request.getProduct().getProduct(), issue.getVulnerability(), issue.getFilename(), request.getBranch());
            xMap.put(key, issue);
        }
        return xMap;
    }

    /**
     * = Generates an HTML message describing the discovered issue.
     *
     * @param issue The issue to add the comment too
     * @return string with the HTML message
     */
    public static String getHTMLBody(ScanResults.XIssue issue, ScanRequest request, FlowProperties flowProperties) {
        String branch = request.getBranch();
        StringBuilder body = new StringBuilder();
        body.append("<div>");

        if (Optional.ofNullable(issue.getScaDetails()).isPresent()) {
            setSCAHtmlBody(issue, request, body);

        } else {
            setSASTHtmlBody(issue, flowProperties, branch, body);
        }
        body.append(DIV_CLOSING_TAG);
        return body.toString();
    }

    /**
     *
     * @param request   The scanRequest object
     * @param issue     The scanResults issue
     * @param extraTags Extra tags array. Jira issue prefix/postfix are on the [0], [1] positions
     * @return  Issue key according to the bug type parameter
     */
    public static String getScaSummaryIssueKey(ScanRequest request, ScanResults.XIssue issue, String... extraTags) {
        ScanResults.ScaDetails scaDetails = issue.getScaDetails().get(0);
        String bugType = request.getBugTracker().getType().getType();

        switch (bugType) {
            case "JIRA":
                String issuePrefix = extraTags[0];
                String issuePostfix = extraTags[1];

                Finding detailsFindings = scaDetails.getFinding();
                Package vulnerabilityPackage = scaDetails.getVulnerabilityPackage();

                return getJiraScaSummaryIssueKey(request, issuePrefix, issuePostfix, detailsFindings, vulnerabilityPackage);
            case "CUSTOM":
                return getCustomScaSummaryIssueKey(request, scaDetails);
            default:
                throw new NotImplementedException("Summary issue key wasn't implemented yet for bug type: {}", bugType);
        }
    }

    private static void setSASTMDBody(ScanResults.XIssue issue, String branch, String fileUrl, FlowProperties flowProperties, StringBuilder body) {
        log.debug("Building MD body for SAST scanner");
        body.append(String.format(ISSUE_BODY, issue.getVulnerability(), issue.getFilename(), branch)).append(CRLF).append(CRLF);
        if(!ScanUtils.empty(issue.getDescription())) {
            body.append("*").append(issue.getDescription().trim()).append("*").append(CRLF).append(CRLF);
        }
        if(!ScanUtils.empty(issue.getSeverity())) {
            body.append(SEVERITY).append(issue.getSeverity()).append(CRLF).append(CRLF);
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
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
            Map<Integer, ScanResults.IssueDetails> fpIssues = issue.getDetails().entrySet().stream()
                    .filter(x -> x.getKey( ) != null && x.getValue() != null && x.getValue().isFalsePositive())
                    .sorted(comparingByKey())
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
            if(!trueIssues.isEmpty()) {
                body.append("Lines: ");
                for (Entry<Integer, ScanResults.IssueDetails> entry : trueIssues.entrySet()) {
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
                for (Entry<Integer, ScanResults.IssueDetails> entry : fpIssues.entrySet()) {
                    if (fileUrl != null) {  //[<line>](<url>)
                        body.append("[").append(entry.getKey()).append("](").append(fileUrl).append("#L").append(entry.getKey()).append(") ");
                    } else { //if the fileUrl is not provided, simply putting the line number (no link) - ADO for example
                        body.append(entry.getKey()).append(" ");
                    }
                }
                body.append(CRLF).append(CRLF);
            }
            for (Entry<Integer, ScanResults.IssueDetails> entry : trueIssues.entrySet()) {
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
                appendOsaDetails(body, o);
                body.append("```");
                body.append(CRLF);
            }
        }
    }

    private static void setSCAMDBody(String branch, StringBuilder body, List<ScanResults.ScaDetails> scaDetails) {
        log.debug("Building MD body for SCA scanner");
        scaDetails.stream().findAny().ifPresent(any -> {
            body.append("**Description**").append(CRLF).append(CRLF);
            body.append(any.getFinding().getDescription()).append(CRLF).append(CRLF);
            body.append(String.format(SCATicketingConstants.SCA_CUSTOM_ISSUE_BODY, any.getFinding().getSeverity(),
                    any.getVulnerabilityPackage().getName(), branch)).append(CRLF).append(CRLF);

            Map<String, String> scaDetailsMap = new LinkedHashMap<>();
            scaDetailsMap.put("**Vulnerability ID", any.getFinding().getId());
            scaDetailsMap.put("**Package Name", any.getVulnerabilityPackage().getName());
            scaDetailsMap.put("**Severity", any.getFinding().getSeverity().name());
            scaDetailsMap.put("**CVSS Score", String.valueOf(any.getFinding().getScore()));
            scaDetailsMap.put("**Publish Date", any.getFinding().getPublishDate());
            scaDetailsMap.put("**Current Package Version", any.getVulnerabilityPackage().getVersion());
            Optional.ofNullable(any.getFinding().getFixResolutionText()).ifPresent(f ->
                    scaDetailsMap.put("**Remediation Upgrade Recommendation", f)

            );

            scaDetailsMap.forEach((key, value) ->
                    body.append(key).append(":** ").append(value).append(CRLF).append(CRLF)
            );
            String findingLink = constructVulnerabilityUrl(any.getVulnerabilityLink(), any.getFinding());
            body.append("[Link To SCA](").append(findingLink).append(")").append(CRLF).append(CRLF);

            String cveName = any.getFinding().getCveName();
            if (!empty(cveName)) {
                body.append("[Reference – NVD link](").append(NVD_URL_PREFIX).append(cveName).append(")").append(ScanUtils.CRLF).append(ScanUtils.CRLF);
            }
        });
    }

    private static void setSASTHtmlBody(ScanResults.XIssue issue, FlowProperties flowProperties, String branch, StringBuilder body) {
        body.append(String.format(ISSUE_BODY, issue.getVulnerability(), issue.getFilename(), branch)).append(CRLF);

        if(!ScanUtils.empty(issue.getDescription())) {
            body.append(ITALIC_OPENING_DIV).append(issue.getDescription().trim()).append(ITALIC_CLOSING_DIV);
        }
        body.append(CRLF);
        if(!ScanUtils.empty(issue.getSeverity())) {
            body.append("<div><b>Severity:</b> ").append(issue.getSeverity()).append(DIV_CLOSING_TAG);
        }
        if(!ScanUtils.empty(issue.getCwe())) {
            body.append("<div><b>CWE:</b>").append(issue.getCwe()).append(DIV_CLOSING_TAG);
            if(!ScanUtils.empty(flowProperties.getMitreUrl())) {
                body.append(DIV_A_HREF).append(
                        String.format(
                                flowProperties.getMitreUrl(),
                                issue.getCwe()
                        )
                ).append("\'>Vulnerability details and guidance</a></div>");
            }
        }
        if(!ScanUtils.empty(flowProperties.getWikiUrl())) {
            body.append(DIV_A_HREF).append(flowProperties.getWikiUrl()).append("\'>Internal Guidance</a></div>");
        }
        if(!ScanUtils.empty(issue.getLink())){
            body.append(DIV_A_HREF).append(issue.getLink()).append("\'>Checkmarx</a></div>");
        }
        if(issue.getDetails() != null && !issue.getDetails().isEmpty()) {
            Map<Integer, ScanResults.IssueDetails> trueIssues = issue.getDetails().entrySet().stream()
                    .filter(x -> x.getKey( ) != null && x.getValue() != null && !x.getValue().isFalsePositive())
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
            Map<Integer, ScanResults.IssueDetails> fpIssues = issue.getDetails().entrySet().stream()
                    .filter(x -> x.getKey( ) != null && x.getValue() != null && x.getValue().isFalsePositive())
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
            if(!trueIssues.isEmpty()) {
                body.append("<div><b>Lines: </b>");
                for (Entry<Integer, ScanResults.IssueDetails> entry : trueIssues.entrySet()) {
                    body.append(entry.getKey()).append(" ");
                }
                body.append(DIV_CLOSING_TAG);
            }
            if(flowProperties.isListFalsePositives() && !fpIssues.isEmpty()) {//List the false positives / not exploitable
                body.append("<div><b>Lines Marked Not Exploitable: </b>");
                for (Entry<Integer, ScanResults.IssueDetails> entry : fpIssues.entrySet()) {
                    body.append(entry.getKey()).append(" ");
                }
                body.append(DIV_CLOSING_TAG);
            }
            for (Entry<Integer, ScanResults.IssueDetails> entry : trueIssues.entrySet()) {
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
                appendOsaDetails(body, o);
                body.append("</div></code></pre><div>");
                body.append(CRLF);
            }
        }
    }

    private static void setSCAHtmlBody(ScanResults.XIssue issue, ScanRequest request, StringBuilder body) {
        log.debug("Building HTML body for SCA scanner");
        issue.getScaDetails().stream().findAny().ifPresent(any -> {
            body.append(ITALIC_OPENING_DIV).append(any.getFinding().getDescription())
                    .append(ITALIC_CLOSING_DIV).append(LINE_BREAK);
            body.append(String.format(SCATicketingConstants.SCA_HTML_ISSUE_BODY, any.getFinding().getSeverity(),
                    any.getVulnerabilityPackage().getName(), request.getBranch()))
                    .append(DIV_CLOSING_TAG).append(LINE_BREAK);
        });

        Map<String, String> scaDetailsMap = new LinkedHashMap<>();
        issue.getScaDetails().stream().findAny().ifPresent(any -> {
            scaDetailsMap.put("<b>Vulnerability ID", any.getFinding().getId());
            scaDetailsMap.put("<b>Package Name", any.getVulnerabilityPackage().getName());
            scaDetailsMap.put("<b>Severity", any.getFinding().getSeverity().name());
            scaDetailsMap.put("<b>CVSS Score", String.valueOf(any.getFinding().getScore()));
            scaDetailsMap.put("<b>Publish Date", any.getFinding().getPublishDate());
            scaDetailsMap.put("<b>Current Package Version", any.getVulnerabilityPackage().getVersion());
            Optional.ofNullable(any.getFinding().getFixResolutionText()).ifPresent(f ->
                    scaDetailsMap.put("<b>Remediation Upgrade Recommendation", f)

            );

            scaDetailsMap.forEach((key, value) ->
                    body.append(key).append(":</b> ").append(value).append(LINE_BREAK)
            );

            String findingLink = ScanUtils.constructVulnerabilityUrl(any.getVulnerabilityLink(), any.getFinding());
            body.append(DIV_A_HREF).append(findingLink).append("\'>Link To SCA</a></div>");

            String cveName = any.getFinding().getCveName();
            if (!ScanUtils.empty(cveName)) {
                body.append(DIV_A_HREF).append(NVD_URL_PREFIX).append(cveName).append("\'>Reference – NVD link</a></div>");
            }
        });
    }

    private static String getCustomScaSummaryIssueKey(ScanRequest request, ScanResults.ScaDetails scaDetails) {
        return String.format(SCATicketingConstants.SCA_SUMMARY_CUSTOM_ISSUE_KEY, scaDetails.getFinding().getSeverity(),
                scaDetails.getFinding().getScore(), scaDetails.getFinding().getId(),
                scaDetails.getVulnerabilityPackage().getName(),
                scaDetails.getVulnerabilityPackage().getVersion(), request.getRepoName(), request.getBranch());
    }

    private static String getJiraScaSummaryIssueKey(ScanRequest request, String issuePrefix, String issuePostfix, Finding detailsFindings, Package vulnerabilityPackage) {
        return String.format(SCATicketingConstants.SCA_JIRA_ISSUE_KEY, issuePrefix, detailsFindings.getSeverity(),
                detailsFindings.getScore(), detailsFindings.getId(),
                vulnerabilityPackage.getName(),
                vulnerabilityPackage.getVersion(), request.getRepoName(), request.getBranch(), issuePostfix);
    }

    private static void appendOsaDetails(StringBuilder body, ScanResults.OsaDetails o) {
        if (!ScanUtils.empty(o.getSeverity())) {
            body.append(SEVERITY).append(o.getSeverity()).append(CRLF);
        }
        if (!ScanUtils.empty(o.getVersion())) {
            body.append(VERSION).append(o.getVersion()).append(CRLF);
        }
        if (!ScanUtils.empty(o.getDescription())) {
            body.append(DESCRIPTION).append(o.getDescription()).append(CRLF);
        }
        if (!ScanUtils.empty(o.getRecommendation())) {
            body.append(RECOMMENDATION).append(o.getRecommendation()).append(CRLF);
        }
        if (!ScanUtils.empty(o.getUrl())) {
            body.append(URL).append(o.getUrl());
        }
    }

    /**
     * TODO  BB has a different format
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

    public static String cleanStringUTF8(String dirty){
        if (log.isDebugEnabled()) {
            log.debug(String.valueOf(dirty.length()));
        }
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
            body.append(SEVERITY).append(issue.getSeverity()).append(CRLF);
        }
        if(!ScanUtils.empty(issue.getCwe())) {
            body.append("CWE: ").append(issue.getCwe()).append(CRLF);
            if(!ScanUtils.empty(flowProperties.getMitreUrl())) {
                body.append(DETAILS)
                        .append(
                        String.format(
                                flowProperties.getMitreUrl(),
                                issue.getCwe()
                        )
                ).append(" - Vulnerability details and guidance").append(CRLF);
            }
        }
        if(!ScanUtils.empty(flowProperties.getWikiUrl())) {
            body.append(DETAILS).append(flowProperties.getWikiUrl()).append(" - Internal Guidance ").append(CRLF);
        }
        if(!ScanUtils.empty(issue.getLink())){
            body.append(DETAILS).append(issue.getLink()).append(" - Checkmarx").append(CRLF);
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
                appendOsaDetails(body, o);
                body.append(CRLF);
            }
        }
        return body.toString();
    }

    private static List<Finding> getFindingsListBySeverity(List<Finding> findingList, Filter.Severity severity) {
        return findingList.stream()
                .filter(f -> f.getSeverity().name().equals(severity.name()))
                .collect(Collectors.toList());
    }

    private static Package getScaPackageByFinding(List<Package> packageList, Finding finding) {
        return packageList.stream().filter(p -> p.id.equals(finding.getPackageId())).findFirst().orElse(new Package());
    }

    private static String extractPackageNameFromFindings(SCAResults r, Finding f) {
        return r.getPackages().stream().filter(p -> p.id.equals(f.getPackageId())).map(Package::getName).findFirst().orElse("");
    }

    private static String extractPackageVersionFromFindings(SCAResults r, Finding f) {
        return r.getPackages().stream().filter(p -> p.id.equals(f.getPackageId())).map(Package::getVersion).findFirst().orElse("");
    }

    private static List<ScanResults.ScaDetails> getScaDetailsListBySeverity(SCAResults scaResults, List<Finding> scaFindingsBySeverity) {
        List<ScanResults.ScaDetails> scaDetailsList = new ArrayList<>();

        scaFindingsBySeverity.forEach(f -> {
            ScanResults.ScaDetails scaDetails = ScanResults.ScaDetails.builder()
                    .finding(f)
                    .vulnerabilityPackage(getScaPackageByFinding(scaResults.getPackages(), f))
                    .vulnerabilityLink(constructVulnerabilityUrl(scaResults.getWebReportLink(), f))
                    .build();

            scaDetailsList.add(scaDetails);
        });
        return scaDetailsList;
    }

    public static String constructVulnerabilityUrl(String allVulnerabilitiesReportUrl, Finding finding) {
        StringBuilder vulnerabilityUrl = new StringBuilder();
        String urlColonEncode = "";

        try {
            urlColonEncode = URLEncoder.encode(":", StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            log.error("Encoding error: {}", e.getMessage());
        }

        vulnerabilityUrl.append(allVulnerabilitiesReportUrl).append("/vulnerabilities/");
        String urlCompatiblePackageId = finding.getPackageId().replace(":", urlColonEncode);

        vulnerabilityUrl.append(finding.getId())
                .append(urlColonEncode).append(urlCompatiblePackageId).append("/vulnerabilityDetails");

        return vulnerabilityUrl.toString();
    }

    public static String getStringWithEncodedCharacter(String str)
    {
        String encodedString = "";
        if (!ScanUtils.empty(str))
        {
            try {
                encodedString = URLEncoder.encode(str, StandardCharsets.UTF_8.toString());
            } catch (UnsupportedEncodingException e) {
                log.error("Encoding error:", e);
            }
        }
        return encodedString;
    }
}
