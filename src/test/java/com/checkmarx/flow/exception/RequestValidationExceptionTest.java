package com.checkmarx.flow.exception;

import org.junit.Test;

public class RequestValidationExceptionTest {

    @Test
    public void setNothing() {
        RequestValidationException requestValidationException = new RequestValidationException();
        assert requestValidationException.getMessage() == null;
    }
}