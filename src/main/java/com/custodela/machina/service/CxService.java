package com.custodela.machina.service;

import com.custodela.machina.config.CxProperties;
import com.custodela.machina.dto.Filter;
import com.custodela.machina.dto.ScanResults;
import com.custodela.machina.dto.cx.*;
import com.custodela.machina.dto.cx.xml.CxXMLResultsType;
import com.custodela.machina.dto.cx.xml.QueryType;
import com.custodela.machina.dto.cx.xml.ResultType;
import com.custodela.machina.exception.CheckmarxLegacyException;
import com.custodela.machina.exception.InvalidCredentialsException;
import com.custodela.machina.exception.MachinaException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.beans.ConstructorProperties;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Class used to orchestrate submitting scans and retrieving results
 */
@Service
public class CxService {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(CxService.class);
    private final CxProperties cxProperties;
    private final CxLegacyService cxLegacyService;
    private final RestTemplate restTemplate;
    private String token = null;
    private LocalDateTime tokenExpires = null;
    private static final String LOGIN = "/auth/identity/connect/token";
    private static final String TEAMS = "/auth/teams";
    private static final String PROJECTS = "/projects";
    private static final String PROJECT = "/projects/{id}";
    private static final String PROJECT_SOURCE = "/projects/{id}/sourceCode/remoteSettings/git";
    private static final String PROJECT_SOURCE_FILE = "/projects/{id}/sourceCode/attachments";
    private static final String PROJECT_EXCLUDE = "/projects/{id}/sourceCode/excludeSettings";
    private static final String PRESETS = "/sast/presets";
    private static final String SCAN_CONFIGURATIONS = "/sast/engineConfigurations";
    private static final String SCAN_SETTINGS = "/sast/scanSettings";
    private static final String SCAN = "/sast/scans";
    private static final String PROJECT_SCANS = "/sast/scans?projectId={pid}&scanStatus=Scanning";//TODO handle all scan statuses (all but failed, finished)
    private static final String SCAN_STATUS = "/sast/scans/{id}";
    private static final String REPORT = "/reports/sastScan";
    private static final String REPORT_DOWNLOAD = "/reports/sastScan/{id}";
    private static final String REPORT_STATUS = "/reports/sastScan/{id}/status";
    public static final String UNKNOWN = "-1";
    public static final Integer UNKNOWN_INT = -1;
    public static final String OSA_VULN = "Vulnerable_Library";

    public static final Integer SCAN_STATUS_NEW = 1;
    public static final Integer SCAN_STATUS_PRESCAN = 2;
    public static final Integer SCAN_STATUS_QUEUED = 3;
    public static final Integer SCAN_STATUS_SCANNING = 4;
    public static final Integer SCAN_STATUS_POST_SCAN = 6;
    public static final Integer SCAN_STATUS_FINISHED = 7;
    public static final Integer SCAN_STATUS_CANCELED = 8;
    public static final Integer SCAN_STATUS_FAILED = 9;
    public static final Integer SCAN_STATUS_SOURCE_PULLING = 10;
    public static final Integer SCAN_STATUS_NONE = 1001;
    /*report statuses TODO*/
    public static final Integer REPORT_STATUS_CREATED = 2;
    public static final Integer REPORT_STATUS_FINISHED = 7;
    static final Map<String, Integer> STATUS_MAP = ImmutableMap.of(
            "CONFIRMED", 2,
            "URGENT", 3
    );

    @ConstructorProperties({"cxProperties", "cxLegacyService", "restTemplate"})
    public CxService(CxProperties cxProperties, CxLegacyService cxLegacyService, RestTemplate restTemplate) {
        this.cxProperties = cxProperties;
        this.cxLegacyService = cxLegacyService;
        this.restTemplate = restTemplate;
    }

    /**
     * Create Scan for a projectId
     *
     * @param projectId
     * @param incremental
     * @param isPublic
     * @param forceScan
     * @param comment
     * @return
     */
    public Integer createScan(Integer projectId, boolean incremental, boolean isPublic, boolean forceScan, String comment){
        CxScan scan = CxScan.builder()
                .projectId(projectId)
                .isIncremental(incremental)
                .forceScan(forceScan)
                .isPublic(isPublic)
                .comment(comment)
                .build();
        HttpEntity<CxScan> requestEntity = new HttpEntity<>(scan, createAuthHeaders());

        log.info("Creating Scan for project Id {}", projectId);
        try {
            String response = restTemplate.postForObject(cxProperties.getUrl().concat(SCAN), requestEntity, String.class);
            JSONObject obj = new JSONObject(response);
            String id = obj.get("id").toString();
            log.info("Scan created with Id {} for project Id {}", id, projectId);
            return Integer.parseInt(id);
        }catch (HttpStatusCodeException e){
            log.error("Error occurred while creating Scan for project {}, http error {}", projectId, e.getStatusCode());
            e.printStackTrace();
        }
        return UNKNOWN_INT;
    }

