package com.checkmarx.flow.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class Status{

    public static final String SUCCESS_MSG = "SUCCESS";
    public static final String FAILURE_MSG = "FAILURE";

    public static final Status SUCCESS = new Status((short)0, SUCCESS_MSG);
    public static final Status FAILURE = new Status((short)1, FAILURE_MSG);
    
    private short code;
    private String message;

    Status(short code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public Status build(String message){
        this.message = message;
        return this;
    }
    
    public String getMessage(){
        return message;
    }
    
    public Short getCode() { return code;}
}
