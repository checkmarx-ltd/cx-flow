package com.checkmarx.flow.custom;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitLabProperties;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.gitdashboardnewver.Flag;
import com.checkmarx.flow.gitdashboardnewver.SCA.SecurityDashboardNewVerSCA;
import com.checkmarx.flow.gitdashboardnewver.Scan;
import com.checkmarx.flow.gitdashboardnewver.Scanner__1;
import com.checkmarx.flow.gitdashboardnewver.SecurityDashboardNewVer;
import com.checkmarx.flow.gitdashboardnewverfifteen.SAST.Gitlabdashboard;
import com.checkmarx.flow.gitdashboardnewverfifteen.SCA.*;
import com.checkmarx.flow.gitlabdashboardfifteen.sast.GitllabScgemaFifteen;
import com.checkmarx.flow.gitlabdashboardfifteen.sast.Items;
import com.checkmarx.flow.gitlabdashboardfifteen.sca.DependencyFile;
import com.checkmarx.flow.gitlabdashboardfifteen.sca.SCADashboard;
import com.checkmarx.flow.service.FilenameFormatter;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.sca.SCAResults;
import com.checkmarx.sdk.dto.sca.report.Finding;
import com.checkmarx.sdk.dto.sca.report.Package;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("GitLabDashboard")
@RequiredArgsConstructor
@Slf4j
public class GitLabSecurityDashboard extends ImmutableIssueTracker {
    private static final String ISSUE_FORMAT = "%s @ %s : %d";
    private static final String CHECKMARX = "Checkmarx";
    private final GitLabProperties properties;
    private final FlowProperties flowProperties;
    private final FilenameFormatter filenameFormatter;

    @Override
    public void init(ScanRequest request, ScanResults results){
        log.info("In the GitLab Security Dashboard Init method");
    }

    @Override
    public void complete(ScanRequest request, ScanResults results) throws MachinaException {
        deleteFilesifExist(properties.getSastFilePath(),properties.getScaFilePath());
        if(results.getXIssues() != null) {
            log.info("Finalizing SAST Dashboard output");
            fileInit(request, results, properties.getSastFilePath(), filenameFormatter, log);
            if(properties.getGitlabdashboardversion().equalsIgnoreCase("2.0")){
                getSastResultsDashboard(request,results);
            }else if (properties.getGitlabdashboardversion().equalsIgnoreCase("14.1.2")){
                getSastResultsDashboardNewVersion(request,results);
            }else if (properties.getGitlabdashboardversion().equalsIgnoreCase("15.0.4")){
                getSastResultsDashboardNewVersionFifteen(request,results);
            }else{
                getSastGitLabResultsDashboard(request,results);
            }

        }
        if(results.getScaResults() != null) {
            log.info("Finalizing SCA Dashboard output");
            fileInit(request, results, properties.getScaFilePath(), filenameFormatter, log);
            if(properties.getGitlabdashboardversion().equalsIgnoreCase("2.0")) {
                getScaResultsDashboard(request, results);
            }else if (properties.getGitlabdashboardversion().equalsIgnoreCase("14.1.2")){
                getScaResultsDashboardNewVer(request, results);
            }else if (properties.getGitlabdashboardversion().equalsIgnoreCase("15.0.4")){
                getScaResultsDashboardNewVerFifteen(request, results);
            }else{
                getScaGitLabResultsDashboard(request, results);
            }

        }
    }

    private void deleteFilesifExist(String sastFilePath, String scaFilePath) {
        try {
            Files.deleteIfExists(Paths.get(sastFilePath));
            Files.deleteIfExists(Paths.get(scaFilePath));
        }
        catch (IOException e) {
            log.error("Issue deleting existing files {} or {}", sastFilePath,scaFilePath, e);
        }

    }

    private void getScaResultsDashboard(ScanRequest request, ScanResults results) throws MachinaException {
        List<Vulnerability> vulns = new ArrayList<>();
        Scanner scanner = Scanner.builder().id("Checkmarx-SCA").name("Checkmarx-SCA").build();
        List<Finding> findings = results.getScaResults().getFindings();
        List<Package> packages = new ArrayList<>(results.getScaResults()
                .getPackages());
        Map<String, Package> map = new HashMap<>();
        for (Package p : packages) map.put(p.getId(), p);
        for (Finding finding:findings){
            // for each finding, get the associated package list.
            // for each object of the associated list, check the occurences of locations
            // if multiple locations exist, construct multiple objects.
            // if only single location exist, construct single object
            Package indPackage = map.get(finding.getPackageId());
            for(String loc : indPackage.getLocations()) {
                vulns.add(Vulnerability.builder()
                        .category("dependency_scanning")
                        .id(UUID.nameUUIDFromBytes(finding.getPackageId().concat("@").concat(loc).concat(":").concat(finding.getCveName()).getBytes()).toString())
                        .name(finding.getPackageId().concat("@").concat(loc).concat(":").concat(finding.getCveName()))
                        .message(finding.getPackageId().concat("@").concat(loc).concat(":").concat(finding.getCveName()))
                        .description(finding.getDescription())
                        .severity(String.valueOf(finding.getSeverity()))
                        .confidence(String.valueOf(finding.getSeverity()))
                        .solution(finding.getFixResolutionText())
                        .location(Location.builder().file(loc).dependency(Dependency.builder().pkg(Name.builder().dependencyname(finding.getPackageId()).build()).version(finding.getPackageId().split("-")[finding.getPackageId().split("-").length-1]).build()).build())
                        .identifiers(getScaIdentifiers(results.getScaResults(),finding))
                        .scanner(scanner)
                        .build());
            }

        }
        SecurityDashboard report  = SecurityDashboard.builder()
                .vulnerabilities(vulns)
                .build();

        writeJsonOutput(request, report, log);
    }


