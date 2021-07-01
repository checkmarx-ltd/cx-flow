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
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
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

    private boolean sslEnabledOnIast;
    private SSLContext context;

    public IastServiceRequests(IastProperties iastProperties) {
        this.iastProperties = iastProperties;
    }

    @PostConstruct
    public void init() throws IOException, InterruptedException {
        if (iastProperties != null
                && iastProperties.getUpdateTokenSeconds() != null
                && iastProperties.getUrl() != null
                && iastProperties.getManagerPort() != null) {
            this.updateTokenSeconds = iastProperties.getUpdateTokenSeconds();
            this.iastUrlRoot = iastProperties.getUrl() + ":" + iastProperties.getManagerPort() + "/iast/";
            sslEnabledOnIast = iastUrlRoot.contains("https://");
            loadCerToKeyStoreAndSslContext();
        }
    }

    public ScanVulnerabilities apiScanVulnerabilities(Long scanId) throws IOException {
        return objectMapper.readValue(resultGetBodyOfDefaultConnectionToIast("scans/" + scanId + "/vulnerabilities"),
                ScanVulnerabilities.class);
    }

    public List<ResultInfo> apiScanResults(Long scanId, Long vulnerabilityId) throws IOException {
        return objectMapper.readValue(
                resultGetBodyOfDefaultConnectionToIast("scans/" + scanId + "/results?queryId=" + vulnerabilityId),
                new TypeReference<List<ResultInfo>>() {
                });
    }

    public Scan apiScansScanTagFinish(String scanTag) throws IOException {
        return objectMapper
                .readValue(resultPutBodyOfDefaultConnectionToIast("scans/scan-tag/" + scanTag + "/finish"), Scan.class);
    }

    /**
     * Update IAST authorization token if needed.
     */
    private void checkAuthorization() {
        if (authTokenHeader == null ||
                authTokenHeaderDateGeneration.plusSeconds(updateTokenSeconds).isBefore(LocalDateTime.now())) {
            authorization();
        }
    }

    private String resultGetBodyOfDefaultConnectionToIast(String urlConnection) throws IOException {
        return resultBodyOfDefaultConnectionToIast(urlConnection, HttpMethod.GET);
    }

    private String resultPutBodyOfDefaultConnectionToIast(String urlConnection) throws IOException {
        return resultBodyOfDefaultConnectionToIast(urlConnection, HttpMethod.PUT);
    }

    private String resultBodyOfDefaultConnectionToIast(String urlConnection, HttpMethod requestMethod)
            throws IOException {
        log.trace("request to IAST manager. Url:" + urlConnection);
        URLConnection con = generateConnectionToIast(urlConnection, requestMethod);
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            return response.toString();
        } catch (IOException e) {
            String msg = "Can't stop scan. " + e.getMessage();
            if (e.getMessage().contains("/iast/scans/scan-tag/")) {
                msg = "Scan have to start and you must use a unique scan tag. " +
                        "You may have used a non-unique scan tag. " + msg;
            }
            throw new IOException(msg, e);
        }
    }

    private URLConnection generateConnectionToIast(String urlConnection, HttpMethod requestMethod) throws IOException {
        checkAuthorization();
        URL url = new URL(iastUrlRoot + urlConnection);
        URLConnection con = createUrlConnection(url, requestMethod);
        con.setRequestProperty("Authorization", "Bearer " + authTokenHeader);
        return con;
    }

    private void authorization() {
        try {
            URL url = new URL(iastUrlRoot + "login");
            URLConnection con = createUrlConnection(url, HttpMethod.POST);
            JSONObject params = new JSONObject();
            params.put("userName", iastProperties.getUsername());
            params.put("password", iastProperties.getPassword());
            String jsonInputString = params.toString();
            try (OutputStream os = con.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
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

    /**
     * Loads a self-signed SSL certificate if it was provided.
     * <p><b>Note:</b> The certificate is not validated in any way.
     */
    private void loadCerToKeyStoreAndSslContext() {
        final String cerFilePath = iastProperties.getSslCertificateFilePath();
        if (StringUtils.isEmpty(cerFilePath)) {
            return;
        }
        final String normalizedPath = FilenameUtils.separatorsToSystem(cerFilePath);
        try (FileInputStream fin = new FileInputStream(normalizedPath)) {
            CertificateFactory f = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) f.generateCertificate(fin);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(null);
            ks.setCertificateEntry("CxIAST-CxFlow", certificate);
            tmf.init(ks);
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, tmf.getTrustManagers(), null);
            context = sslContext;
        } catch (Exception e) {
            log.error("Couldn't load .cer file from: " + normalizedPath, e);
        }
    }

    private URLConnection createUrlConnection(URL url, HttpMethod requestMethod) throws IOException {
        if (sslEnabledOnIast) {
            final HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            if (context != null) {
                con.setSSLSocketFactory(context.getSocketFactory());
            }
            setConnectionProperties(con, requestMethod);
            return con;
        }
        final HttpURLConnection con = (HttpURLConnection) url.openConnection();
        setConnectionProperties(con, requestMethod);
        return con;
    }

    private void setConnectionProperties(HttpURLConnection con, HttpMethod requestMethod) throws ProtocolException {
        con.setRequestMethod(requestMethod.toString());
        con.setRequestProperty("Content-Type", "application/json; utf-8");
        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(true);
    }
}
