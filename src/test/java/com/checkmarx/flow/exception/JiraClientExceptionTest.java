package com.checkmarx.flow.exception;

import org.junit.Test;

import static org.junit.Assert.*;

public class JiraClientExceptionTest {
    @Test
    public void setNullMessage() {
        JiraClientException jiraClientException = new JiraClientException(null);
        assert jiraClientException.getMessage() == null;
    }

    @Test
    public void setNothing() {
        JiraClientException jiraClientException = new JiraClientException();
        assert jiraClientException.getMessage() == null;
    }

    @Test
    public void setEmptyMessage() {
        JiraClientException jiraClientException = new JiraClientException("");
        assert jiraClientException.getMessage().equals("");
    }

    @Test
    public void setMessage() {
        String message = "message";
        JiraClientException jiraClientException = new JiraClientException(message);
        assert jiraClientException.getMessage().equals(message);
    }
}