    private void getScaResultsDashboardNewVer(ScanRequest request, ScanResults results) throws MachinaException {

        List<com.checkmarx.flow.gitdashboardnewver.SCA.DependencyFile> dependencyFilesLst= new ArrayList<>();

        List<com.checkmarx.flow.gitdashboardnewver.SCA.Vulnerability> vulns = new ArrayList<>();
        com.checkmarx.flow.gitdashboardnewver.SCA.Scanner__1 scanner =
                com.checkmarx.flow.gitdashboardnewver.SCA.Scanner__1.builder().id("Checkmarx-SCA").name("Checkmarx-SCA").build();
        List<Finding> findings = results.getScaResults().getFindings();
        List<Package> packages = new ArrayList<>(results.getScaResults()
                .getPackages());
        Map<String, Package> map = new HashMap<>();
        for (Package p : packages) map.put(p.getId(), p);
        for (Finding finding:findings){
            // for each finding, get the associated package list.
            // for each object of the associated list, check the occurences of locations
            // if multiple locations exist, construct multiple objects.
            // if only single location exist, construct single object
            Package indPackage = map.get(finding.getPackageId());
            com.checkmarx.flow.gitdashboardnewver.SCA.DependencyFile objDependencyFile =new com.checkmarx.flow.gitdashboardnewver.SCA.DependencyFile();

            objDependencyFile.setPath(indPackage.getPackageRepository());
            objDependencyFile.setPackageManager(indPackage.getName());


            for(String loc : indPackage.getLocations()) {
                vulns.add(com.checkmarx.flow.gitdashboardnewver.SCA.Vulnerability.builder()
                        .id(UUID.nameUUIDFromBytes(finding.getPackageId().concat("@").concat(loc).concat(":").concat(finding.getCveName()).getBytes()).toString())
                        .category("dependency_scanning")
                        .name(finding.getPackageId().concat("@").concat(loc).concat(":").concat(finding.getCveName()))
                        .message(finding.getPackageId().concat("@").concat(loc).concat(":").concat(finding.getCveName()))
                        .description(finding.getDescription())
                        .cve(finding.getCveName())
                        .severity(com.checkmarx.flow.gitdashboardnewver.SCA.Vulnerability.Severity.valueOf(String.valueOf(finding.getSeverity())))
                        .confidence(com.checkmarx.flow.gitdashboardnewver.SCA.Vulnerability.Confidence.valueOf(String.valueOf(finding.getSeverity())))
                        .solution(finding.getFixResolutionText())
                        .scanner(scanner)
                        .identifiers(getScaIdentifiersNewVer(results.getScaResults(),finding))
                        .links(getLinksSCANewVer(finding))
                        .tracking(com.checkmarx.flow.gitdashboardnewver.SCA.Tracking.builder()
                                .lstItems(getItemsSca(finding.getCveName()))
                                .build())
                        .flags(getSCAFlagsNewVer(finding))
                        .location(com.checkmarx.flow.gitdashboardnewver.SCA.LocationSCA.builder().file(loc)
                                .dependency(com.checkmarx.flow.gitdashboardnewver.SCA.Dependency.builder()
                                        .dependencyPath(findDependencyPath(indPackage.getDependencyPaths()))
                                        .iid(123)
                                        .direct(indPackage.isIsDirectDependency())
                                        ._package(com.checkmarx.flow.gitdashboardnewver.SCA.Package.builder().name(finding.getPackageId()).build())
                                        .version(finding.getPackageId().split("-")[finding.getPackageId().
                                                split("-").length-1]).build()).build())
                        .build());
                objDependencyFile.setDependencies(findDependencyPath(indPackage.getDependencyPaths()));
            }
            dependencyFilesLst.add(objDependencyFile);
        }
        SecurityDashboardNewVerSCA report  = SecurityDashboardNewVerSCA.builder()
                .dependencyFiles(dependencyFilesLst)
                .version("14.1.2")
                .vulnerabilities(vulns)
                .scan(com.checkmarx.flow.gitdashboardnewver.SCA.Scan.builder()
                        .type(com.checkmarx.flow.gitdashboardnewver.SCA.Scan.Type.valueOf("DEPENDENCY_SCANNING"))
                        .status(com.checkmarx.flow.gitdashboardnewver.SCA.Scan.Status.valueOf("SUCCESS"))
                        .analyzer(com.checkmarx.flow.gitdashboardnewver.SCA.Analyzer.builder().vendor(
                                com.checkmarx.flow.gitdashboardnewver.SCA.Vendor.builder().build()
                        ).build())
                        .scanner((com.checkmarx.flow.gitdashboardnewver.SCA.Scanner.builder()
                                .vendor(com.checkmarx.flow.gitdashboardnewver.SCA.Vendor__1.builder().build())
                                .build()))
                        .build())
                .schema(URI.create("https://gitlab.com/gitlab-org/gitlab/-/blob/8a42b7e8ab41ec2920f02fb4b36f244bbbb4bfb8/lib/gitlab/ci/parsers/security/validators/schemas/14.1.2/dependency-scanning-report-format.json"))
                .build();

        writeJsonOutput(request, report, log);
    }

    private void getScaResultsDashboardNewVerFifteen(ScanRequest request, ScanResults results) throws MachinaException {

        List<com.checkmarx.flow.gitlabdashboardfifteen.sca.DependencyFile> dependencyFilesLst= new ArrayList<>();
        List<com.checkmarx.flow.gitlabdashboardfifteen.sca.Vulnerability> vulns = new ArrayList<>();
        com.checkmarx.flow.gitlabdashboardfifteen.sca.Scanner scanner =
        com.checkmarx.flow.gitlabdashboardfifteen.sca.Scanner.builder().id("Checkmarx-SCA").name("Checkmarx-SCA").build();
        List<Finding> findings = results.getScaResults().getFindings();
        List<Package> packages = new ArrayList<>(results.getScaResults()
                .getPackages());
        Map<String, Package> map = new HashMap<>();
        for (Package p : packages) map.put(p.getId(), p);
        for (Finding finding:findings){
            // for each finding, get the associated package list.
            // for each object of the associated list, check the occurences of locations
            // if multiple locations exist, construct multiple objects.
            // if only single location exist, construct single object
            Package indPackage = map.get(finding.getPackageId());


            for(String loc : indPackage.getLocations()) {
                vulns.add(com.checkmarx.flow.gitlabdashboardfifteen.sca.Vulnerability.builder()
                        .id(UUID.nameUUIDFromBytes(finding.getPackageId().concat("@").concat(loc).concat(":").concat(finding.getCveName()).getBytes()).toString())
                       // .category("dependency_scanning")
                        .name(finding.getPackageId().concat("@").concat(loc).concat(":").concat(finding.getCveName()))
                       // .message(finding.getPackageId().concat("@").concat(loc).concat(":").concat(finding.getCveName()))
                        .description(finding.getDescription())
                       // .cve(finding.getCveName())
                        .severity(com.checkmarx.flow.gitlabdashboardfifteen.sca.Vulnerability.Severity.valueOf(String.valueOf(finding.getSeverity())))
                     //   .confidence(com.checkmarx.flow.gitdashboardnewver.SCA.Vulnerability.Confidence.valueOf(String.valueOf(finding.getSeverity())))
                        .solution(finding.getFixResolutionText())

                     //   .scanner(scanner)
                        .identifiers(getScaIdentifiersNewVerFifteen(results.getScaResults(),finding))
                        .links(getLinksSCANewVerFifteen(finding))
                        .tracking(com.checkmarx.flow.gitlabdashboardfifteen.sca.Tracking.builder()
                        .lstItems(getItemsScaFifteen(finding.getCveName()))
                                .build())

                        .flags(getSCAFlagsNewVerFifteen(finding))
                        .location(com.checkmarx.flow.gitlabdashboardfifteen.sca.Location.builder().file(loc)
                                .dependency(com.checkmarx.flow.gitlabdashboardfifteen.sca.Dependency.builder()
                                        .dependencyPath(findDependencyPathFifteen(indPackage.getDependencyPaths()))
                                        .iid(123.0)
                                        .direct(indPackage.isIsDirectDependency())
                                        ._package(com.checkmarx.flow.gitlabdashboardfifteen.sca.Package.builder().name(finding.getPackageId()).build())
                                        .version(finding.getPackageId().split("-")[finding.getPackageId().
                                                split("-").length-1]).build()).build())
                        .build());
            }
            dependencyFilesLst.add(DependencyFile.builder().dependencies(findObjectDependencyFifteen(packages)).path(indPackage.getPackageRepository()).packageManager(indPackage.getName()).build());
        }
        SCADashboard report  = SCADashboard.builder()
                .dependencyFiles(dependencyFilesLst)
                .version("15.0.4")
                .vulnerabilities(vulns)
                .scan(com.checkmarx.flow.gitlabdashboardfifteen.sca.Scan.builder()
                        .type(com.checkmarx.flow.gitlabdashboardfifteen.sca.Scan.Type.valueOf("DEPENDENCY_SCANNING"))
                        .status(com.checkmarx.flow.gitlabdashboardfifteen.sca.Scan.Status.valueOf("SUCCESS"))
                        .analyzer(com.checkmarx.flow.gitlabdashboardfifteen.sca.Analyzer.builder()
                                .name("SCA-Checkmarx")
                                .vendor(
                                com.checkmarx.flow.gitlabdashboardfifteen.sca.Vendor.builder().build()
                        ).build())

                        .scanner((com.checkmarx.flow.gitlabdashboardfifteen.sca.Scanner.builder()
                                .id("1")
                                .name("SCA-Checkmarx")
                                .version("15.0.4")
                                .vendor(com.checkmarx.flow.gitlabdashboardfifteen.sca.Vendor__1.builder().build())
                                .build()))
                        .build())
                .schema("https://gitlab.com/gitlab-org/gitlab/-/blob/master/lib/gitlab/ci/parsers/security/validators/schemas/15.0.4/dependency-scanning-report-format.json")
                .build();

        writeJsonOutput(request, report, log);
    }


