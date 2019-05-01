package com.checkmarx.flow.dto.cx;

import com.fasterxml.jackson.annotation.JsonProperty;


public class CxAuthRequest {

    @JsonProperty("username")
    private String username;

    @JsonProperty("password")
    private String password;

    @JsonProperty("grant_type")
    private String grantType = "password";

    @JsonProperty("scope")
    private String scope = "sast_rest_api";

    @JsonProperty("client_id")
    private String clientId = "resource_owner_client";

    @JsonProperty("client_secret")
    private String clientSecret;

    @java.beans.ConstructorProperties({"username", "password", "grantType", "scope", "clientId", "clientSecret"})
    CxAuthRequest(String username, String password, String grantType, String scope, String clientId, String clientSecret) {
        this.username = username;
        this.password = password;
        this.grantType = grantType;
        this.scope = scope;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public static CxAuthRequestBuilder builder() {
        return new CxAuthRequestBuilder();
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public String getGrantType() {
        return this.grantType;
    }

    public String getScope() {
        return this.scope;
    }

    public String getClientId() {
        return this.clientId;
    }

    public String getClientSecret() {
        return this.clientSecret;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String toString() {
        return "CxAuthRequest(username=" + this.getUsername() + ", password=" + this.getPassword() + ", grantType=" + this.getGrantType() + ", scope=" + this.getScope() + ", clientId=" + this.getClientId() + ", clientSecret=" + this.getClientSecret() + ")";
    }

    public static class CxAuthRequestBuilder {
        private String username;
        private String password;
        private String grantType;
        private String scope;
        private String clientId;
        private String clientSecret;

        CxAuthRequestBuilder() {
        }

        public CxAuthRequest.CxAuthRequestBuilder username(String username) {
            this.username = username;
            return this;
        }

        public CxAuthRequest.CxAuthRequestBuilder password(String password) {
            this.password = password;
            return this;
        }

        public CxAuthRequest.CxAuthRequestBuilder grantType(String grantType) {
            this.grantType = grantType;
            return this;
        }

        public CxAuthRequest.CxAuthRequestBuilder scope(String scope) {
            this.scope = scope;
            return this;
        }

        public CxAuthRequest.CxAuthRequestBuilder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public CxAuthRequest.CxAuthRequestBuilder clientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
            return this;
        }

        public CxAuthRequest build() {
            return new CxAuthRequest(username, password, grantType, scope, clientId, clientSecret);
        }

        public String toString() {
            return "CxAuthRequest.CxAuthRequestBuilder(username=" + this.username + ", password=" + this.password + ", grantType=" + this.grantType + ", scope=" + this.scope + ", clientId=" + this.clientId + ", clientSecret=" + this.clientSecret + ")";
        }
    }
}
