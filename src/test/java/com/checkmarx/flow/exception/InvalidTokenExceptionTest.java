package com.checkmarx.flow.exception;

import org.junit.Test;

public class InvalidTokenExceptionTest {
    @Test
    public void setNothing() {
        InvalidTokenException invalidTokenException = new InvalidTokenException();
        assert invalidTokenException.getMessage() == null;
    }

}