package com.atlassian.jira.rest.client.internal.async;
import com.atlassian.jira.rest.client.api.AuthenticationHandler;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.auth.BasicHttpAuthenticationHandler;
import java.net.URI;


public class CustomAsynchronousJiraRestClientFactory extends AsynchronousJiraRestClientFactory {
    private static final String AUTHORIZATION_HEADER = "Authorization";

    public JiraRestClient createCustom(final URI serverUri, final AuthenticationHandler authenticationHandler, int socketTimeoutInMs) {
        final DisposableHttpClient httpClient = new CustomAsynchronousHttpClientFactory()
                .createClientCustom(serverUri, authenticationHandler,socketTimeoutInMs);
        return new AsynchronousJiraRestClient(serverUri, httpClient);
    }

    public JiraRestClient createWithBasicHttpAuthenticationCustom(final URI serverUri, final String username, final String password, final int socketTimeoutInMs) {
        return createCustom(serverUri, new BasicHttpAuthenticationHandler(username, password),socketTimeoutInMs);
    }

    public JiraRestClient createWithPATHttpAuthenticationCustom(final URI serverUri, final String token, final int socketTimeoutInMs ){
        return createCustom(serverUri,builder -> builder.setHeader(AUTHORIZATION_HEADER, "Bearer " + token),socketTimeoutInMs);
    }

}