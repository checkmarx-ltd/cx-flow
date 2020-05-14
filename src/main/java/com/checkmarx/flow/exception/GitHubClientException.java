package com.checkmarx.flow.exception;


public class GitHubClientException extends MachinaException {
    public GitHubClientException() {
    }

    public GitHubClientException(String message) {
        super(message);
    }

    public GitHubClientException(String message, Throwable t) {
        super(message, t);
    }
}
