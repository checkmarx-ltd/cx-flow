package com.checkmarx.flow.dto.jira;

import java.util.List;

public class JiraSearchRequest {
    private String jql;
    private List<String> fields;
    private Integer maxResults;
    private String nextPageToken;

    public String getJql() {
        return jql;
    }

    public void setJql(String jql) {
        this.jql = jql;
    }

    public List<String> getFields() {
        return fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    public Integer getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(Integer maxResults) {
        this.maxResults = maxResults;
    }

    public String getNextPageToken() {
        return nextPageToken;
    }

    public void setNextPageToken(String nextPageToken) {
        this.nextPageToken = nextPageToken;
    }
}