package com.checkmarx.flow.exception;

public class IastPropertiesNotSetupException extends RuntimeException {
    public IastPropertiesNotSetupException(String errorMsg) {
        super(errorMsg);
    }
}
