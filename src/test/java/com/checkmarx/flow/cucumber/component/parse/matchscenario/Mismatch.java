package com.checkmarx.flow.cucumber.component.parse.matchscenario;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Contains differences between a specific JSON file and its corresponding reference file.
 */
public class Mismatch {
    private final String baseName;
    private final JsonNode differences;

    public Mismatch(String baseName, JsonNode differences) {
        this.baseName = baseName;
        this.differences = differences;
    }

    public String getBaseName() {
        return baseName;
    }

    public JsonNode getDifferences() {
        return differences;
    }
}
