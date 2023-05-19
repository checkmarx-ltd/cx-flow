package com.checkmarx.flow.dto.itop.service;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceResponse {

    private int code;
    private String message;
}