    private void getScaGitLabResultsDashboard(ScanRequest request, ScanResults results) throws MachinaException {

        List<com.checkmarx.flow.gitdashboardnewverfifteen.SCA.DependencyFile> dependencyFilesLst= new ArrayList<>();
        List<com.checkmarx.flow.gitdashboardnewverfifteen.SCA.Vulnerability> vulns = new ArrayList<>();
        com.checkmarx.flow.gitdashboardnewverfifteen.SCA.Scanner scanner =
                com.checkmarx.flow.gitdashboardnewverfifteen.SCA.Scanner.builder().id("Checkmarx-SCA").name("Checkmarx-SCA").build();
        List<Finding> findings = results.getScaResults().getFindings();
        List<Package> packages = new ArrayList<>(results.getScaResults()
                .getPackages());
        Map<String, Package> map = new HashMap<>();
        for (Package p : packages) map.put(p.getId(), p);
        for (Finding finding:findings){
            Package indPackage = map.get(finding.getPackageId());
            for(String loc : indPackage.getLocations()) {
                vulns.add(com.checkmarx.flow.gitdashboardnewverfifteen.SCA.Vulnerability.builder()
                        .id(UUID.nameUUIDFromBytes(finding.getPackageId().concat("@").concat(loc).concat(":").concat(finding.getCveName()).getBytes()).toString())
                        .name(finding.getPackageId().concat("@").concat(loc).concat(":").concat(finding.getCveName()))
                        .description(finding.getDescription())
                        .severity(com.checkmarx.flow.gitdashboardnewverfifteen.SCA.Vulnerability.Severity.valueOf(String.valueOf(finding.getSeverity()).toUpperCase()))
                        .solution(finding.getFixResolutionText())
                        .identifiers(getScaIdentifiersGitLabDashBoard(results.getScaResults(),finding))
                        .links(getLinksSCAGitlabDashboard(finding))
                        .tracking(Tracking.builder()
                                .lstItems(getItemsScaGitLabDashBoard(finding.getCveName()))
                                .build())
                        .flags(getSCAFlagsGitLabDashboard(finding))
                        .location(com.checkmarx.flow.gitdashboardnewverfifteen.SCA.Location.builder().file(loc)
                                .dependency(com.checkmarx.flow.gitdashboardnewverfifteen.SCA.Dependency.builder()
                                        .dependencyPath(findDependencyPathGitLabDashBoard(indPackage.getDependencyPaths()))
                                        .iid(123.0)
                                        .direct(indPackage.isIsDirectDependency())
                                        ._package(com.checkmarx.flow.gitdashboardnewverfifteen.SCA.Package.builder().name(finding.getPackageId()).build())
                                        .version(finding.getPackageId().split("-")[finding.getPackageId().
                                                split("-").length-1]).build()).build())
                        .build());
            }
            dependencyFilesLst.add(com.checkmarx.flow.gitdashboardnewverfifteen.SCA.DependencyFile.builder().dependencies(findObjectDependencyGitLabDashBoard(packages)).path(indPackage.getPackageRepository()).packageManager(indPackage.getName()).build());
        }
        SCASecurityDashboard report  = SCASecurityDashboard.builder()
                .dependencyFiles(dependencyFilesLst)
                .version("15.0.6")
                .vulnerabilities(vulns)
                .scan(com.checkmarx.flow.gitdashboardnewverfifteen.SCA.Scan.builder()
                        .type(com.checkmarx.flow.gitdashboardnewverfifteen.SCA.Scan.Type.valueOf("DEPENDENCY_SCANNING"))
                        .status(com.checkmarx.flow.gitdashboardnewverfifteen.SCA.Scan.Status.valueOf("SUCCESS"))
                        .analyzer(com.checkmarx.flow.gitdashboardnewverfifteen.SCA.Analyzer.builder()
                                .name("SCA-Checkmarx")
                                .vendor(
                                        com.checkmarx.flow.gitdashboardnewverfifteen.SCA.Vendor.builder().build()
                                ).build())

                        .scanner((com.checkmarx.flow.gitdashboardnewverfifteen.SCA.Scanner.builder()
                                .id("1")
                                .name("SCA-Checkmarx")
                                .version("15.0.6")
                                .vendor(com.checkmarx.flow.gitdashboardnewverfifteen.SCA.Vendor__1.builder().build())
                                .build()))
                        .build())
                .schema("https://gitlab.com/gitlab-org/gitlab/-/blob/master/lib/gitlab/ci/parsers/security/validators/schemas/15.0.6/dependency-scanning-report-format.json")
                .build();
        writeJsonOutput(request, report, log);
    }

    public static List<com.checkmarx.flow.gitdashboardnewver.SCA.DependencyPath> findDependencyPath(
            List<com.checkmarx.sdk.dto.sca.report.DependencyPath> dependencyPath ){
        List<com.checkmarx.flow.gitdashboardnewver.SCA.DependencyPath> dependencyPathList = new ArrayList<>();
        dependencyPath.forEach((k)-> k.forEach((v)-> dependencyPathList.add(
                com.checkmarx.flow.gitdashboardnewver.SCA.DependencyPath.builder()

                        .name(v.getName())
                        .version(v.getVersion())
                        .isDevelopment(String.valueOf(v.isDevelopment()))
                        .isResolved(String.valueOf(v.isResolved())).build())));
        return dependencyPathList;
    }

    public static List<com.checkmarx.flow.gitlabdashboardfifteen.sca.Dependency__1> findObjectDependencyFifteen(
            List<com.checkmarx.sdk.dto.sca.report.Package> dependencyPath ){
        List<com.checkmarx.flow.gitlabdashboardfifteen.sca.Dependency__1> dependencyPathList = new ArrayList<>();
        dependencyPath.forEach((k)-> dependencyPathList.add(
                com.checkmarx.flow.gitlabdashboardfifteen.sca.Dependency__1.builder()
                        ._package(com.checkmarx.flow.gitlabdashboardfifteen.sca.Package__1.builder().name(k.getName()).build())
                        .version(k.getVersion())
                        .iid(123.0)
                        .direct(k.isIsDirectDependency())
                        .dependencyPath(findeachDependencyPath(k.getDependencyPaths()))
                        .build()));
        return dependencyPathList;
    }

