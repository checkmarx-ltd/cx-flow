package com.checkmarx.flow.service;

import com.checkmarx.flow.config.IastProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.dto.iast.manager.dto.*;
import com.checkmarx.flow.exception.JiraClientException;
import com.checkmarx.flow.utils.ScanUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    private final int UPDATE_TOKEN_SECONDS = 115; //Token live only 2 minutes


    private String iastUrlRoot;
    private final IastProperties iastProperties;

    private String authTokenHeader;
    private LocalDateTime authTokenHeaderDateGeneration;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());



    @Value("${iast-cmd}")
    private String iastCmd;

    @Value("${iast-scan-tag}")
    private String iastScanTag;

    @Autowired
    private JiraProperties jiraProperties;

    @Autowired
    private JiraService jiraService;


    public IastService(IastProperties iastProperties) {
        this.iastProperties = iastProperties;
        severityToPriority.put(0, "Low");
        severityToPriority.put(1, "Low");
        severityToPriority.put(2, "Medium");
        severityToPriority.put(3, "High");
    }

    @PostConstruct
    public void init() throws IOException, InterruptedException {

        if (iastProperties == null
                || ScanUtils.empty(iastProperties.getUrl())
                || ScanUtils.empty(iastProperties.getUsername())
                || ScanUtils.empty(iastProperties.getPassword())
                || ScanUtils.empty(iastProperties.getManagerPort())) {
            log.error("not all IAST properties doesn't setup.");
            throw new RuntimeException("IAST properties doesn't setup.");
        }

        this.iastUrlRoot = iastProperties.getUrl() + ":" + iastProperties.getManagerPort() + "/iast/";




        switch (iastCmd.toLowerCase(Locale.ROOT)) {
            case "get-scan-tag" :
                System.out.println(generateUniqTag());
                return;
            case "create-jira-issue" :
                stopScanAndCreateJiraIssueFromIastSummary(iastScanTag);
                break;
        }
    }

    public String generateUniqTag() {
        return "cx-flow-" + LocalDateTime.now() + "-" + Math.abs(random.nextLong());
    }

    public void stopScanAndCreateJiraIssueFromIastSummary(String scanTag) throws IOException {
        log.debug("start stopScanAndCreateJiraIssueFromIastSummary with scanTag:" + scanTag);
        Scan scan = null;
        try {
            scan = apiScansScanTagFinish(scanTag);
        } catch (FileNotFoundException e){
            log.warn("Can't find scan with current tag: " + scanTag, e);
        }

        if (scan == null){
            return;
        }

        getVulnerabilitiesAndCreateJiraIssue(scan);
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

    private void getVulnerabilitiesAndCreateJiraIssue(Scan scan) {
        try {
            final ScanVulnerabilities scanVulnerabilities = apiScanVulnerabilities(scan.getScanId());

            List<VulnerabilityInfo> vulnerabilities = scanVulnerabilities.getVulnerabilities();
            for (VulnerabilityInfo vulnerability : vulnerabilities) {

                if (vulnerability.getNewCount() != 0) {
                    final List<ResultInfo> scansResultsQuery = apiScanResults(scan.getScanId(), vulnerability.getId());

                    for (ResultInfo scansResultQuery : scansResultsQuery) {
                        if (scansResultQuery.isNewResult()) {

                            String title = scansResultQuery.getName() + ": " + scansResultQuery.getUrl();

                            String description = iastProperties.getUrl() + ":" + iastProperties.getManagerPort()
                                    + "/iast-ui/#!/project/" + scanVulnerabilities.getProjectId()
                                    + "/scan/" + scanVulnerabilities.getScanId()
                                    + "?rid=" + scansResultQuery.getResultId()
                                    + "&vid=" + vulnerability.getId();


                            jiraService.createIssue(
                                    jiraProperties.getProject(),
                                    title,
                                    description,
                                    iastProperties.getJira().getUsername(),
                                    severityToPriority.get(scansResultQuery.getSeverity().toValue()),
                                    iastProperties.getJira().getIssueType());

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



    private ScanVulnerabilities apiScanVulnerabilities(Long scanId) throws IOException {
        return objectMapper.readValue(resultGetBodyOfDefaultConnectionToIast("scans/" + scanId + "/vulnerabilities"), ScanVulnerabilities.class);
    }

    private List<ResultInfo> apiScanResults(Long scanId, Long vulnerabilityId) throws IOException {
        return objectMapper.readValue(resultGetBodyOfDefaultConnectionToIast("scans/" + scanId + "/results?queryId=" + vulnerabilityId),  new TypeReference<List<ResultInfo>>(){});
    }

    private List<ProjectSummary> apiProjectsSummary() throws IOException {
        return objectMapper.readValue(resultGetBodyOfDefaultConnectionToIast("projects/summary"), new TypeReference<List<ProjectSummary>>(){});
    }

    private Scan apiScansScanTagFinish(String scanTag) throws IOException {
        return objectMapper.readValue(resultPutBodyOfDefaultConnectionToIast("scans/scan-tag/" + scanTag + "/finish"), Scan.class);
    }

    private Scan apiScanAggregated(Long scanId) throws IOException {
        return objectMapper.readValue(resultGetBodyOfDefaultConnectionToIast("scans/" + scanId + "?aggregated=false"), Scan.class);
    }

    private Page<Scan> apiScanAggregation(Long projectId, int pageNumber) throws IOException {
        return objectMapper.readValue(resultGetBodyOfDefaultConnectionToIast("scans/aggregation?pageNumber=" + pageNumber + "&pageSize=100&projectId=" + projectId), new TypeReference<Page<Scan>>(){});
    }

    /**
     * Check need to update token.
     * If that is need, then update it.
     */
    private void checkAuthorization(){
        if (authTokenHeader == null || authTokenHeaderDateGeneration.plusSeconds(UPDATE_TOKEN_SECONDS).isAfter(LocalDateTime.now())){
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
