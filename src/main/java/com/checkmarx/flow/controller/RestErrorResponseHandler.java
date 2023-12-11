package com.checkmarx.flow.controller;

import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;

import static org.springframework.http.HttpStatus.Series.CLIENT_ERROR;
import static org.springframework.http.HttpStatus.Series.SERVER_ERROR;

@Component
public class RestErrorResponseHandler implements ResponseErrorHandler {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(RestErrorResponseHandler.class);

    @Override
    public boolean hasError(ClientHttpResponse httpResponse) throws IOException {
        log.info("Calling CxFlow hasError");
        return (
                httpResponse.getStatusCode().value() == CLIENT_ERROR.value()
                        || httpResponse.getStatusCode().value() == SERVER_ERROR.value());
    }

    @Override
    public void handleError(ClientHttpResponse httpResponse)
            throws IOException {
        log.info("Calling CxFlow handleError");
        if (httpResponse.getStatusCode()
                .value() == HttpStatus.Series.SERVER_ERROR.value()) {
            // handle SERVER_ERROR
        } else if (httpResponse.getStatusCode()
                .value() == HttpStatus.Series.CLIENT_ERROR.value()) {
            // handle CLIENT_ERROR
            if (httpResponse.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new IOException();
            }
        }
    }
}