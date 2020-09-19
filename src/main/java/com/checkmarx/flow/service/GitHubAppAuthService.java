package com.checkmarx.flow.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.exception.GitHubClientRunTimeException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.Base64;
import java.util.Date;
import java.util.Objects;

import javax.annotation.PostConstruct;

@Service
@Slf4j
public class GitHubAppAuthService {
    private static final String INSTALLATION = "/app/installations";
    private static final String BEARER = "Bearer ";
    private final RestTemplate restTemplate;
    private final GitHubProperties properties;
    private String appInstallationUrl;
    private JWTVerifier jwtVerifier;
    private String jwt;
    private RSAPrivateKey privateKey;
    private String appToken;
    private Instant appTokenExp;

    public GitHubAppAuthService(@Qualifier("flowRestTemplate") RestTemplate restTemplate,
                                GitHubProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @PostConstruct
    private void init() {
        //Check if the appId and appKeyFile exists and pre-initialize
        if (!StringUtils.isEmpty(properties.getAppId()) &&
                !StringUtils.isEmpty(properties.getAppKeyFile())) {
            log.info("Initializing GitHub App Authentication Service for AppId {} using Key file {}",
                    properties.getAppId(), properties.getAppKeyFile());
            if (!Files.exists(Paths.get(properties.getAppKeyFile()))) {
                throw new GitHubClientRunTimeException("GitHub App Key File not found");
            }
             try {
                 this.privateKey = getPrivateKey(new File(properties.getAppKeyFile()));
                 Algorithm algorithm = Algorithm.RSA256(null, this.privateKey);
                 this.jwtVerifier = JWT.require(algorithm)
                         .withIssuer(properties.getAppId())
                         .build();
                 //set the initial Jwt to confirm signing is correct before proceeding
                 getJwt(properties.getAppId());
             }catch (IOException e){
                 throw new GitHubClientRunTimeException("GitHub App Key File not found");
             }
        }
    }


    /**
     * Get header for GitHub App authentication.
     *
     * @return headers with authentication.
     */
    public HttpHeaders createAuthHeaders() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.AUTHORIZATION, BEARER.concat(createAppToken()));
        return httpHeaders;
    }

    private String createAppToken() {
        //Check if the token exists, and if it is not expired yet
        if(!StringUtils.isEmpty(this.appToken) &&
                this.appTokenExp != null &&
                this.appTokenExp.isAfter(Instant.now())){
            return this.appToken;
        }
        log.info("GitHub token expired, generating a new one.");
        String installationUrl = getInstallationUrl();

        String token = getJwt(properties.getAppId());

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.AUTHORIZATION, BEARER.concat(token));
        httpHeaders.set(HttpHeaders.ACCEPT, "application/vnd.github.v3+json");

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                installationUrl,
                HttpMethod.POST, new HttpEntity<>(httpHeaders),
                JsonNode.class);

        if (response.hasBody()) {
            JsonNode it = response.getBody();
            assert it != null;
            String expireAt = it.get("expires_at").asText();
            TemporalAccessor expires = DateTimeFormatter.ISO_DATE_TIME.parse(it.get("expires_at").asText());
            log.info("Generated a GitHub token which expires at {}", expireAt);
            this.appTokenExp = Instant.from(expires).minus(3, ChronoUnit.MINUTES);
            this.appToken = it.get("token").textValue();
            return this.appToken;
        } else {
            throw new GitHubClientRunTimeException("Cannot access GitHub app access token endpoint");
        }
    }

    String getInstallationUrl() {
        if (!StringUtils.isEmpty(this.appInstallationUrl)) {
            return this.appInstallationUrl;
        }
        String token = getJwt(properties.getAppId());

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.AUTHORIZATION, BEARER.concat(token));
        httpHeaders.set(HttpHeaders.ACCEPT, "application/vnd.github.v3+json");

        String url = properties.getApiUrl().concat(INSTALLATION);
        ResponseEntity<JsonNode[]> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(httpHeaders),
                JsonNode[].class);
        if (response.hasBody()) {
            JsonNode installationRef = Objects.requireNonNull(response.getBody())[0];
            this.appInstallationUrl = installationRef.get("access_tokens_url").textValue();
            return this.appInstallationUrl;
        } else {
            throw new GitHubClientRunTimeException("GitHub apps is not installed on the given org");
        }
    }

    /**
     * Generate the Private Key from the Key File
     *
     * @param keyFile PEM file location downloaded from GitHub
     * @return
     * @throws IOException
     */
     RSAPrivateKey getPrivateKey(File keyFile) throws IOException {
        String key = new String(Files.readAllBytes(keyFile.toPath()), Charset.defaultCharset());

        if (key.startsWith("-----BEGIN RSA PRIVATE KEY-----")) {
            log.error("PKCS#1 key. Please convert to PKCS#8 with command line like: `openssl pkcs8 -topk8 -inform PEM -outform PEM -in private.pem -out private8.pem -nocrypt`");
            throw new GitHubClientRunTimeException("Private key is in PKCS#1 format. Need PKCS#8.");
        }
        String noBoundary = key
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\n", "");

        byte[] encoded = Base64.getDecoder().decode(noBoundary);

        try {
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) kf.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new GitHubClientRunTimeException("Private key is not in a correct format", e);
        }
    }

    private String getJwt(String appId) {
        //Check if current token is set and if it is verified
        if(this.jwt != null){
            try{
                jwtVerifier.verify(jwt);
                return this.jwt;
            }catch (TokenExpiredException e){
                log.debug("JWT has expired, generating a new one.");
            }catch (JWTVerificationException e){
                log.error("Error occurred verifying JWT", e);
                throw new GitHubClientRunTimeException("Error occurred verifying JWT");
            }
        }
        //If the jwt was null or expired, we hit this block to create new
        LocalDateTime issued = LocalDateTime.now();
        LocalDateTime exp = issued.plusMinutes(10); // 10 minutes in future
        assert this.privateKey != null;
        Algorithm algorithm = Algorithm.RSA256(null, this.privateKey);
        //set the current token and return it
        this.jwt = JWT.create()
                .withIssuer(appId)
                .withIssuedAt(Date.from(issued.toInstant(ZoneOffset.UTC)))
                .withExpiresAt(Date.from(exp.toInstant(ZoneOffset.UTC)))
                .sign(algorithm);
        return this.jwt;
    }
}