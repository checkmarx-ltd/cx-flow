package com.checkmarx.flow.exception;

public class PullRequestCommentUnknownException extends  RuntimeException {
    public PullRequestCommentUnknownException(String message) {
        super(message);
    }
}
