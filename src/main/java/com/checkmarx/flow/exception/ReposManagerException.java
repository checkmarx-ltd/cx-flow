package com.checkmarx.flow.exception;

public class ReposManagerException extends RuntimeException {

    public ReposManagerException(String message) {
        super(message);
    }

    public ReposManagerException(String message, Throwable cause) {
        super(message, cause);
    }
}