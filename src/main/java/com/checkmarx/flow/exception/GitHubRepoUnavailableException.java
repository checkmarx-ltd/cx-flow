package com.checkmarx.flow.exception;

public class GitHubRepoUnavailableException extends RuntimeException {

    public GitHubRepoUnavailableException() {
    }

    public GitHubRepoUnavailableException(String message) {
        super(message);
    }

    public GitHubRepoUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}