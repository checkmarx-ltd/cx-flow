package com.checkmarx.flow.service;

import com.checkmarx.flow.config.IastProperties;
import com.checkmarx.flow.dto.iast.manager.dto.ResultInfo;
import com.checkmarx.flow.dto.iast.manager.dto.Scan;
import com.checkmarx.flow.dto.iast.manager.dto.ScanVulnerabilities;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
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
import java.util.List;

@Slf4j
@Service
public class IastServiceRequests {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());


    @Setter
    private int updateTokenSeconds;

    private final IastProperties iastProperties;
    private String authTokenHeader;
    private LocalDateTime authTokenHeaderDateGeneration;
    private String iastUrlRoot;

    public IastServiceRequests(IastProperties iastProperties) {
        this.iastProperties = iastProperties;
    }

    @PostConstruct
    public void init() throws IOException, InterruptedException {

        this.updateTokenSeconds = iastProperties.getUpdateTokenSeconds();
        this.iastUrlRoot = iastProperties.getUrl() + ":" + iastProperties.getManagerPort() + "/iast/";
    }

    public ScanVulnerabilities apiScanVulnerabilities(Long scanId) throws IOException {
        return objectMapper.readValue(resultGetBodyOfDefaultConnectionToIast("scans/" + scanId + "/vulnerabilities"), ScanVulnerabilities.class);
    }

    public List<ResultInfo> apiScanResults(Long scanId, Long vulnerabilityId) throws IOException {
        return objectMapper.readValue(resultGetBodyOfDefaultConnectionToIast("scans/" + scanId + "/results?queryId=" + vulnerabilityId), new TypeReference<List<ResultInfo>>() {
        });
    }

    public Scan apiScansScanTagFinish(String scanTag) throws IOException {
        return objectMapper.readValue(resultPutBodyOfDefaultConnectionToIast("scans/scan-tag/" + scanTag + "/finish"), Scan.class);
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

    private String resultPutBodyOfDefaultConnectionToIast(String urlConnection) throws IOException {
        return resultBodyOfDefaultConnectionToIast(urlConnection, "PUT");
    }

    private String resultBodyOfDefaultConnectionToIast(String urlConnection, String requestMethod) throws IOException {
        log.trace("request to IAST manager. Url:" + urlConnection);
        HttpURLConnection con = generateConnectionToIast(urlConnection, requestMethod);


        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            return response.toString();
        } catch (IOException e){
            String msg = "Can't stop scan. " + e.getMessage();
            if (e.getMessage().contains("/iast/scans/scan-tag/")) {
                msg = "Scan have to start and you must use a unique scan tag. You may have used a non-unique scan tag. " + msg;
            }
            throw new IOException(msg, e);
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
}
