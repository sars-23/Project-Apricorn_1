package com.apricorn.judge.service;

import com.apricorn.problem.dto.CreateProblemRequest;
import com.apricorn.problem.entity.Difficulty;
import com.apricorn.problem.entity.Problem;
import com.apricorn.problem.entity.TestCase;
import com.apricorn.problem.repository.ProblemRepository;
import com.apricorn.problem.repository.TestCaseRepository;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
public class JudgeCorrectnessIntegrationTest {

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubmissionRepository submissionRepository;
    
    @Autowired
    private JudgeService dockerJudgeService;

    private User testUser;
    private Problem testProblem;

    @BeforeEach
    void setUp() {
        Optional<User> existingUser = userRepository.findByUsername("judge_test_user");
        if (existingUser.isEmpty()) {
            testUser = new User();
            testUser.setUsername("judge_test_user");
            testUser.setEmail("judge_test@apricorn.com");
            testUser.setPassword("hash");
            testUser.setRole(Role.USER);
            testUser = userRepository.saveAndFlush(testUser);
        } else {
            testUser = existingUser.get();
        }

        Optional<Problem> existingProb = problemRepository.findBySlug("judge-test-prob");
        if (existingProb.isEmpty()) {
            testProblem = new Problem();
            testProblem.setTitle("Judge Test Prob");
            testProblem.setSlug("judge-test-prob");
            testProblem.setDescription("Output A + B");
            testProblem.setDifficulty(Difficulty.EASY);
            testProblem.setCreatedBy(testUser);
            testProblem.setTimeLimitMs(2000);
            testProblem.setMemoryLimitMb(256);
            testProblem = problemRepository.saveAndFlush(testProblem);

            TestCase tc1 = new TestCase();
            tc1.setProblem(testProblem);
            tc1.setInput("2 3");
            tc1.setExpectedOutput("5");
            tc1.setOrderIndex(1);
            tc1.setSample(true);
            testCaseRepository.saveAndFlush(tc1);

            TestCase tc2 = new TestCase();
            tc2.setProblem(testProblem);
            tc2.setInput("-5 10");
            tc2.setExpectedOutput("5");
            tc2.setOrderIndex(2);
            tc2.setSample(false);
            testCaseRepository.saveAndFlush(tc2);
        } else {
            testProblem = existingProb.get();
        }
    }


    private Submission evaluateCode(String code) {
        CreateSubmissionRequest req = new CreateSubmissionRequest(Language.JAVA, code);
        SubmissionCreatedResponse res = submissionService.createSubmission(testProblem.getSlug(), req, testUser.getEmail());
        
        // Ensure submission was created
        assertThat(res.submissionId()).isNotNull();
        
        // This is async, so we must wait for completion
        int attempts = 0;
        Submission sub = null;
        while (attempts < 20) {
            sub = submissionRepository.findById(res.submissionId()).orElseThrow();
            if (sub.getStatus() == com.apricorn.submission.entity.SubmissionStatus.COMPLETED) {
                return sub;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            attempts++;
        }
        return sub;
    }

    @Test
    void testCorrectSolutionReturnsAccepted() {
        String code = "import java.util.Scanner;\n" +
                "public class Main {\n" +
                "    public static void main(String[] args) {\n" +
                "        Scanner sc = new Scanner(System.in);\n" +
                "        int a = sc.nextInt();\n" +
                "        int b = sc.nextInt();\n" +
                "        System.out.println(a + b);\n" +
                "    }\n" +
                "}\n";

        Submission sub = evaluateCode(code);
        
        assertThat(sub.getStatus()).isEqualTo(SubmissionStatus.COMPLETED);
        assertThat(sub.getVerdict()).isEqualTo(Verdict.ACCEPTED);
        assertThat(sub.getPassedTestCases()).isEqualTo(2);
        assertThat(sub.getTotalTestCases()).isEqualTo(2);
    }

    @Test
    void testIncorrectSolutionReturnsWrongAnswer() {
        String code = "import java.util.Scanner;\n" +
                "public class Main {\n" +
                "    public static void main(String[] args) {\n" +
                "        Scanner sc = new Scanner(System.in);\n" +
                "        int a = sc.nextInt();\n" +
                "        int b = sc.nextInt();\n" +
                "        System.out.println(a - b);\n" +
                "    }\n" +
                "}\n";

        Submission sub = evaluateCode(code);
        
        assertThat(sub.getStatus()).isEqualTo(SubmissionStatus.COMPLETED);
        assertThat(sub.getVerdict()).isEqualTo(Verdict.WRONG_ANSWER);
        assertThat(sub.getPassedTestCases()).isLessThan(2);
        assertThat(sub.getTotalTestCases()).isEqualTo(2);
    }

    @Test
    void testInvalidSyntaxReturnsCompilationError() {
        String code = "public class Main {\n" +
                "    public static void main(String[] args) {\n" +
                "        System.out.println(\"broken\")\n" +
                "    }\n" +
                "}\n";

        Submission sub = evaluateCode(code);
        
        assertThat(sub.getStatus()).isEqualTo(SubmissionStatus.COMPLETED);
        assertThat(sub.getVerdict()).isEqualTo(Verdict.COMPILATION_ERROR);
    }
}
