package com.checkmarx.flow.exception;

import org.junit.Test;

public class GitLabClientRuntimeExceptionTest {
    @Test
    public void setNothing() {
        GitLabClientRuntimeException gitLabClientRuntimeException = new GitLabClientRuntimeException();
        assert gitLabClientRuntimeException.getMessage() == null;
    }

}