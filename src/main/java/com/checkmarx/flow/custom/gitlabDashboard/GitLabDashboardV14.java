package com.checkmarx.flow.custom.gitlabDashboard;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.gitlabdashboardv14.Analyzer;
import com.checkmarx.flow.dto.gitlabdashboardv14.Flag;
import com.checkmarx.flow.dto.gitlabdashboardv14.Identifier;
import com.checkmarx.flow.dto.gitlabdashboardv14.Link;
import com.checkmarx.flow.dto.gitlabdashboardv14.Location;
import com.checkmarx.flow.dto.gitlabdashboardv14.*;
import com.checkmarx.flow.dto.gitlabdashboardv14.SCA.*;
import com.checkmarx.flow.dto.gitlabdashboardv14.SCA.Dependency;
import com.checkmarx.flow.dto.gitlabdashboardv14.SCA.DependencyFile;
import com.checkmarx.flow.dto.gitlabdashboardv14.SCA.Items;
import com.checkmarx.flow.dto.gitlabdashboardv14.SCA.Scanner;
import com.checkmarx.flow.dto.gitlabdashboardv14.SCA.Signature;
import com.checkmarx.flow.dto.gitlabdashboardv14.Scan;
import com.checkmarx.flow.dto.gitlabdashboardv14.Scanner__1;
import com.checkmarx.flow.dto.gitlabdashboardv14.Tracking;
import com.checkmarx.flow.dto.gitlabdashboardv14.Vendor;
import com.checkmarx.flow.dto.gitlabdashboardv14.Vendor__1;
import com.checkmarx.flow.dto.gitlabdashboardv14.Vulnerability;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.dto.ScanResults;
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

@RequiredArgsConstructor
@Slf4j
public class GitLabDashboardV14 implements GitLabDashboardStrategy {
    private static final String ISSUE_FORMAT = "%s @ %s : %d";
    private static final String CHECKMARX = "Checkmarx";
    private final FlowProperties flowProperties;



    @Override
    public void generateSastDashboard(ScanRequest request, ScanResults results) throws MachinaException {
        getSastResultsDashboardNewVersion(request, results);
    }

    @Override
    public void generateScaDashboard(ScanRequest request, ScanResults results) throws MachinaException {
        getScaResultsDashboardNewVer(request, results);
    }

    private void getSastResultsDashboardNewVersion(ScanRequest request, ScanResults results) throws MachinaException {

        List<Vulnerability> vulns = new ArrayList<>();
        Scanner__1 scanner = Scanner__1.builder().id("checkmarx-sast").name("checkmarx-sast").build();
        for(ScanResults.XIssue issue : results.getXIssues()) {
            if(issue.getDetails() != null && issue.getVulnerabilityStatus()!=null) {
                issue.getDetails().forEach((k, v) -> {

                    Vulnerability vuln= Vulnerability.builder()
                            .id(issue.getVulnerability().concat(":").concat(issue.getFilename()).concat(":").concat(k.toString()))
                            .category("sast-Checkmarx")
                            .name(issue.getVulnerability())
                            .message(String.format(ISSUE_FORMAT, issue.getVulnerability(), issue.getFilename(), k))
                            .description(issue.getDescription())
                            .cve(issue.getVulnerability().concat(":").concat(issue.getFilename()).concat(":").concat(k.toString()))
                            .severity(Vulnerability.Severity.valueOf(StringUtils.capitalize(issue.getSeverity().toLowerCase())))
                            .confidence(Vulnerability.Confidence.valueOf(StringUtils.capitalize(issue.getSeverity().toLowerCase())))
                            .solution(issue.getLink())
                            .scanner(scanner)
                            .identifiers(getIdentifiersNewVer(issue,k))
                            .links(getLinksNewVer(issue))
                            //.details(issue.getDetails())
                            .tracking(Tracking.builder()
                                    .lstItems(getItems(issue))
                                    .build())
                            .flags(getFlagsNewVer(issue))
                            .location(
                                    Location.builder()
                                            .file(issue.getFilename())
                                            .startLine(Double.valueOf(k))
                                            .endLine(Double.valueOf(k))
                                            ._class(issue.getFilename())
                                            .build()
                            )
                            .build();
                    vulns.add(vuln);
                });
            }
        }
        SecurityDashboardNewVer report  = SecurityDashboardNewVer.builder()
                .vulnerabilities(vulns)
                .scan(Scan.builder()
                        .startTime(calculateEndTime(String.valueOf(results.getAdditionalDetails().get("scanStartDate")),"0"))
                        .endTime(calculateEndTime(results.getReportCreationTime(),results.getScanTime()))
                        .analyzer(Analyzer.builder()
                                .vendor(Vendor.builder().build())
                                .version(results.getVersion()).
                                build())
                        .scanner(com.checkmarx.flow.dto.gitlabdashboardv14.Scanner.builder()
                                .vendor(Vendor__1.builder().build())
                                .version(results.getVersion()).build())
                        .status(Scan.Status.valueOf("SUCCESS"))
                        .type(Scan.Type.SAST)
                        .build())
                .build();

        writeJsonOutput(request, report, log);

    }

