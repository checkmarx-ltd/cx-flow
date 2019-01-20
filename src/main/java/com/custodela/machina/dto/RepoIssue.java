package com.custodela.machina.dto;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;

import java.util.Objects;

public class RepoIssue {
    private String title;

    public String getTitle(){
        return this.title;
    }

    public void setTitle(String title){
        this.title = title;
    }


    /**
     * JSON http request body comments
     *
     * @param comment
     * @return
     */
    public static JSONObject getJSONComment(String bodyField, String comment) throws JSONException{
        JSONObject requestBody = new JSONObject();
        requestBody.put(bodyField, comment);
        return requestBody;
    }


    /**
     * @return JSON Object for close issue request
     */
    public static JSONObject getJSONCloseIssue(String transitionField, String transition) throws JSONException{
        JSONObject requestBody = new JSONObject();
        requestBody.put(transitionField, transition);
        return requestBody;
    }

    /**
     * Create JSON http request body for an create/update Issue POST request to GitLab
     *
     * @param title
     * @param bodyField
     * @param body
     * @param transitionField
     * @param transition
     * @return
     */
    public static JSONObject getJSONUpdateIssue(String title, String bodyField, String body, String transitionField, String transition) throws JSONException{
        JSONObject requestBody = new JSONObject();
        requestBody.put("title", title);
        requestBody.put(bodyField, body);
        requestBody.put(transitionField, transition);
        return requestBody;
    }
    /**
     * Create JSON http request body for an create/update Issue POST request to GitHub/GitLab
     *
     * @return JSON Object of create issue request
     */
    public static JSONObject getJSONCreateIssue(String title, String bodyField, String body) throws JSONException{
        JSONObject requestBody = new JSONObject();
        requestBody.put("title", title);
        requestBody.put(bodyField, body);
        return requestBody;
    }

    public static String getNextURIFromHeaders(HttpHeaders headers) {
        String headerName = "link";
        String rel = "next";

        if (headers == null || headers.get(headerName) == null) {
            return null;
        }

        String linkHeader = Objects.requireNonNull(headers.get(headerName)).get(0);
        String uriWithSpecifiedRel = null;
        final String[] links = linkHeader.split(", ");
        String linkRelation;
        for (final String link : links) {
            final int positionOfSeparator = link.indexOf(';');
            linkRelation = link.substring(positionOfSeparator + 1, link.length()).trim();
            if (extractTypeOfRelation(linkRelation).equals(rel)) {
                uriWithSpecifiedRel = link.substring(1, positionOfSeparator - 1);
                break;
            }
        }

        return uriWithSpecifiedRel;
    }

    static String extractTypeOfRelation(final String linkRelation) {
        int positionOfEquals = linkRelation.indexOf('=');
        return linkRelation.substring(positionOfEquals + 2, linkRelation.length() - 1).trim();
    }

}
