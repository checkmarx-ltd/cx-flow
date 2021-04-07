package com.checkmarx.flow.utils;


import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.exception.InvalidTokenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenUtils {

    private final FlowProperties properties;

    /**
     * Validates given token against the token value defined in the cx-flow section of the application yml.
     *
     * @param token token to validate
     */
    public void validateToken(String token) {
        log.info("Validating REST API token");
        if (!properties.getToken().equals(token)) {
            log.error("REST API token validation failed");
            throw new InvalidTokenException();
        }
        log.info("Validation successful");
    }
}
