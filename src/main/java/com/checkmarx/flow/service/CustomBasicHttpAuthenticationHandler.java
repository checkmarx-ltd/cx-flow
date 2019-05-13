package com.checkmarx.flow.service;


import com.atlassian.httpclient.api.Request;
import com.atlassian.jira.rest.client.api.AuthenticationHandler;
import org.apache.commons.codec.binary.Base64;

public class CustomBasicHttpAuthenticationHandler implements AuthenticationHandler {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final String username;
    private final String password;

    public CustomBasicHttpAuthenticationHandler(final String username, final String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public void configure(Request.Builder builder) {
        builder.setHeader(AUTHORIZATION_HEADER, "Basic " + encodeCredentials());
    }

    private String encodeCredentials() {
        byte[] credentials = (username + ':' + password).getBytes();
        return new String(Base64.encodeBase64(credentials));
    }

}