package com.apricorn.judge.runner;

import java.io.File;

public interface DockerRunner {
    boolean isDockerAvailable();
    ExecutionResult runContainer(
            String image,
            File workspaceDir,
            String command,
            String stdinInput,
            long timeoutMs,
            int memoryMb,
            double cpus,
            long maxOutputBytes
    );
}
