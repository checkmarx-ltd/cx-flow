package com.checkmarx.flow.config.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * Config override coming from an external web service.
 */
@Getter
@Setter
public class CxGoDynamicConfig {
    public static final String SECTION_NAME = "cxGoConfig";

    @JsonProperty("team")
    private String team;
    @JsonProperty("cxgoSecret")
    private String clientSecret;
    @JsonProperty("scmAccessToken")
    private String scmAccessToken;
}