    Integer getLastScanId(Integer projectId){
        HttpEntity requestEntity = new HttpEntity<>(createAuthHeaders());

        log.info("Finding last Scan Id for project Id {}", projectId);
        try {
            ResponseEntity<String> response = restTemplate.exchange(cxProperties.getUrl().concat(SCAN)
                    .concat("?projectId=").concat(projectId.toString().concat("&scanStatus=").concat(SCAN_STATUS_FINISHED.toString())
                    .concat("&last=1")), HttpMethod.GET, requestEntity, String.class);

            JSONArray arr = new JSONArray(response.getBody());
            if(arr.length() < 1){
                return UNKNOWN_INT;
            }
            JSONObject obj = arr.getJSONObject(0);
            String id = obj.get("id").toString();
            log.info("Scan found with Id {} for project Id {}", id, projectId);
            return Integer.parseInt(id);
        }catch (HttpStatusCodeException e){
            log.error("Error occurred while creating Scan for project {}, http error {}", projectId, e.getStatusCode());
            e.printStackTrace();
        }
        return UNKNOWN_INT;
    }

    LocalDateTime getLastScanDate(Integer projectId){
        HttpEntity requestEntity = new HttpEntity<>(createAuthHeaders());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

        log.info("Finding last Scan Id for project Id {}", projectId);
        try {
            ResponseEntity<String> response = restTemplate.exchange(cxProperties.getUrl().concat(SCAN)
                    .concat("?projectId=").concat(projectId.toString().concat("&scanStatus=").concat(SCAN_STATUS_FINISHED.toString())
                            .concat("&last=1")), HttpMethod.GET, requestEntity, String.class);

            JSONArray arr = new JSONArray(response.getBody());
            if(arr.length() < 1){
                return null;
            }
            JSONObject obj = arr.getJSONObject(0);
            JSONObject dateAndTime = obj.getJSONObject("dateAndTime");
            //example: "finishedOn": "2018-06-18T01:09:12.707"
            return LocalDateTime.parse(dateAndTime.getString("finishedOn"), formatter);
        }catch (HttpStatusCodeException e){
            log.error("Error occurred while creating Scan for project {}, http error {}", projectId, e.getStatusCode());
            e.printStackTrace();
        }
        return null;
    }


    /**
     * Get the status of a given scanId
     *
     * @param scanId
     * @return
     */
    Integer getScanStatus(Integer scanId){
        HttpEntity httpEntity = new HttpEntity<>(createAuthHeaders());
        log.debug("Retrieving xml status of xml Id {}", scanId);
        try {
            ResponseEntity<String> projects = restTemplate.exchange(cxProperties.getUrl().concat(SCAN_STATUS), HttpMethod.GET, httpEntity, String.class, scanId);
            JSONObject obj = new JSONObject(projects.getBody());
            JSONObject status = obj.getJSONObject("status");
            log.debug("status id {}, status name {}", status.getInt("id"), status.getString("name"));
            return status.getInt("id");
        }catch (HttpStatusCodeException e){
            log.error("HTTP Status Code of {} while getting xml status for xml Id {}", e.getStatusCode(),scanId);
            e.printStackTrace();
        }catch (JSONException e){
            log.error("Error processing JSON Response");
            e.printStackTrace();
        }
        return UNKNOWN_INT;
    }

    /**
     * Generate a scan report request (xml) based on ScanId
     * @param scanId
     * @return
     */
    Integer createScanReport(Integer scanId){
        String strJSON = "{'reportType':'XML', 'scanId':%d}";
        strJSON = String.format(strJSON, scanId);
        HttpEntity requestEntity = new HttpEntity<>(strJSON, createAuthHeaders());

        try {
            log.info("Creating report for xml Id {}", scanId);
            ResponseEntity<String> response = restTemplate.exchange(cxProperties.getUrl().concat(REPORT), HttpMethod.POST, requestEntity, String.class);
            JSONObject obj = new JSONObject(response.getBody());
            Integer id = obj.getInt("reportId");
            log.info("Report with Id {} created", id );
            return id;
        }catch (HttpStatusCodeException e){
            log.error("HTTP Status Code of {} while creating xml report for xml Id {}", e.getStatusCode(),scanId);
            e.printStackTrace();
        }catch (JSONException e){
            log.error("Error processing JSON Response");
            e.printStackTrace();
        }
        return UNKNOWN_INT;
    }

    /**
     * Get the status of a report being generated by reportId
     *
     * @param reportId
     * @return
     */
    Integer getReportStatus(Integer reportId){
        HttpEntity httpEntity = new HttpEntity<>(createAuthHeaders());
        log.info("Retrieving report status of report Id {}", reportId);
        try {
            ResponseEntity<String> projects = restTemplate.exchange(cxProperties.getUrl().concat(REPORT_STATUS), HttpMethod.GET, httpEntity, String.class, reportId);
            JSONObject obj = new JSONObject(projects.getBody());
            JSONObject status = obj.getJSONObject("status");
            log.debug("Report status is {} - {} for report Id {}", status.getInt("id"), status.getString("value"), reportId);
            return status.getInt("id");
        }catch (HttpStatusCodeException e){
            log.error("HTTP Status Code of {} while getting report status for report Id {}", e.getStatusCode(),reportId);
            e.printStackTrace();
        }catch (JSONException e){
            log.error("Error processing JSON Response");
            e.printStackTrace();
        }
        return UNKNOWN_INT;
    }

