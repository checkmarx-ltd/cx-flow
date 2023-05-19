package com.checkmarx.flow.dto.itop.organization;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrganizationResponse {
    private int code;
    private String message;
}
