package com.checkmarx.flow.exception;

import org.junit.Test;

public class InvalidCredentialsExceptionTest {
    @Test
    public void setNothing() {
        InvalidCredentialsException invalidCredentialsException = new InvalidCredentialsException();
        assert invalidCredentialsException.getMessage() == null;
    }
}