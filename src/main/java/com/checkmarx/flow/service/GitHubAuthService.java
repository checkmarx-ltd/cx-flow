package com.checkmarx.flow.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.config.ScmConfigOverrider;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.GitHubClientRunTimeException;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.Base64;
import java.util.Date;

@Service
public class GitHubAuthService {
    private static final String INSTALLATION = "/orgs/{orgs}/installation";

    private static final Logger log = LoggerFactory.getLogger(GitHubAuthService.class);

    private final RestTemplate restTemplate;
    private final GitHubProperties properties;
    private final ScmConfigOverrider scmConfigOverrider;

    // Poor man cache ... Sorry ...
    private String appToken="";
    private Instant appExpiration=Instant.EPOCH;
    private String appInstallationUrl="";

    public GitHubAuthService(@Qualifier("flowRestTemplate") RestTemplate restTemplate,
                             GitHubProperties properties,
                             ScmConfigOverrider scmConfigOverrider){
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.scmConfigOverrider = scmConfigOverrider;
    }

    /**
     * Get GitHub token either directly or via the app.
     *
     * @return valid token
     */
    public String getToken(String scmInstance) {
        return properties.getApp().getId()==0?scmConfigOverrider.determineConfigToken(properties, scmInstance):createAppToken();
    }

    /**
     * Get the basic auth parameter (GitHub https access).
     *  For a static token it is a token, but for app, it is login:pass with login=id of the app, pass=token
     *
     * @return valid header
     */
    public String getBasicAuth(String scmInstance) {
        String token=getToken(scmInstance);
        return properties.getApp().getId()==0?token:properties.getApp().getId()+":"+token;
    }

    /**
     * Get header for GitHub authentication.
     *
     * @return headers with authentication.
     */
    public HttpHeaders createAuthHeaders(ScanRequest scanRequest){
        String token=getToken(scanRequest.getScmInstance());

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.AUTHORIZATION, "token ".concat(token));
        return httpHeaders;
    }

    private String createAppToken() {
        if (Instant.now().isBefore(this.appExpiration)){
            return this.appToken;
        }
        log.info("GitHub token expired, generating a new one.");
        String installationUrl=getInstallationUrl();

        String jwt=generateJWT();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.AUTHORIZATION, "Bearer ".concat(jwt));
        httpHeaders.set(HttpHeaders.ACCEPT, "application/vnd.github.machine-man-preview+json");

        ResponseEntity<JsonNode> response=restTemplate.exchange(
                installationUrl,
                HttpMethod.POST, new HttpEntity<>(httpHeaders),
                JsonNode.class);

        if (response.hasBody()) {
            JsonNode it=response.getBody();
            String expireAt=it.get("expires_at").asText();
            TemporalAccessor expires=DateTimeFormatter.ISO_DATE_TIME.parse(it.get("expires_at").asText());
            log.info("Generated a GitHub token which expires at {}", expireAt);
            this.appExpiration=Instant.from(expires).minus(3, ChronoUnit.MINUTES);
            this.appToken=it.get("token").textValue();
            return this.appToken;
        }else {
            throw new GitHubClientRunTimeException("Cannot access GitHub app access token endpoint");
        }
    }

    private String getInstallationUrl() {
        if (!this.appInstallationUrl.isEmpty()){
            return this.appInstallationUrl;
        }
        String jwt=generateJWT();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.AUTHORIZATION, "Bearer ".concat(jwt));
        httpHeaders.set(HttpHeaders.ACCEPT, "application/vnd.github.machine-man-preview+json");

        String url=properties.getApiUrl().concat(INSTALLATION);
        ResponseEntity<JsonNode> response=restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(httpHeaders), JsonNode.class, properties.getApp().getOrg());
        if (response.hasBody()) {
            this.appInstallationUrl=response.getBody().get("access_tokens_url").textValue();
            return this.appInstallationUrl;
        }else {
            throw new GitHubClientRunTimeException("GitHub apps is not installed on the given org");
        }
    }

    private RSAPrivateKey convertPrivateKey() {
        String secretKey=properties.getApp().getSecretKey();
        if (properties.getApp().getSecretKey().startsWith("-----BEGIN RSA PRIVATE KEY-----")){
            log.error("PKCS#1 key. Please convert to PKCS#8 with command line like: `openssl pkcs8 -topk8 -inform PEM -outform PEM -in private.pem -out private8.pem -nocrypt`");
            throw new GitHubClientRunTimeException("Private key is in PKCS#1 format. Need PKCS#8.");
        }
        String noBoundary = secretKey
                .replace("-----BEGIN PRIVATE KEY-----","")
                .replace("-----END PRIVATE KEY-----","")
                .replace("\n","");

        byte[] encoded = Base64.getDecoder().decode(noBoundary);

        try {
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) kf.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException|InvalidKeySpecException e) {
            throw new GitHubClientRunTimeException("Private key is not in a correct format", e);
        }
    }

    private String generateJWT(){
        Date date=new Date();
        Date exp=new Date(date.getTime()+10*60*1000); // 10 minutes in future

        Algorithm algorithm = Algorithm.RSA256(null, convertPrivateKey());
        return JWT.create()
                .withIssuer(Integer.toString(properties.getApp().getId()))
                .withIssuedAt(date)
                .withExpiresAt(exp)
                .sign(algorithm);
    }
}
