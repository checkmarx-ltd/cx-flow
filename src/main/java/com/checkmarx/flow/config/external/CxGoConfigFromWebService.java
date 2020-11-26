package com.checkmarx.flow.config.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Config override coming from an external web service.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CxGoConfigFromWebService {
    public static final String SECTION_NAME = "cxGoConfig";

    private String team;
    private String cxgoSecret;
    private String scmAccessToken;
}