    /**
     * Retrieve the report by reportId, mapped to ScanResults DTO, applying filtering as requested
     * @param reportId
     * @param filter
     * @return
     * @throws MachinaException
     */
    ScanResults getReportContent(Integer reportId, List<Filter> filter) throws MachinaException{
        HttpHeaders headers = createAuthHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        HttpEntity httpEntity = new HttpEntity<>(headers);
        String session = null;
        try{
            /* login to legacy SOAP CX Client to retrieve description */
            session = cxLegacyService.login(cxProperties.getUsername(), cxProperties.getPassword());
        }
        catch (CheckmarxLegacyException e){
            log.error("Error occurring while logging into Legacy SOAP based WebService - issue description will remain blank");
        }
        log.info("Retrieving report contents of report Id {} in XML format", reportId);
        try {
            ResponseEntity<String> resultsXML = restTemplate.exchange(cxProperties.getUrl().concat(REPORT_DOWNLOAD), HttpMethod.GET, httpEntity, String.class, reportId);
            log.info("Report downloaded for report Id {}", reportId);
            InputStream xmlStream = new ByteArrayInputStream(Objects.requireNonNull(resultsXML.getBody()).getBytes());

            /* protect against XXE */
            JAXBContext jc = JAXBContext.newInstance(CxXMLResultsType.class);
            XMLInputFactory xif = XMLInputFactory.newInstance();
            xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            xif.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            xif.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE,false);
            XMLStreamReader xsr = xif.createXMLStreamReader(xmlStream);
            Unmarshaller unmarshaller = jc.createUnmarshaller();

            List<ScanResults.XIssue> xIssueList = new ArrayList<>();
            CxXMLResultsType cxResults = (CxXMLResultsType) unmarshaller.unmarshal(xsr);
            ScanResults.ScanResultsBuilder cxScanBuilder = ScanResults.builder();
            cxScanBuilder.projectId(cxResults.getProjectId());
            cxScanBuilder.link(cxResults.getDeepLink());
            cxScanBuilder.files(cxResults.getFilesScanned());
            cxScanBuilder.loc(cxResults.getLinesOfCodeScanned());
            cxScanBuilder.scanType(cxResults.getScanType());
            getIssues(filter, session, xIssueList, cxResults);
            cxScanBuilder.xIssues(xIssueList);
            return cxScanBuilder.build();

        }catch (HttpStatusCodeException e) {
            log.error("HTTP Status Code of {} while getting downloading report contents of report Id {}", e.getStatusCode(), reportId);
            e.printStackTrace();
            throw new MachinaException("Error while processing scan results for report Id ".concat(reportId.toString()));
        }
        catch (XMLStreamException | JAXBException e){
            log.error("Error with XML report");
            e.printStackTrace();
            throw new MachinaException("Error while processing scan results for report Id ".concat(reportId.toString()));

        }
        catch (NullPointerException e){
            log.info("Null Error");
            e.printStackTrace();
            throw new MachinaException("Error while processing scan results for report Id ".concat(reportId.toString()));
        }
    }

    /**
     * Parse CX report file, mapped to ScanResults DTO, applying filtering as requested
     *
     * @param file
     * @param filter
     * @return
     * @throws MachinaException
     */
    ScanResults getReportContent(File file, List<Filter> filter) throws MachinaException{

        if(file == null){
            throw new MachinaException("File not provided for processing of results");
        }
        String session = null;
        try{
            if(!cxProperties.getOffline()) {
                session = cxLegacyService.login(cxProperties.getUsername(), cxProperties.getPassword());
            }
        }
        catch (CheckmarxLegacyException e){
            log.error("Error occurring while logging into Legacy SOAP based WebService - issue description will remain blank");
        }
        try {

            /* protect against XXE */
            JAXBContext jc = JAXBContext.newInstance(CxXMLResultsType.class);
            XMLInputFactory xif = XMLInputFactory.newInstance();
            xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            xif.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            xif.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
            Unmarshaller unmarshaller = jc.createUnmarshaller();

            List<ScanResults.XIssue> issueList = new ArrayList<>();
            CxXMLResultsType cxResults = (CxXMLResultsType) unmarshaller.unmarshal(file);
            ScanResults.ScanResultsBuilder cxScanBuilder = ScanResults.builder();
            cxScanBuilder.link(cxResults.getDeepLink());
            cxScanBuilder.files(cxResults.getFilesScanned());
            cxScanBuilder.loc(cxResults.getLinesOfCodeScanned());
            cxScanBuilder.scanType(cxResults.getScanType());
            getIssues(filter, session, issueList, cxResults);
            cxScanBuilder.xIssues(issueList);
            return cxScanBuilder.build();

        } catch (JAXBException e){
            log.error("Error with XML report");
            e.printStackTrace();
            throw new MachinaException("Error while processing scan results");
        }
        catch (NullPointerException e){
            log.info("Null error");
            e.printStackTrace();
            throw new MachinaException("Error while processing scan results");
        }
    }

    /**
     *
     * @param vulnsFile
     * @param libsFile
     * @param filter
     * @return
     * @throws MachinaException
     */
    ScanResults getOsaReportContent(File vulnsFile, File libsFile, List<Filter> filter) throws MachinaException{
        if(vulnsFile == null || libsFile == null){
            throw new MachinaException("Files not provided for processing of OSA results");
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            List<ScanResults.XIssue> issueList = new ArrayList<>();

            //convert json string to object
            List<CxOsa> osaVulns = objectMapper.readValue(vulnsFile, new TypeReference<List<CxOsa>>(){});
            List<CxOsaLib> osaLibs = objectMapper.readValue(libsFile, new TypeReference<List<CxOsaLib>>(){});
            Map<String, CxOsaLib> libsMap = getOsaLibsMap(osaLibs);
            Map<String, Integer> severityMap = ImmutableMap.of(
                    "LOW", 1,
                    "MEDIUM", 2,
                    "HIGH", 3
            );

            for(CxOsa o: osaVulns){

                if(filterOsa(filter, o) && libsMap.containsKey(o.getLibraryId())){
                    CxOsaLib lib = libsMap.get(o.getLibraryId());
                    String filename = lib.getName();

                    ScanResults.XIssue issue = ScanResults.XIssue.builder()
                            .file(filename)
                            .vulnerability(OSA_VULN)
                            .severity(o.getSeverity().getName())
                            .cve(o.getCveName())
                            .build();
                    ScanResults.OsaDetails details = ScanResults.OsaDetails.builder()
                            .severity(o.getSeverity().getName())
                            .cve(o.getCveName())
                            .description(o.getDescription())
                            .recommendation(o.getRecommendations())
                            .url(o.getUrl())
                            .version(lib.getVersion())
                            .build();
                    //update
                    if(issueList.contains(issue)){
                        issue = issueList.get(issueList.indexOf(issue));
                        //bump up the severity if required
                        if(severityMap.get(issue.getSeverity().toUpperCase()) < severityMap.get(o.getSeverity().getName().toUpperCase())){
                            issue.setSeverity(o.getSeverity().getName());
                        }
                        issue.setCve(issue.getCve().concat(",").concat(o.getCveName()));
                        issue.getOsaDetails().add(details);
                    }
                    else {//new
                        List<ScanResults.OsaDetails> dList = new ArrayList<>();
                        dList.add(details);
                        issue.setOsaDetails(dList);
                        issueList.add(issue);
                    }
                }
            }

            return ScanResults.builder()
                    .osa(true)
                    .xIssues(issueList)
                    .build();

        } catch ( IOException e){
            log.error("Error parsing JSON OSA report");
            e.printStackTrace();
            throw new MachinaException("Error while processing scan results");
        }
        catch (NullPointerException e){
            log.info("Null error");
            e.printStackTrace();
            throw new MachinaException("Error while processing scan results");
        }
    }

    private boolean filterOsa(List<Filter> filters, CxOsa osa){
        boolean all = true;
        for(Filter f: filters){
            if(f.getType().equals(Filter.Type.SEVERITY)){
                all = false;  //if no SEVERITY filters, everything is applied
                if(f.getValue().equalsIgnoreCase(osa.getSeverity().getName())){
                    return true;
                }
            }
        }
        return all;
    }

    private Map<String, CxOsaLib> getOsaLibsMap(List<CxOsaLib> libs){
        Map<String, CxOsaLib> libMap = new HashMap<>();
        for(CxOsaLib o: libs){
            libMap.put(o.getId(), o);
        }
        return libMap;
    }


    private List<CxOsa> getOSAVulnsByLibId(List<CxOsa> osaVulns, String libId){
       List<CxOsa> vulns = new ArrayList<>();
       for(CxOsa v: osaVulns){
           if(v.getLibraryId().equals(libId)){
               vulns.add(v);
           }
       }
        return vulns;
    }


    /**
     *
     * @param filter
     * @param session
     * @param cxIssueList
     * @param cxResults
     */
    private void getIssues(List<Filter> filter, String session, List<ScanResults.XIssue> cxIssueList, CxXMLResultsType cxResults) {
        for (QueryType q : cxResults.getQuery()) {
            if (checkFilter(q, filter)) {
                ScanResults.XIssue.XIssueBuilder xIssueBuilder = ScanResults.XIssue.builder();
                /*Top node of each issue*/
                for (ResultType r : q.getResult()) {
                    if (r.getFalsePositive().toUpperCase().equals("FALSE") && checkFilter(r, filter)) {
                        /*Map issue details*/
                        xIssueBuilder.cwe(q.getCweId());
                        xIssueBuilder.language(q.getLanguage());
                        xIssueBuilder.severity(q.getSeverity());
                        xIssueBuilder.vulnerability(q.getName());
                        xIssueBuilder.file(r.getFileName());
                        xIssueBuilder.severity(r.getSeverity());
                        xIssueBuilder.link(r.getDeepLink());

                        Map<Integer, String> details = new HashMap<>();
                        try {
                            /* Call the CX SOAP Service to get Issue Description*/
                            if (session != null) {
                                try {
                                    xIssueBuilder.description(this.getIssueDescription(session, Long.parseLong(cxResults.getScanId()), Long.parseLong(r.getPath().getPathId())));
                                } catch (HttpStatusCodeException e){
                                    xIssueBuilder.description("");
                                }
                            } else {
                                xIssueBuilder.description("");
                            }
                            details.put(Integer.parseInt(r.getPath().getPathNode().get(0).getLine()),
                                    r.getPath().getPathNode().get(0).getSnippet().getLine().getCode());
                        } catch (NullPointerException e) {
                            log.warn("Problem grabbing snippet.  Snippet may not exist for finding for Node ID");
                            /*Defaulting to initial line number with no snippet*/
                            details.put(Integer.parseInt(r.getLine()), null);
                        }
                        xIssueBuilder.details(details);
                        ScanResults.XIssue issue = xIssueBuilder.build();
                        checkForDuplicateIssue(cxIssueList, r, details, issue);
                    }
                }
            }
        }
    }


    /**
     * Check if the highlevel Query resultset meets the filter criteria
     *
     * @param q
     * @param filters
     * @return
     */
    private boolean checkFilter(QueryType q, List<Filter> filters){
        if(filters == null || filters.isEmpty()){
            return true;
        }
        List<String> severity = new ArrayList<>();
        List<String> cwe = new ArrayList<>();
        List<String> category = new ArrayList<>();

        for(Filter f: filters){
            if(f.getType().equals(Filter.Type.SEVERITY)){
                severity.add(f.getValue().toUpperCase());
            }
            else if(f.getType().equals(Filter.Type.TYPE)){
                category.add(f.getValue().toUpperCase());
            }
            else if(f.getType().equals(Filter.Type.CWE)){
                cwe.add(f.getValue().toUpperCase());
            }
        }
        if(!severity.isEmpty() && !severity.contains(q.getSeverity().toUpperCase())){
            return false;
        }
        if(!cwe.isEmpty() && !cwe.contains(q.getCweId())){
            return false;
        }

        return category.isEmpty() || category.contains(q.getName().toUpperCase());
    }

    private boolean checkFilter(ResultType r, List<Filter> filters){
        if(filters == null || filters.isEmpty()){
            return true;
        }
        List<Integer> status = new ArrayList<>();

        for(Filter f: filters){
            if(f.getType().equals(Filter.Type.STATUS)){
                status.add(STATUS_MAP.get(f.getValue().toUpperCase()));
            }
        }
        return status.isEmpty() || status.contains(Integer.parseInt(r.getState()));
    }

    private void checkForDuplicateIssue(List<ScanResults.XIssue> cxIssueList, ResultType r, Map<Integer, String> details, ScanResults.XIssue issue) {
        if(cxIssueList.contains(issue)){
            /*Get existing issue of same vuln+filename*/
            ScanResults.XIssue existingIssue = cxIssueList.get(cxIssueList.indexOf(issue));
            /*If no reference exists for this particular line, append it to the details (line+snippet)*/
            if(!existingIssue.getDetails().containsKey(Integer.parseInt(r.getLine()))){
                try {
                    existingIssue.getDetails().put(Integer.parseInt(r.getPath().getPathNode().get(0).getLine()),
                            r.getPath().getPathNode().get(0).getSnippet().getLine().getCode());
                }catch (NullPointerException e){
                    details.put(Integer.parseInt(r.getLine()),null);
                }
            }
        }
        else{
            cxIssueList.add(issue);
        }
    }

    private String getIssueDescription(String session, Long scanId, Long pathId){
        return cxLegacyService.getDescription(session, scanId, pathId);
    }

    /**
     * Creates a CX Project.
     *
     * Naming convention is namespace-repo-branch
     */
    public Integer createProject(String ownerId, String name){
        CxCreateProject project = CxCreateProject.builder()
                .name(name)
                .owningTeam(ownerId)
                .isPublic(true)
                .build();
        HttpEntity<CxCreateProject> requestEntity = new HttpEntity<>(project, createAuthHeaders());

        log.info("Creating Project {} for ownerId {}", name, ownerId);
        try {
            String response = restTemplate.postForObject(cxProperties.getUrl().concat(PROJECTS), requestEntity, String.class);
            JSONObject obj = new JSONObject(response);
            String id = obj.get("id").toString();
            return Integer.parseInt(id);
        }catch (HttpStatusCodeException e){
            log.error("HTTP error code {} while creating project with name {} under owner id {}", e.getStatusCode(), name, ownerId);
            e.printStackTrace();
        }catch (JSONException e){
            log.error("Error processing JSON Response");
            e.printStackTrace();
        }
        return UNKNOWN_INT;
    }


    /**
     * Get All Projects in Checkmarx
     *
     * @return
     */
    public CxProject[] getProjects() throws MachinaException{
        HttpEntity httpEntity = new HttpEntity<>(createAuthHeaders());
        try {
            ResponseEntity<CxProject[]> projects = restTemplate.exchange(cxProperties.getUrl().concat(PROJECTS), HttpMethod.GET, httpEntity, CxProject[].class);
            return projects.getBody();
        }catch (HttpStatusCodeException e){
            log.warn("Error occurred while retrieving projects, http error {}", e.getStatusCode());
            e.printStackTrace();
            throw new MachinaException("Error retrieving Projects");
        }
    }

    /**
     * Get All Projects in Checkmarx
     *
     * @return
     */
    public List<CxProject> getProjects(String teamId) throws MachinaException{
        HttpEntity httpEntity = new HttpEntity<>(createAuthHeaders());
        List<CxProject> teamProjects = new ArrayList<>();
        try {
            ResponseEntity<CxProject[]> projects = restTemplate.exchange(cxProperties.getUrl().concat(PROJECTS), HttpMethod.GET, httpEntity, CxProject[].class);

            if(projects.getBody() != null) {
                for (CxProject p : projects.getBody()) {
                    if(p.getTeamId().equals(teamId)){
                        teamProjects.add(p);
                    }
                }
            }
            return teamProjects;
        }catch (HttpStatusCodeException e){
            log.warn("Error occurred while retrieving projects, http error {}", e.getStatusCode());
            log.debug(ExceptionUtils.getStackTrace(e));
            throw new MachinaException("Error retrieving Projects");
        }
    }


    /**
     * Get All Projects under a specific team within Checkmarx
     *
     * using TeamId does not work.
     * @param ownerId
     * @return
     */
    /*public CxProject[] getProjects(String ownerId) throws MachinaException{
        HttpEntity httpEntity = new HttpEntity<>(createAuthHeaders());
        try {
            ResponseEntity<CxProject[]> projects = restTemplate.exchange(cxProperties.getUrl().concat(PROJECTS).concat("?teamId=").concat(ownerId), HttpMethod.GET, httpEntity, CxProject[].class);
            return projects.getBody();
        }catch (HttpStatusCodeException e){
            log.warn("Error occurred while retrieving projects for team id {}, http error {}", ownerId, e.getStatusCode());
            e.printStackTrace();
            throw new MachinaException("Error retrieving Projects");
        }
    }*/

    /*
    public Integer getProjectId(String ownerId, String name){
        HttpEntity httpEntity = new HttpEntity<>(createAuthHeaders());
        try {
            ResponseEntity<String> projects = restTemplate.exchange(cxProperties.getUrl().concat(PROJECTS), HttpMethod.GET, httpEntity, String.class);
            JSONArray jsonArray = new JSONArray(projects.getBody());
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String x = obj.getString("name").toString();
                Integer id = obj.getInt("id");
                if(x.equals(name)){
                    return id;
                }
            }
        }catch (HttpStatusCodeException e){
            log.error("Error occurred while retrieving project with name {}, http error {}", name, e.getStatusCode());
            e.printStackTrace();
        } catch (JSONException e){
            log.error("Error processing JSON Response");
            e.printStackTrace();
        }
        return UNKNOWN_INT;
    }
    */
    public Integer getProjectId(String ownerId, String name){
        HttpEntity httpEntity = new HttpEntity<>(createAuthHeaders());
        try {
            ResponseEntity<String> projects = restTemplate.exchange(cxProperties.getUrl().concat(PROJECTS)
                    .concat("?projectName=").concat(name).concat("&teamId=").concat(ownerId), HttpMethod.GET, httpEntity, String.class);
            JSONArray arr = new JSONArray(projects.getBody());
            if(arr.length() > 1){
                return UNKNOWN_INT;
            }
            JSONObject obj =  arr.getJSONObject(0);
            return obj.getInt("id");
        }catch (HttpStatusCodeException e){
            if(!e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                log.warn("Error occurred while retrieving project with name {}, http error {}", name, e.getStatusCode());
                log.error(ExceptionUtils.getStackTrace(e));
            }
        } catch (JSONException e){
            log.error("Error processing JSON Response");
            log.error(ExceptionUtils.getStackTrace(e));
        }
        return UNKNOWN_INT;
    }

    /**
     * Return Project based on projectId
     *
     * @return
     */
    public CxProject getProject(Integer projectId){
        HttpEntity httpEntity = new HttpEntity<>(createAuthHeaders());
        try {
            ResponseEntity<CxProject> project = restTemplate.exchange(cxProperties.getUrl().concat(PROJECT), HttpMethod.GET, httpEntity, CxProject.class, projectId);
            return project.getBody();
        }catch (HttpStatusCodeException e){
            log.error("Error occurred while retrieving project with id {}, http error {}", projectId, e.getStatusCode());
            log.error(ExceptionUtils.getStackTrace(e));
        } catch (JSONException e){
            log.error("Error processing JSON Response");
            log.error(ExceptionUtils.getStackTrace(e));
        }
        return null;
    }


    /**
     * Check if a scan exists for a projectId
     *
     * @param projectId
     * @return
     */
    public boolean scanExists(Integer projectId){
        HttpEntity httpEntity = new HttpEntity<>(createAuthHeaders());
        try {
            ResponseEntity<String> scans = restTemplate.exchange(cxProperties.getUrl().concat(PROJECT_SCANS), HttpMethod.GET, httpEntity, String.class, projectId);
            JSONArray jsonArray = new JSONArray(scans.getBody());
            if(jsonArray.length() > 0){
                return true;
            }
            else{
                return false;
            }

        }catch (HttpStatusCodeException e){
            log.error("Error occurred while retrieving project with id {}, http error {}", projectId, e.getStatusCode());
            log.error(ExceptionUtils.getStackTrace(e));
        } catch (JSONException e){
            log.error("Error processing JSON Response");
            log.error(ExceptionUtils.getStackTrace(e));
        }
        return false;
    }

    /**
     * Create Scan Settings
     *
     * @param projectId
     * @param presetId
     * @param engineConfigId
     * @return
     */
    Integer createScanSetting(Integer projectId, Integer presetId, Integer engineConfigId){
        CxScanSettings scanSettings = CxScanSettings.builder()
                .projectId(projectId)
                .engineConfigurationId(engineConfigId)
                .presetId(presetId)
                .build();
        HttpEntity<CxScanSettings> requestEntity = new HttpEntity<>(scanSettings, createAuthHeaders());

        log.info("Creating ScanSettings for project Id {}", projectId);
        try {
            String response = restTemplate.postForObject(cxProperties.getUrl().concat(SCAN_SETTINGS), requestEntity, String.class);
            JSONObject obj = new JSONObject(response);
            String id = obj.get("id").toString();
            return Integer.parseInt(id);
        }catch (HttpStatusCodeException e){
            log.error("Error occurred while creating ScanSettings for project {}, http error {}", projectId, e.getStatusCode());
            log.error(ExceptionUtils.getStackTrace(e));
        } catch (JSONException e){
            log.error("Error processing JSON Response");
            log.error(ExceptionUtils.getStackTrace(e));
        }
        return UNKNOWN_INT;
    }

    /**
     * Set Repository details for a project
     *
     * @param projectId
     * @param gitUrl
     * @param branch
     * @throws MachinaException
     */
    void setProjectRepositoryDetails(Integer projectId, String gitUrl, String branch) throws MachinaException {
        CxProjectSource projectSource = CxProjectSource.builder()
                .url(gitUrl)
                .branch(branch)
                .build();
        log.debug("branch {}", branch);
        log.debug("project {}", projectId);
        HttpEntity<CxProjectSource> requestEntity = new HttpEntity<>(projectSource, createAuthHeaders());

        try {
            log.info("Updating Source details for project Id {}", projectId);
            restTemplate.exchange(cxProperties.getUrl().concat(PROJECT_SOURCE), HttpMethod.POST, requestEntity, String.class, projectId);
        }   catch (HttpStatusCodeException e) {
            log.error("Error occurred while updating Project source info for project {}.", projectId);
            throw new MachinaException("Error occurred while adding source details to project.  Please ensure GIT is defined within Checkmarx");
        }
    }

    /**
     * Upload file (zip of source) for a project
     *
     * @param projectId
     * @param file
     * @throws MachinaException
     */
    public void uploadProjectSource(Integer projectId, File file) throws MachinaException{
        HttpHeaders headers = createAuthHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
        FileSystemResource value = new FileSystemResource(file);
        map.add("zippedSource", value);

        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(map, headers);

        try {
            log.info("Updating Source details for project Id {}", projectId);
            restTemplate.exchange(cxProperties.getUrl().concat(PROJECT_SOURCE_FILE), HttpMethod.POST, requestEntity, String.class, projectId);
        }   catch (HttpStatusCodeException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            log.error("Error occurred while uploading Project source for project id {}.", projectId);
            throw new MachinaException("Error occurred while uploading source");
        }
    }

    public void setProjectExcludeDetails(Integer projectId, List<String> excludeFolders, List<String> excludeFiles){
        String excludeFilesStr = "";
        String excludeFolderStr = "";

        if(excludeFiles != null && !excludeFiles.isEmpty()){
            excludeFilesStr = String.join(",", excludeFiles);
        }
        if(excludeFolders != null && !excludeFolders.isEmpty()){
            excludeFolderStr = String.join(",", excludeFolders);
        }

        String strJSON = "{'excludeFoldersPattern':'%s', 'excludeFilesPattern':'%s'}";
        strJSON = String.format(strJSON, excludeFolderStr, excludeFilesStr);
        HttpEntity requestEntity = new HttpEntity<>(strJSON, createAuthHeaders());

        try {
            log.info("Updating Project folder and file exclusion details for project Id {}", projectId);
            restTemplate.exchange(cxProperties.getUrl().concat(PROJECT_EXCLUDE), HttpMethod.PUT, requestEntity, String.class, projectId);
        }   catch (HttpStatusCodeException e) {
            log.error("Error occurred while updating Project source info for project {}.");
            log.error(ExceptionUtils.getStackTrace(e));
        }
    }

    /**
     * Get teamId for given path
     *
     * @param teamPath
     * @return
     * @throws MachinaException
     */
    public String getTeamId(String teamPath) throws MachinaException{
        HttpEntity httpEntity = new HttpEntity<>(createAuthHeaders());
        try {
            log.info("Retrieving Cx teams");
            ResponseEntity<CxTeam[]> response = restTemplate.exchange(cxProperties.getUrl().concat(TEAMS), HttpMethod.GET, httpEntity, CxTeam[].class);
            CxTeam[] teams = response.getBody();
            if(teams == null){
                throw new MachinaException("Error obtaining Team Id");
            }
            for(CxTeam team: teams){
                if(team.getFullName().equals(teamPath)){
                    log.info("Found team {} with ID {}", teamPath, team.getId());
                    return team.getId();
                }
            }
        }   catch (HttpStatusCodeException e) {
            log.error("Error occurred while retrieving Teams");
            log.error(ExceptionUtils.getStackTrace(e));
        }
        return UNKNOWN;
    }

    /**
     * Create team under given parentId
     *
     * @param parentTeamId
     * @param teamName
     * @return
     * @throws MachinaException
     */
    public String createTeam(String parentTeamId, String teamName) throws MachinaException {
        String session;
        try{
            session = cxLegacyService.login(cxProperties.getUsername(), cxProperties.getPassword());
            cxLegacyService.createTeam(session, parentTeamId, teamName);
            return getTeamId(cxProperties.getTeam().concat("\\").concat(teamName));
        }
        catch (CheckmarxLegacyException e){
            log.error("Error occurring while logging into Legacy SOAP based WebService to create new team {} under parent {}", teamName, parentTeamId);
            throw new MachinaException("Error logging into legacy SOAP WebService for Team Creation");
        }
    }

    /**
     * Get scan configuration Id
     * @param configuration
     * @return
     * @throws MachinaException
     */
    public Integer getScanConfiguration(String configuration) throws MachinaException {
        HttpEntity httpEntity = new HttpEntity<>(createAuthHeaders());

        try {
            log.info("Retrieving Cx engineConfigurations");
            ResponseEntity<CxScanEngine[]> response = restTemplate.exchange(cxProperties.getUrl().concat(SCAN_CONFIGURATIONS), HttpMethod.GET, httpEntity, CxScanEngine[].class);
            CxScanEngine[] engines = response.getBody();
            if(engines == null){
                throw new MachinaException("Error obtaining Scan configurations");
            }
            for(CxScanEngine engine: engines){
                if(engine.getName().equals(configuration)){
                    log.info("Found xml/engine configuration {} with ID {}", configuration, engine.getId());
                    return engine.getId();
                }
            }
        }   catch (HttpStatusCodeException e) {
            log.error("Error occurred while retrieving engine configurations");
            log.error(ExceptionUtils.getStackTrace(e));
            throw new MachinaException("Error obtaining Team Id");
        }
        return UNKNOWN_INT;
    }

    public Integer getPresetId(String preset) throws MachinaException {
        HttpEntity httpEntity = new HttpEntity<>(createAuthHeaders());

        try {
            log.info("Retrieving Cx presets");
            ResponseEntity<CxPreset[]> response = restTemplate.exchange(cxProperties.getUrl().concat(PRESETS), HttpMethod.GET, httpEntity, CxPreset[].class);
            CxPreset[] cxPresets = response.getBody();
            if(cxPresets == null){
                throw new MachinaException("Error obtaining Team Id");
            }
            for(CxPreset cxPreset: cxPresets){
                if(cxPreset.getName().equals(preset)){
                    log.info("Found preset {} with ID {}", preset, cxPreset.getId());
                    return cxPreset.getId();
                }
            }
        }   catch (HttpStatusCodeException e) {
            log.error("Error occurred while retrieving presets");
            log.error(ExceptionUtils.getStackTrace(e));
        }
        return UNKNOWN_INT;
    }

    /**
     * Get Auth Token
     */
    private void getAuthToken(){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("username", cxProperties.getUsername());
        map.add("password", cxProperties.getPassword());
        map.add("grant_type", "password");
        map.add("scope", "sast_rest_api");
        map.add("client_id", "resource_owner_client");
        map.add("client_secret", cxProperties.getClientSecret());

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(map, headers);

        try {
            //get the access token
            CxAuthResponse response = restTemplate.postForObject(cxProperties.getUrl().concat(LOGIN), requestEntity,  CxAuthResponse.class);
            if(response == null){
                throw new InvalidCredentialsException();
            }
            token = response.getAccessToken();
            tokenExpires = LocalDateTime.now().plusSeconds(response.getExpiresIn()-500); //expire 500 seconds early
        }
        catch (HttpStatusCodeException e) {
            log.error("Error occurred white obtaining Access Token.  Possibly incorrect credentials");
            log.error(ExceptionUtils.getStackTrace(e));
            throw new InvalidCredentialsException();
        }
    }

    private boolean isTokenExpired(){
        if(tokenExpires == null){
            return true;
        }
        return !LocalDateTime.now().isAfter(tokenExpires);
    }

    private HttpHeaders createAuthHeaders(){
        //get a new access token if the current one is expired.
        if(token == null || isTokenExpired()){
            getAuthToken();
        }
        return new HttpHeaders() {{
            set("Authorization", "Bearer ".concat(token));
            setContentType(MediaType.APPLICATION_JSON_UTF8);
        }};
    }
}
