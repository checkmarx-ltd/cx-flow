package com.checkmarx.flow.utils;

import org.springframework.http.HttpHeaders;

import java.util.Base64;

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

}
