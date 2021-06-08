package com.checkmarx.flow.exception;

public class IastValidationScanTagFailedException extends RuntimeException {
    public IastValidationScanTagFailedException(String msgError) {
        super(msgError);
    }
}
