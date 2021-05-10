package com.checkmarx.flow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.PRECONDITION_FAILED, reason = "Thresholds Severity")
public class IastThresholdsSeverityException extends RuntimeException {
    public IastThresholdsSeverityException(String s) {
        super(s);
    }

    public IastThresholdsSeverityException() {
        super();
    }
}
