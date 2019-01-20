package com.custodela.machina.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Request validation failed")
public class RequestValidationException extends RuntimeException {
}
