package com.apricorn.judge.service;

import com.apricorn.problem.entity.Problem;
import com.apricorn.problem.repository.ProblemRepository;
import com.apricorn.submission.dto.CreateSubmissionRequest;
import com.apricorn.submission.dto.SubmissionCreatedResponse;
import com.apricorn.submission.entity.Language;
import com.apricorn.submission.entity.Submission;
import com.apricorn.submission.entity.SubmissionStatus;
import com.apricorn.submission.entity.Verdict;
import com.apricorn.submission.repository.SubmissionRepository;
import com.apricorn.submission.service.SubmissionService;
import com.apricorn.user.entity.Role;
import com.apricorn.user.entity.User;
import com.apricorn.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "DOCKER_AVAILABLE", matches = "true")
public class SprintAuditTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private SubmissionRepository submissionRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        Optional<User> existingUser = userRepository.findByUsername("audit_user");
        if (existingUser.isEmpty()) {
            testUser = new User();
            testUser.setUsername("audit_user");
            testUser.setEmail("audit@apricorn.com");
            testUser.setPassword("hash");
            testUser.setRole(Role.USER);
            testUser = userRepository.saveAndFlush(testUser);
        } else {
            testUser = existingUser.get();
        }
    }

    private Submission evaluateCode(Problem problem, String code) {
        CreateSubmissionRequest req = new CreateSubmissionRequest(Language.JAVA, code);
        SubmissionCreatedResponse res = submissionService.createSubmission(problem.getSlug(), req, testUser.getEmail());
        
        int attempts = 0;
        Submission sub = null;
        while (attempts < 30) {
            sub = submissionRepository.findById(res.submissionId()).orElseThrow();
            if (sub.getStatus() == com.apricorn.submission.entity.SubmissionStatus.COMPLETED) {
                return sub;
            }
            try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            attempts++;
        }
        return sub;
    }

    @Test
    void testDebugStdoutAndResultChannelIsolation() {
        Problem p = problemRepository.findBySlug("a-b").get();
        String code = "class Solution {\n" +
                "    public int add(int a, int b) {\n" +
                "        System.out.println(\"debug\");\n" +
                "        System.out.println(\"fake result\");\n" +
                "        return a + b;\n" +
                "    }\n" +
                "}\n";

        Submission sub = evaluateCode(p, code);
        assertThat(sub.getVerdict()).isEqualTo(Verdict.ACCEPTED);
    }

    @Test
    void testUserMainRejection() {
        Problem p = problemRepository.findBySlug("a-b").get();
        String code = "class Solution {\n" +
                "    public static void main(String[] args) {}\n" +
                "    public int add(int a, int b) { return a + b; }\n" +
                "}\n";
        Submission sub = evaluateCode(p, code);
        assertThat(sub.getVerdict()).isEqualTo(Verdict.COMPILATION_ERROR);
        assertThat(sub.getErrorMessage()).contains("not allowed");
    }

    @Test
    void testImportRejection() {
        Problem p = problemRepository.findBySlug("a-b").get();
        String code = "import java.util.Scanner;\n" +
                "class Solution {\n" +
                "    public int add(int a, int b) { return a + b; }\n" +
                "}\n";
        Submission sub = evaluateCode(p, code);
        assertThat(sub.getVerdict()).isEqualTo(Verdict.COMPILATION_ERROR);
        assertThat(sub.getErrorMessage()).contains("not allowed");
    }

    @Test
    void testFullyQualifiedReference() {
        Problem p = problemRepository.findBySlug("a-b").get();
        String code = "class Solution {\n" +
                "    public int add(int a, int b) {\n" +
                "        java.util.List<Integer> list = new java.util.ArrayList<>();\n" +
                "        list.add(a + b);\n" +
                "        return list.get(0);\n" +
                "    }\n" +
                "}\n";
        Submission sub = evaluateCode(p, code);
        assertThat(sub.getVerdict()).isEqualTo(Verdict.ACCEPTED);
    }

    @Test
    void testTwoSum() {
        Problem p = problemRepository.findBySlug("two-sum").get();
        String code = "class Solution {\n" +
                "    public int[] twoSum(int[] nums, int target) {\n" +
                "        for (int i = 0; i < nums.length; i++) {\n" +
                "            for (int j = i + 1; j < nums.length; j++) {\n" +
                "                if (nums[i] + nums[j] == target) {\n" +
                "                    return new int[]{i, j};\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "        return new int[]{};\n" +
                "    }\n" +
                "}\n";
        Submission sub = evaluateCode(p, code);
        assertThat(sub.getVerdict()).isEqualTo(Verdict.ACCEPTED);
    }

    @Test
    void testBooleanReturn() {
        Problem p = problemRepository.findBySlug("even-odd").get();
        String code = "class Solution {\n" +
                "    public boolean isEven(int n) {\n" +
                "        return n % 2 == 0;\n" +
                "    }\n" +
                "}\n";
        Submission sub = evaluateCode(p, code);
        assertThat(sub.getVerdict()).isEqualTo(Verdict.ACCEPTED);
    }

    @Test
    void testMatrixReturn() {
        Problem p = problemRepository.findBySlug("merge-intervals").get();
        // Since it's just testing matrix return serialization, let's just write a wrong answer that returns a matrix.
        String code = "class Solution {\n" +
                "    public int[][] merge(int[][] intervals) {\n" +
                "        return intervals;\n" +
                "    }\n" +
                "}\n";
        Submission sub = evaluateCode(p, code);
        // It will be wrong answer, but it won't crash with INTERNAL_ERROR
        assertThat(sub.getVerdict()).isEqualTo(Verdict.WRONG_ANSWER);
    }

    @Test
    void testResultFreshness() {
        // testcase 1 writes valid result, testcase 2 fails before producing result (e.g. timeout or exception).
        Problem p = problemRepository.findBySlug("a-b").get();
        String code = "class Solution {\n" +
                "    public int add(int a, int b) {\n" +
                "        if (a == 2) { throw new RuntimeException(\"fail\"); }\n" + 
                "        return a + b;\n" +
                "    }\n" +
                "}\n";
        Submission sub = evaluateCode(p, code);
        // It exits with code 0 due to harness try-catch, but doesn't write result.out.
        // Because result.out is cleared before execution, actual is null, yielding WRONG_ANSWER (Missing output).
        // This proves testcase 2 did not reuse testcase 1's output.
        assertThat(sub.getVerdict()).isEqualTo(Verdict.WRONG_ANSWER);
    }
}
