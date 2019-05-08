package com.checkmarx.flow.exception;

import org.junit.Test;

public class GitLabClientExceptionTest {
    @Test
    public void setNullMessage() {
        GitLabClientException gitLabClientException = new GitLabClientException(null);
        assert gitLabClientException.getMessage() == null;
    }

    @Test
    public void setNothing() {
        GitLabClientException gitLabClientException = new GitLabClientException();
        assert gitLabClientException.getMessage() == null;
    }

    @Test
    public void setEmptyMessage() {
        GitLabClientException gitLabClientException = new GitLabClientException("");
        assert gitLabClientException.getMessage().equals("");
    }

    @Test
    public void setMessage() {
        String message = "message";
        GitLabClientException gitLabClientException = new GitLabClientException(message);
        assert gitLabClientException.getMessage().equals(message);
    }
}