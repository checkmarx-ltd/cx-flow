package com.checkmarx.flow.dto;

import lombok.Getter;
import lombok.Setter;

public enum Status{

    SUCCESS((short) 0, "SUCCESS"),
    FAILURE((short) 1, "FAILURE");

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
}
