package com.checkmarx.flow.exception;

public class JiraClientException extends MachinaException {
    public JiraClientException() {
    }

    public JiraClientException(String message) {
        super(message);
    }

    public JiraClientException(String message, Exception e) {
        super(message, e);
    }
    //TODO Step?
}
