package com.apricorn.judge.contract;

import com.apricorn.problem.entity.Problem;
import com.apricorn.problem.entity.TestCase;
import com.apricorn.problem.repository.ProblemRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "DOCKER_AVAILABLE", matches = "true")
public class CanonicalContractIntegrityTest {

    @Autowired
    private ProblemRepository problemRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "int", "long", "boolean", "String", "int[]", "String[]", "int[][]"
    );

    @Test
    @Transactional
    public void verifyAllVisibleCanonicalProblemsHaveValidContracts() throws Exception {
        List<Problem> problems = problemRepository.findAll();
        
        for (Problem problem : problems) {
            if (!problem.isVisible() || problem.getSlug().startsWith("judge-test")) {
                continue; // Skip invisible problems like lru-cache and integration test dummies
            }

            // 1. Basic Metadata Presence
            assertThat(problem.getMethodName()).as("methodName for " + problem.getSlug()).isNotNull().isNotBlank();
            assertThat(problem.getReturnType()).as("returnType for " + problem.getSlug()).isNotNull().isNotBlank();
            assertThat(problem.getParameterMetadata()).as("parameterMetadata for " + problem.getSlug()).isNotNull().isNotBlank();
            assertThat(problem.getStarterCode()).as("starterCode for " + problem.getSlug()).isNotNull().isNotBlank();

            // 2. Supported Types Validation
            assertThat(SUPPORTED_TYPES).as("returnType supported for " + problem.getSlug())
                    .contains(problem.getReturnType());

            JsonNode paramNode = objectMapper.readTree(problem.getParameterMetadata());
            assertThat(paramNode.isArray()).as("parameterMetadata should be an array for " + problem.getSlug()).isTrue();

            for (JsonNode pNode : paramNode) {
                assertThat(pNode.has("name")).as("param missing name").isTrue();
                assertThat(pNode.has("type")).as("param missing type").isTrue();
                String type = pNode.get("type").asText();
                assertThat(SUPPORTED_TYPES).as("param type supported for " + problem.getSlug()).contains(type);
            }

            // 3. Test Cases Validation
            List<TestCase> testCases = problem.getTestCases();
            assertThat(testCases).as("At least one testcase for " + problem.getSlug()).isNotEmpty();

            for (TestCase tc : testCases) {
                // Parse structured JSON
                JsonNode inputNode = objectMapper.readTree(tc.getInput());
                JsonNode expectedOutputNode = objectMapper.readTree(tc.getExpectedOutput());

                assertThat(inputNode.isArray()).as("testcase input must be JSON array for " + problem.getSlug() + " (input: " + tc.getInput() + ")").isTrue();
                
                // Argument count matches parameter count
                assertThat(inputNode.size()).as("Argument count matches param count for " + problem.getSlug())
                        .isEqualTo(paramNode.size());
            }
        }
    }
}
