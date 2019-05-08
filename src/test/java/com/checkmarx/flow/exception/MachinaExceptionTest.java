package com.checkmarx.flow.exception;

import org.junit.Test;

import static org.junit.Assert.*;

public class MachinaExceptionTest {

    private final static MachinaException.Step nullStep = null;
    private final static MachinaException.Step step = MachinaException.Step.SUBMITTED;

    @Test
    public void stepMessage() {
        assert step.getStep().equals(MachinaException.Step.SUBMITTED.getStep());
    }

    @Test
    public void getStep() {
        MachinaException machinaException = new MachinaException(null);
        machinaException.setStep(step);
        assert machinaException.getStep() == step;
    }

    @Test
    public void getNullStep() {
        MachinaException machinaException = new MachinaException(null);
        assert machinaException.getStep() == null;
    }

    @Test
    public void setNullStep() {
        MachinaException machinaException = new MachinaException(null);
        machinaException.setStep(nullStep);
        assert machinaException.getStep() == null;
    }

    @Test
    public void setNullMessage() {
        MachinaException machinaException = new MachinaException(null);
        assert machinaException.getMessage() == null;
    }

    @Test
    public void setNothing() {
        MachinaException machinaException = new MachinaException();
        assert machinaException.getMessage() == null;
    }

    @Test
    public void setEmptyMessage() {
        MachinaException machinaException = new MachinaException("");
        assert machinaException.getMessage().equals("");
    }

    @Test
    public void setMessage() {
        String message = "message";
        MachinaException machinaException = new MachinaException(message);
        assert machinaException.getMessage().equals(message);
    }
}