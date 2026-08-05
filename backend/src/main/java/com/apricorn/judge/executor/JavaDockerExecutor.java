package com.apricorn.judge.executor;

import com.apricorn.judge.comparator.OutputComparator;
import com.apricorn.judge.config.JudgeProperties;
import com.apricorn.judge.runner.DockerRunner;
import com.apricorn.judge.runner.ExecutionResult;
import com.apricorn.problem.entity.TestCase;
import com.apricorn.submission.entity.Language;
import com.apricorn.submission.entity.Submission;
import com.apricorn.submission.entity.Verdict;
import com.apricorn.submission.model.JudgeResult;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JavaDockerExecutor implements LanguageExecutor {
    private static final Logger log = LoggerFactory.getLogger(JavaDockerExecutor.class);
    private final DockerRunner dockerRunner;
    private final OutputComparator outputComparator;

    public JavaDockerExecutor(DockerRunner dockerRunner, OutputComparator outputComparator) {
        this.dockerRunner = dockerRunner;
        this.outputComparator = outputComparator;
    }

    @Override
    public boolean supports(Language language) {
        return language == Language.JAVA;
    }

    @Override
    public JudgeResult execute(Submission submission, List<TestCase> testCases, JudgeProperties properties) {
        if (submission.getLanguage() != Language.JAVA) {
            return new JudgeResult(
                    Verdict.INTERNAL_ERROR, 0, 0, 0, testCases.size(),
                    "Unsupported programming language for Java executor: " + submission.getLanguage()
            );
        }

        if (!submission.getSourceCode().contains("public class Main")) {
            return new JudgeResult(
                    Verdict.COMPILATION_ERROR, 0, 0, 0, testCases.size(),
                    "Java submission contract violation: Source code must define 'public class Main'."
            );
        }

        String uniqueDirName = submission.getId() + "-" + UUID.randomUUID();
        Path workspacePath = Paths.get(properties.getWorkspaceRoot(), uniqueDirName);
        File workspaceDir = workspacePath.toFile();

        try {
            Files.createDirectories(workspacePath);
            File sourceFile = new File(workspaceDir, "Main.java");
            Files.writeString(sourceFile.toPath(), submission.getSourceCode());

            ExecutionResult compileResult = dockerRunner.runContainer(
                    properties.getJavaImage(),
                    workspaceDir,
                    "javac Main.java",
                    null,
                    properties.getCompileTimeoutMs(),
                    properties.getContainerMemoryMb(),
                    properties.getContainerCpus(),
                    properties.getMaxOutputBytes()
            );

            if (compileResult.timedOut()) {
                return new JudgeResult(
                        Verdict.COMPILATION_ERROR, 0, 0, 0, testCases.size(),
                        "Compilation timed out after " + properties.getCompileTimeoutMs() + "ms."
                );
            }

            if (compileResult.exitCode() != 0) {
                String sanitizedStderr = sanitizeErrorMessage(compileResult.stderr(), workspaceDir.getAbsolutePath());
                return new JudgeResult(
                        Verdict.COMPILATION_ERROR, 0, 0, 0, testCases.size(),
                        sanitizedStderr
                );
            }

            List<TestCase> sortedTestCases = testCases.stream()
                    .sorted(Comparator.comparingInt(TestCase::getOrderIndex))
                    .toList();

            int totalCases = sortedTestCases.size();
            if (totalCases == 0) {
                return new JudgeResult(
                        Verdict.INTERNAL_ERROR, 0, 0, 0, 0,
                        "Judge Configuration Error: No test cases found for this problem."
                );
            }

            int passedCases = 0;
            long maxExecutionTimeMs = 0;

            int timeLimitMs = submission.getProblem() != null ? submission.getProblem().getTimeLimitMs() : 2000;
            int memoryLimitMb = submission.getProblem() != null ? submission.getProblem().getMemoryLimitMb() : properties.getContainerMemoryMb();

            for (TestCase tc : sortedTestCases) {
                ExecutionResult execResult = dockerRunner.runContainer(
                        properties.getJavaImage(),
                        workspaceDir,
                        "java Main",
                        tc.getInput(),
                        timeLimitMs,
                        memoryLimitMb,
                        properties.getContainerCpus(),
                        properties.getMaxOutputBytes()
                );

                maxExecutionTimeMs = Math.max(maxExecutionTimeMs, execResult.executionTimeMs());

                if (execResult.timedOut()) {
                    return new JudgeResult(
                            Verdict.TIME_LIMIT_EXCEEDED,
                            (int) maxExecutionTimeMs,
                            0,
                            passedCases,
                            totalCases,
                            "Time limit exceeded (" + timeLimitMs + "ms)."
                    );
                }

                if (execResult.outputLimitExceeded()) {
                    return new JudgeResult(
                            Verdict.RUNTIME_ERROR,
                            (int) maxExecutionTimeMs,
                            0,
                            passedCases,
                            totalCases,
                            "Output limit exceeded."
                    );
                }

                if (execResult.memoryExceeded()) {
                    return new JudgeResult(
                            Verdict.MEMORY_LIMIT_EXCEEDED,
                            (int) maxExecutionTimeMs,
                            0,
                            passedCases,
                            totalCases,
                            "Memory limit exceeded."
                    );
                }

                if (execResult.exitCode() != 0) {
                    return new JudgeResult(
                            Verdict.RUNTIME_ERROR,
                            (int) maxExecutionTimeMs,
                            0,
                            passedCases,
                            totalCases,
                            "Runtime error (exit code " + execResult.exitCode() + ")."
                    );
                }

                if (!outputComparator.isEqual(tc.getExpectedOutput(), execResult.stdout())) {
                    String formattedError = "Failed Test Case:\n" + tc.getInput() + 
                                            "\n\nExpected Output:\n" + tc.getExpectedOutput() + 
                                            "\n\nActual Output:\n" + (execResult.stdout() != null ? execResult.stdout() : "");
                    return new JudgeResult(
                            Verdict.WRONG_ANSWER,
                            (int) maxExecutionTimeMs,
                            0,
                            passedCases,
                            totalCases,
                            formattedError
                    );
                }

                passedCases++;
            }

            return new JudgeResult(
                    Verdict.ACCEPTED,
                    (int) maxExecutionTimeMs,
                    0,
                    passedCases,
                    totalCases,
                    null
            );

        } catch (Exception e) {
            log.error("Execution error in JavaDockerExecutor: {}", e.getMessage(), e);
            return new JudgeResult(
                    Verdict.INTERNAL_ERROR, 0, 0, 0, testCases.size(),
                    "Internal error during code evaluation."
            );
        } finally {
            deleteDirectoryQuietly(workspaceDir);
        }
    }

    private String sanitizeErrorMessage(String stderr, String workspacePath) {
        if (stderr == null) return "Compilation error.";
        String sanitized = stderr.replace(workspacePath, "")
                .replace("/app/", "")
                .trim();
        if (sanitized.length() > 2000) {
            sanitized = sanitized.substring(0, 2000) + "\n...[truncated]";
        }
        return sanitized.isEmpty() ? "Compilation error." : sanitized;
    }

    private void deleteDirectoryQuietly(File dir) {
        if (dir == null || !dir.exists()) return;
        try {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) deleteDirectoryQuietly(f);
                    else f.delete();
                }
            }
            dir.delete();
        } catch (Exception e) {
            log.warn("Failed to delete workspace directory: {}", dir.getAbsolutePath(), e);
        }
    }
}
