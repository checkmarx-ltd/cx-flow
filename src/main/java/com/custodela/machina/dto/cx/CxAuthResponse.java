package com.custodela.machina.dto.cx;

import com.fasterxml.jackson.annotation.JsonProperty;


public class CxAuthResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("expires_in")
    private Long expiresIn;

    @JsonProperty("tokenType")
    private String tokenType;

    @java.beans.ConstructorProperties({"accessToken", "expiresIn", "tokenType"})
    public CxAuthResponse(String accessToken, Long expiresIn, String tokenType) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.tokenType = tokenType;
    }

    public CxAuthResponse() {
    }

    public static CxAuthResponseBuilder builder() {
        return new CxAuthResponseBuilder();
    }

    public String toString() {
        return "CxAuthResponse(accessToken=" + this.getAccessToken() + ", expiresIn=" + this.getExpiresIn() + ", tokenType=" + this.getTokenType() + ")";
    }

    public String getAccessToken() {
        return this.accessToken;
    }

    public Long getExpiresIn() {
        return this.expiresIn;
    }

    public String getTokenType() {
        return this.tokenType;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public static class CxAuthResponseBuilder {
        private String accessToken;
        private Long expiresIn;
        private String tokenType;

        CxAuthResponseBuilder() {
        }

        public CxAuthResponse.CxAuthResponseBuilder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public CxAuthResponse.CxAuthResponseBuilder expiresIn(Long expiresIn) {
            this.expiresIn = expiresIn;
            return this;
        }

        public CxAuthResponse.CxAuthResponseBuilder tokenType(String tokenType) {
            this.tokenType = tokenType;
            return this;
        }

        public CxAuthResponse build() {
            return new CxAuthResponse(accessToken, expiresIn, tokenType);
        }

        public String toString() {
            return "CxAuthResponse.CxAuthResponseBuilder(accessToken=" + this.accessToken + ", expiresIn=" + this.expiresIn + ", tokenType=" + this.tokenType + ")";
        }
    }
}
