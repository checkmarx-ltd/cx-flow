package com.checkmarx.flow.cucumber.component.parse.matchscenario;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of comparison between a list of CxFlow reports and their corresponding reference report files.
 */
public class ComparisonResult {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final ObjectWriter prettyJsonFormatter = objectMapper.writerWithDefaultPrettyPrinter();

    final List<Mismatch> mismatches = new ArrayList<>();

    public boolean containsMismatches() {
        return !mismatches.isEmpty();
    }

    public void addMismatch(Mismatch mismatch) {
        mismatches.add(mismatch);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (Mismatch mismatch : mismatches) {
            String differences = formatDifferences(mismatch.getDifferences());
            String message = String.format("%s: %s\n\n", mismatch.getBaseName(), differences);
            result.append(message);
        }
        return result.toString();
    }

    private String formatDifferences(JsonNode differences) {
        String result;
        try {
            result = prettyJsonFormatter.writeValueAsString(differences);
        } catch (JsonProcessingException e) {
            result = "<JSON formatting error>";
        }
        return result;
    }
}
