package com.checkmarx.jira;

public class JiraUtilsException extends RuntimeException {
    public JiraUtilsException(String message) {
        super(message);
    }

    public JiraUtilsException(String message, Throwable t) {
        super(message, t);
    }
}
