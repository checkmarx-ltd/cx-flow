package com.checkmarx.flow.cucumber.integration.end2end.genericendtoend;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.SearchRestClient;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.internal.async.CustomAsynchronousJiraRestClientFactory;
import com.checkmarx.flow.config.JiraProperties;

import io.atlassian.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

@Slf4j
enum BugTracker {
    JIRA {
        private JiraProperties jiraProperties;
        private List<String> issueCreatedKeys = new ArrayList<>();
        private JiraRestClient client;
        private SearchRestClient searchClient;


        @Override
        void init(GenericEndToEndSteps genericEndToEndSteps) {
            jiraProperties = genericEndToEndSteps.jiraProperties;
            CustomAsynchronousJiraRestClientFactory factory = new CustomAsynchronousJiraRestClientFactory();
            URI jiraURI;
            try {
                jiraURI = new URI(jiraProperties.getUrl());
            } catch (URISyntaxException e) {
                fail("Error constructing URI for JIRA");
                jiraURI = null;
            }
            client = factory.createWithBasicHttpAuthenticationCustom(jiraURI, jiraProperties.getUsername(),
                    jiraProperties.getToken(), jiraProperties.getHttpTimeout());
            searchClient = client.getSearchClient();
        }

        @Override
        void verifyIssueCreated(String severities) {
            String jql = String.format("project = %s and priority  in %s", jiraProperties.getProject(), severities);
            log.info("filtering issue with jql: {}", jql);
            Set<String> fields = new HashSet<>();
            fields.addAll(
                    Arrays.asList("key", "project", "issuetype", "summary", "labels", "created", "updated", "status"));
            SearchResult result = null;
            boolean found = false;
            for (int retries = 0; retries < 20; retries++) {
                Promise<SearchResult> temp = searchClient.searchJql(jql, 10, 0, fields);
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (Exception e) {
                    log.info("starting attempt {}", retries + 1);
                }
                try {
                    result = temp.get(500, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    log.info("failed attempt {}", retries + 1);
                }

                if (result != null && result.getTotal() > 0) {
                    found = true;
                    Iterator<Issue> itr = result.getIssues().iterator();
                    assertTrue(itr.hasNext(), "Jira is missing the issues");
                    while (itr.hasNext()) {
                        this.issueCreatedKeys.add(itr.next().getKey());
                    }
                    break;
                }
            }

            if (!found) {
                String msg = "failed to find update in Jira after expected time";
                log.error(msg);
                fail(msg);
            }
        }

        @Override
        void deleteIssues() {
            Optional.ofNullable(issueCreatedKeys).ifPresent(ks -> ks.forEach(
                k->   client.getIssueClient().deleteIssue(k, false)
            ));
        }
    };

    static BugTracker setTo(String bugTracker, GenericEndToEndSteps genericEndToEndSteps) {
        log.info("setting bug-tracker to {}", bugTracker);
        BugTracker bt = valueOf(bugTracker);
        bt.init(genericEndToEndSteps);
        return bt;
    }

    abstract void verifyIssueCreated(String severities);

    abstract void deleteIssues();

    abstract void init(GenericEndToEndSteps genericEndToEndSteps);
}