    private void getScaResultsDashboardNewVer(ScanRequest request, ScanResults results) throws MachinaException {

        List<com.checkmarx.flow.dto.gitlabdashboardv14.SCA.DependencyFile> dependencyFilesLst= new ArrayList<>();

        List<com.checkmarx.flow.dto.gitlabdashboardv14.SCA.Vulnerability> vulns = new ArrayList<>();
        com.checkmarx.flow.dto.gitlabdashboardv14.SCA.Scanner__1 scanner =
                com.checkmarx.flow.dto.gitlabdashboardv14.SCA.Scanner__1.builder().id("Checkmarx-SCA").name("Checkmarx-SCA").build();
        List<Finding> findings = results.getScaResults().getFindings();
        List<com.checkmarx.sdk.dto.sca.report.Package> packages = new ArrayList<>(results.getScaResults()
                .getPackages());
        Map<String, com.checkmarx.sdk.dto.sca.report.Package> map = new HashMap<>();
        for (com.checkmarx.sdk.dto.sca.report.Package p : packages) map.put(p.getId(), p);
        for (Finding finding:findings){
            // for each finding, get the associated package list.
            // for each object of the associated list, check the occurrences of locations
            // if multiple locations exist, construct multiple objects.
            // if only single location exist, construct single object
            Package indPackage = map.get(finding.getPackageId());
            if(indPackage!=null){
                com.checkmarx.flow.dto.gitlabdashboardv14.SCA.DependencyFile objDependencyFile =new DependencyFile();

                objDependencyFile.setPath(indPackage.getPackageRepository());
                objDependencyFile.setPackageManager(indPackage.getName());


                for(String loc : indPackage.getLocations()) {
                    vulns.add(com.checkmarx.flow.dto.gitlabdashboardv14.SCA.Vulnerability.builder()
                            .id(UUID.nameUUIDFromBytes(finding.getPackageId().concat("@").concat(loc).concat(":").concat(finding.getCveName()).getBytes()).toString())
                            .category("dependency_scanning")
                            .name(finding.getPackageId().concat("@").concat(loc).concat(":").concat(finding.getCveName()))
                            .message(finding.getPackageId().concat("@").concat(loc).concat(":").concat(finding.getCveName()))
                            .description(finding.getDescription())
                            .cve(finding.getCveName())
                            .severity(com.checkmarx.flow.dto.gitlabdashboardv14.SCA.Vulnerability.Severity.valueOf(String.valueOf(finding.getSeverity())))
                            .confidence(com.checkmarx.flow.dto.gitlabdashboardv14.SCA.Vulnerability.Confidence.valueOf(String.valueOf(finding.getSeverity())))
                            .solution(finding.getFixResolutionText())
                            .scanner(scanner)
                            .identifiers(getScaIdentifiersNewVer(results.getScaResults(),finding))
                            .links(getLinksSCANewVer(finding))
                            .tracking(com.checkmarx.flow.dto.gitlabdashboardv14.SCA.Tracking.builder()
                                    .lstItems(getItemsSca(finding.getCveName()))
                                    .build())
                            .flags(getSCAFlagsNewVer(finding))
                            .location(LocationSCA.builder().file(loc)
                                    .dependency(Dependency.builder()
                                            .dependencyPath(findDependencyPath(indPackage.getDependencyPaths()))
                                            .direct(indPackage.isIsDirectDependency())
                                            ._package(com.checkmarx.flow.dto.gitlabdashboardv14.SCA.Package.builder().name(finding.getPackageId()).build())
                                            .version(finding.getPackageId().split("-")[finding.getPackageId().
                                                    split("-").length-1]).build()).build())
                            .build());
                    objDependencyFile.setDependencies(findDependencyPath(indPackage.getDependencyPaths()));
                }
                dependencyFilesLst.add(objDependencyFile);
            }else{
                log.warn("Package not found for the finding: {}", finding.getPackageId());
            }
        }
        SecurityDashboardNewVerSCA report  = SecurityDashboardNewVerSCA.builder()
                .dependencyFiles(dependencyFilesLst)
                .version("14.1.2")
                .vulnerabilities(vulns)
                .scan(com.checkmarx.flow.dto.gitlabdashboardv14.SCA.Scan.builder()
                        .type(com.checkmarx.flow.dto.gitlabdashboardv14.SCA.Scan.Type.valueOf("DEPENDENCY_SCANNING"))
                        .status(com.checkmarx.flow.dto.gitlabdashboardv14.SCA.Scan.Status.valueOf("SUCCESS"))
                        .analyzer(com.checkmarx.flow.dto.gitlabdashboardv14.SCA.Analyzer.builder().vendor(
                                com.checkmarx.flow.dto.gitlabdashboardv14.SCA.Vendor.builder().build()
                        ).build())
                        .scanner((Scanner.builder()
                                .vendor(com.checkmarx.flow.dto.gitlabdashboardv14.SCA.Vendor__1.builder().build())
                                .build()))
                        .build())
                .schema(URI.create("https://gitlab.com/gitlab-org/gitlab/-/blob/8a42b7e8ab41ec2920f02fb4b36f244bbbb4bfb8/lib/gitlab/ci/parsers/security/validators/schemas/14.1.2/dependency-scanning-report-format.json"))
                .build();

        writeJsonOutput(request, report, log);
    }

