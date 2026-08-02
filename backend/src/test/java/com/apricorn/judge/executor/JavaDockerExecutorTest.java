package com.apricorn.judge.executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.apricorn.judge.comparator.OutputComparator;
import com.apricorn.judge.config.JudgeProperties;
import com.apricorn.judge.runner.DockerRunner;
import com.apricorn.judge.runner.ExecutionResult;
import com.apricorn.problem.entity.Problem;
import com.apricorn.problem.entity.TestCase;
import com.apricorn.submission.entity.Language;
import com.apricorn.submission.entity.Submission;
import com.apricorn.submission.entity.Verdict;
import com.apricorn.submission.model.JudgeResult;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JavaDockerExecutorTest {
    private DockerRunner dockerRunner;
    private OutputComparator outputComparator;
    private JavaDockerExecutor javaDockerExecutor;
    private JudgeProperties judgeProperties;

    @BeforeEach
    void setUp() {
        dockerRunner = mock(DockerRunner.class);
        outputComparator = new OutputComparator();
        javaDockerExecutor = new JavaDockerExecutor(dockerRunner, outputComparator);
        judgeProperties = new JudgeProperties();
    }

    @Test
    void rejectsSubmissionWithoutMainClass() {
        Submission submission = new Submission();
        submission.setId(1L);
        submission.setLanguage(Language.JAVA);
        submission.setSourceCode("public class Solution {}");

        JudgeResult result = javaDockerExecutor.execute(submission, List.of(), judgeProperties);

        assertThat(result.verdict()).isEqualTo(Verdict.COMPILATION_ERROR);
        assertThat(result.errorMessage()).contains("Source code must define 'public class Main'");
    }

    @Test
    void handlesCompilationError() {
        Submission submission = new Submission();
        submission.setId(1L);
        submission.setLanguage(Language.JAVA);
        submission.setSourceCode("public class Main { syntax error }");

        when(dockerRunner.runContainer(any(), any(), eq("javac Main.java"), any(), anyLong(), anyInt(), anyDouble(), anyLong()))
                .thenReturn(new ExecutionResult(1, "", "Main.java:1: error: ';' expected", 100, false, false, false));

        JudgeResult result = javaDockerExecutor.execute(submission, List.of(), judgeProperties);

        assertThat(result.verdict()).isEqualTo(Verdict.COMPILATION_ERROR);
        assertThat(result.errorMessage()).contains("error: ';' expected");
    }

    @Test
    void handlesTimeLimitExceeded() {
        Problem problem = new Problem();
        problem.setTimeLimitMs(1000);

        Submission submission = new Submission();
        submission.setId(1L);
        submission.setLanguage(Language.JAVA);
        submission.setSourceCode("public class Main { public static void main(String[] args) {} }");
        submission.setProblem(problem);

        TestCase tc = new TestCase();
        tc.setInput("1");
        tc.setExpectedOutput("1");
        tc.setOrderIndex(0);

        when(dockerRunner.runContainer(any(), any(), eq("javac Main.java"), any(), anyLong(), anyInt(), anyDouble(), anyLong()))
                .thenReturn(new ExecutionResult(0, "", "", 100, false, false, false));

        when(dockerRunner.runContainer(any(), any(), eq("java Main"), eq("1"), anyLong(), anyInt(), anyDouble(), anyLong()))
                .thenReturn(new ExecutionResult(-1, "", "", 1050, true, false, false));

        JudgeResult result = javaDockerExecutor.execute(submission, List.of(tc), judgeProperties);

        assertThat(result.verdict()).isEqualTo(Verdict.TIME_LIMIT_EXCEEDED);
    }

    @Test
    void handlesWrongAnswer() {
        Problem problem = new Problem();
        problem.setTimeLimitMs(2000);

        Submission submission = new Submission();
        submission.setId(1L);
        submission.setLanguage(Language.JAVA);
        submission.setSourceCode("public class Main { public static void main(String[] args) { System.out.println(43); } }");
        submission.setProblem(problem);

        TestCase tc = new TestCase();
        tc.setInput("1");
        tc.setExpectedOutput("42");
        tc.setOrderIndex(0);

        when(dockerRunner.runContainer(any(), any(), eq("javac Main.java"), any(), anyLong(), anyInt(), anyDouble(), anyLong()))
                .thenReturn(new ExecutionResult(0, "", "", 100, false, false, false));

        when(dockerRunner.runContainer(any(), any(), eq("java Main"), eq("1"), anyLong(), anyInt(), anyDouble(), anyLong()))
                .thenReturn(new ExecutionResult(0, "43\n", "", 50, false, false, false));

        JudgeResult result = javaDockerExecutor.execute(submission, List.of(tc), judgeProperties);

        assertThat(result.verdict()).isEqualTo(Verdict.WRONG_ANSWER);
        assertThat(result.passedTestCases()).isEqualTo(0);
        assertThat(result.totalTestCases()).isEqualTo(1);
    }

    @Test
    void handlesAcceptedSubmission() {
        Problem problem = new Problem();
        problem.setTimeLimitMs(2000);

        Submission submission = new Submission();
        submission.setId(1L);
        submission.setLanguage(Language.JAVA);
        submission.setSourceCode("public class Main { public static void main(String[] args) { System.out.println(42); } }");
        submission.setProblem(problem);

        TestCase tc = new TestCase();
        tc.setInput("1");
        tc.setExpectedOutput("42");
        tc.setOrderIndex(0);

        when(dockerRunner.runContainer(any(), any(), eq("javac Main.java"), any(), anyLong(), anyInt(), anyDouble(), anyLong()))
                .thenReturn(new ExecutionResult(0, "", "", 100, false, false, false));

        when(dockerRunner.runContainer(any(), any(), eq("java Main"), eq("1"), anyLong(), anyInt(), anyDouble(), anyLong()))
                .thenReturn(new ExecutionResult(0, "42\n", "", 50, false, false, false));

        JudgeResult result = javaDockerExecutor.execute(submission, List.of(tc), judgeProperties);

        assertThat(result.verdict()).isEqualTo(Verdict.ACCEPTED);
        assertThat(result.passedTestCases()).isEqualTo(1);
        assertThat(result.totalTestCases()).isEqualTo(1);
    }
}
