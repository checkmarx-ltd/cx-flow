package com.checkmarx.flow.utils;


import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.constants.SCATicketingConstants;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.Field;
import com.checkmarx.flow.dto.FlowOverride;
import com.checkmarx.flow.dto.RepoIssue;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaRuntimeException;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.dto.sast.Filter;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.sca.SCAResults;
import com.checkmarx.sdk.dto.ast.report.FindingNode;
import com.checkmarx.sdk.dto.cx.CxScanSummary;

import com.checkmarx.sdk.dto.sca.report.Finding;
import com.checkmarx.sdk.dto.sca.report.Package;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;


public class ScanUtils {


    public static final String RUNNING = "running";

    public static final String ISSUE_TITLE_KEY_WITH_BRANCH = "%s %s @ %s [%s]";
    public static final String ISSUE_TITLE_KEY = "%s %s @ %s";

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ScanUtils.class);

    public static final String RECOMMENDED_FIX = "recommendedFix";
    public static final String URL = "URL: ";

    public static final String SEVERITY = "Severity: ";

    private static final String TBD = "TBD";
    private static final String CATEGORIES = "categories";
    private static final String STATE = "state";
    private static final String SOURCE = "source";
    private static final String SINK = "sink";
    private static final String RESULTS = "results";
    private static final String SCAN_ID = "scanId";


    private ScanUtils() {
        // this is to hide the public constructor
    }
    /**
     * Function used to determine if file extension of full filename is preset in list
     *
     * @param value - extension of file, or full filename
     */
    public static boolean fileListContains(List<String> list, String value){
        for(String s: list){
            if(s.endsWith(value)){
                return true;
            }
        }
        return false;
    }
    
    public static boolean isSAST(ScanResults.XIssue issue) {
        return issue.getScaDetails() == null;
    }

    public static List<ScanResults.XIssue> setASTXIssuesInScanResults(ScanResults results) {
        List<ScanResults.XIssue> issueList = new ArrayList<>();
        HashMap<String, Object> mapAdditionalDetails = new HashMap<>();
        ScanResults.XIssue.XIssueBuilder xIssueBuilder = ScanResults.XIssue.builder();

        mapAdditionalDetails.put(SCAN_ID, results.getAstResults().getScanId());
        results.setAdditionalDetails(mapAdditionalDetails);
        
        setAstScanSummary(results);

        Map<String, Integer> severityCount = new HashMap<>();

        List<com.checkmarx.sdk.dto.ast.report.Finding> findings = results.getAstResults().getFindings();
        findings.forEach(finding -> {
            
            xIssueBuilder.cwe("" + finding.getCweID());
            xIssueBuilder.severity(finding.getSeverity());
            xIssueBuilder.vulnerability(finding.getQueryName());
            if(!finding.getNodes().isEmpty()) {
                xIssueBuilder.file(finding.getNodes().get(0).getFileName());
            }
            xIssueBuilder.vulnerabilityStatus(finding.getState());
            xIssueBuilder.similarityId("" + finding.getSimilarityID());
            xIssueBuilder.description(finding.getDescription());
                 
            Map<Integer, ScanResults.IssueDetails> details = new HashMap<>();
            ScanResults.IssueDetails issueDetails = new ScanResults.IssueDetails()
                    .falsePositive(Boolean.FALSE);
            
            details.put(finding.getNodes().get(0).getLine(), issueDetails);
            xIssueBuilder.details(details);
            
            // Add additional details
            Map<String, Object> additionalDetails = getAdditionalIssueDetails(finding);
            xIssueBuilder.additionalDetails(additionalDetails);
            xIssueBuilder.groupBySeverity(false);
            
            ScanResults.XIssue issue = xIssueBuilder.build();

            removeDuplicateIssues(issueList, issue, issue.getDetails(), severityCount);
            
        });

        results.getAdditionalDetails().put(Constants.SUMMARY_KEY, severityCount);
        results.setXIssues(issueList);
        
        return issueList;
    }

    private static void removeDuplicateIssues(List<ScanResults.XIssue> issueList, ScanResults.XIssue issue, Map<Integer, ScanResults.IssueDetails> details, Map<String, Integer> severityMap) {
        
        if (issueList.contains(issue)) {
            ScanResults.XIssue existingIssue = issueList.get(issueList.indexOf(issue));
            existingIssue.getDetails().putAll(details);
        } else {
            Integer severityCount = Optional.ofNullable(severityMap.get(issue.getSeverity())).orElse(0) ;
            severityMap.put(issue.getSeverity(), ++severityCount);
            issueList.add(issue);
        }
    }

    private static void setAstScanSummary( ScanResults results) {
        CxScanSummary scanSummary = new CxScanSummary();
        scanSummary.setHighSeverity(results.getAstResults().getSummary().getHighVulnerabilityCount() );
        scanSummary.setMediumSeverity(results.getAstResults().getSummary().getMediumVulnerabilityCount() );
        scanSummary.setLowSeverity(results.getAstResults().getSummary().getLowVulnerabilityCount() );
        scanSummary.setInfoSeverity(0);
        results.setLink(results.getAstResults().getWebReportLink());
        results.setScanSummary( scanSummary);
        
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
                                .groupBySeverity(false)
                                .build();
                        issue.setScaDetails(getScaDetailsListBySeverity(scaResults, v));
                        issue.setFalsePositiveCount(getFalsePositiveCount(v));
                        issueList.add(issue);
                    });
                });
        return issueList;
    }

    /**
     * Check if string is empty or null
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

  

    private static Map<String, Object> getAdditionalIssueDetails(com.checkmarx.sdk.dto.ast.report.Finding finding) {
        Map<String, Object> additionalDetails = new HashMap<>();
         additionalDetails.put(CATEGORIES, TBD);
        additionalDetails.put(RECOMMENDED_FIX, TBD);

        List<Map<String, Object>> results = new ArrayList<>();
        // Source / Sink data
        Map<String, Object> result = new HashMap<>();
        result.put(STATE, TBD);

        result.put(SOURCE, getNodeData(finding.getNodes(), 0));
        result.put(SINK, getNodeData(finding.getNodes(), finding.getNodes().size() - 1)); // Last node in dataFlow
        
        results.add(result);
        additionalDetails.put(RESULTS, results);
        return additionalDetails;
    }

    private static Map<String, String> getNodeData(List<FindingNode> nodes, int nodeIndex) {
        // Node data: file/line/object
        Map<String, String> nodeData = new HashMap<>();
        FindingNode node = nodes.get(nodeIndex);
        nodeData.put("file", node.getFileName());
        nodeData.put("line", "" + node.getLine());
        nodeData.put("column", "" + node.getColumn());
        nodeData.put("object", node.getName());
        return nodeData;
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
                return repoUrl.concat("src/").concat(request.getHash()).concat("/").concat(filename);
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

                return anyEmpty(request.getNamespace(), request.getRepoName(), request.getBranch())
                ? getJiraScaSummaryIssueKeyWithoutBranch(request, issuePrefix, issuePostfix, detailsFindings, vulnerabilityPackage)
                : getJiraScaSummaryIssueKey(request, issuePrefix, issuePostfix, detailsFindings, vulnerabilityPackage);
            case "CUSTOM":
                return anyEmpty(request.getBranch(), request.getNamespace(), request.getRepoName())
                        ? getCustomScaSummaryIssueKeyWithoutBranch(request, scaDetails)
                        : getCustomScaSummaryIssueKey(request, scaDetails);

            default:
                throw new NotImplementedException("Summary issue key wasn't implemented yet for bug type: {}", bugType);
        }
    }

    private static String getCustomScaSummaryIssueKey(ScanRequest request, ScanResults.ScaDetails scaDetails) {
        String currentPackageVersion = getCurrentPackageVersion(scaDetails.getVulnerabilityPackage().getId());

        return String.format(SCATicketingConstants.SCA_SUMMARY_CUSTOM_ISSUE_KEY, request.getProduct().getProduct(),
                scaDetails.getFinding().getId(),
                removePackageCurrentVersionFromPath(scaDetails.getVulnerabilityPackage().getId(), currentPackageVersion),
                currentPackageVersion, request.getRepoName(), request.getBranch());
    }

    private static String getCustomScaSummaryIssueKeyWithoutBranch(ScanRequest request, ScanResults.ScaDetails scaDetails) {
        String currentPackageVersion = getCurrentPackageVersion(scaDetails.getVulnerabilityPackage().getId());

        return String.format(SCATicketingConstants.SCA_SUMMARY_CUSTOM_ISSUE_KEY_WITHOUT_BRANCH, request.getProduct().getProduct(),
                scaDetails.getFinding().getId(),
                removePackageCurrentVersionFromPath(scaDetails.getVulnerabilityPackage().getId(), currentPackageVersion),
                currentPackageVersion, request.getRepoName());
    }

    private static String getJiraScaSummaryIssueKey(ScanRequest request, String issuePrefix, String issuePostfix, Finding detailsFindings, Package vulnerabilityPackage) {
        String currentPackageVersion = getCurrentPackageVersion(vulnerabilityPackage.getId());

        return String.format(SCATicketingConstants.SCA_JIRA_ISSUE_KEY, issuePrefix,
                detailsFindings.getId(),
                removePackageCurrentVersionFromPath(vulnerabilityPackage.getId(), currentPackageVersion),
                currentPackageVersion, request.getRepoName(), request.getBranch(), issuePostfix);
    }

    private static String getJiraScaSummaryIssueKeyWithoutBranch(ScanRequest request, String issuePrefix, String issuePostfix, Finding detailsFindings, Package vulnerabilityPackage) {
        String currentPackageVersion = getCurrentPackageVersion(vulnerabilityPackage.getId());

        return String.format(SCATicketingConstants.SCA_JIRA_ISSUE_KEY_WITHOUT_BRANCH, issuePrefix,
                detailsFindings.getId(),
                removePackageCurrentVersionFromPath(vulnerabilityPackage.getId(), currentPackageVersion),
                currentPackageVersion, request.getRepoName(), issuePostfix);
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
            log.debug(String.format("Could not parse given URL: %s", url), e);
        }
        return hostWithProtocol;
    }
    public static String getBranchFromRef(String ref){
        // refs/head/master (get 2nd position of /
        int index = StringUtils.ordinalIndexOf(ref, "/", 2);
        if(index < 0) return ref;
        return ref.substring(index+1);
    }


    private static List<Finding> getFindingsListBySeverity(List<Finding> findingList, Filter.Severity severity) {
        return findingList.stream()
                .filter(f -> f.getSeverity().name().equals(severity.name()))
                .collect(Collectors.toList());
    }

    private static Package getScaPackageByFinding(List<Package> packageList, Finding finding) {
        return packageList.stream().filter(p -> p.getId().equals(finding.getPackageId())).findFirst().orElse(new Package());
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

    private static int getFalsePositiveCount(List<Finding> v) {
    	
    	long falsePositiveCount = v.stream().filter(f-> f.isIgnored()).collect(Collectors.counting());
        	
        return (int)falsePositiveCount;
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

    public static String constructVulnerabilityUrl(String allVulnerabilitiesReportUrl, Finding finding) {
        StringBuilder vulnerabilityUrl = new StringBuilder();
        String urlColonEncode = "";

        try {
            urlColonEncode = URLEncoder.encode(":", UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            log.error("Encoding error: {}", e.getMessage());
        }

        vulnerabilityUrl.append(allVulnerabilitiesReportUrl).append("/vulnerabilities/");
        String urlCompatiblePackageId = finding.getPackageId().replace(":", urlColonEncode);

        vulnerabilityUrl.append(finding.getId())
                .append(urlColonEncode).append(urlCompatiblePackageId).append("/vulnerabilityDetails");

        return vulnerabilityUrl.toString();
    }

    /**
     * Returns the string with first letter in uppercase and the remainder in lowercase
     */
    public static String toProperCase(String s) {
        return s.substring(0, 1).toUpperCase() +
                s.substring(1).toLowerCase();
    }

    public static String convertMapToString(Map<?, ?> map) {
        return map.keySet().stream()
                .map(key -> key + "=" + map.get(key))
                .collect(Collectors.joining(", ", "{", "}"));
    }

    public static String removePackageCurrentVersionFromPath(String packageName, String currentPackageVersion) {
        return packageName.replace("-" + currentPackageVersion, "");
    }

    public static String getCurrentPackageVersion(String packageName) {
        return StringUtils.substringAfterLast(packageName, "-");
    }
}