    private List<Identifier> getIdentifiersNewVer(ScanResults.XIssue issue, Integer k){
        List<Identifier> identifiers = new ArrayList<>();

        identifiers.add(
                Identifier.builder()
                        .type("checkmarx_finding")
                        .name("Checkmarx-".concat(issue.getVulnerability()))
                        .value(issue.getVulnerability().concat(":").concat(issue.getFilename()).concat(":").concat(k.toString()))
                        .url(URI.create(issue.getLink()))
                        .build()
        );
        if (!ScanUtils.empty(flowProperties.getMitreUrl())) {
            identifiers.add(
                    Identifier.builder()
                            .type("cwe")
                            .name("CWE-".concat(issue.getCwe()))
                            .value(issue.getCwe())
                            .url(URI.create(String.format(flowProperties.getMitreUrl(), issue.getCwe())))
                            .build()
            );
        }else {
            log.info("mitre-url property is empty");
        }

        return identifiers;
    }

    private List<Link> getLinksNewVer(ScanResults.XIssue issue){
        List<Link> links = new ArrayList<>();
        issue.getAdditionalDetails().forEach((k, v) -> {
            if(checkingURI(String.valueOf(v))){
                links.add(
                        Link.builder()
                                .name("Checkmarx-".concat(k))
                                .url(URI.create(String.valueOf(v)))
                                .build());
            }

        });
        return links;
    }
    private List<com.checkmarx.flow.dto.gitlabdashboardv14.Items> getItems(ScanResults.XIssue issue){
        List<com.checkmarx.flow.dto.gitlabdashboardv14.Items> Items = new ArrayList<>();
        Items.add(com.checkmarx.flow.dto.gitlabdashboardv14.Items.builder()
                .signatures(getSignature())
                .file(issue.getFilename()).build());

        return Items;
    }

    private List<Flag> getFlagsNewVer(ScanResults.XIssue issue){
        List<Flag> flags = new ArrayList<>();


        flags.add(
                Flag.builder()
                        .type(Flag.Type.valueOf("FLAGGED_AS_LIKELY_FALSE_POSITIVE"))
                        .origin("sast")
                        .description(issue.getDescription()).build());


        return flags;
    }

    private List<com.checkmarx.flow.dto.gitlabdashboardv14.Signature> getSignature(){
        List<com.checkmarx.flow.dto.gitlabdashboardv14.Signature> Signature = new ArrayList<>();
        Signature.add(com.checkmarx.flow.dto.gitlabdashboardv14.Signature.builder().build());
        return Signature;
    }


