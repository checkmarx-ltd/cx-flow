package com.checkmarx.flow.config.properties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OptionalScmInstanceProperties {

    private String webhookToken;
    private String token;
    private String url;
    private String apiUrl;
}