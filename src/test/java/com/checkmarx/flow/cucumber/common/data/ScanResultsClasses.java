package com.checkmarx.flow.cucumber.common.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class ScanResultsClasses {

    public static class HeaderAdditionalDetails {
        @JsonProperty("flow-summary")
        public Map<String, Integer> flowSummary;
    }
}