    public static List<com.checkmarx.flow.gitdashboardnewverfifteen.SCA.Dependency__1> findObjectDependencyGitLabDashBoard(
            List<com.checkmarx.sdk.dto.sca.report.Package> dependencyPath ){
        List<com.checkmarx.flow.gitdashboardnewverfifteen.SCA.Dependency__1> dependencyPathList = new ArrayList<>();
        dependencyPath.forEach((k)-> dependencyPathList.add(
                com.checkmarx.flow.gitdashboardnewverfifteen.SCA.Dependency__1.builder()
                        ._package(com.checkmarx.flow.gitdashboardnewverfifteen.SCA.Package__1.builder().name(k.getName()).build())
                        .version(k.getVersion())
                        .iid(123.0)
                        .direct(k.isIsDirectDependency())
                        .dependencyPath(findeachDependencyPathGitlabDashBoard(k.getDependencyPaths()))
                        .build()));
        return dependencyPathList;
    }

    public static List<com.checkmarx.flow.gitlabdashboardfifteen.sca.DependencyPath__1> findeachDependencyPath(
            List<com.checkmarx.sdk.dto.sca.report.DependencyPath> dependencyPath ){
        List<com.checkmarx.flow.gitlabdashboardfifteen.sca.DependencyPath__1> dependencyPathList = new ArrayList<>();
        dependencyPath.forEach((k)-> k.forEach((v)-> dependencyPathList.add(
                com.checkmarx.flow.gitlabdashboardfifteen.sca.DependencyPath__1.builder()
                        .build())));
        return dependencyPathList;
    }
    public static List<com.checkmarx.flow.gitdashboardnewverfifteen.SCA.DependencyPath__1> findeachDependencyPathGitlabDashBoard(
            List<com.checkmarx.sdk.dto.sca.report.DependencyPath> dependencyPath ){
        List<com.checkmarx.flow.gitdashboardnewverfifteen.SCA.DependencyPath__1> dependencyPathList = new ArrayList<>();
        dependencyPath.forEach((k)-> k.forEach((v)-> dependencyPathList.add(
                com.checkmarx.flow.gitdashboardnewverfifteen.SCA.DependencyPath__1.builder()
                        .build())));
        return dependencyPathList;
    }
    public static List<com.checkmarx.flow.gitlabdashboardfifteen.sca.DependencyPath> findDependencyPathFifteen(
            List<com.checkmarx.sdk.dto.sca.report.DependencyPath> dependencyPath ){
        List<com.checkmarx.flow.gitlabdashboardfifteen.sca.DependencyPath> dependencyPathList = new ArrayList<>();
        dependencyPath.forEach((k)-> k.forEach((v)->{
            dependencyPathList.add(
                    com.checkmarx.flow.gitlabdashboardfifteen.sca.DependencyPath.builder().build());

        }));
        return dependencyPathList;
    }

    public static List<com.checkmarx.flow.gitdashboardnewverfifteen.SCA.DependencyPath> findDependencyPathGitLabDashBoard(
            List<com.checkmarx.sdk.dto.sca.report.DependencyPath> dependencyPath ){
        List<com.checkmarx.flow.gitdashboardnewverfifteen.SCA.DependencyPath> dependencyPathList = new ArrayList<>();
        dependencyPath.forEach((k)-> k.forEach((v)->{
            dependencyPathList.add(
                    com.checkmarx.flow.gitdashboardnewverfifteen.SCA.DependencyPath.builder().build());

        }));
        return dependencyPathList;
    }


