package com.checkmarx.flow.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.exception.GitHubClientRunTimeException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.*;

import javax.annotation.PostConstruct;

@Service
@Slf4j
public class GitHubAppAuthService {
    private static final String INSTALLATION_TOKEN_PATH = "/installations/{installation_id}/access_tokens"; // NOSONAR
    private static final String BEARER_HEADER = "Bearer ";
    private final RestTemplate restTemplate;
    private final GitHubProperties properties;
    private String jwt;
    private RSAPrivateKey privateKey;
    private Map<Integer, AppToken> appTokens = new HashMap<>();

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
    public String getInstallationToken(Integer installationId) {
        return createAppToken(installationId);
    }

    private String createAppToken(Integer installationId) {
        //Check if the token exists for the installation, and if it is not expired yet

        if(this.appTokens.containsKey(installationId) &&
                !this.appTokens.get(installationId).isExpired()){
            return this.appTokens.get(installationId).getToken();
        }
        log.info("GitHub token expired for installation {}, generating a new.", installationId);
        String jwtToken = getJwt(properties.getAppId());

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.AUTHORIZATION, BEARER_HEADER.concat(jwtToken));
        httpHeaders.set(HttpHeaders.ACCEPT, properties.getAppHeader());

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                properties.getAppUrl().concat(INSTALLATION_TOKEN_PATH),
                HttpMethod.POST,
                new HttpEntity<>(httpHeaders),
                JsonNode.class,
                installationId);

        if (response.hasBody()) {
            AppToken appToken = new AppToken();
            JsonNode it = response.getBody();
            assert it != null;
            String expireAt = it.get("expires_at").asText();
            TemporalAccessor expires = DateTimeFormatter.ISO_DATE_TIME.parse(it.get("expires_at").asText());
            log.info("Generated a GitHub token for installation id {}, which expires at {}", installationId, expireAt);
            appToken.setTokenExp(Instant.from(expires).minus(3, ChronoUnit.MINUTES));
            appToken.setToken(it.get("token").textValue());
            //Store the token for future Installation use until expired
            this.appTokens.put(installationId, appToken);
            return appToken.getToken();
        } else {
            throw new GitHubClientRunTimeException("Cannot access GitHub app access token endpoint");
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
         Path keyFilePath = keyFile.toPath();
         StringBuilder sb = new StringBuilder();
         try(BufferedReader bufferedReader = Files.newBufferedReader(keyFilePath)) {
             String line = bufferedReader.readLine();
             while(line != null) {
                 sb.append(line);
                 line = bufferedReader.readLine();
             }
         }
         String key = sb.toString();
         if (key.startsWith("-----BEGIN RSA PRIVATE KEY-----")) {
             log.error("PKCS#1 key. Please convert to PKCS#8 with command line like: `openssl pkcs8 -topk8 -inform PEM -outform PEM -in private.pem -out private8.pem -nocrypt`");
             throw new GitHubClientRunTimeException("Private key is in PKCS#1 format. Need PKCS#8.");
         }
         final String startBoundary = "-----BEGIN PRIVATE KEY-----";
         final String endBoundary = "-----END PRIVATE KEY-----";
         int start = 0;
         int end = key.length();
         if (key.startsWith(startBoundary)) {
             start = startBoundary.length();
         }
         if (key.endsWith(endBoundary)) {
             end = end - endBoundary.length();
         }
         String noBoundary = key.substring(start, end);
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
        LocalDateTime currentDateTime = LocalDateTime.now(ZoneOffset.UTC);
        if(this.jwt != null) {
            DecodedJWT decodedJwt = JWT.decode(this.jwt);

            Instant currentTime = currentDateTime.plusMinutes(1).toInstant(ZoneOffset.UTC);
            if (currentTime.isBefore(decodedJwt.getExpiresAt().toInstant())) {
                return this.jwt;
            }
        }
        //If the jwt was null or expired, we hit this block to create new
        LocalDateTime exp = currentDateTime.plusMinutes(10); // 10 minutes in future
        assert this.privateKey != null;
        Algorithm algorithm = Algorithm.RSA256(null, this.privateKey);
        //set the current token and return it
        this.jwt = JWT.create()
                .withIssuer(appId)
                .withIssuedAt(Date.from(currentDateTime.toInstant(ZoneOffset.UTC)))
                .withExpiresAt(Date.from(exp.toInstant(ZoneOffset.UTC)))
                .sign(algorithm);
        return this.jwt;
    }

    @Data
    public static class AppToken{
         private String token;
         private Instant tokenExp;

         public boolean isExpired(){
             return this.tokenExp.isBefore(Instant.now(Clock.systemUTC()));
         }
    }
}