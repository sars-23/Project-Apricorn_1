package com.apricorn.judge.runner;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DefaultDockerRunner implements DockerRunner {
    private static final Logger log = LoggerFactory.getLogger(DefaultDockerRunner.class);

    @Override
    public boolean isDockerAvailable() {
        try {
            Process process = new ProcessBuilder("docker", "info").start();
            boolean finished = process.waitFor(3, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            log.warn("Docker availability check failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public ExecutionResult runContainer(
            String image,
            File workspaceDir,
            String command,
            String stdinInput,
            long timeoutMs,
            int memoryMb,
            double cpus,
            long maxOutputBytes) {

        String absoluteWorkspace = workspaceDir.getAbsolutePath();

        List<String> dockerCmd = new ArrayList<>();
        dockerCmd.add("docker");
        dockerCmd.add("run");
        dockerCmd.add("--rm");
        dockerCmd.add("-i");
        dockerCmd.add("--network");
        dockerCmd.add("none");
        dockerCmd.add("--memory");
        dockerCmd.add(memoryMb + "m");
        dockerCmd.add("--cpus");
        dockerCmd.add(String.valueOf(cpus));
        dockerCmd.add("--pids-limit");
        dockerCmd.add("64");
        dockerCmd.add("--cap-drop");
        dockerCmd.add("ALL");
        dockerCmd.add("--security-opt");
        dockerCmd.add("no-new-privileges");
        dockerCmd.add("-v");
        dockerCmd.add(absoluteWorkspace + ":/app:rw");
        dockerCmd.add("-w");
        dockerCmd.add("/app");
        dockerCmd.add(image);
        dockerCmd.add("sh");
        dockerCmd.add("-c");
        dockerCmd.add(command);

        long startTime = System.currentTimeMillis();
        ProcessBuilder pb = new ProcessBuilder(dockerCmd);

        boolean timedOut = false;
        boolean outputLimitExceeded = false;
        int exitCode = -1;
        String stdoutStr = "";
        String stderrStr = "";

        try {
            Process process = pb.start();

            if (stdinInput != null && !stdinInput.isEmpty()) {
                try (OutputStream os = process.getOutputStream()) {
                    os.write(stdinInput.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                } catch (Exception ignored) {}
            } else {
                process.getOutputStream().close();
            }

            ByteArrayOutputStream stdoutBuffer = new ByteArrayOutputStream();
            ByteArrayOutputStream stderrBuffer = new ByteArrayOutputStream();

            Thread stdoutThread = new Thread(() -> streamOutput(process.getInputStream(), stdoutBuffer, maxOutputBytes));
            Thread stderrThread = new Thread(() -> streamOutput(process.getErrorStream(), stderrBuffer, maxOutputBytes));

            stdoutThread.start();
            stderrThread.start();

            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            long executionTimeMs = System.currentTimeMillis() - startTime;

            if (!finished) {
                timedOut = true;
                process.destroyForcibly();
            } else {
                exitCode = process.exitValue();
            }

            stdoutThread.join(1000);
            stderrThread.join(1000);

            if (stdoutBuffer.size() >= maxOutputBytes || stderrBuffer.size() >= maxOutputBytes) {
                outputLimitExceeded = true;
            }

            stdoutStr = stdoutBuffer.toString(StandardCharsets.UTF_8);
            stderrStr = stderrBuffer.toString(StandardCharsets.UTF_8);

            return new ExecutionResult(
                    exitCode,
                    stdoutStr,
                    stderrStr,
                    executionTimeMs,
                    timedOut,
                    false,
                    outputLimitExceeded
            );
        } catch (Exception e) {
            log.error("Container execution failed: {}", e.getMessage(), e);
            return new ExecutionResult(-1, "", e.getMessage(), 0, false, false, false);
        }
    }

    private void streamOutput(InputStream in, ByteArrayOutputStream out, long maxBytes) {
        byte[] buffer = new byte[1024];
        int bytesRead;
        try {
            while ((bytesRead = in.read(buffer)) != -1) {
                if (out.size() + bytesRead > maxBytes) {
                    int allowed = (int) (maxBytes - out.size());
                    if (allowed > 0) {
                        out.write(buffer, 0, allowed);
                    }
                    break;
                }
                out.write(buffer, 0, bytesRead);
            }
        } catch (Exception ignored) {}
    }
}
