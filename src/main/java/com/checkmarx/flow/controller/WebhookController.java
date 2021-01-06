package com.checkmarx.flow.controller;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.ControllerRequest;
import com.checkmarx.flow.dto.EventResponse;
import com.checkmarx.flow.dto.ScanRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Contains common logic for controllers that receive webhook requests.
 */
@Slf4j
public abstract class WebhookController {
    protected ResponseEntity<EventResponse> getSuccessMessage() {
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



    protected void setBugTracker(FlowProperties flowProperties, ControllerRequest target) {
        if (StringUtils.isEmpty(target.getBug())) {
            target.setBug(flowProperties.getBugTracker());
        }
    }

    protected List<String> getBranches(ControllerRequest request, FlowProperties flowProperties) {
        List<String> result = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(request.getBranch())) {
            result.addAll(request.getBranch());
        } else if (CollectionUtils.isNotEmpty(flowProperties.getBranches())) {
            result.addAll(flowProperties.getBranches());
        }
        return result;
    }
    

    protected ControllerRequest ensureNotNull(ControllerRequest requestToCheck) {
        return Optional.ofNullable(requestToCheck)
                .orElseGet(() -> ControllerRequest.builder().build());
    }

    protected void setScmInstance(ControllerRequest controllerRequest, ScanRequest request) {
        Optional.ofNullable(controllerRequest.getScmInstance()).ifPresent(request::setScmInstance);
    }
}
