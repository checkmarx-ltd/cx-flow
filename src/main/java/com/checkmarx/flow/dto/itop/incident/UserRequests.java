package com.checkmarx.flow.dto.itop.incident;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class UserRequests {
    private int code;
    private String message;
    private Map<String, UserRequestResponse> objects;
    
    public String toString() {
        return "UserRequests{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", objects=" + objects +
                '}';
    }
}
