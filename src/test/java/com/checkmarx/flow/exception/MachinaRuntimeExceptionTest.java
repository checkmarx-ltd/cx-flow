package com.checkmarx.flow.exception;

import org.junit.Test;

import static org.junit.Assert.*;

public class MachinaRuntimeExceptionTest {

    @Test
    public void setNothing() {
        MachinaRuntimeException machinaRuntimeException = new MachinaRuntimeException();
        assert machinaRuntimeException.getMessage() == null;
    }
}