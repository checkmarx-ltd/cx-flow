package com.checkmarx.flow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Error communicating with GitLab API")
public class GitLabClientRuntimeException extends RuntimeException {
}