    private List<Identifier> getScaIdentifiers(SCAResults results, Finding finding) {
        List<Identifier> identifiers = new ArrayList<>();
        identifiers.add(
                Identifier.builder()
                        .type("checkmarx_finding")
                        .name(CHECKMARX.concat("-").concat(finding.getPackageId()))
                        .value(CHECKMARX.concat("-").concat(finding.getPackageId()))
                        .url(results.getWebReportLink())
                        .build()
        );
        if (!finding.getReferences().isEmpty()) {
            for(String reference:finding.getReferences()){
                identifiers.add(
                        Identifier.builder()
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


    private List<com.checkmarx.flow.gitdashboardnewver.SCA.Identifier> getScaIdentifiersNewVer(SCAResults results, Finding finding)  {
        List<com.checkmarx.flow.gitdashboardnewver.SCA.Identifier> identifiers = new ArrayList<>();

        URI mainUrl;
        try {
            mainUrl=URI.create(results.getWebReportLink());
        } catch (Exception e) {
            mainUrl=URI.create("http://esw.w3.org/topic/");
        }


        identifiers.add(
                com.checkmarx.flow.gitdashboardnewver.SCA.Identifier.builder()
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
                        com.checkmarx.flow.gitdashboardnewver.SCA.Identifier.builder()
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


    private List<com.checkmarx.flow.gitlabdashboardfifteen.sca.Identifier> getScaIdentifiersNewVerFifteen(SCAResults results, Finding finding)  {
        List<com.checkmarx.flow.gitlabdashboardfifteen.sca.Identifier> identifiers = new ArrayList<>();

        identifiers.add(
                com.checkmarx.flow.gitlabdashboardfifteen.sca.Identifier.builder()
                        .type("checkmarx_finding")
                        .name(CHECKMARX.concat("-").concat(finding.getPackageId()))
                        .value(CHECKMARX.concat("-").concat(finding.getPackageId()))
                        .url(results.getWebReportLink())
                        .build()
        );
        if (!finding.getReferences().isEmpty()) {
            for(String reference:finding.getReferences()){
                identifiers.add(
                        com.checkmarx.flow.gitlabdashboardfifteen.sca.Identifier.builder()
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


    private List<com.checkmarx.flow.gitdashboardnewverfifteen.SCA.Identifier> getScaIdentifiersGitLabDashBoard(SCAResults results, Finding finding)  {
        List<com.checkmarx.flow.gitdashboardnewverfifteen.SCA.Identifier> identifiers = new ArrayList<>();

        identifiers.add(
                com.checkmarx.flow.gitdashboardnewverfifteen.SCA.Identifier.builder()
                        .type("checkmarx_finding")
                        .name(CHECKMARX.concat("-").concat(finding.getPackageId()))
                        .value(CHECKMARX.concat("-").concat(finding.getPackageId()))
                        .url(results.getWebReportLink())
                        .build()
        );
        if (!finding.getReferences().isEmpty()) {
            for(String reference:finding.getReferences()){
                identifiers.add(
                        com.checkmarx.flow.gitdashboardnewverfifteen.SCA.Identifier.builder()
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




    private void getSastResultsDashboard(ScanRequest request, ScanResults results) throws MachinaException {
        List<Vulnerability> vulns = new ArrayList<>();
        Scanner scanner = Scanner.builder().id("Checkmarx-SAST").name("Checkmarx-SAST").build();
        for(ScanResults.XIssue issue : results.getXIssues()) {
            if(issue.getDetails() != null) {
                issue.getDetails().forEach((k, v) -> {
                    Vulnerability vuln = Vulnerability.builder()
                            .category("sast")
                            .id(issue.getVulnerability().concat(":").concat(issue.getFilename()).concat(":").concat(k.toString()))
                            .cve(issue.getVulnerability().concat(":").concat(issue.getFilename()).concat(":").concat(k.toString()))
                            .name(issue.getVulnerability())
                            .message(String.format(ISSUE_FORMAT, issue.getVulnerability(), issue.getFilename(), k))
                            .description(issue.getVulnerability())
                            .severity(issue.getSeverity())
                            .confidence(issue.getSeverity())
                            .solution(issue.getLink())
                            .scanner(scanner)
                            .identifiers(getIdentifiers(issue))
                            .location(
                                    Location.builder()
                                            .file(issue.getFilename())
                                            .startLine(k)
                                            .endLine(k)
                                            .build()
                            )
                            .build();
                    vulns.add(vuln);
                });
            }
        }
        SecurityDashboard report  = SecurityDashboard.builder()
                .vulnerabilities(vulns)
                .build();

        writeJsonOutput(request, report, log);

    }


    private void getSastResultsDashboardNewVersion(ScanRequest request, ScanResults results) throws MachinaException {

        List<com.checkmarx.flow.gitdashboardnewver.Vulnerability> vulns = new ArrayList<>();
        Scanner__1 scanner = Scanner__1.builder().id("checkmarx-sast").name("checkmarx-sast").build();
        for(ScanResults.XIssue issue : results.getXIssues()) {
            if(issue.getDetails() != null) {
                issue.getDetails().forEach((k, v) -> {

                    com.checkmarx.flow.gitdashboardnewver.Vulnerability vuln= com.checkmarx.flow.gitdashboardnewver.Vulnerability.builder()
                            .id(issue.getVulnerability().concat(":").concat(issue.getFilename()).concat(":").concat(k.toString()))
                            .category("sast-Checkmarx")
                            .name(issue.getVulnerability())
                            .message(String.format(ISSUE_FORMAT, issue.getVulnerability(), issue.getFilename(), k))
                            .description(issue.getDescription())
                            .cve(issue.getVulnerability().concat(":").concat(issue.getFilename()).concat(":").concat(k.toString()))
                            .severity(com.checkmarx.flow.gitdashboardnewver.Vulnerability.Severity.valueOf(issue.getSeverity()))
                            .confidence(com.checkmarx.flow.gitdashboardnewver.Vulnerability.Confidence.valueOf(issue.getSeverity()))//Need To add as per Enum
                            .solution(issue.getLink())
                            .scanner(scanner)//__1 wala scanner use karenge
                            .identifiers(getIdentifiersNewVer(issue,k))//Identifiers seems correct but need to add Link and Description flags
                            .links(getLinksNewVer(issue))//New Added
                            //.details(issue.getDetails()) //Yet To Add
                            .tracking(com.checkmarx.flow.gitdashboardnewver.Tracking.builder()
                                    .lstItems(getItems(issue))
                                    .build())
                            .flags(getFlagsNewVer(issue))
                            .location(
                                    com.checkmarx.flow.gitdashboardnewver.Location.builder()
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
                .scan(com.checkmarx.flow.gitdashboardnewver.Scan.builder()
                        .startTime(calculateEndTime(String.valueOf(results.getAdditionalDetails().get("scanStartDate")),"0"))
                        .endTime(calculateEndTime(results.getReportCreationTime(),results.getScanTime()))
                        .analyzer(com.checkmarx.flow.gitdashboardnewver.Analyzer.builder()
                                .vendor(com.checkmarx.flow.gitdashboardnewver.Vendor.builder().build())
                                .version(results.getVersion()).
                                build())
                        .scanner(com.checkmarx.flow.gitdashboardnewver.Scanner.builder()
                                .vendor(com.checkmarx.flow.gitdashboardnewver.Vendor__1.builder().build())
                                .version(results.getVersion()).build())
                        .status(Scan.Status.valueOf("SUCCESS"))
                        .type(Scan.Type.SAST)
                        .build())
                .build();

        writeJsonOutput(request, report, log);

    }


    //Adding code for version 15.0.4
    private void getSastResultsDashboardNewVersionFifteen(ScanRequest request, ScanResults results) throws MachinaException {

        List<com.checkmarx.flow.gitlabdashboardfifteen.sast.Vulnerability> vulns = new ArrayList<>();
        Scanner__1 scanner = Scanner__1.builder().id("checkmarx-sast").name("checkmarx-sast").build();
        for(ScanResults.XIssue issue : results.getXIssues()) {
            if(issue.getDetails() != null) {
                issue.getDetails().forEach((k, v) -> {

                    com.checkmarx.flow.gitlabdashboardfifteen.sast.Vulnerability vuln= com.checkmarx.flow.gitlabdashboardfifteen.sast.Vulnerability.builder()
                            .id(issue.getVulnerability().concat(":").concat(issue.getFilename()).concat(":").concat(k.toString()))
                           //.category("sast-Checkmarx")
                            .name(issue.getVulnerability())
                           // .message(String.format(ISSUE_FORMAT, issue.getVulnerability(), issue.getFilename(), k))
                            .description(issue.getDescription())
                           //.cve(issue.getVulnerability().concat(":").concat(issue.getFilename()).concat(":").concat(k.toString()))
                            .severity(com.checkmarx.flow.gitlabdashboardfifteen.sast.Vulnerability.Severity.valueOf(issue.getSeverity()))
                           // .confidence(com.checkmarx.flow.gitdashboardnewver.Vulnerability.Confidence.valueOf(issue.getSeverity()))//Need To add as per Enum
                            .solution(issue.getLink())
                           //.scanner(scanner)//__1 wala scanner use karenge
                            .identifiers(getIdentifiersNewVerFifteen(issue,k))//Identifiers seems correct but need to add Link and Description flags
                            .links(getLinksNewVerFifteen(issue))//New Added
                            //.details(issue.getDetails()) //Yet To Add
                            .tracking(com.checkmarx.flow.gitlabdashboardfifteen.sast.Tracking.builder()
                                    .lstItems(getItemsFifteen(issue))
                                    .build())
                            .flags(getFlagsNewVerFifteen(issue))
                            .location(
                                    com.checkmarx.flow.gitlabdashboardfifteen.sast.Location.builder()
                                            .file(issue.getFilename())
                                            .startLine(k)
                                            .endLine(k)
                                            ._class(issue.getFilename())
                                            .build()
                            )
                            .build();
                    vulns.add(vuln);
                });
            }
        }
        GitllabScgemaFifteen report  = GitllabScgemaFifteen.builder()
                .vulnerabilities(vulns)
                .version("15.0.4")
                .scan(com.checkmarx.flow.gitlabdashboardfifteen.sast.Scan.builder()
                        .startTime(calculateEndTime(String.valueOf(results.getAdditionalDetails().get("scanStartDate")),"0"))
                        .endTime(calculateEndTime(results.getReportCreationTime(),results.getScanTime()))
                        .analyzer(com.checkmarx.flow.gitlabdashboardfifteen.sast.Analyzer.builder()
                                .id(results.getProjectId())
                                .name("sast-Checkmarx")
                                .vendor(com.checkmarx.flow.gitlabdashboardfifteen.sast.Vendor.builder().build())
                                .version(results.getVersion()).
                                build())
                        .scanner(com.checkmarx.flow.gitlabdashboardfifteen.sast.Scanner.builder()
                                .id(results.getProjectId())
                                .name("sast-Checkmarx")
                                .vendor(com.checkmarx.flow.gitlabdashboardfifteen.sast.Vendor__1.builder().build())
                                .version(results.getVersion()).build())
                        .status(com.checkmarx.flow.gitlabdashboardfifteen.sast.Scan.Status.valueOf("SUCCESS"))
                        .type(com.checkmarx.flow.gitlabdashboardfifteen.sast.Scan.Type.SAST)
                        .build())
                .build();

        writeJsonOutput(request, report, log);

    }
    //End

    //Adding code for version 15.0.6
    private void getSastGitLabResultsDashboard(ScanRequest request, ScanResults results) throws MachinaException {

        List<com.checkmarx.flow.gitdashboardnewverfifteen.SAST.Vulnerability> vulns = new ArrayList<>();
        for(ScanResults.XIssue issue : results.getXIssues()) {
            if(issue.getDetails() != null) {
                issue.getDetails().forEach((k, v) -> {

                    com.checkmarx.flow.gitdashboardnewverfifteen.SAST.Vulnerability vuln= com.checkmarx.flow.gitdashboardnewverfifteen.SAST.Vulnerability.builder()
                            .id(issue.getVulnerability().concat(":").concat(issue.getFilename()).concat(":").concat(k.toString()))
                            .name(issue.getVulnerability())
                            .description(issue.getDescription())
                            .severity(com.checkmarx.flow.gitdashboardnewverfifteen.SAST.Vulnerability.Severity.valueOf(issue.getSeverity()))
                            .solution(issue.getLink())
                            .identifiers(getIdentifiersGitLabDashBoard(issue,k))//Identifiers seems correct but need to add Link and Description flags
                            .links(getLinksGitLabDashBoard(issue))//New Added
                            .tracking(com.checkmarx.flow.gitdashboardnewverfifteen.SAST.Tracking.builder()
                                    .lstItems(getItemsGitLabDashBoard(issue))
                                    .build())
                            .flags(getFlagsGitLabDashboard(issue))
                            .location(
                                    com.checkmarx.flow.gitdashboardnewverfifteen.SAST.Location.builder()
                                            .file(issue.getFilename())
                                            .startLine(k)
                                            .endLine(k)
                                            ._class(issue.getFilename())
                                            .build()
                            )
                            .build();
                    vulns.add(vuln);
                });
            }
        }
        Gitlabdashboard report  = Gitlabdashboard.builder()
                .vulnerabilities(vulns)
                .version("15.0.6")
                .scan(com.checkmarx.flow.gitdashboardnewverfifteen.SAST.Scan.builder()
                        .startTime(calculateEndTime(String.valueOf(results.getAdditionalDetails().get("scanStartDate")),"0"))
                        .endTime(calculateEndTime(results.getReportCreationTime(),results.getScanTime()))
                        .analyzer(com.checkmarx.flow.gitdashboardnewverfifteen.SAST.Analyzer.builder()
                                .id(results.getProjectId())
                                .name("sast-Checkmarx")
                                .vendor(com.checkmarx.flow.gitdashboardnewverfifteen.SAST.Vendor.builder().build())
                                .version(results.getVersion()).
                                build())
                        .scanner(com.checkmarx.flow.gitdashboardnewverfifteen.SAST.Scanner.builder()
                                .id(results.getProjectId())
                                .name("sast-Checkmarx")
                                .vendor(com.checkmarx.flow.gitdashboardnewverfifteen.SAST.Vendor__1.builder().build())
                                .version(results.getVersion()).build())
                        .status(com.checkmarx.flow.gitdashboardnewverfifteen.SAST.Scan.Status.valueOf("SUCCESS"))
                        .type(com.checkmarx.flow.gitdashboardnewverfifteen.SAST.Scan.Type.SAST)
                        .build())
                .build();

        writeJsonOutput(request, report, log);

    }
    //End


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


    private List<com.checkmarx.flow.gitdashboardnewver.Items> getItems(ScanResults.XIssue issue){
        List<com.checkmarx.flow.gitdashboardnewver.Items> Items = new ArrayList<>();
        Items.add(com.checkmarx.flow.gitdashboardnewver.Items.builder()
                .signatures(getSignature())
                .file(issue.getFilename()).build());

        return Items;
    }

    private List<com.checkmarx.flow.gitlabdashboardfifteen.sast.Items> getItemsFifteen(ScanResults.XIssue issue){
        List<com.checkmarx.flow.gitlabdashboardfifteen.sast.Items> Items = new ArrayList<>();
        Items.add(com.checkmarx.flow.gitlabdashboardfifteen.sast.Items.builder()
                .signatures(getSignatureFifteen())
                .file(issue.getFilename()).build());

        return Items;
    }

    private List<com.checkmarx.flow.gitdashboardnewverfifteen.SAST.Items> getItemsGitLabDashBoard(ScanResults.XIssue issue){
        List<com.checkmarx.flow.gitdashboardnewverfifteen.SAST.Items> Items = new ArrayList<>();
        Items.add(com.checkmarx.flow.gitdashboardnewverfifteen.SAST.Items.builder()
                .signatures(getSignatureGitLabDashboard())
                .file(issue.getFilename()).build());

        return Items;
    }


    private List<com.checkmarx.flow.gitdashboardnewver.SCA.Items> getItemsSca(String name){
        List<com.checkmarx.flow.gitdashboardnewver.SCA.Items> Items = new ArrayList<>();
        Items.add(com.checkmarx.flow.gitdashboardnewver.SCA.Items.builder()
                .signatures(getSignatureSca())
                .file(name).build());

        return Items;
    }
    private List<com.checkmarx.flow.gitlabdashboardfifteen.sca.Items> getItemsScaFifteen(String name){
        List<com.checkmarx.flow.gitlabdashboardfifteen.sca.Items> Items = new ArrayList<>();
        Items.add(com.checkmarx.flow.gitlabdashboardfifteen.sca.Items.builder()
                .signatures(getSignatureScaFifteen())
                .file(name).build());

        return Items;
    }

    private List<com.checkmarx.flow.gitdashboardnewverfifteen.SCA.Items> getItemsScaGitLabDashBoard(String name){
        List<com.checkmarx.flow.gitdashboardnewverfifteen.SCA.Items> Items = new ArrayList<>();
        Items.add(com.checkmarx.flow.gitdashboardnewverfifteen.SCA.Items.builder()
                .signatures(getSignatureScaGitLabDashBoard())
                .file(name).build());

        return Items;
    }


    private List<com.checkmarx.flow.gitdashboardnewver.SCA.Signature> getSignatureSca(){
        List<com.checkmarx.flow.gitdashboardnewver.SCA.Signature> Signature = new ArrayList<>();
        Signature.add(com.checkmarx.flow.gitdashboardnewver.SCA.Signature.builder().build());
        return Signature;
    }

    private List<com.checkmarx.flow.gitlabdashboardfifteen.sca.Signature> getSignatureScaFifteen(){
        List<com.checkmarx.flow.gitlabdashboardfifteen.sca.Signature> Signature = new ArrayList<>();
        Signature.add(com.checkmarx.flow.gitlabdashboardfifteen.sca.Signature.builder().build());
        return Signature;
    }

    private List<com.checkmarx.flow.gitdashboardnewverfifteen.SCA.Signature> getSignatureScaGitLabDashBoard(){
        List<com.checkmarx.flow.gitdashboardnewverfifteen.SCA.Signature> Signature = new ArrayList<>();
        Signature.add(com.checkmarx.flow.gitdashboardnewverfifteen.SCA.Signature.builder().build());
        return Signature;
    }


    private List<com.checkmarx.flow.gitdashboardnewver.Signature> getSignature(){
        List<com.checkmarx.flow.gitdashboardnewver.Signature> Signature = new ArrayList<>();
        Signature.add(com.checkmarx.flow.gitdashboardnewver.Signature.builder().build());
        return Signature;
    }

    private List<com.checkmarx.flow.gitlabdashboardfifteen.sast.Signature> getSignatureFifteen(){
        List<com.checkmarx.flow.gitlabdashboardfifteen.sast.Signature> Signature = new ArrayList<>();
        Signature.add(com.checkmarx.flow.gitlabdashboardfifteen.sast.Signature.builder().build());
        return Signature;
    }

    private List<com.checkmarx.flow.gitdashboardnewverfifteen.SAST.Signature> getSignatureGitLabDashboard(){
        List<com.checkmarx.flow.gitdashboardnewverfifteen.SAST.Signature> Signature = new ArrayList<>();
        Signature.add(com.checkmarx.flow.gitdashboardnewverfifteen.SAST.Signature.builder().build());
        return Signature;
    }

    private List<Identifier> getIdentifiers(ScanResults.XIssue issue){
        List<Identifier> identifiers = new ArrayList<>();
        identifiers.add(
                Identifier.builder()
                        .type("checkmarx_finding")
                        .name("Checkmarx-".concat(issue.getVulnerability()))
                        .value(issue.getVulnerability())
                        .url(issue.getLink())
                        .build()
        );
        if (!ScanUtils.empty(flowProperties.getMitreUrl())) {
            identifiers.add(
                    Identifier.builder()
                            .type("cwe")
                            .name("CWE-".concat(issue.getCwe()))
                            .value(issue.getCwe())
                            .url(String.format(flowProperties.getMitreUrl(), issue.getCwe()))
                            .build()
            );
        }else {
            log.info("mitre-url property is empty");
        }

        return identifiers;
    }


    private List<com.checkmarx.flow.gitdashboardnewver.Identifier> getIdentifiersNewVer(ScanResults.XIssue issue,Integer k){
        List<com.checkmarx.flow.gitdashboardnewver.Identifier> identifiers = new ArrayList<>();

        identifiers.add(
                com.checkmarx.flow.gitdashboardnewver.Identifier.builder()
                        .type("checkmarx_finding")
                        .name("Checkmarx-".concat(issue.getVulnerability()))
                        .value(issue.getVulnerability().concat(":").concat(issue.getFilename()).concat(":").concat(k.toString()))
                        .url(URI.create(issue.getLink()))
                        .build()
        );
        if (!ScanUtils.empty(flowProperties.getMitreUrl())) {
            identifiers.add(
                    com.checkmarx.flow.gitdashboardnewver.Identifier.builder()
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

    private List<com.checkmarx.flow.gitlabdashboardfifteen.sast.Identifier> getIdentifiersNewVerFifteen(ScanResults.XIssue issue,Integer k){
        List<com.checkmarx.flow.gitlabdashboardfifteen.sast.Identifier> identifiers = new ArrayList<>();

        identifiers.add(
                com.checkmarx.flow.gitlabdashboardfifteen.sast.Identifier.builder()
                        .type("checkmarx_finding")
                        .name("Checkmarx-".concat(issue.getVulnerability()))
                        .value(issue.getVulnerability().concat(":").concat(issue.getFilename()).concat(":").concat(k.toString()))
                        .url(issue.getLink())
                        .build()
        );
        if (!ScanUtils.empty(flowProperties.getMitreUrl())) {
            identifiers.add(
                    com.checkmarx.flow.gitlabdashboardfifteen.sast.Identifier.builder()
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

    private List<com.checkmarx.flow.gitdashboardnewverfifteen.SAST.Identifier> getIdentifiersGitLabDashBoard(ScanResults.XIssue issue,Integer k){
        List<com.checkmarx.flow.gitdashboardnewverfifteen.SAST.Identifier> identifiers = new ArrayList<>();

        identifiers.add(
                com.checkmarx.flow.gitdashboardnewverfifteen.SAST.Identifier.builder()
                        .type("checkmarx_finding")
                        .name("Checkmarx-".concat(issue.getVulnerability()))
                        .value(issue.getVulnerability().concat(":").concat(issue.getFilename()).concat(":").concat(k.toString()))
                        .url(issue.getLink())
                        .build()
        );
        if (!ScanUtils.empty(flowProperties.getMitreUrl())) {
            identifiers.add(
                    com.checkmarx.flow.gitdashboardnewverfifteen.SAST.Identifier.builder()
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

    private List<com.checkmarx.flow.gitdashboardnewver.Link> getLinksNewVer(ScanResults.XIssue issue){
        List<com.checkmarx.flow.gitdashboardnewver.Link> links = new ArrayList<>();
        issue.getAdditionalDetails().forEach((k, v) -> {
            if(chckkingURI(String.valueOf(v))){
                links.add(
                        com.checkmarx.flow.gitdashboardnewver.Link.builder()
                                .name("Checkmarx-".concat(k))
                                .url(URI.create(String.valueOf(v)))
                                .build());
            }

        });
        return links;
    }

    private List<com.checkmarx.flow.gitlabdashboardfifteen.sast.Link> getLinksNewVerFifteen(ScanResults.XIssue issue){
        List<com.checkmarx.flow.gitlabdashboardfifteen.sast.Link> links = new ArrayList<>();
        issue.getAdditionalDetails().forEach((k, v) -> {
            if(chckkingURI(String.valueOf(v))){
                links.add(
                        com.checkmarx.flow.gitlabdashboardfifteen.sast.Link.builder()
                                .name("Checkmarx-".concat(k))
                                .url(String.valueOf(v))
                                .build());
            }
        });
        return links;
    }

    private List<com.checkmarx.flow.gitdashboardnewverfifteen.SAST.Link> getLinksGitLabDashBoard(ScanResults.XIssue issue){
        List<com.checkmarx.flow.gitdashboardnewverfifteen.SAST.Link> links = new ArrayList<>();
        issue.getAdditionalDetails().forEach((k, v) -> {
            if(chckkingURI(String.valueOf(v))){
                links.add(
                        com.checkmarx.flow.gitdashboardnewverfifteen.SAST.Link.builder()
                                .name("Checkmarx-".concat(k))
                                .url(String.valueOf(v))
                                .build());
            }
        });
        return links;
    }

    private List<com.checkmarx.flow.gitdashboardnewver.SCA.Link> getLinksSCANewVer(Finding refrences){
        List<com.checkmarx.flow.gitdashboardnewver.SCA.Link> links = new ArrayList<>();
        refrences.getReferences().forEach((k) -> {

            URI mainUrl;
            try {
                mainUrl=URI.create(k);
            } catch (Exception e) {
                mainUrl=URI.create("http://esw.w3.org/topic/");
            }


            links.add(
                    com.checkmarx.flow.gitdashboardnewver.SCA.Link.builder()
                            .name(refrences.getCveName() != null ? refrences.getCveName().concat("(").concat(k).concat(")") : k)
                            .url(mainUrl)
                            .build());

        });
        return links;
    }

    private List<com.checkmarx.flow.gitlabdashboardfifteen.sca.Link> getLinksSCANewVerFifteen(Finding refrences){
        List<com.checkmarx.flow.gitlabdashboardfifteen.sca.Link> links = new ArrayList<>();
        refrences.getReferences().forEach((k) -> {
            links.add(
                    com.checkmarx.flow.gitlabdashboardfifteen.sca.Link.builder()
                            .name(refrences.getCveName() != null ? refrences.getCveName().concat("(").concat(k).concat(")") : k)
                            .url(k)
                            .build());

        });
        return links;
    }

    private List<com.checkmarx.flow.gitdashboardnewverfifteen.SCA.Link> getLinksSCAGitlabDashboard(Finding refrences){
        List<com.checkmarx.flow.gitdashboardnewverfifteen.SCA.Link> links = new ArrayList<>();
        refrences.getReferences().forEach((k) -> {
            links.add(
                    com.checkmarx.flow.gitdashboardnewverfifteen.SCA.Link.builder()
                            .name(refrences.getCveName() != null ? refrences.getCveName().concat("(").concat(k).concat(")") : k)
                            .url(k)
                            .build());

        });
        return links;
    }



    private List<com.checkmarx.flow.gitdashboardnewver.Flag> getFlagsNewVer(ScanResults.XIssue issue){
        List<com.checkmarx.flow.gitdashboardnewver.Flag> flags = new ArrayList<>();


        flags.add(
                com.checkmarx.flow.gitdashboardnewver.Flag.builder()
                        .type(Flag.Type.valueOf("FLAGGED_AS_LIKELY_FALSE_POSITIVE"))
                        .origin("sast")
                        .description(issue.getDescription()).build());


        return flags;
    }


    private List<com.checkmarx.flow.gitlabdashboardfifteen.sast.Flag> getFlagsNewVerFifteen(ScanResults.XIssue issue){
        List<com.checkmarx.flow.gitlabdashboardfifteen.sast.Flag> flags = new ArrayList<>();
                flags.add(
                com.checkmarx.flow.gitlabdashboardfifteen.sast.Flag.builder()
                        .type(com.checkmarx.flow.gitlabdashboardfifteen.sast.Flag.Type.valueOf("FLAGGED_AS_LIKELY_FALSE_POSITIVE"))
                        .origin("Cx-flow sast")
                        .description(issue.getDescription()).build());
        return flags;
    }

    private List<com.checkmarx.flow.gitdashboardnewverfifteen.SAST.Flag> getFlagsGitLabDashboard(ScanResults.XIssue issue){
        List<com.checkmarx.flow.gitdashboardnewverfifteen.SAST.Flag> flags = new ArrayList<>();
        if(issue.isAllFalsePositive()){
            flags.add(
                    com.checkmarx.flow.gitdashboardnewverfifteen.SAST.Flag.builder()
                            .type(com.checkmarx.flow.gitdashboardnewverfifteen.SAST.Flag.Type.valueOf("FLAGGED_AS_LIKELY_FALSE_POSITIVE"))
                            .origin("Cx-flow sast")
                            .description(issue.getDescription()).build());
        }
        return flags;
    }

    private List<com.checkmarx.flow.gitdashboardnewver.SCA.Flag> getSCAFlagsNewVer(Finding finding){
        List<com.checkmarx.flow.gitdashboardnewver.SCA.Flag> flags = new ArrayList<>();



        flags.add(
                com.checkmarx.flow.gitdashboardnewver.SCA.Flag.builder()
                        .type(com.checkmarx.flow.gitdashboardnewver.SCA.Flag.Type.valueOf("FLAGGED_AS_LIKELY_FALSE_POSITIVE"))
                        .origin("SCA")
                        .description(finding.getDescription()).build());


        return flags;
    }

    private List<com.checkmarx.flow.gitlabdashboardfifteen.sca.Flag> getSCAFlagsNewVerFifteen(Finding finding){
        List<com.checkmarx.flow.gitlabdashboardfifteen.sca.Flag> flags = new ArrayList<>();
        flags.add(
                com.checkmarx.flow.gitlabdashboardfifteen.sca.Flag.builder()
                        .type(com.checkmarx.flow.gitlabdashboardfifteen.sca.Flag.Type.valueOf("FLAGGED_AS_LIKELY_FALSE_POSITIVE"))
                        .origin("SCA")
                        .description(finding.getDescription()).build());


        return flags;
    }

    private List<com.checkmarx.flow.gitdashboardnewverfifteen.SCA.Flag> getSCAFlagsGitLabDashboard(Finding finding){
        List<com.checkmarx.flow.gitdashboardnewverfifteen.SCA.Flag> flags = new ArrayList<>();
        if(finding.isIgnored()){
            flags.add(
                    com.checkmarx.flow.gitdashboardnewverfifteen.SCA.Flag.builder()
                            .type(com.checkmarx.flow.gitdashboardnewverfifteen.SCA.Flag.Type.valueOf("FLAGGED_AS_LIKELY_FALSE_POSITIVE"))
                            .origin("SCA")
                            .description(finding.getDescription()).build());
        }
        return flags;
    }



    public boolean chckkingURI(String s){
        String URL_REGEX = "^((https?|ftp)://|(www|ftp)\\.)?[a-z0-9-]+(\\.[a-z0-9-]+)+([/?].*)?$";

        Pattern p = Pattern.compile(URL_REGEX);
        Matcher m = p.matcher(s);//replace with string to compare
        if(m.find()) {
            return  true;
        }
        return false;

    }


    @Data
    @Builder
    public static class SecurityDashboard {
        @JsonProperty("version")
        @Builder.Default
        public String version = "2.0";
        @JsonProperty("vulnerabilities")
        public List<Vulnerability> vulnerabilities;
        @JsonProperty("remediations")
        public List<String> remediations;
    }

    @Data
    @Builder
    public static class Vulnerability {
        @JsonProperty("id")
        public String id;
        @JsonProperty("cve")
        public String cve;
        @JsonProperty("category")
        @Builder.Default
        public String category = "sast";
        @JsonProperty("name")
        public String name;
        @JsonProperty("message")
        public String message;
        @JsonProperty("description")
        public String description;
        @JsonProperty("location")
        public Location location;
        @JsonProperty("severity")
        public String severity;
        @JsonProperty("confidence")
        public String confidence;
        @JsonProperty("solution")
        public String solution;
        @JsonProperty("scanner")
        public Scanner scanner;
        @JsonProperty("identifiers")
        public List<Identifier> identifiers;
    }

    @Data
    @Builder
    public static class Scanner {
        @JsonProperty("id")
        @Builder.Default
        public String id = CHECKMARX;
        @JsonProperty("name")
        @Builder.Default
        public String name = CHECKMARX;
    }

    @Data
    @Builder
    public static class Location {
        @JsonProperty("file")
        public String file;
        @JsonProperty("start_line")
        public Integer startLine;
        @JsonProperty("end_line")
        public Integer endLine;
        @Builder.Default
        @JsonProperty("class")
        public String clazz = "N/A";
        @Builder.Default
        @JsonProperty("method")
        public String method = "N/A";
        @JsonProperty("dependency")
        public Dependency dependency;
    }


    @Data
    @Builder
    public static class Dependency {
        @JsonProperty("package")
        public Object pkg;
        @JsonProperty("version")
        public String version;
    }

    @Data
    @Builder
    public static class Name {
        @JsonProperty("name")
        public String dependencyname;
    }

    @Data
    @Builder
    public static class Identifier {
        @JsonProperty("type")
        public String type;
        @JsonProperty("name")
        public String name;
        @JsonProperty("value")
        public String value;
        @JsonProperty("url")
        public String url;
    }
}
