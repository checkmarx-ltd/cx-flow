package com.checkmarx.flow.config.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String cxgoToken;
    private String scmAccessToken;
}
