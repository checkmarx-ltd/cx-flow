package com.checkmarx.flow.exception;

public class IastScanRequestMustProvideException extends RuntimeException {
    public IastScanRequestMustProvideException(String msgError) {
        super(msgError);
    }

    public IastScanRequestMustProvideException(String msgError, Exception e) {
        super(msgError, e);
    }
}
