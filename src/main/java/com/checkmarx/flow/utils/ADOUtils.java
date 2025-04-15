package com.checkmarx.flow.utils;

import org.springframework.http.HttpHeaders;

import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ADOUtils {

    private ADOUtils() {
    }

    public static HttpHeaders createPatchAuthHeaders(String token){
        HttpHeaders httpHeaders = createAuthHeaders(token);
        httpHeaders.set("Content-Type", "application/json-patch+json");
        return httpHeaders;
    }

    public static HttpHeaders createAuthHeaders(String token){
        String encoding = Base64.getEncoder().encodeToString(":".concat(token).getBytes());
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("Content-Type", "application/json");
        httpHeaders.set("Authorization", "Basic ".concat(encoding));
        httpHeaders.set("Accept", "application/json");
        return httpHeaders;
    }

    public static String extractRegex(String regexPattern,String errorMessage,String inputText){
        Pattern pattern= Pattern.compile(regexPattern);
        Matcher matcher= pattern.matcher(inputText);
        if(matcher.find()){
            return matcher.group(1);
        }
        else{
            throw new IllegalStateException(errorMessage);
        }
    }

}
