package com.checkmarx.flow.service;

import com.checkmarx.flow.config.CxProperties;
import com.checkmarx.flow.dto.Filter;
import com.checkmarx.flow.dto.ScanResults;
import com.checkmarx.flow.dto.cx.*;
import com.checkmarx.flow.dto.cx.xml.*;
import com.checkmarx.flow.exception.CheckmarxLegacyException;
import com.checkmarx.flow.exception.InvalidCredentialsException;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.utils.Constants;
import com.checkmarx.flow.utils.ScanUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
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
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.beans.ConstructorProperties;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Class used to orchestrate submitting scans and retrieving results
 */
@Service
public class CxService {

    public static final String UNKNOWN = "-1";
    public static final Integer UNKNOWN_INT = -1;
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
    public static final Map<String, Integer> STATUS_MAP = ImmutableMap.of(
            "TO VERIFY", 1,
            "CONFIRMED", 2,
            "URGENT", 3
    );
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(CxService.class);
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
    private static final String SCAN_SUMMARY = "/sast/scans/{id}/resultsStatistics";
    private static final String PROJECT_SCANS = "/sast/scans?projectId={pid}";
    private static final String SCAN_STATUS = "/sast/scans/{id}";
    private static final String REPORT = "/reports/sastScan";
    private static final String REPORT_DOWNLOAD = "/reports/sastScan/{id}";
    private static final String REPORT_STATUS = "/reports/sastScan/{id}/status";
    private static final String OSA_VULN = "Vulnerable_Library";
    private final CxProperties cxProperties;
    private final CxLegacyService cxLegacyService;
    private final RestTemplate restTemplate;
    private String token = null;
    private LocalDateTime tokenExpires = null;

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
    public Integer createScan(Integer projectId, boolean incremental, boolean isPublic, boolean forceScan, String comment) {
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
        } catch (HttpStatusCodeException e) {
            log.error("Error occurred while creating Scan for project {}, http error {}", projectId, e.getStatusCode());
            log.error(ExceptionUtils.getStackTrace(e));
        }
        return UNKNOWN_INT;
    }

    Integer getLastScanId(Integer projectId) {
        HttpEntity requestEntity = new HttpEntity<>(createAuthHeaders());

        log.info("Finding last Scan Id for project Id {}", projectId);
        try {
            ResponseEntity<String> response = restTemplate.exchange(cxProperties.getUrl().concat(SCAN)
                            .concat("?projectId=").concat(projectId.toString().concat("&scanStatus=")
                                    .concat(SCAN_STATUS_FINISHED.toString()).concat("&last=1")),
                    HttpMethod.GET, requestEntity, String.class);

            JSONArray arr = new JSONArray(response.getBody());
            if (arr.length() < 1) {
                return UNKNOWN_INT;
            }
            JSONObject obj = arr.getJSONObject(0);
            String id = obj.get("id").toString();
            log.info("Scan found with Id {} for project Id {}", id, projectId);
            return Integer.parseInt(id);
        } catch (HttpStatusCodeException e) {
            log.error("Error occurred while creating Scan for project {}, http error {}", projectId, e.getStatusCode());
            log.error(ExceptionUtils.getStackTrace(e));
        }
        return UNKNOWN_INT;
    }

    /**
     * Fetches scan data based on given scan identifier, as a {@link JSONObject}.
     * @param scanId scan ID to use
     * @return  populated {@link JSONObject} if scan data was fetched; empty otherwise.
     */
    protected JSONObject getScanData(String scanId) {
        HttpEntity requestEntity = new HttpEntity<>(createAuthHeaders());
        JSONObject scanData = new JSONObject();
        log.info("Fetching Scan data for Id {}", scanId);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    cxProperties.getUrl().concat(SCAN).concat("/").concat(scanId),
                    HttpMethod.GET, requestEntity, String.class);

            scanData = new JSONObject(response.getBody());
        } catch (HttpStatusCodeException e) {
            log.error("Error occurred while fetching Scan data for scan Id {}, http error {}", scanId, e.getStatusCode());
            log.error(ExceptionUtils.getStackTrace(e));
        }
        return scanData;
    }

    LocalDateTime getLastScanDate(Integer projectId) {
        HttpEntity requestEntity = new HttpEntity<>(createAuthHeaders());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        log.info("Finding last Scan Id for project Id {}", projectId);
        try {
            ResponseEntity<String> response = restTemplate.exchange(cxProperties.getUrl().concat(SCAN)
                            .concat("?projectId=").concat(projectId.toString().concat("&scanStatus=").concat(SCAN_STATUS_FINISHED.toString())
                                    .concat("&last=").concat(cxProperties.getIncrementalNumScans().toString())),
                    HttpMethod.GET, requestEntity, String.class);

            JSONArray arr = new JSONArray(response.getBody());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject scan = arr.getJSONObject(i);
                if (!scan.getBoolean("isIncremental")) {
                    JSONObject dateAndTime = scan.getJSONObject("dateAndTime");
                    log.debug("Last full scan was {}", dateAndTime);
                    //example: "finishedOn": "2018-06-18T01:09:12.707", Grab only first 19 digits due to inconsistency of checkmarx results
                    LocalDateTime d;
                    try {
                        String finishedOn = dateAndTime.getString("finishedOn");
                        finishedOn = finishedOn.substring(0, 19);
                        log.debug("finishedOn: {}", finishedOn);
                        d = LocalDateTime.parse(finishedOn, formatter);
                        return d;
                    } catch (DateTimeParseException e) {
                        log.warn("Error Parsing last finished scan time {}", e.getParsedString());
                        log.error(ExceptionUtils.getStackTrace(e));
                        return null;
                    }
                }
            }
        } catch (HttpStatusCodeException e) {
            log.error("Error occurred while creating Scan for project {}, http error {}", projectId, e.getStatusCode());
            log.error(ExceptionUtils.getStackTrace(e));
        } catch (NullPointerException e) {
            log.error("Error parsing JSON response for dateAndTime status. {}");
        }
        return null;
    }


    /**
     * Get the status of a given scanId
     *
     * @param scanId
     * @return
     */
    Integer getScanStatus(Integer scanId) {
        HttpEntity httpEntity = new HttpEntity<>(createAuthHeaders());
        log.debug("Retrieving xml status of xml Id {}", scanId);
        try {
            ResponseEntity<String> projects = restTemplate.exchange(cxProperties.getUrl().concat(SCAN_STATUS), HttpMethod.GET, httpEntity, String.class, scanId);
            JSONObject obj = new JSONObject(projects.getBody());
            JSONObject status = obj.getJSONObject("status");
            log.debug("status id {}, status name {}", status.getInt("id"), status.getString("name"));
            return status.getInt("id");
        } catch (HttpStatusCodeException e) {
            log.error("HTTP Status Code of {} while getting xml status for xml Id {}", e.getStatusCode(), scanId);
            log.error(ExceptionUtils.getStackTrace(e));
        } catch (JSONException e) {
            log.error("Error processing JSON Response");
            log.error(ExceptionUtils.getStackTrace(e));
        }
        return UNKNOWN_INT;
    }

    /**
     * Generate a scan report request (xml) based on ScanId
     *
     * @param scanId
     * @return
     */
    Integer createScanReport(Integer scanId) {
        String strJSON = "{'reportType':'XML', 'scanId':%d}";
        strJSON = String.format(strJSON, scanId);
        HttpEntity requestEntity = new HttpEntity<>(strJSON, createAuthHeaders());

        try {
            log.info("Creating report for xml Id {}", scanId);
            ResponseEntity<String> response = restTemplate.exchange(cxProperties.getUrl().concat(REPORT), HttpMethod.POST, requestEntity, String.class);
            JSONObject obj = new JSONObject(response.getBody());
            Integer id = obj.getInt("reportId");
            log.info("Report with Id {} created", id);
            return id;
        } catch (HttpStatusCodeException e) {
            log.error("HTTP Status Code of {} while creating xml report for xml Id {}", e.getStatusCode(), scanId);
            log.error(ExceptionUtils.getStackTrace(e));
        } catch (JSONException e) {
            log.error("Error processing JSON Response");
            log.error(ExceptionUtils.getStackTrace(e));
        }
        return UNKNOWN_INT;
    }

    /**
     * Get the status of a report being generated by reportId
     *
     * @param reportId
     * @return
     */
    Integer getReportStatus(Integer reportId) {
        HttpEntity httpEntity = new HttpEntity<>(createAuthHeaders());
        log.info("Retrieving report status of report Id {}", reportId);
        try {
            ResponseEntity<String> projects = restTemplate.exchange(cxProperties.getUrl().concat(REPORT_STATUS), HttpMethod.GET, httpEntity, String.class, reportId);
            JSONObject obj = new JSONObject(projects.getBody());
            JSONObject status = obj.getJSONObject("status");
            log.debug("Report status is {} - {} for report Id {}", status.getInt("id"), status.getString("value"), reportId);
            return status.getInt("id");
        } catch (HttpStatusCodeException e) {
            log.error("HTTP Status Code of {} while getting report status for report Id {}", e.getStatusCode(), reportId);
            log.error(ExceptionUtils.getStackTrace(e));
        } catch (JSONException e) {
            log.error("Error processing JSON Response");
            log.error(ExceptionUtils.getStackTrace(e));
        }
        return UNKNOWN_INT;
    }

    /**
     * Retrieve the report by reportId, mapped to ScanResults DTO, applying filtering as requested
     *
     * @param reportId
     * @param filter
     * @return
     * @throws MachinaException
     */
    ScanResults getReportContent(Integer reportId, List<Filter> filter) throws MachinaException {
        HttpHeaders headers = createAuthHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        HttpEntity httpEntity = new HttpEntity<>(headers);
        String session = null;
        try {
            /* login to legacy SOAP CX Client to retrieve description */
            session = cxLegacyService.login(cxProperties.getUsername(), cxProperties.getPassword());
        } catch (CheckmarxLegacyException e) {
            log.error("Error occurring while logging into Legacy SOAP based WebService - issue description will remain blank");
        }
        log.info("Retrieving report contents of report Id {} in XML format", reportId);
        try {
            ResponseEntity<String> resultsXML = restTemplate.exchange(cxProperties.getUrl().concat(REPORT_DOWNLOAD), HttpMethod.GET, httpEntity, String.class, reportId);
            String xml = resultsXML.getBody();
            log.debug("Report length: {}", xml.length());
            log.debug("Headers: {}", resultsXML.getHeaders().toSingleValueMap().toString());
            log.info("Report downloaded for report Id {}", reportId);
            log.debug("XML String Output: {}", xml);
            log.debug("Base64:", Base64.getEncoder().encodeToString(resultsXML.toString().getBytes()));
            /*Remove any chars before the start xml tag*/
            xml = xml.trim().replaceFirst("^([\\W]+)<", "<");
            log.debug("Report length: {}", xml.length());
            String xml2 = ScanUtils.cleanStringUTF8_2(xml);
            log.trace("XML2: {}", xml2);
            InputStream xmlStream = new ByteArrayInputStream(Objects.requireNonNull(xml2.getBytes()));

            /* protect against XXE */
            JAXBContext jc = JAXBContext.newInstance(CxXMLResultsType.class);
            XMLInputFactory xif = XMLInputFactory.newInstance();
            xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            xif.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            xif.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
            List<ScanResults.XIssue> xIssueList = new ArrayList<>();
            CxXMLResultsType cxResults;
            try {
                XMLStreamReader xsr = xif.createXMLStreamReader(xmlStream);
                Unmarshaller unmarshaller = jc.createUnmarshaller();
                cxResults = (CxXMLResultsType) unmarshaller.unmarshal(xsr);
            }catch (UnmarshalException e){
                log.warn("Issue occurred performing unmashall step - trying again {}", ExceptionUtils.getMessage(e));
                if(resultsXML.getBody() != null) {
                    log.error("Writing raw response from CX to {}", "CX_".concat(String.valueOf(reportId)));
                    ScanUtils.writeByte("CX_".concat(String.valueOf(reportId)), resultsXML.getBody().getBytes());
                    xml2 = ScanUtils.cleanStringUTF8(xml);
                    xmlStream = new ByteArrayInputStream(Objects.requireNonNull(xml2.getBytes()));
                    XMLStreamReader xsr = xif.createXMLStreamReader(xmlStream);
                    Unmarshaller unmarshaller = jc.createUnmarshaller();
                    cxResults = (CxXMLResultsType) unmarshaller.unmarshal(xsr);
                }
                else{
                    log.error("CX Response for report {} was null", reportId);
                    throw new MachinaException("CX report was empty (null)");
                }
            }

            ScanResults.ScanResultsBuilder cxScanBuilder = ScanResults.builder();
            cxScanBuilder.projectId(cxResults.getProjectId());
            cxScanBuilder.team(cxResults.getTeam());
            cxScanBuilder.project(cxResults.getProjectName());
            cxScanBuilder.link(cxResults.getDeepLink());
            cxScanBuilder.files(cxResults.getFilesScanned());
            cxScanBuilder.loc(cxResults.getLinesOfCodeScanned());
            cxScanBuilder.scanType(cxResults.getScanType());
            Map<String, Integer> summary = getIssues(filter, session, xIssueList, cxResults);
            cxScanBuilder.xIssues(xIssueList);
            cxScanBuilder.additionalDetails(getAdditionalScanDetails(cxResults));
            CxScanSummary scanSummary = getScanSummary(Integer.valueOf(cxResults.getScanId()));
            cxScanBuilder.scanSummary(scanSummary);
            ScanResults results = cxScanBuilder.build();
            //Add the summary map (severity, count)
            results.getAdditionalDetails().put(Constants.SUMMARY_KEY, summary);
            if (cxProperties.getPreserveXml()) {
                results.setOutput(xml);
            }
            return results;
        } catch (HttpStatusCodeException e) {
            log.error("HTTP Status Code of {} while getting downloading report contents of report Id {}", e.getStatusCode(), reportId);
            log.error(ExceptionUtils.getStackTrace(e));
            throw new MachinaException("Error while processing scan results for report Id ".concat(reportId.toString()));
        } catch (XMLStreamException | JAXBException e) {
            log.error("Error with XML report");
            log.error(ExceptionUtils.getStackTrace(e));
            throw new MachinaException("Error while processing scan results for report Id ".concat(reportId.toString()));
        } catch (NullPointerException e) {
            log.info("Null Error");
            log.error(ExceptionUtils.getStackTrace(e));
            throw new MachinaException("Error while processing scan results for report Id ".concat(reportId.toString()));
        }
    }

    /**
     * Creates a map of additional scan details, such as scanId, scan start date, scan risk,
     * scan risk severity, number of failed LOC, etc.
     *
     * @param cxResults the source to use
     * @return  a map of additional scan details
     */
    protected Map<String, Object> getAdditionalScanDetails(CxXMLResultsType cxResults) {
        // Add additional data from the results
        Map<String, Object> additionalDetails = new HashMap<String, Object>();
        additionalDetails.put("scanId", cxResults.getScanId());
        additionalDetails.put("scanStartDate", cxResults.getScanStart());
        JSONObject jsonObject = getScanData(cxResults.getScanId());
        if (jsonObject != null) {
            additionalDetails.put("scanRisk", String.valueOf(jsonObject.getInt("scanRisk")));
            additionalDetails.put("scanRiskSeverity", String.valueOf(jsonObject.getInt("scanRiskSeverity")));
            JSONObject scanState = jsonObject.getJSONObject("scanState");
            if (scanState != null) {
                additionalDetails.put("numFailedLoc", String.valueOf(scanState.getInt("failedLinesOfCode")));
            }
        }
        // Add custom field values if requested
        Map<String, String> customFields = getCustomFields(cxResults.getProjectId());
        additionalDetails.put("customFields", customFields);
        return additionalDetails;
    }

    /**
     * Returns custom field values read from a Checkmarx project, based on given projectId.
     *
     * @param projectId ID of project to lookup from Checkmarx
     * @return Map of custom field names to values
     */
    protected Map<String, String> getCustomFields(String projectId) {
        Map<String, String> customFields = new HashMap<String, String>();
        log.info("Fetching custom fields from project ID ".concat(projectId));
        CxProject cxProject = getProject(Integer.valueOf(projectId));
        if (cxProject != null) {
            for (CxProject.CustomField customField : cxProject.getCustomFields()) {
                customFields.put(customField.getName(), customField.getValue());
            }
        } else {
            log.error("Could not find project with ID ".concat(projectId));
        }
        return customFields;
    }

    /**
     * Parse CX report file, mapped to ScanResults DTO, applying filtering as requested
     *
     * @param file
     * @param filter
     * @return
     * @throws MachinaException
     */
    ScanResults getReportContent(File file, List<Filter> filter) throws MachinaException {

        if (file == null) {
            throw new MachinaException("File not provided for processing of results");
        }
        String session = null;
        try {
            if (!cxProperties.getOffline()) {
                session = cxLegacyService.login(cxProperties.getUsername(), cxProperties.getPassword());
            }
        } catch (CheckmarxLegacyException e) {
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
            cxScanBuilder.projectId(cxResults.getProjectId());
            cxScanBuilder.team(cxResults.getTeam());
            cxScanBuilder.project(cxResults.getProjectName());
            cxScanBuilder.link(cxResults.getDeepLink());
            cxScanBuilder.files(cxResults.getFilesScanned());
            cxScanBuilder.loc(cxResults.getLinesOfCodeScanned());
            cxScanBuilder.scanType(cxResults.getScanType());
            getIssues(filter, session, issueList, cxResults);
            cxScanBuilder.xIssues(issueList);
            cxScanBuilder.additionalDetails(getAdditionalScanDetails(cxResults));
            if (!cxProperties.getOffline() && !ScanUtils.empty(cxResults.getScanId())) {
                CxScanSummary scanSummary = getScanSummary(Integer.valueOf(cxResults.getScanId()));
                cxScanBuilder.scanSummary(scanSummary);
            }
            return cxScanBuilder.build();

        } catch (JAXBException e) {
            log.error("Error with XML report");
            log.error(ExceptionUtils.getStackTrace(e));
            throw new MachinaException("Error while processing scan results");
        } catch (NullPointerException e) {
            log.info("Null error");
            log.error(ExceptionUtils.getStackTrace(e));
            throw new MachinaException("Error while processing scan results");
        }
    }

    /**
     * @param vulnsFile
     * @param libsFile
     * @param filter
     * @return
     * @throws MachinaException
     */
    ScanResults getOsaReportContent(File vulnsFile, File libsFile, List<Filter> filter) throws MachinaException {
        if (vulnsFile == null || libsFile == null) {
            throw new MachinaException("Files not provided for processing of OSA results");
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            List<ScanResults.XIssue> issueList = new ArrayList<>();

            //convert json string to object
            List<CxOsa> osaVulns = objectMapper.readValue(vulnsFile, new TypeReference<List<CxOsa>>() {
            });
            List<CxOsaLib> osaLibs = objectMapper.readValue(libsFile, new TypeReference<List<CxOsaLib>>() {
            });
            Map<String, CxOsaLib> libsMap = getOsaLibsMap(osaLibs);
            Map<String, Integer> severityMap = ImmutableMap.of(
                    "LOW", 1,
                    "MEDIUM", 2,
                    "HIGH", 3
            );

            for (CxOsa o : osaVulns) {

                if (filterOsa(filter, o) && libsMap.containsKey(o.getLibraryId())) {
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
                    if (issueList.contains(issue)) {
                        issue = issueList.get(issueList.indexOf(issue));
                        //bump up the severity if required
                        if (severityMap.get(issue.getSeverity().toUpperCase(Locale.ROOT)) < severityMap.get(o.getSeverity().getName().toUpperCase(Locale.ROOT))) {
                            issue.setSeverity(o.getSeverity().getName());
                        }
                        issue.setCve(issue.getCve().concat(",").concat(o.getCveName()));
                        issue.getOsaDetails().add(details);
                    } else {//new
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

        } catch (IOException e) {
            log.error("Error parsing JSON OSA report");
            log.error(ExceptionUtils.getStackTrace(e));
            throw new MachinaException("Error while processing scan results");
        } catch (NullPointerException e) {
            log.info("Null error");
            log.error(ExceptionUtils.getStackTrace(e));
            throw new MachinaException("Error while processing scan results");
        }
    }

    private boolean filterOsa(List<Filter> filters, CxOsa osa) {
        boolean all = true;
        for (Filter f : filters) {
            if (f.getType().equals(Filter.Type.SEVERITY)) {
                all = false;  //if no SEVERITY filters, everything is applied
                if (f.getValue().equalsIgnoreCase(osa.getSeverity().getName())) {
                    return true;
                }
            }
        }
        return all;
    }

    private Map<String, CxOsaLib> getOsaLibsMap(List<CxOsaLib> libs) {
        Map<String, CxOsaLib> libMap = new HashMap<>();
        for (CxOsaLib o : libs) {
            libMap.put(o.getId(), o);
        }
        return libMap;
    }


    private List<CxOsa> getOSAVulnsByLibId(List<CxOsa> osaVulns, String libId) {
        List<CxOsa> vulns = new ArrayList<>();
        for (CxOsa v : osaVulns) {
            if (v.getLibraryId().equals(libId)) {
                vulns.add(v);
            }
        }
        return vulns;
    }


    /**
     * @param filter
     * @param session
     * @param cxIssueList
     * @param cxResults
     */
    private Map<String, Integer> getIssues(List<Filter> filter, String session, List<ScanResults.XIssue> cxIssueList, CxXMLResultsType cxResults) {
        Map<String, Integer> summary = new HashMap<>();
        for (QueryType q : cxResults.getQuery()) {
            if (checkFilter(q, filter)) {
                ScanResults.XIssue.XIssueBuilder xIssueBuilder = ScanResults.XIssue.builder();
                /*Top node of each issue*/
                for (ResultType r : q.getResult()) {
                    if (r.getFalsePositive().equalsIgnoreCase("FALSE") && checkFilter(r, filter)) {
                        if(!summary.containsKey(r.getSeverity())){
                            summary.put(r.getSeverity(), 0);
                        }
                        int x = summary.get(r.getSeverity());
                        x++;
                        summary.put(r.getSeverity(), x);
                        /*Map issue details*/
                        xIssueBuilder.cwe(q.getCweId());
                        xIssueBuilder.language(q.getLanguage());
                        xIssueBuilder.severity(q.getSeverity());
                        xIssueBuilder.vulnerability(q.getName());
                        xIssueBuilder.file(r.getFileName());
                        xIssueBuilder.severity(r.getSeverity());
                        xIssueBuilder.link(r.getDeepLink());

                        // Add additional details
                        Map<String, Object> additionalDetails = getAdditionalIssueDetails(q, r);
                        xIssueBuilder.additionalDetails(additionalDetails);

                        Map<Integer, String> details = new HashMap<>();
                        try {
                            /* Call the CX SOAP Service to get Issue Description*/
                            if (session != null) {
                                try {
                                    xIssueBuilder.description(this.getIssueDescription(session, Long.parseLong(cxResults.getScanId()), Long.parseLong(r.getPath().getPathId())));
                                } catch (HttpStatusCodeException e) {
                                    xIssueBuilder.description("");
                                }
                            } else {
                                xIssueBuilder.description("");
                            }
                            details.put(Integer.parseInt(r.getPath().getPathNode().get(0).getLine()),
                                    r.getPath().getPathNode().get(0).getSnippet().getLine().getCode());
                            xIssueBuilder.similarityId(r.getPath().getSimilarityId());
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
        return summary;
    }

    private Map<String, Object> getAdditionalIssueDetails(QueryType q, ResultType r) {
        Map<String, Object> additionalDetails = new HashMap<String, Object>();
        additionalDetails.put("categories", q.getCategories());
        String descUrl = ScanUtils.getHostWithProtocol(r.getDeepLink()) +
                "/CxWebClient/ScanQueryDescription.aspx?queryID=" + q.getId() +
                "&queryVersionCode=" + q.getQueryVersionCode() +
                "&queryTitle=" + q.getName();
        additionalDetails.put("recommendedFix", descUrl);

        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        // Source / Sink data
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("state", r.getState());
        PathType path = r.getPath();
        if (path != null) {
            List<PathNodeType> nodes = path.getPathNode();
            if (!nodes.isEmpty()) {
                result.put("source", getNodeData(nodes, 0));
                result.put("sink", getNodeData(nodes, nodes.size() - 1)); // Last node in dataFlow
            } else {
                log.debug(String.format("Result %s%s did not have node paths to process.", q.getName(), r.getNodeId()));
            }
        }
        results.add(result);
        additionalDetails.put("results", results);
        return additionalDetails;
    }

    /**
     * Creates a {@link Map} of data values - file, line, column and object,
     * based on the node index in the given dataflow path.
     *
     * @param nodes List of nodes representing the data flow from source to sink
     * @param nodeIndex index of node to fetch data from
     * @return  Map of data values - specifically file, line, column and object.
     */
    private Map<String, String> getNodeData(List<PathNodeType> nodes, int nodeIndex) {
        // Node data: file/line/object
        Map<String, String> nodeData = new HashMap<String, String>();
        PathNodeType node = nodes.get(nodeIndex);
        nodeData.put("file", node.getFileName());
        nodeData.put("line", node.getLine());
        nodeData.put("column", node.getColumn());
        nodeData.put("object", node.getName());
        return nodeData;
    }


    /**
     * Check if the highlevel Query resultset meets the filter criteria
     *
     * @param q
     * @param filters
     * @return
     */
    private boolean checkFilter(QueryType q, List<Filter> filters) {
        if (filters == null || filters.isEmpty()) {
            return true;
        }
        List<String> severity = new ArrayList<>();
        List<String> cwe = new ArrayList<>();
        List<String> category = new ArrayList<>();

        for(Filter f: filters){
            Filter.Type type = f.getType();
            String value = f.getValue();
            if(type.equals(Filter.Type.SEVERITY)){
                severity.add(value.toUpperCase(Locale.ROOT));
            }
            else if(type.equals(Filter.Type.TYPE)){
                category.add(value.toUpperCase(Locale.ROOT));
            }
            else if(type.equals(Filter.Type.CWE)){
                cwe.add(value.toUpperCase(Locale.ROOT));
            }
        }
        if (!severity.isEmpty() && !severity.contains(q.getSeverity().toUpperCase(Locale.ROOT))) {
            return false;
        }
        if (!cwe.isEmpty() && !cwe.contains(q.getCweId())) {
            return false;
        }

        return category.isEmpty() || category.contains(q.getName().toUpperCase(Locale.ROOT));
    }

    private boolean checkFilter(ResultType r, List<Filter> filters) {
        if (filters == null || filters.isEmpty()) {
            return true;
        }
        List<Integer> status = new ArrayList<>();

        for (Filter f : filters) {
            if (f.getType().equals(Filter.Type.STATUS)) {
                //handle New Status separately (this field is Status as opposed to State for the others
                if(f.getValue().equalsIgnoreCase("New")){
                    if(r.getStatus().equalsIgnoreCase("New")){
                        return true;
                    }
                }
                status.add(STATUS_MAP.get(f.getValue().toUpperCase(Locale.ROOT)));
            }
        }
        return status.isEmpty() || status.contains(Integer.parseInt(r.getState()));
    }

    private void checkForDuplicateIssue(List<ScanResults.XIssue> cxIssueList, ResultType r, Map<Integer, String> details, ScanResults.XIssue issue) {
        if (cxIssueList.contains(issue)) {
            /*Get existing issue of same vuln+filename*/
            ScanResults.XIssue existingIssue = cxIssueList.get(cxIssueList.indexOf(issue));
            /*If no reference exists for this particular line, append it to the details (line+snippet)*/
            if (!existingIssue.getDetails().containsKey(Integer.parseInt(r.getLine()))) {
                try {
                    existingIssue.getDetails().put(Integer.parseInt(r.getPath().getPathNode().get(0).getLine()),
                            r.getPath().getPathNode().get(0).getSnippet().getLine().getCode());
                } catch (NullPointerException e) {
                    details.put(Integer.parseInt(r.getLine()), null);
                }
            }
            // Copy additionalData.results from issue to existingIssue
            List<Map<String, Object>> results = (List<Map<String, Object>>) existingIssue.getAdditionalDetails().get("results");
            results.addAll((List<Map<String, Object>>)issue.getAdditionalDetails().get("results"));

        } else {
            cxIssueList.add(issue);
        }
    }

    private String getIssueDescription(String session, Long scanId, Long pathId) {
        return cxLegacyService.getDescription(session, scanId, pathId);
    }

    /**
     * Creates a CX Project.
     * <p>
     * Naming convention is namespace-repo-branch
     */
    public Integer createProject(String ownerId, String name) {
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
        } catch (HttpStatusCodeException e) {
            log.error("HTTP error code {} while creating project with name {} under owner id {}", e.getStatusCode(), name, ownerId);
            log.error(ExceptionUtils.getStackTrace(e));
        } catch (JSONException e) {
            log.error("Error processing JSON Response");
            log.error(ExceptionUtils.getStackTrace(e));
        }
        return UNKNOWN_INT;
    }


    /**
     * Get All Projects in Checkmarx
     *
     * @return
     */
    public CxProject[] getProjects() throws MachinaException {
        HttpEntity httpEntity = new HttpEntity<>(createAuthHeaders());
        try {
            ResponseEntity<CxProject[]> projects = restTemplate.exchange(cxProperties.getUrl().concat(PROJECTS), HttpMethod.GET, httpEntity, CxProject[].class);
            return projects.getBody();
        } catch (HttpStatusCodeException e) {
            log.warn("Error occurred while retrieving projects, http error {}", e.getStatusCode());
            log.error(ExceptionUtils.getStackTrace(e));
            throw new MachinaException("Error retrieving Projects");
        }
    }

    /**
     * Get All Projects in Checkmarx
     *
     * @return
     */
    public List<CxProject> getProjects(String teamId) throws MachinaException {
        HttpEntity httpEntity = new HttpEntity<>(createAuthHeaders());
        List<CxProject> teamProjects = new ArrayList<>();
        try {
            ResponseEntity<CxProject[]> projects = restTemplate.exchange(cxProperties.getUrl().concat(PROJECTS), HttpMethod.GET, httpEntity, CxProject[].class);

            if (projects.getBody() != null) {
                for (CxProject p : projects.getBody()) {
                    if (p.getTeamId().equals(teamId)) {
                        teamProjects.add(p);
                    }
                }
            }
            return teamProjects;
        } catch (HttpStatusCodeException e) {
            log.warn("Error occurred while retrieving projects, http error {}", e.getStatusCode());
            log.debug(ExceptionUtils.getStackTrace(e));
            throw new MachinaException("Error retrieving Projects");
        }
    }


    /**
     * Get All Projects under a specific team within Checkmarx
     * <p>
     * using TeamId does not work.
     *
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
    public Integer getProjectId(String ownerId, String name) {
        HttpEntity httpEntity = new HttpEntity<>(createAuthHeaders());
        try {
            ResponseEntity<String> projects = restTemplate.exchange(cxProperties.getUrl().concat(PROJECTS)
                    .concat("?projectName=").concat(name).concat("&teamId=").concat(ownerId), HttpMethod.GET, httpEntity, String.class);
            JSONArray arr = new JSONArray(projects.getBody());
            if (arr.length() > 1) {
                return UNKNOWN_INT;
            }
            JSONObject obj = arr.getJSONObject(0);
            return obj.getInt("id");
        } catch (HttpStatusCodeException e) {
            if (!e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                log.warn("Error occurred while retrieving project with name {}, http error {}", name, e.getStatusCode());
                log.error(ExceptionUtils.getStackTrace(e));
            }
        } catch (JSONException e) {
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
    public CxProject getProject(Integer projectId) {
        HttpEntity httpEntity = new HttpEntity<>(createAuthHeaders());
        try {
            ResponseEntity<CxProject> project = restTemplate.exchange(cxProperties.getUrl().concat(PROJECT), HttpMethod.GET, httpEntity, CxProject.class, projectId);
            return project.getBody();
        } catch (HttpStatusCodeException e) {
            log.error("Error occurred while retrieving project with id {}, http error {}", projectId, e.getStatusCode());
            log.error(ExceptionUtils.getStackTrace(e));
        } catch (JSONException e) {
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
    public boolean scanExists(Integer projectId) {
        HttpEntity httpEntity = new HttpEntity<>(createAuthHeaders());
        try {

            ResponseEntity<String> scans = restTemplate.exchange(cxProperties.getUrl().concat(PROJECT_SCANS), HttpMethod.GET, httpEntity, String.class, projectId);
            JSONArray jsonArray = new JSONArray(scans.getBody());
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject scan = jsonArray.getJSONObject(i);
                JSONObject status = scan.getJSONObject("status");
                int statusId = status.getInt("id");
                if (SCAN_STATUS_QUEUED.equals(statusId) || SCAN_STATUS_NEW.equals(statusId) || SCAN_STATUS_SCANNING.equals(statusId) ||
                        SCAN_STATUS_PRESCAN.equals(statusId) || SCAN_STATUS_SOURCE_PULLING.equals(statusId)) {
                    log.debug("Scan status is {}", statusId);
                    return true;
                }
            }
            log.debug("No scans in the queue that are in progress");
            return false;

        } catch (HttpStatusCodeException e) {
            log.error("Error occurred while retrieving project with id {}, http error {}", projectId, e.getStatusCode());
            log.error(ExceptionUtils.getStackTrace(e));
        } catch (JSONException e) {
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
    Integer createScanSetting(Integer projectId, Integer presetId, Integer engineConfigId) {
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
        } catch (HttpStatusCodeException e) {
            log.error("Error occurred while creating ScanSettings for project {}, http error {}", projectId, e.getStatusCode());
            log.error(ExceptionUtils.getStackTrace(e));
        } catch (JSONException e) {
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
        } catch (HttpStatusCodeException e) {
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
    public void uploadProjectSource(Integer projectId, File file) throws MachinaException {
        HttpHeaders headers = createAuthHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
        FileSystemResource value = new FileSystemResource(file);
        map.add("zippedSource", value);

        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(map, headers);

        try {
            log.info("Updating Source details for project Id {}", projectId);
            restTemplate.exchange(cxProperties.getUrl().concat(PROJECT_SOURCE_FILE), HttpMethod.POST, requestEntity, String.class, projectId);
        } catch (HttpStatusCodeException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            log.error("Error occurred while uploading Project source for project id {}.", projectId);
            throw new MachinaException("Error occurred while uploading source");
        }
    }

    void setProjectExcludeDetails(Integer projectId, List<String> excludeFolders, List<String> excludeFiles) {
        String excludeFilesStr = "";
        String excludeFolderStr = "";

        if (excludeFiles != null && !excludeFiles.isEmpty()) {
            excludeFilesStr = String.join(",", excludeFiles);
        }
        if (excludeFolders != null && !excludeFolders.isEmpty()) {
            excludeFolderStr = String.join(",", excludeFolders);
        }

        String strJSON = "{'excludeFoldersPattern':'%s', 'excludeFilesPattern':'%s'}";
        strJSON = String.format(strJSON, excludeFolderStr, excludeFilesStr);
        HttpEntity requestEntity = new HttpEntity<>(strJSON, createAuthHeaders());

        try {
            log.info("Updating Project folder and file exclusion details for project Id {}", projectId);
            restTemplate.exchange(cxProperties.getUrl().concat(PROJECT_EXCLUDE), HttpMethod.PUT, requestEntity, String.class, projectId);
        } catch (HttpStatusCodeException e) {
            log.error("Error occurred while updating Project source info for project {}.", projectId);
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
    public String getTeamId(String teamPath) throws MachinaException {
        HttpEntity httpEntity = new HttpEntity<>(createAuthHeaders());
        try {
            log.info("Retrieving Cx teams");
            ResponseEntity<CxTeam[]> response = restTemplate.exchange(cxProperties.getUrl().concat(TEAMS), HttpMethod.GET, httpEntity, CxTeam[].class);
            CxTeam[] teams = response.getBody();
            if (teams == null) {
                throw new MachinaException("Error obtaining Team Id");
            }
            for (CxTeam team : teams) {
                if (team.getFullName().equals(teamPath)) {
                    log.info("Found team {} with ID {}", teamPath, team.getId());
                    return team.getId();
                }
            }
        } catch (HttpStatusCodeException e) {
            log.error("Error occurred while retrieving Teams");
            log.error(ExceptionUtils.getStackTrace(e));
        }
        log.info("No team was found for {}", teamPath);
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
        try {
            session = cxLegacyService.login(cxProperties.getUsername(), cxProperties.getPassword());
            cxLegacyService.createTeam(session, parentTeamId, teamName);
            return getTeamId(cxProperties.getTeam().concat("\\").concat(teamName));
        } catch (CheckmarxLegacyException e) {
            log.error("Error occurring while logging into Legacy SOAP based WebService to create new team {} under parent {}", teamName, parentTeamId);
            throw new MachinaException("Error logging into legacy SOAP WebService for Team Creation");
        }
    }

    /**
     * Get scan configuration Id
     *
     * @param configuration
     * @return
     * @throws MachinaException
     */
    public Integer getScanConfiguration(String configuration) throws MachinaException {
        HttpEntity httpEntity = new HttpEntity<>(createAuthHeaders());
        int defaultConfigId = UNKNOWN_INT;
        try {
            log.info("Retrieving Cx engineConfigurations");
            ResponseEntity<CxScanEngine[]> response = restTemplate.exchange(cxProperties.getUrl().concat(SCAN_CONFIGURATIONS), HttpMethod.GET, httpEntity, CxScanEngine[].class);
            CxScanEngine[] engines = response.getBody();
            if (engines == null) {
                throw new MachinaException("Error obtaining Scan configurations");
            }
            for(CxScanEngine engine: engines){
                String engineName = engine.getName();
                int engineId = engine.getId();
                if(engineName.equalsIgnoreCase(configuration)){
                    log.info("Found xml/engine configuration {} with ID {}", configuration, engineId);
                    return engineId;
                }
            }
            log.warn("No scan configuration found for {}", configuration);
            log.warn("Scan Configuration {} with ID {} will be used instead", Constants.CX_DEFAULT_CONFIGURATION, defaultConfigId);
            return defaultConfigId;
        }   catch (HttpStatusCodeException e) {
            log.error("Error occurred while retrieving engine configurations");
            log.error(ExceptionUtils.getStackTrace(e));
            throw new MachinaException("Error obtaining Configuration Id");
        }
    }

    public Integer getPresetId(String preset) throws MachinaException {
        HttpEntity httpEntity = new HttpEntity<>(createAuthHeaders());
        int defaultPresetId = UNKNOWN_INT;
        try {
            log.info("Retrieving Cx presets");
            ResponseEntity<CxPreset[]> response = restTemplate.exchange(cxProperties.getUrl().concat(PRESETS), HttpMethod.GET, httpEntity, CxPreset[].class);
            CxPreset[] cxPresets = response.getBody();
            if (cxPresets == null) {
                throw new MachinaException("Error obtaining Team Id");
            }
            for(CxPreset cxPreset: cxPresets){
                String presetName = cxPreset.getName();
                int presetId = cxPreset.getId();
                if(presetName.equalsIgnoreCase(preset)){
                    log.info("Found preset {} with ID {}", preset, presetId);
                    return cxPreset.getId();
                }
                if(presetName.equalsIgnoreCase(Constants.CX_DEFAULT_PRESET)){
                    defaultPresetId =  presetId;
                }
            }
            log.warn("No Preset was found for {}", preset);
            log.warn("Default Preset {} with ID {} will be used instead", Constants.CX_DEFAULT_PRESET, defaultPresetId);
            return defaultPresetId;
        }   catch (HttpStatusCodeException e) {
            log.error("Error occurred while retrieving presets");
            log.error(ExceptionUtils.getStackTrace(e));
            throw new MachinaException("Error obtaining Preset Id");
        }
    }

    /**
     * Get scan summary for given scanId
     *
     * @param scanId
     * @return
     * @throws MachinaException
     */
    public CxScanSummary getScanSummary(Integer scanId) throws MachinaException {
        HttpEntity httpEntity = new HttpEntity<>(createAuthHeaders());
        try {
            log.debug("Retrieving scan summary for scan id: {}", scanId);
            ResponseEntity<CxScanSummary> response = restTemplate.exchange(cxProperties.getUrl().concat(SCAN_SUMMARY), HttpMethod.GET, httpEntity, CxScanSummary.class, scanId);
            CxScanSummary scanSummary = response.getBody();
            if (scanSummary == null) {
                log.warn("No scan summary was available for scan id: {}", scanId);
            }
            return scanSummary;
        } catch (HttpStatusCodeException e) {
            log.error("Error occurred while retrieving scan summary for scan id: {}", scanId);
            log.error(ExceptionUtils.getStackTrace(e));
        }
        log.warn("No scan summary was available for scan id: {}", scanId);
        return null;
    }


    /**
     * Get Auth Token
     */
    private void getAuthToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("username", cxProperties.getUsername());
        map.add("password", cxProperties.getPassword());
        map.add("grant_type", "password");
        map.add("scope", "sast_rest_api");
        map.add("client_id", "resource_owner_client");
        map.add("client_secret", cxProperties.getClientSecret());

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(map, headers);

        try {
            //get the access token
            log.info("Logging into Checkmarx {}", cxProperties.getUrl().concat(LOGIN));
            CxAuthResponse response = restTemplate.postForObject(cxProperties.getUrl().concat(LOGIN), requestEntity, CxAuthResponse.class);
            if (response == null) {
                throw new InvalidCredentialsException();
            }
            token = response.getAccessToken();
            tokenExpires = LocalDateTime.now().plusSeconds(response.getExpiresIn()-500); //expire 500 seconds early
        }
        catch (NullPointerException | HttpStatusCodeException e) {
            log.error("Error occurred white obtaining Access Token.  Possibly incorrect credentials");
            log.error(ExceptionUtils.getStackTrace(e));
            throw new InvalidCredentialsException();
        }
    }

    private boolean isTokenExpired() {
        if (tokenExpires == null) {
            return true;
        }
        return LocalDateTime.now().isAfter(tokenExpires);
    }

    private HttpHeaders createAuthHeaders() {
        //get a new access token if the current one is expired.
        if (token == null || isTokenExpired()) {
            getAuthToken();
        }
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.AUTHORIZATION, "Bearer ".concat(token));
        httpHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
        return httpHeaders;
    }
}
