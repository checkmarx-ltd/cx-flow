package com.checkmarx.flow.exception;

public class JiraClientRunTimeException extends RuntimeException {

    public JiraClientRunTimeException() {
    }

    public JiraClientRunTimeException(String message) {
        super(message);
    }

    public JiraClientRunTimeException(String message, Throwable cause) {
        super(message, cause);
    }
}