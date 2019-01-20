package com.custodela.machina.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.FORBIDDEN, reason = "Invalid Credentials")
public class InvalidCredentialsException extends RuntimeException {
}
