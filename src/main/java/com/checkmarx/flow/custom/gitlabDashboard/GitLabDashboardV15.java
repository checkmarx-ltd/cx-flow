package com.checkmarx.flow.custom.gitlabDashboard;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.gitlabdashboardv15.SAST.*;
import com.checkmarx.flow.dto.gitlabdashboardv15.SAST.Analyzer;
import com.checkmarx.flow.dto.gitlabdashboardv15.SAST.Flag;
import com.checkmarx.flow.dto.gitlabdashboardv15.SAST.Identifier;
import com.checkmarx.flow.dto.gitlabdashboardv15.SAST.Items;
import com.checkmarx.flow.dto.gitlabdashboardv15.SAST.Link;
import com.checkmarx.flow.dto.gitlabdashboardv15.SAST.Location;
import com.checkmarx.flow.dto.gitlabdashboardv15.SAST.Scan;
import com.checkmarx.flow.dto.gitlabdashboardv15.SAST.Scanner;
import com.checkmarx.flow.dto.gitlabdashboardv15.SAST.Signature;
import com.checkmarx.flow.dto.gitlabdashboardv15.SAST.Tracking;
import com.checkmarx.flow.dto.gitlabdashboardv15.SAST.Vendor;
import com.checkmarx.flow.dto.gitlabdashboardv15.SAST.Vendor__1;
import com.checkmarx.flow.dto.gitlabdashboardv15.SAST.Vulnerability;
import com.checkmarx.flow.dto.gitlabdashboardv15.SCA.*;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.sdk.dto.sca.SCAResults;
import com.checkmarx.sdk.dto.sca.report.Finding;
import com.checkmarx.sdk.dto.sca.report.Package;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class GitLabDashboardV15 implements GitLabDashboardStrategy {
    private static final String CHECKMARX = "Checkmarx";
    private final FlowProperties flowProperties;
    private static final SimpleDateFormat GITLAB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");


    @Override
    public void generateSastDashboard(ScanRequest request, ScanResults results) throws MachinaException {
        getSastGitLabResultsDashboard(request, results);
    }

    @Override
    public void generateScaDashboard(ScanRequest request, ScanResults results) throws MachinaException {
        getScaGitLabResultsDashboard(request, results);
    }

    private void getSastGitLabResultsDashboard(ScanRequest request, ScanResults results) throws MachinaException {
        List<Vulnerability> vulns = new ArrayList<>();
        for(ScanResults.XIssue issue : results.getXIssues()) {
            if(issue.getDetails() != null) {
                issue.getDetails().forEach((k, v) -> {
                    Vulnerability vuln= Vulnerability.builder()
                            .id(issue.getVulnerability().concat(":").concat(issue.getFilename()).concat(":").concat(k.toString()))
                            .name(issue.getVulnerability())
                            .description(issue.getDescription())
                            .severity(Vulnerability.Severity.valueOf(StringUtils.capitalize(issue.getSeverity().toLowerCase())))
                            .solution(issue.getLink())
                            .identifiers(getIdentifiersGitLabDashBoard(issue,k))
                            .links(getLinksGitLabDashBoard(issue))//New Added
                            .tracking(Tracking.builder()
                                    .lstItems(getItemsGitLabDashBoard(issue))
                                    .build())
                            .flags(getFlagsGitLabDashboard(issue))
                            .location(
                                    Location.builder()
                                            .file(issue.getFilename())
                                            .startLine(k)
                                            .endLine(k)
                                            ._class(issue.getFilename())
                                            .build()
                            ).details(getDetailsGitLabDashBoard(issue))
                            .build();
                    // Java
                    vulns.add(vuln);
                });
            }
        }
        Gitlabdashboard report  = Gitlabdashboard.builder()
                .vulnerabilities(vulns)
                .version("15.2.3")
                .scan(Scan.builder()
                        .startTime(calculateEndTime(String.valueOf(results.getAdditionalDetails().get("scanStartDate")),"0"))
                        .endTime(calculateEndTime(results.getReportCreationTime(),results.getScanTime()))
                        .analyzer(Analyzer.builder()
                                .id(results.getProjectId())
                                .name("sast-Checkmarx")
                                .vendor(Vendor.builder().build())
                                .version(results.getVersion()).
                                build())
                        .scanner(Scanner.builder()
                                .id(results.getProjectId())
                                .name("sast-Checkmarx")
                                .vendor(Vendor__1.builder().build())
                                .version(results.getVersion()).build())
                        .status(Scan.Status.valueOf("SUCCESS"))
                        .type(Scan.Type.SAST)
                        .build())
                .build();

        writeJsonOutput(request, report, log);

    }


    private Details getDetailsGitLabDashBoard(ScanResults.XIssue issue) {
        Object resultsObj = issue.getAdditionalDetails().get("results");

        if (!(resultsObj instanceof List)) {
            return buildEmptyDetails();
        }

        List<Map<String, Object>> results = (List<Map<String, Object>>) resultsObj;
        List<List<Item>> allFlows = new ArrayList<>();

        for (Map<String, Object> result : results) {
            List<Item> sortedFlowItems = buildSortedFlowItems(result);
            if (!sortedFlowItems.isEmpty()) {
                allFlows.add(sortedFlowItems);
            }
        }

        List<Item> flattenedList = allFlows.stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());

        List<List<Item>> finalItems = Collections.singletonList(flattenedList);
        return buildDetails(finalItems);
    }



    /**
     * Builds a single flow (inner list) of Items.
     * - Removes duplicates for propagation nodes based on file name + line number.
     * - Removes propagation if a source or sink already exists at that location.
     * - Sorts nodes by logical order: source → propagation → sink.
     */
    private List<Item> buildSortedFlowItems(Map<String, Object> result) {
        if (result == null || result.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Item> flowItemMap = new HashMap<>();

        for (Map.Entry<String, Object> entry : result.entrySet()) {
            if (entry.getValue() instanceof Map) {
                Map<String, Object> valueMap = (Map<String, Object>) entry.getValue();
                createItemFromMap(entry.getKey(), valueMap).ifPresent(item -> {
                    String file = item.getFileLocation().getFileName();
                    int line = item.getFileLocation().getLineStart();
                    String key = file + ":" + line;
                    String nodeType = item.getNodeType().toLowerCase();

                    // If a source or sink already exists for same line, skip propagation
                    if ("propagation".equals(nodeType)) {
                        Item existing = flowItemMap.get(key);
                        if (existing == null || "propagation".equals(existing.getNodeType().toLowerCase())) {
                            flowItemMap.put(key, item);
                        }
                    } else {
                        // For source or sink → always prefer them over propagation
                        flowItemMap.put(key, item);
                    }
                });
            }
        }

        // Build sorted list
        List<Item> sortedList = new ArrayList<>(flowItemMap.values());
        sortedList.sort(Comparator.comparingInt(this::nodeOrderIndex));
        return sortedList;
    }

    /**
     * Safely creates an Item from raw map values, returning Optional.empty() if invalid.
     */
    private Optional<Item> createItemFromMap(String key, Map<String, Object> valueMap) {
        if (valueMap == null) return Optional.empty();

        String file = safeString(valueMap.get("file"));
        String lineStr = safeString(valueMap.get("line"));
        if (file.isEmpty() || lineStr.isEmpty()) return Optional.empty();

        int lineNumber = safeParseInt(lineStr);
        if (lineNumber <= 0) return Optional.empty();

        String nodeType = normalizeNodeType(key);

        FileLocation fileLocation = FileLocation.builder()
                .fileName(file)
                .lineStart(lineNumber)
                .lineEnd(lineNumber)
                .type("file-location")
                .build();

        Item item = Item.builder()
                .fileLocation(fileLocation)
                .nodeType(nodeType)
                .type("code-flow-node")
                .build();

        return Optional.of(item);
    }

    /**
     * Maps the node key to its normalized type (source, propagation, sink).
     */
    private String normalizeNodeType(String key) {
        if (key == null) return "propagation";
        key = key.trim().toLowerCase();
        return (key.equals("source") || key.equals("sink")) ? key : "propagation";
    }

    /**
     * Returns index for sorting based on nodeType order.
     */
    private int nodeOrderIndex(Item item) {
        List<String> order = Arrays.asList("source", "propagation", "sink");
        int idx = order.indexOf(item.getNodeType().toLowerCase());
        return idx == -1 ? order.size() : idx;
    }

    /**
     * Safely converts object to string.
     */
    private String safeString(Object obj) {
        return obj == null ? "" : obj.toString().trim();
    }

    /**
     * Safely parses integer; returns default if invalid.
     */
    private int safeParseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    /**
     * Builds the final Details object with flattened items.
     */
    private Details buildDetails(List<List<Item>> finalItems) {
        return Details.builder()
                .codeFlows(CodeFlows.builder()
                        .items(finalItems)
                        .name("code_flows")
                        .type("code-flows")
                        .build())
                .build();
    }

    /**
     * Returns an empty Details object safely.
     */
    private Details buildEmptyDetails() {
        return buildDetails(Collections.singletonList(Collections.emptyList()));
    }



    private void getScaGitLabResultsDashboard(ScanRequest request, ScanResults results) throws MachinaException {

        List<DependencyFile> dependencyFilesLst= new ArrayList<>();
        List<com.checkmarx.flow.dto.gitlabdashboardv15.SCA.Vulnerability> vulns = new ArrayList<>();
        com.checkmarx.flow.dto.gitlabdashboardv15.SCA.Scanner scanner =
                com.checkmarx.flow.dto.gitlabdashboardv15.SCA.Scanner.builder().id("Checkmarx-SCA").name("Checkmarx-SCA").build();
        List<Finding> findings = results.getScaResults().getFindings();
        List<com.checkmarx.sdk.dto.sca.report.Package> packages = new ArrayList<>(results.getScaResults()
                .getPackages());
        Map<String, com.checkmarx.sdk.dto.sca.report.Package> map = new HashMap<>();
        for (com.checkmarx.sdk.dto.sca.report.Package p : packages) map.put(p.getId(), p);
        for (Finding finding:findings){
            Package indPackage = map.get(finding.getPackageId());
            if(indPackage!=null){
                for(String loc : indPackage.getLocations()) {
                    vulns.add(com.checkmarx.flow.dto.gitlabdashboardv15.SCA.Vulnerability.builder()
                            .id(UUID.nameUUIDFromBytes(finding.getPackageId().concat("@").concat(loc).concat(":").concat(finding.getCveName()).getBytes()).toString())
                            .name(finding.getPackageId().concat("@").concat(loc).concat(":").concat(finding.getCveName()))
                            .description(finding.getDescription())
                            .severity(com.checkmarx.flow.dto.gitlabdashboardv15.SCA.Vulnerability.Severity.fromValue(
                                    StringUtils.capitalize(String.valueOf(finding.getSeverity()).toLowerCase())
                            ))
                            .solution(finding.getFixResolutionText())
                            .identifiers(getScaIdentifiersGitLabDashBoard(results.getScaResults(),finding))
                            .links(getLinksSCAGitlabDashboard(finding))
                            .tracking(com.checkmarx.flow.dto.gitlabdashboardv15.SCA.Tracking.builder()
                                    .lstItems(getItemsScaGitLabDashBoard(finding.getCveName()))
                                    .build())
                            .flags(getSCAFlagsGitLabDashboard(finding))
                            .location(com.checkmarx.flow.dto.gitlabdashboardv15.SCA.Location.builder().file(loc)
                                    .dependency(Dependency.builder()
                                            .dependencyPath(findDependencyPathGitLabDashBoard(indPackage.getDependencyPaths()))
                                            .direct(indPackage.isIsDirectDependency())
                                            ._package(com.checkmarx.flow.dto.gitlabdashboardv15.SCA.Package.builder().name(finding.getPackageId()).build())
                                            .version(finding.getPackageId().split("-")[finding.getPackageId().
                                                    split("-").length-1]).build()).build())
                            .build());
                }
                dependencyFilesLst.add(DependencyFile.builder().dependencies(findObjectDependencyGitLabDashBoard(packages)).path(indPackage.getPackageRepository()).packageManager(indPackage.getName()).build());
            }else{
                log.warn("Package not found for the finding: {}", finding.getPackageId());
            }
        }

        SCASecurityDashboard report  = SCASecurityDashboard.builder()
                .dependencyFiles(dependencyFilesLst)
                .version("15.2.3")
                .vulnerabilities(vulns)
                .scan(com.checkmarx.flow.dto.gitlabdashboardv15.SCA.Scan.builder()
                        .startTime(formatGitlabDate(results.getScaResults().getSummary().getCreatedOn(),2))
                        .endTime(formatGitlabDate(results.getScaResults().getSummary().getCreatedOn()))
                        .type(com.checkmarx.flow.dto.gitlabdashboardv15.SCA.Scan.Type.valueOf("DEPENDENCY_SCANNING"))
                        .status(com.checkmarx.flow.dto.gitlabdashboardv15.SCA.Scan.Status.valueOf("SUCCESS"))
                        .analyzer(com.checkmarx.flow.dto.gitlabdashboardv15.SCA.Analyzer.builder()
                                .name("SCA-Checkmarx")
                                .vendor(
                                        com.checkmarx.flow.dto.gitlabdashboardv15.SCA.Vendor.builder().build()
                                ).build())

                        .scanner((com.checkmarx.flow.dto.gitlabdashboardv15.SCA.Scanner.builder()
                                .id("1")
                                .name("SCA-Checkmarx")
                                .version("15.2.3")
                                .vendor(com.checkmarx.flow.dto.gitlabdashboardv15.SCA.Vendor__1.builder().build())
                                .build()))
                        .build())
                .schema("https://gitlab.com/gitlab-org/security-products/security-report-schemas/-/blob/v15.2.3/dist/dependency-scanning-report-format.json")
                .build();
        writeJsonOutput(request, report, log);
    }



    private String formatGitlabDate(String date) {
        if (date == null) return null;
        try {
            Date parsedDate = GITLAB_DATE_FORMAT.parse(date);
            return GITLAB_DATE_FORMAT.format(parsedDate);
        } catch (ParseException e) {
            log.error("Failed to parse date: {}", date, e);
            return null;
        }
    }

    private String formatGitlabDate(String date, int minutes) {
        if (date == null) return null;
        try {
            Date parsedDate = GITLAB_DATE_FORMAT.parse(date);
            Calendar cal = Calendar.getInstance();
            cal.setTime(parsedDate);
            cal.add(Calendar.MINUTE, -minutes);
            return GITLAB_DATE_FORMAT.format(cal.getTime());
        } catch (ParseException e) {
            log.error("Failed to parse date: {}", date, e);
            return null;
        }
    }
    private List<Identifier> getIdentifiersGitLabDashBoard(ScanResults.XIssue issue, Integer k){
        List<Identifier> identifiers = new ArrayList<>();

        identifiers.add(
                Identifier.builder()
                        .type("checkmarx_finding")
                        .name("Checkmarx-".concat(issue.getVulnerability()))
                        .value(issue.getVulnerability().concat(":").concat(issue.getFilename()).concat(":").concat(k.toString()))
                        .url(issue.getLink())
                        .build()
        );
        if (!ScanUtils.empty(flowProperties.getMitreUrl())) {
            identifiers.add(
                    Identifier.builder()
                            .type("cwe")
                            .name("CWE-".concat(issue.getCwe()))
                            .value(issue.getCwe())
                            .url(URI.create(String.format(flowProperties.getMitreUrl(), issue.getCwe())).toString())
                            .build()
            );
        }else {
            log.info("mitre-url property is empty");
        }

        return identifiers;
    }

    private List<Link> getLinksGitLabDashBoard(ScanResults.XIssue issue){
        List<Link> links = new ArrayList<>();
        issue.getAdditionalDetails().forEach((k, v) -> {
            if(checkingURI(String.valueOf(v))){
                links.add(
                        Link.builder()
                                .name("Checkmarx-".concat(k))
                                .url(String.valueOf(v))
                                .build());
            }
        });
        return links;
    }
    private List<Signature> getSignatureGitLabDashboard(){
        List<Signature> Signature = new ArrayList<>();
        Signature.add(com.checkmarx.flow.dto.gitlabdashboardv15.SAST.Signature.builder().build());
        return Signature;
    }

    private List<Items> getItemsGitLabDashBoard(ScanResults.XIssue issue){
        List<Items> Items = new ArrayList<>();
        Items.add(com.checkmarx.flow.dto.gitlabdashboardv15.SAST.Items.builder()
                .signatures(getSignatureGitLabDashboard())
                .file(issue.getFilename()).build());

        return Items;
    }

    private List<Flag> getFlagsGitLabDashboard(ScanResults.XIssue issue){
        List<Flag> flags = new ArrayList<>();
        if(issue.isAllFalsePositive()){
            flags.add(
                    Flag.builder()
                            .type(Flag.Type.valueOf("FLAGGED_AS_LIKELY_FALSE_POSITIVE"))
                            .origin("Cx-flow sast")
                            .description(issue.getDescription()).build());
        }
        return flags;
    }

    //SCA utils methods
    private List<com.checkmarx.flow.dto.gitlabdashboardv15.SCA.Identifier> getScaIdentifiersGitLabDashBoard(SCAResults results, Finding finding)  {
        List<com.checkmarx.flow.dto.gitlabdashboardv15.SCA.Identifier> identifiers = new ArrayList<>();

        identifiers.add(
                com.checkmarx.flow.dto.gitlabdashboardv15.SCA.Identifier.builder()
                        .type("checkmarx_finding")
                        .name(CHECKMARX.concat("-").concat(finding.getPackageId()))
                        .value(CHECKMARX.concat("-").concat(finding.getPackageId()))
                        .url(results.getWebReportLink())
                        .build()
        );
        if (!finding.getReferences().isEmpty()) {
            for(String reference:finding.getReferences()){
                identifiers.add(
                        com.checkmarx.flow.dto.gitlabdashboardv15.SCA.Identifier.builder()
                                .type("cve")
                                .name(finding.getCveName() != null ? finding.getCveName().concat("(").concat(reference).concat(")") : reference)
                                .value(finding.getCveName() != null ? finding.getCveName().concat("(").concat(reference).concat(")") : reference)
                                .url(reference)
                                .build()
                );
            }
        }


        return identifiers;
    }

    private List<com.checkmarx.flow.dto.gitlabdashboardv15.SCA.Items> getItemsScaGitLabDashBoard(String name){
        List<com.checkmarx.flow.dto.gitlabdashboardv15.SCA.Items> Items = new ArrayList<>();
        Items.add(com.checkmarx.flow.dto.gitlabdashboardv15.SCA.Items.builder()
                .signatures(getSignatureScaGitLabDashBoard())
                .file(name).build());

        return Items;
    }
    private List<com.checkmarx.flow.dto.gitlabdashboardv15.SCA.Link> getLinksSCAGitlabDashboard(Finding refrences){
        List<com.checkmarx.flow.dto.gitlabdashboardv15.SCA.Link> links = new ArrayList<>();
        refrences.getReferences().forEach((k) -> {
            links.add(
                    com.checkmarx.flow.dto.gitlabdashboardv15.SCA.Link.builder()
                            .name(refrences.getCveName() != null ? refrences.getCveName().concat("(").concat(k).concat(")") : k)
                            .url(k)
                            .build());

        });
        return links;
    }

    private List<com.checkmarx.flow.dto.gitlabdashboardv15.SCA.Flag> getSCAFlagsGitLabDashboard(Finding finding){
        List<com.checkmarx.flow.dto.gitlabdashboardv15.SCA.Flag> flags = new ArrayList<>();
        if(finding.isIgnored()){
            flags.add(
                    com.checkmarx.flow.dto.gitlabdashboardv15.SCA.Flag.builder()
                            .type(com.checkmarx.flow.dto.gitlabdashboardv15.SCA.Flag.Type.valueOf("FLAGGED_AS_LIKELY_FALSE_POSITIVE"))
                            .origin("SCA")
                            .description(finding.getDescription()).build());
        }
        return flags;
    }

    private List<com.checkmarx.flow.dto.gitlabdashboardv15.SCA.Signature> getSignatureScaGitLabDashBoard(){
        List<com.checkmarx.flow.dto.gitlabdashboardv15.SCA.Signature> Signature = new ArrayList<>();
        Signature.add(com.checkmarx.flow.dto.gitlabdashboardv15.SCA.Signature.builder().build());
        return Signature;
    }

    public static List<DependencyPath> findDependencyPathGitLabDashBoard(
            List<com.checkmarx.sdk.dto.sca.report.DependencyPath> dependencyPath ){
        List<DependencyPath> dependencyPathList = new ArrayList<>();
        dependencyPath.forEach((k)-> k.forEach((v)->{
            dependencyPathList.add(
                    DependencyPath.builder().build());

        }));
        return dependencyPathList;
    }

    private static List<Dependency__1> findObjectDependencyGitLabDashBoard(
            List<com.checkmarx.sdk.dto.sca.report.Package> dependencyPath ){
        List<Dependency__1> dependencyPathList = new ArrayList<>();
        dependencyPath.forEach((k)-> dependencyPathList.add(
                Dependency__1.builder()
                        ._package(Package__1.builder().name(k.getName()).build())
                        .version(k.getVersion())
                        .direct(k.isIsDirectDependency())
                        .dependencyPath(findeachDependencyPathGitlabDashBoard(k.getDependencyPaths()))
                        .build()));
        return dependencyPathList;
    }

    private static List<DependencyPath__1> findeachDependencyPathGitlabDashBoard(
            List<com.checkmarx.sdk.dto.sca.report.DependencyPath> dependencyPath ){
        List<DependencyPath__1> dependencyPathList = new ArrayList<>();
        dependencyPath.forEach((k)-> k.forEach((v)-> dependencyPathList.add(
                DependencyPath__1.builder()
                        .build())));
        return dependencyPathList;
    }

    public boolean checkingURI(String s){
        String URL_REGEX = "^((https?|ftp)://|(www|ftp)\\.)?[a-z0-9-]+(\\.[a-z0-9-]+)+([/?].*)?$";

        Pattern p = Pattern.compile(URL_REGEX);
        Matcher m = p.matcher(s);//replace with string to compare
        if(m.find()) {
            return  true;
        }
        return false;

    }

    public String calculateEndTime(String ReportCreationTime,String ScanTime){
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMM dd, yyyy HH:mm:ss a");
        Date date = null;
        try {
            date = sdf.parse(ReportCreationTime);


        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        long millis = date.getTime();
        //date.get
        String[] strngArr=(ScanTime.
                replace("h","")
                .replace("m","")
                .replace("s","")).split(":");

        long durationInMilli=(Integer.parseInt(strngArr[0])*3600*1000)+
                (Integer.parseInt(strngArr[0])*60*1000)
                +(Integer.parseInt(strngArr[0])*1000);

        long endTime=millis+durationInMilli;
        SimpleDateFormat formatter = new SimpleDateFormat("EEEE, MMM dd, yyyy HH:mm:ss a");

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(endTime);
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(calendar.getTime());
        return timeStamp;
    }
}