    // SCA Utils Methods
    private List<com.checkmarx.flow.dto.gitlabdashboardv14.SCA.Identifier> getScaIdentifiersNewVer(SCAResults results, Finding finding)  {
        List<com.checkmarx.flow.dto.gitlabdashboardv14.SCA.Identifier> identifiers = new ArrayList<>();

        URI mainUrl;
        try {
            mainUrl=URI.create(results.getWebReportLink());
        } catch (Exception e) {
            mainUrl=URI.create("http://esw.w3.org/topic/");
        }
        identifiers.add(
                com.checkmarx.flow.dto.gitlabdashboardv14.SCA.Identifier.builder()
                        .type("checkmarx_finding")
                        .name(CHECKMARX.concat("-").concat(finding.getPackageId()))
                        .value(CHECKMARX.concat("-").concat(finding.getPackageId()))
                        .url(mainUrl)
                        .build()
        );
        if (!finding.getReferences().isEmpty()) {
            for(String reference:finding.getReferences()){
                URI subUrl;
                try {
                    subUrl=URI.create(reference);
                } catch (Exception e) {
                    subUrl=URI.create("http://esw.w3.org/topic/");
                }

                identifiers.add(
                        com.checkmarx.flow.dto.gitlabdashboardv14.SCA.Identifier.builder()
                                .type("cve")
                                .name(finding.getCveName() != null ? finding.getCveName().concat("(").concat(reference).concat(")") : reference)
                                .value(finding.getCveName() != null ? finding.getCveName().concat("(").concat(reference).concat(")") : reference)
                                .url(subUrl)
                                .build()
                );
            }
        }
        return identifiers;
    }

    private List<com.checkmarx.flow.dto.gitlabdashboardv14.SCA.Link> getLinksSCANewVer(Finding refrences){
        List<com.checkmarx.flow.dto.gitlabdashboardv14.SCA.Link> links = new ArrayList<>();
        refrences.getReferences().forEach((k) -> {

            URI mainUrl;
            try {
                mainUrl=URI.create(k);
            } catch (Exception e) {
                mainUrl=URI.create("http://esw.w3.org/topic/");
            }


            links.add(
                    com.checkmarx.flow.dto.gitlabdashboardv14.SCA.Link.builder()
                            .name(refrences.getCveName() != null ? refrences.getCveName().concat("(").concat(k).concat(")") : k)
                            .url(mainUrl)
                            .build());

        });
        return links;
    }

    private List<Items> getItemsSca(String name){
        List<Items> Items = new ArrayList<>();
        Items.add(com.checkmarx.flow.dto.gitlabdashboardv14.SCA.Items.builder()
                .signatures(getSignatureSca())
                .file(name).build());

        return Items;
    }

    private List<com.checkmarx.flow.dto.gitlabdashboardv14.SCA.Flag> getSCAFlagsNewVer(Finding finding){
        List<com.checkmarx.flow.dto.gitlabdashboardv14.SCA.Flag> flags = new ArrayList<>();
        flags.add(
                com.checkmarx.flow.dto.gitlabdashboardv14.SCA.Flag.builder()
                        .type(com.checkmarx.flow.dto.gitlabdashboardv14.SCA.Flag.Type.valueOf("FLAGGED_AS_LIKELY_FALSE_POSITIVE"))
                        .origin("SCA")
                        .description(finding.getDescription()).build());
        return flags;
    }

    private List<Signature> getSignatureSca(){
        List<Signature> Signature = new ArrayList<>();
        Signature.add(com.checkmarx.flow.dto.gitlabdashboardv14.SCA.Signature.builder().build());
        return Signature;
    }

    public static List<com.checkmarx.flow.dto.gitlabdashboardv14.SCA.DependencyPath> findDependencyPath(
            List<com.checkmarx.sdk.dto.sca.report.DependencyPath> dependencyPath ){
        List<com.checkmarx.flow.dto.gitlabdashboardv14.SCA.DependencyPath> dependencyPathList = new ArrayList<>();
        dependencyPath.forEach((k)-> k.forEach((v)-> dependencyPathList.add(
                com.checkmarx.flow.dto.gitlabdashboardv14.SCA.DependencyPath.builder()

                        .name(v.getName())
                        .version(v.getVersion())
                        .isDevelopment(String.valueOf(v.isDevelopment()))
                        .isResolved(String.valueOf(v.isResolved())).build())));
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
