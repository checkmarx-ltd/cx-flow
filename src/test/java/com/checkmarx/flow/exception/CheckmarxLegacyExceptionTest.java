package com.checkmarx.flow.exception;

import org.junit.Test;

public class CheckmarxLegacyExceptionTest {

    @Test
    public void setNullMessage() {
        CheckmarxLegacyException checkmarxLegacyException = new CheckmarxLegacyException(null);
        assert checkmarxLegacyException.getMessage() == null;
    }

    @Test
    public void setEmptyMessage() {
        CheckmarxLegacyException checkmarxLegacyException = new CheckmarxLegacyException("");
        assert checkmarxLegacyException.getMessage().equals("");
    }

    @Test
    public void setMessage() {
        String message = "message";
        CheckmarxLegacyException checkmarxLegacyException = new CheckmarxLegacyException(message);
        assert checkmarxLegacyException.getMessage().equals(message);
    }
}