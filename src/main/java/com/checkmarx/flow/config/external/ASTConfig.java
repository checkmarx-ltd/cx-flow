package com.checkmarx.flow.config.external;

import com.typesafe.config.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ASTConfig {

    private String apiUrl;
    private String preset;
    @Optional
    private boolean incremental;
    private String clientSecret;
    private String clientId;
}