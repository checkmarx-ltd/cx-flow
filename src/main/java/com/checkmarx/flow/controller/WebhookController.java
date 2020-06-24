package com.checkmarx.flow.controller;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.ControllerRequest;
import com.checkmarx.flow.dto.EventResponse;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.CxProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Contains common logic for controllers that receive webhook requests.
 */
@Slf4j
public class WebhookController {
    protected ResponseEntity<EventResponse> getSuccessResponse() {
        return ResponseEntity.status(HttpStatus.OK).body(EventResponse.builder()
                .message("Scan Request Successfully Submitted")
                .success(true)
                .build());
    }

    protected ResponseEntity<EventResponse> getBadRequestMessage(IllegalArgumentException cause, ControllerRequest controllerRequest, String product) {
        String errorMessage = String.format("Error submitting Scan Request. Product or Bugtracker option incorrect %s | %s",
                StringUtils.defaultIfEmpty(product, ""),
                StringUtils.defaultIfEmpty(controllerRequest.getBug(), ""));

        log.error(errorMessage, cause);
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(EventResponse.builder()
                .message(errorMessage)
                .success(false)
                .build());
    }

    protected void setExclusionProperties(ControllerRequest target, CxProperties cxProperties) {
        if (target.getExcludeFiles() == null && !ScanUtils.empty(cxProperties.getExcludeFiles())) {
            target.setExcludeFiles(Arrays.asList(cxProperties.getExcludeFiles().split(",")));
        }
        if (target.getExcludeFolders() == null && !ScanUtils.empty(cxProperties.getExcludeFolders())) {
            target.setExcludeFolders(Arrays.asList(cxProperties.getExcludeFolders().split(",")));
        }
    }

    protected List<String> getBranches(List<String> branchesFromQuery, FlowProperties flowProperties) {
        List<String> result = new ArrayList<>();
        if (!ScanUtils.empty(branchesFromQuery)) {
            result.addAll(branchesFromQuery);
        } else if (!ScanUtils.empty(flowProperties.getBranches())) {
            result.addAll(flowProperties.getBranches());
        }
        return result;
    }

    protected void overrideScanPreset(ControllerRequest controllerRequest, ScanRequest scanRequest) {
        if (!ScanUtils.empty(controllerRequest.getPreset())) {
            scanRequest.setScanPreset(controllerRequest.getPreset());
            scanRequest.setScanPresetOverride(true);
        }
    }

    protected boolean isScanIncremental(ControllerRequest request, CxProperties cxProperties) {
        return Optional.ofNullable(request.getIncremental())
                .orElse(cxProperties.getIncremental());
    }
}
