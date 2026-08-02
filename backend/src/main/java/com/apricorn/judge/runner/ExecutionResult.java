package com.apricorn.judge.runner;

public record ExecutionResult(
        int exitCode,
        String stdout,
        String stderr,
        long executionTimeMs,
        boolean timedOut,
        boolean memoryExceeded,
        boolean outputLimitExceeded
) {}
