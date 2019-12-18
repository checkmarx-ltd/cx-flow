package com.atlassian.jira.rest.client.internal.async;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.httpclient.apache.httpcomponents.DefaultHttpClientFactory;
import com.atlassian.httpclient.api.HttpClient;
import com.atlassian.httpclient.api.factory.HttpClientOptions;
import com.atlassian.jira.rest.client.api.AuthenticationHandler;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;
import com.atlassian.sal.api.executor.ThreadLocalContextManager;
import javax.annotation.Nonnull;
import java.io.File;
import java.net.URI;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class CustomAsynchronousHttpClientFactory extends AsynchronousHttpClientFactory {

    @SuppressWarnings("unchecked")
    public DisposableHttpClient createClientCustom(final URI serverUri, final AuthenticationHandler authenticationHandler, int socketTimeoutInMs) {
        final HttpClientOptions options = new HttpClientOptions();
        options.setSocketTimeout(socketTimeoutInMs, TimeUnit.MILLISECONDS);
        options.setRequestTimeout(socketTimeoutInMs, TimeUnit.MILLISECONDS);

        final DefaultHttpClientFactory defaultHttpClientFactory = new DefaultHttpClientFactory(new NoOpEventPublisher(),
                new RestClientApplicationProperties(serverUri),
                new ThreadLocalContextManager() {
                    @Override
                    public Object getThreadLocalContext() {
                        return null;
                    }

                    @Override
                    public void setThreadLocalContext(Object context) {
                    }

                    @Override
                    public void clearThreadLocalContext() {
                    }
                });

        final HttpClient httpClient = defaultHttpClientFactory.create(options);

        return new AtlassianHttpClientDecorator(httpClient, authenticationHandler) {
            @Override
            public void destroy() throws Exception {
                defaultHttpClientFactory.dispose(httpClient);
            }
        };
    }


    private static class NoOpEventPublisher implements EventPublisher {
        @Override
        public void publish(Object o) {
        }

        @Override
        public void register(Object o) {
        }

        @Override
        public void unregister(Object o) {
        }

        @Override
        public void unregisterAll() {
        }
    }

    /**
     * These properties are used to present JRJC as a User-Agent during http requests.
     */
    private static class RestClientApplicationProperties implements ApplicationProperties {

        private final String baseUrl;

        private RestClientApplicationProperties(URI jiraURI) {
            this.baseUrl = jiraURI.getPath();
        }

        @Override
        public String getBaseUrl() {
            return baseUrl;
        }

        /**
         * We'll always have an absolute URL as a client.
         */
        @Nonnull
        @Override
        public String getBaseUrl(UrlMode urlMode) {
            return baseUrl;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Atlassian JIRA Rest Java Client";
        }

        @Nonnull
        @Override
        public String getPlatformId() {
            return ApplicationProperties.PLATFORM_JIRA;
        }

        @Nonnull
        @Override
        public String getVersion() {
            return "";
        }

        @Nonnull
        @Override
        public Date getBuildDate() {
            throw new UnsupportedOperationException();
        }

        @Nonnull
        @Override
        public String getBuildNumber() {
            return String.valueOf(0);
        }

        @Override
        public File getHomeDirectory() {
            return new File(".");
        }

        @Override
        public String getPropertyValue(final String s) {
            throw new UnsupportedOperationException("Not implemented");
        }
    }

}