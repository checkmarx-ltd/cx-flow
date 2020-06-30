package com.checkmarx.flow.exception;

public class PullRequestCommentUnknown extends  RuntimeException {
    public  PullRequestCommentUnknown(String message) {
        super(message);
    }
}
