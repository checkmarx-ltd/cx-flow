package com.checkmarx.flow.controller;

import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.azure.AdoDetailsRequest;
import com.checkmarx.sdk.config.Constants;

import java.util.Optional;

public abstract class AdoControllerBase extends WebhookController {
    protected AdoDetailsRequest ensureDetailsNotNull(AdoDetailsRequest requestToCheck) {
        return Optional.ofNullable(requestToCheck)
                .orElseGet(() -> AdoDetailsRequest.builder().build());
    }

    protected void addMetadataToScanRequest(AdoDetailsRequest source, ScanRequest target) {
        target.putAdditionalMetadata(Constants.ADO_ISSUE_KEY, source.getAdoIssue());
        target.putAdditionalMetadata(Constants.ADO_ISSUE_BODY_KEY, source.getAdoBody());
        target.putAdditionalMetadata(Constants.ADO_OPENED_STATE_KEY, source.getAdoOpened());
        target.putAdditionalMetadata(Constants.ADO_CLOSED_STATE_KEY, source.getAdoClosed());
    }
}
