package com.checkmarx.flow.exception;

import org.junit.Test;

public class GitHubClientExceptionTest {

    @Test
    public void setNullMessage() {
        GitHubClientException gitHubClientException = new GitHubClientException(null);
        assert gitHubClientException.getMessage() == null;
    }

    @Test
    public void setNothing() {
        GitHubClientException gitHubClientException = new GitHubClientException();
        assert gitHubClientException.getMessage() == null;
    }

    @Test
    public void setEmptyMessage() {
        GitHubClientException gitHubClientException = new GitHubClientException("");
        assert gitHubClientException.getMessage().equals("");
    }

    @Test
    public void setMessage() {
        String message = "message";
        GitHubClientException gitHubClientException = new GitHubClientException(message);
        assert gitHubClientException.getMessage().equals(message);
    }
}