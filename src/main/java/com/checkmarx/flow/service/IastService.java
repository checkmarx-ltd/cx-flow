package com.checkmarx.flow.service;

import com.checkmarx.flow.config.IastProperties;
import com.checkmarx.flow.exception.JiraClientException;
import com.checkmarx.flow.utils.ScanUtils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@Service
public class IastService {

    private final String NEW_LOW = "newLow";
    private final String NEW_MEDIUM = "newMedium";
    private final String NEW_HIGH = "newHigh";
    private final Map<Integer, String> severityToPriority = new HashMap<>();

    private final int UPDATE_TOKEN_MINUTES = 2; //Token live only 2 minutes


    private String iastUrlRoot;
    private final IastProperties iastProperties;

    private String authTokenHeader;
    private LocalDateTime authTokenHeaderDateGeneration;


    @Autowired
    private JiraService jiraService;


    public IastService(IastProperties iastProperties) {
        this.iastProperties = iastProperties;
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





        stopScanAndGetSummaryFromIast();

    }

    private void stopScanAndGetSummaryFromIast() throws IOException, InterruptedException {
        Long scanId = stopScanWithTag("11111");
        if (scanId == null){
            return;
        }

        JSONObject scanAggregated = getScanAggregated(scanId);
        getVulnerabilitiesAndCreateJiraIssue(scanId);
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
                String responseLine = null;
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

    private void getVulnerabilitiesAndCreateJiraIssue(Long scanId) throws IOException {
        final JSONObject scanVulnerabilities =
                new JSONObject(resultGetBodyOfDefaultConnectionToIast("scans/" + scanId + "/vulnerabilities"));

        final JSONArray vulnerabilities = scanVulnerabilities.getJSONArray("vulnerabilities");
        for (int i = 0; i < vulnerabilities.length(); i++) {
            JSONObject vulnerability = vulnerabilities.getJSONObject(i);

            //TODO: have to be "!= 0"
            if (vulnerability.getNumber("newCount").intValue() == 0) {
                final JSONArray scansResultsQuery =
                        new JSONArray(resultGetBodyOfDefaultConnectionToIast("scans/" + scanId + "/results?queryId=" + vulnerability.getNumber("id").intValue()));
                for (int j = 0; j < scansResultsQuery.length(); j++) {
                    JSONObject scansResultQuery = scansResultsQuery.getJSONObject(j);

                    //TODO: remove !
                    if (!scansResultQuery.getBoolean("newResult")){
                        try {

                            String title = scansResultQuery.getString("name") + ": " + scansResultQuery.getString("url");

                            String description =    iastProperties.getUrl() + ":" + iastProperties.getManagerPort()
                                    + "/iast-ui/#!/project/" + scanVulnerabilities.getNumber("projectId")
                                    + "/scan/" + scanVulnerabilities.getNumber("scanId")
                                    + "?rid=" + scansResultQuery.getNumber("resultId")
                                    + "&vid=" + vulnerability.getNumber("id");


                            jiraService.createIssue(title, description, severityToPriority.get(scansResultQuery.getNumber("severity").intValue()));
                        } catch (JiraClientException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

    }

    private Long stopScanWithTag(String tag) throws IOException, InterruptedException {
        JSONArray projectsSummary = getAllScans();
        for (int i = 0; i < projectsSummary.length(); i++) {
            try {

            JSONObject project = projectsSummary.getJSONObject(i);
            JSONObject projectRunning = project.getJSONObject("running");


//                TODO: Decomment work with tags
//            String projectTag = projectRunning.getString("tag");
//            if (tag.equals(projectTag)){
            if (projectRunning.getNumber("riskScore").intValue() != 0) {
                Long scanId = projectRunning.getNumber("scanId").longValue();

                for (int j = 0; j < 100; j++) {
                    try {
                        finishScanById(scanId);
                    } catch (RuntimeException e){
                        log.trace("Can't stop scan. That is normal situation when we already stop that scan.", e);
                    }
                    if (finishDoneScanById(scanId)){
                        return scanId;
                    } else {
                        Thread.sleep(500);
                    }
                }
            }

            } catch (JSONException e){
                log.trace("Can't find element. That is normal situation when we can't find 'tag'.", e);
            }
        }
        return null;
    }

    private void finishScanById(Long scanId) throws IOException {
        resultPutBodyOfDefaultConnectionToIast("scans/" + scanId + "/finish");
    }

    private Boolean finishDoneScanById(Long scanId) throws IOException {
        HttpURLConnection con = generateConnectionToIast("scans/" + scanId + "/finish/done", "PUT");
        int responseCode = con.getResponseCode();
        if (responseCode == 400){
            return true;
        }
        return false;
    }

    private JSONArray getAllScans() throws IOException {
        return new JSONArray(resultGetBodyOfDefaultConnectionToIast("projects/summary"));
    }

    private JSONObject getScanAggregated(Long scanId) throws IOException {
        return new JSONObject(resultGetBodyOfDefaultConnectionToIast("scans/" + scanId + "?aggregated=false"));
    }

    /**
     * Check need to update token.
     * If that is need, then update it.
     */
    private void checkAuthorization(){
        if (authTokenHeader == null || authTokenHeaderDateGeneration.plusMinutes(UPDATE_TOKEN_MINUTES).isAfter(LocalDateTime.now())){
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
        HttpURLConnection con = generateConnectionToIast(urlConnection, requestMethod);

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine = null;
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
