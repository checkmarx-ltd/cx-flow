package com.checkmarx.flow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Error has occurred")
public class MachinaRuntimeException extends RuntimeException {

    public MachinaRuntimeException() {
    }

    public MachinaRuntimeException(Throwable cause) {
        super(cause);
    }

    public MachinaRuntimeException(String message) {
        super(message);
    }
}
