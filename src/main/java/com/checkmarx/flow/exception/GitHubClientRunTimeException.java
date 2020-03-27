package com.checkmarx.flow.exception;

public class GitHubClientRunTimeException extends RuntimeException {

    public GitHubClientRunTimeException() {
    }

    public GitHubClientRunTimeException(String message) {
        super(message);
    }

    public GitHubClientRunTimeException(String message, Throwable cause) {
        super(message, cause);
    }
}