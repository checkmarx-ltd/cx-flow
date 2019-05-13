package com.checkmarx.flow.exception;

import org.junit.Test;

public class BitBucketClientExceptionTest {

    @Test
    public void setNullMessage() {
        BitBucketClientException bitBucketClientException = new BitBucketClientException(null);
        assert bitBucketClientException.getMessage() == null;
    }

    @Test
    public void setNothing() {
        BitBucketClientException bitBucketClientException = new BitBucketClientException();
        assert bitBucketClientException.getMessage() == null;
    }

    @Test
    public void setEmptyMessage() {
        BitBucketClientException bitBucketClientException = new BitBucketClientException("");
        assert bitBucketClientException.getMessage().equals("");
    }

    @Test
    public void setMessage() {
        String message = "message";
        BitBucketClientException bitBucketClientException = new BitBucketClientException(message);
        assert bitBucketClientException.getMessage().equals(message);
    }
}