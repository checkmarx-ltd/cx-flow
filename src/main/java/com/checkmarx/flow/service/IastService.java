package com.checkmarx.flow.service;

import com.checkmarx.flow.config.IastProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.iast.manager.dto.*;
import com.checkmarx.flow.exception.JiraClientException;
import com.checkmarx.flow.utils.ScanUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;


@Slf4j
@Service
public class IastService {
    private final String NEW_LOW = "newLow";
    private final String NEW_MEDIUM = "newMedium";
    private final String NEW_HIGH = "newHigh";
    private final Map<Integer, String> severityToPriority = new HashMap<>();

    private Random random = new Random();

    private int updateTokenSeconds;

    private String iastUrlRoot;
    private final IastProperties iastProperties;

    private String authTokenHeader;
    private LocalDateTime authTokenHeaderDateGeneration;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Autowired
    private JiraProperties jiraProperties;

    @Autowired
    private JiraService jiraService;

    public IastService(IastProperties iastProperties) {
        this.iastProperties = iastProperties;
    }

    @PostConstruct
    public void init() throws IOException, InterruptedException {
        checkRequiredParameters();

        severityToPriority.put(0, "Low");
        severityToPriority.put(1, "Low");
        severityToPriority.put(2, "Medium");
        severityToPriority.put(3, "High");

        updateTokenSeconds = iastProperties.getUpdateTokenSeconds();
        this.iastUrlRoot = iastProperties.getUrl() + ":" + iastProperties.getManagerPort() + "/iast/";
    }

    private void checkRequiredParameters(){
        if (iastProperties == null){
            log.error("IAST properties doesn't setup.");
            throw new RuntimeException("IAST properties doesn't setup.");
        }

        if (ScanUtils.empty(iastProperties.getUrl())
                || ScanUtils.empty(iastProperties.getUsername())
                || ScanUtils.empty(iastProperties.getPassword())
                || ScanUtils.empty(iastProperties.getManagerPort())
                || ScanUtils.emptyObj(iastProperties.getUpdateTokenSeconds())) {
            log.error("not all IAST properties doesn't setup.");
            throw new RuntimeException("IAST properties doesn't setup.");
        }
    }

    public String generateUniqTag() {
        return "cx-flow-" + LocalDateTime.now() + "-" + Math.abs(random.nextLong());
    }

    public void stopScanAndCreateJiraIssueFromIastSummary(ScanRequest request, String scanTag) throws IOException {
        log.debug("start stopScanAndCreateJiraIssueFromIastSummary with scanTag:" + scanTag);
        Scan scan = null;
        try {
            scan = apiScansScanTagFinish(scanTag);
        } catch (FileNotFoundException e) {
            log.warn("Can't find scan with current tag: " + scanTag, e);
        }

        if (scan == null) {
            return;
        }

        getVulnerabilitiesAndCreateJiraIssue(request, scan);
    }


    public void stopScanAndCreateJiraIssueFromIastSummary(String scanTag) throws IOException {
        stopScanAndCreateJiraIssueFromIastSummary(null, scanTag);
    }

    private void authorization() {
        try {
            URL url = new URL(iastUrlRoot + "login");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; utf-8");
            con.setRequestProperty("Accept", "application/json");
            con.setDoOutput(true);
            JSONObject params = new JSONObject();
            params.put("userName", iastProperties.getUsername());
            params.put("password", iastProperties.getPassword());
            String jsonInputString = params.toString();
            try (OutputStream os = con.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                authTokenHeader = response.substring(1, response.length() - 1); // remove first and last "
                authTokenHeaderDateGeneration = LocalDateTime.now();
            }

        } catch (IOException e) {
            log.error("Can't authorize in IAST server", e);
        }
    }

    private void getVulnerabilitiesAndCreateJiraIssue(ScanRequest request, Scan scan) {
        try {
            final ScanVulnerabilities scanVulnerabilities = apiScanVulnerabilities(scan.getScanId());

            List<VulnerabilityInfo> vulnerabilities = scanVulnerabilities.getVulnerabilities();
            for (VulnerabilityInfo vulnerability : vulnerabilities) {

                if (vulnerability.getNewCount() == 0) {
                    final List<ResultInfo> scansResultsQuery = apiScanResults(scan.getScanId(), vulnerability.getId());

                    for (ResultInfo scansResultQuery : scansResultsQuery) {
                        if (!scansResultQuery.isNewResult()) {
                            createJiraIssue(scanVulnerabilities, request, scansResultQuery, vulnerability, scan);
                        }
                    }
                }
            }
        } catch (JiraClientException e) {
            log.error("Can't create Jira issue", e);
        } catch (IOException e) {
            log.error("Can't send api request", e);
        }
    }


