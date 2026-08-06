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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JavaDockerExecutorTest {
    private DockerRunner dockerRunner;
    private OutputComparator outputComparator;
    private JavaHarnessGenerator javaHarnessGenerator;
    private JavaDockerExecutor javaDockerExecutor;
    private JudgeProperties judgeProperties;

    @TempDir
    Path tempWorkspace;

    @BeforeEach
    void setUp() {
        dockerRunner = mock(DockerRunner.class);
        outputComparator = new OutputComparator();
        javaHarnessGenerator = mock(JavaHarnessGenerator.class);
        javaDockerExecutor = new JavaDockerExecutor(dockerRunner, outputComparator, javaHarnessGenerator);
        
        judgeProperties = new JudgeProperties();
        judgeProperties.setWorkspaceRoot(tempWorkspace.toAbsolutePath().toString());
    }

    @Test
    void rejectsSubmissionWithMainMethod() {
        Submission submission = new Submission();
        submission.setId(1L);
        submission.setLanguage(Language.JAVA);
        submission.setSourceCode("public class Solution { public static void main(String[] args) {} }");

        JudgeResult result = javaDockerExecutor.execute(submission, List.of(), judgeProperties);

        assertThat(result.verdict()).isEqualTo(Verdict.COMPILATION_ERROR);
        assertThat(result.errorMessage()).contains("User-defined 'main' method is not allowed");
    }

    @Test
    void rejectsSubmissionWithExplicitImport() {
        Submission submission = new Submission();
        submission.setId(1L);
        submission.setLanguage(Language.JAVA);
        submission.setSourceCode("import java.util.List;\npublic class Solution {}");

        JudgeResult result = javaDockerExecutor.execute(submission, List.of(), judgeProperties);

        assertThat(result.verdict()).isEqualTo(Verdict.COMPILATION_ERROR);
        assertThat(result.errorMessage()).contains("Explicit import statements are not allowed");
    }

    @Test
    void handlesCompilationError() throws Exception {
        Submission submission = new Submission();
        submission.setId(1L);
        submission.setLanguage(Language.JAVA);
        submission.setSourceCode("public class Solution { syntax error }");

        when(javaHarnessGenerator.generateHarness(any(), any())).thenReturn("public class Main {}");
        when(dockerRunner.runContainer(any(), any(), eq("javac Main.java Solution.java"), any(), anyLong(), anyInt(), anyDouble(), anyLong()))
                .thenReturn(new ExecutionResult(1, "", "Solution.java:1: error: ';' expected", 100, false, false, false));

        JudgeResult result = javaDockerExecutor.execute(submission, List.of(), judgeProperties);

        assertThat(result.verdict()).isEqualTo(Verdict.COMPILATION_ERROR);
        assertThat(result.errorMessage()).contains("error: ';' expected");
    }

    @Test
    void handlesTimeLimitExceeded() throws Exception {
        Problem problem = new Problem();
        problem.setTimeLimitMs(1000);

        Submission submission = new Submission();
        submission.setId(1L);
        submission.setLanguage(Language.JAVA);
        submission.setSourceCode("public class Solution { public int add(int a, int b){ return a+b;} }");
        submission.setProblem(problem);

        TestCase tc = new TestCase();
        tc.setInput("[1, 2]");
        tc.setExpectedOutput("3");
        tc.setOrderIndex(0);

        when(javaHarnessGenerator.generateHarness(any(), any())).thenReturn("public class Main {}");
        when(dockerRunner.runContainer(any(), any(), eq("javac Main.java Solution.java"), any(), anyLong(), anyInt(), anyDouble(), anyLong()))
                .thenReturn(new ExecutionResult(0, "", "", 100, false, false, false));

        when(dockerRunner.runContainer(any(), any(), eq("java Main 0"), any(), anyLong(), anyInt(), anyDouble(), anyLong()))
                .thenReturn(new ExecutionResult(-1, "", "", 1050, true, false, false));

        JudgeResult result = javaDockerExecutor.execute(submission, List.of(tc), judgeProperties);

        assertThat(result.verdict()).isEqualTo(Verdict.TIME_LIMIT_EXCEEDED);
    }
}
