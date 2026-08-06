package com.apricorn.judge.comparator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OutputComparator {
    private static final Logger log = LoggerFactory.getLogger(OutputComparator.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ComparisonResult compare(String expectedJson, String actualJson, String returnType) {
        if (expectedJson == null || actualJson == null) {
            return new ComparisonResult(false, expectedJson, actualJson, "Missing output");
        }

        try {
            JsonNode expectedNode = objectMapper.readTree(expectedJson);
            JsonNode actualNode = objectMapper.readTree(actualJson);

            boolean isMatch = expectedNode.equals(actualNode);

            String expectedFormatted = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(expectedNode);
            String actualFormatted = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(actualNode);

            if (isMatch) {
                return new ComparisonResult(true, expectedFormatted, actualFormatted, null);
            } else {
                return new ComparisonResult(false, expectedFormatted, actualFormatted, "Return value did not match expected value.");
            }
        } catch (Exception e) {
            log.error("Failed to parse JSON for comparison. Expected: {}, Actual: {}", expectedJson, actualJson, e);
            return new ComparisonResult(false, expectedJson, actualJson, "Malformed result structure.");
        }
    }

    public record ComparisonResult(boolean isMatch, String expectedFormatted, String actualFormatted, String errorReason) {}
}