    private void createJiraIssue(ScanVulnerabilities scanVulnerabilities,
                                 ScanRequest request,
                                 ResultInfo scansResultQuery,
                                 VulnerabilityInfo vulnerability,
                                 Scan scan) throws JiraClientException {

        String title = scansResultQuery.getName() + ": " + scansResultQuery.getUrl();

        String assignee;
        String issueType;
        String project;
        if (request != null && request.getBugTracker() != null) {
            BugTracker bugTracker = request.getBugTracker();

            assignee = bugTracker.getAssignee() != null ? bugTracker.getAssignee()
                    : jiraProperties.getUsername();

            issueType = bugTracker.getIssueType() != null ? bugTracker.getIssueType()
                    : jiraProperties.getIssueType();

            project = bugTracker.getProjectKey() != null ? bugTracker.getProjectKey()
                    : jiraProperties.getProject();

        } else {
            assignee = jiraProperties.getUsername();
            issueType = jiraProperties.getIssueType();
            project = jiraProperties.getProject();
        }


        String description = iastProperties.getUrl() + ":" + iastProperties.getManagerPort()
                + "/iast-ui/#!/project/" + scanVulnerabilities.getProjectId()
                + "/scan/" + scanVulnerabilities.getScanId()
                + "?rid=" + scansResultQuery.getResultId()
                + "&vid=" + vulnerability.getId();

        jiraService.createIssue(
                project,
                title,
                description,
                assignee,
                severityToPriority.get(scansResultQuery.getSeverity().toValue()),
                issueType,
                Collections.singletonList(scan.getTag()));
    }

    private ScanVulnerabilities apiScanVulnerabilities(Long scanId) throws IOException {
        return objectMapper.readValue(resultGetBodyOfDefaultConnectionToIast("scans/" + scanId + "/vulnerabilities"), ScanVulnerabilities.class);
    }

    private List<ResultInfo> apiScanResults(Long scanId, Long vulnerabilityId) throws IOException {
        return objectMapper.readValue(resultGetBodyOfDefaultConnectionToIast("scans/" + scanId + "/results?queryId=" + vulnerabilityId), new TypeReference<List<ResultInfo>>() {
        });
    }

    private List<ProjectSummary> apiProjectsSummary() throws IOException {
        return objectMapper.readValue(resultGetBodyOfDefaultConnectionToIast("projects/summary"), new TypeReference<List<ProjectSummary>>() {
        });
    }

    private Scan apiScansScanTagFinish(String scanTag) throws IOException {
        return objectMapper.readValue(resultPutBodyOfDefaultConnectionToIast("scans/scan-tag/" + scanTag + "/finish"), Scan.class);
    }

    private Scan apiScanAggregated(Long scanId) throws IOException {
        return objectMapper.readValue(resultGetBodyOfDefaultConnectionToIast("scans/" + scanId + "?aggregated=false"), Scan.class);
    }

    private Page<Scan> apiScanAggregation(Long projectId, int pageNumber) throws IOException {
        return objectMapper.readValue(resultGetBodyOfDefaultConnectionToIast("scans/aggregation?pageNumber=" + pageNumber + "&pageSize=100&projectId=" + projectId), new TypeReference<Page<Scan>>() {
        });
    }

    /**
     * Check need to update token.
     * If that is need, then update it.
     */
    private void checkAuthorization() {
        if (authTokenHeader == null || authTokenHeaderDateGeneration.plusSeconds(updateTokenSeconds).isBefore(LocalDateTime.now())) {
            authorization();
        }
    }

    private String resultGetBodyOfDefaultConnectionToIast(String urlConnection) throws IOException {
        return resultBodyOfDefaultConnectionToIast(urlConnection, "GET");
    }

    private String resultPostBodyOfDefaultConnectionToIast(String urlConnection) throws IOException {
        return resultBodyOfDefaultConnectionToIast(urlConnection, "POST");
    }

    private String resultPutBodyOfDefaultConnectionToIast(String urlConnection) throws IOException {
        return resultBodyOfDefaultConnectionToIast(urlConnection, "PUT");
    }

    private String resultBodyOfDefaultConnectionToIast(String urlConnection, String requestMethod) throws IOException {
        log.info("request to IAST manager. Url:" + urlConnection);
        HttpURLConnection con = generateConnectionToIast(urlConnection, requestMethod);

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            return response.toString();
        }
    }

    private HttpURLConnection generateConnectionToIast(String urlConnection, String requestMethod) throws IOException {
        checkAuthorization();

        URL url = new URL(iastUrlRoot + urlConnection);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod(requestMethod);
        con.setRequestProperty("Content-Type", "application/json; utf-8");
        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(true);

        con.setRequestProperty("Authorization", "Bearer " + authTokenHeader);

        return con;
    }
}
