package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.constants.FlowConstants;
import com.checkmarx.sdk.dto.ScanResults;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;


public class CodeBashingService {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(JiraService.class);
    Map<String,String> lessonsMap = null;
    RestTemplate restTemplate = new RestTemplate();
    FlowProperties flowProperties;

    public CodeBashingService(FlowProperties properties){
        flowProperties = properties;
    }
    public void createLessonsMap(ScanResults results) {

        try{
            HttpEntity<?> httpEntity = new HttpEntity<>(createAuthHeaders());
            ResponseEntity<String> response = restTemplate.exchange("https://api.devcodebashing.com/lessons", HttpMethod.GET, httpEntity, String.class);

            if (response.getBody()==null){
             log.error("can't get codebashing lessons. response is null");
            }

            log.info("codebashing API get lessons response: {} - {}", response.getStatusCode(), response.getStatusCodeValue());

            JSONArray lessonsArray = new JSONArray(response.getBody());
            lessonsMap = createLessonMapByCwe(lessonsArray);
        }
        catch (Exception ex){
            log.error("can't get codbasing lessons map - {}", ex.getMessage());
        }
    }

    private HashMap<String, String> createLessonMapByCwe(JSONArray jArray) throws Exception {
        HashMap<String, String> map = new HashMap<>();
        log.info("creating codebashing lessons map");

        if (jArray != null) {
            for (int i=0;i<jArray.length();i++){
                //listdata.add(jArray.getJSONObject(i));

                JSONObject lessonObject = jArray.getJSONObject(i);
                String CWE = lessonObject.getString("cwe_id").split("-")[1];
                String lessonPath = lessonObject.getString("path");

                if(StringUtils.isEmpty(CWE) || StringUtils.isEmpty(lessonPath)){
                    throw new Exception("can't find CWE and lesson path in " + lessonObject.toMap().toString());
                }

                if (!map.containsKey(CWE)){
                    log.debug("adding codebashing lesson '{}' path to cwe {}", lessonPath, CWE);
                    map.put(CWE, lessonPath);
                }
            }
        }
        return map;
    }

    public void addCodebashingUrlToIssue(ScanResults.XIssue xIssue){

        if (lessonsMap.get(xIssue.getCwe()) != null) {
            String lessonPath = String.format("https://cxa.codebashing.com/%s", lessonsMap.get(xIssue.getCwe()));
            xIssue.getAdditionalDetails().put(FlowConstants.CODE_BASHING_LESSON, lessonPath);
        }
        else{
            xIssue.getAdditionalDetails().put(FlowConstants.CODE_BASHING_LESSON, flowProperties.getCodebashUrl());
        }
    }

    private HttpHeaders createAuthHeaders(){
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        httpHeaders.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        httpHeaders.set("x-api-key", "DOwEHVb1aF1YpYRhk41HL4oeh54B149Q5SPH2b3E");
        return httpHeaders;
    }

    public static class Lesson{
        private String cweId;
        private String lessonPath;
    }
}
