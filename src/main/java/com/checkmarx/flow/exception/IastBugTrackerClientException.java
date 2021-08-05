package com.checkmarx.flow.exception;

public class IastBugTrackerClientException extends RuntimeException {
    public IastBugTrackerClientException(String msg) {
        super(msg);
    }

    public IastBugTrackerClientException(String msg, Exception e) {
        super(msg, e);
    }
}
