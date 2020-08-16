package com.checkmarx.flow.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class OptionalScmInstanceProperties {

    private String webhookToken;
    private String token;
    private String url;
    private String apiUrl;
}