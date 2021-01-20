package com.checkmarx.flow.service;

import com.checkmarx.flow.config.CodebashingProperties;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.constants.FlowConstants;
import com.checkmarx.sdk.dto.ScanResults;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import javax.validation.ValidationException;
import java.io.InvalidObjectException;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Service
@Slf4j
public class CodeBashingService {

    private Map<String,String> lessonsMap;
    private RestTemplate restTemplate = new RestTemplate();
    private final FlowProperties flowProperties;
    private final CodebashingProperties codebashingProperties;

    public void createLessonsMap() {

        try{
            validateCodeBashingLessonsIntegration();
            log.info("sending codebashing API [{}] request to get lessons map", codebashingProperties.getCodebashingApiUrl());
            HttpEntity<?> httpEntity = new HttpEntity<>(createAuthHeaders());
            ResponseEntity<String> response = restTemplate.exchange(codebashingProperties.getCodebashingApiUrl(), HttpMethod.GET, httpEntity, String.class);

            if (response.getBody()==null){
             log.error("can't get codebashing lessons. response is null");
            }

            log.info("codebashing API get lessons response: {} - {}", response.getStatusCode(), response.getStatusCodeValue());

            JSONArray lessonsArray = new JSONArray(response.getBody());
            lessonsMap = createLessonMapByCwe(lessonsArray);
        }
        catch (ValidationException validationException){
            log.info("not using CodeBashing lessons integration - {}", validationException.getMessage());
        }
        catch (InvalidObjectException invalidValueException){
            log.warn("can't create codebashing lessons map - {}", invalidValueException.getMessage());
        }
        catch (Exception ex){
            log.error("can't get codbashing lessons map - {}", ex.getMessage());
        }
    }

    private HashMap<String, String> createLessonMapByCwe(JSONArray jArray) throws InvalidObjectException {
        HashMap<String, String> map = new HashMap<>();
        log.info("creating codebashing lessons map");
        String cweSplitter = "-";

        if (jArray != null) {
            for (int i=0;i<jArray.length();i++){
                JSONObject lessonObject = jArray.getJSONObject(i);
                String CWE = lessonObject.getString("cwe_id").split(cweSplitter)[1];
                String lessonPath = lessonObject.getString("path");
                String language = lessonObject.getString("lang");
                int queryId = lessonObject.getInt("cxQueryId");

                String mapKey = buildMapKey(CWE, language, String.valueOf(queryId));
                if(StringUtils.isEmpty(CWE) || StringUtils.isEmpty(lessonPath)){
                    throw new InvalidObjectException("can't find CWE and lesson path in " + lessonObject.toMap().toString());
                }

                if (!map.containsKey(mapKey)){
                    log.debug("adding codebashing lesson '{}' path to cwe {}", lessonPath, mapKey);
                    map.put(mapKey, lessonPath);
                }
            }
        }
        return map;
    }

    private String buildMapKey(String cwe, String language, String queryId) {
        return String.format("%s-%s-%s", cwe, language, queryId);
    }

    void addCodebashingUrlToIssue(ScanResults.XIssue xIssue){
        String mapKey = xIssue.getCwe();
        if(validateXIssueFields(xIssue) && codebashingProperties.getTenantBaseUrl() != null) {
            mapKey = buildMapKey(xIssue.getCwe(), xIssue.getLanguage(), xIssue.gerQueryId());
            if (lessonsMap != null && lessonsMap.get(mapKey) != null) {
                String lessonPath = String.format("%s%s", codebashingProperties.getTenantBaseUrl(), lessonsMap.get(mapKey));
                xIssue.getAdditionalDetails().put(FlowConstants.CODE_BASHING_LESSON, lessonPath);
            } else {
                xIssue.getAdditionalDetails().put(FlowConstants.CODE_BASHING_LESSON, flowProperties.getCodebashUrl());
            }
        }
        else {
            if (xIssue.getAdditionalDetails() == null){
                xIssue.setAdditionalDetails(new HashMap<>());
            }
            xIssue.getAdditionalDetails().put(FlowConstants.CODE_BASHING_LESSON, flowProperties.getCodebashUrl());
        }

        Object lesson = xIssue.getAdditionalDetails().get(FlowConstants.CODE_BASHING_LESSON);
        if (lesson != null) {
            log.debug("added codebashing lesson {} to xIssue: {}", lesson, mapKey);
        }
    }

    private HttpHeaders createAuthHeaders(){
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        httpHeaders.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        httpHeaders.set("x-api-key", codebashingProperties.getApiSecret());
        return httpHeaders;
    }

    private void validateCodeBashingLessonsIntegration() {
         if(codebashingProperties == null ||
                 codebashingProperties.getCodebashingApiUrl() == null ||
                 codebashingProperties.getTenantBaseUrl() == null ||
                 codebashingProperties.getApiSecret() == null) {
             throw new ValidationException("one or more of the mandatory properties is missing");
        }
    }

    private boolean validateXIssueFields(ScanResults.XIssue xIssue) {
        return xIssue.getCwe() != null && xIssue.getLanguage() != null && xIssue.gerQueryId() != null && xIssue.getAdditionalDetails() != null;
    }
}
