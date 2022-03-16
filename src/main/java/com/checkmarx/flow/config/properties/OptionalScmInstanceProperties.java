package com.checkmarx.flow.config.properties;

import lombok.Data;

@Data
public class OptionalScmInstanceProperties {

    private String webhookToken;
    private String token;
    private String url;
    private String apiUrl;
}