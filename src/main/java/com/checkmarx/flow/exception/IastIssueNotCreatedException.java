package com.checkmarx.flow.exception;

public class IastIssueNotCreatedException extends RuntimeException {
    public IastIssueNotCreatedException(String msg, Exception e) {
        super(msg, e);
    }

}
