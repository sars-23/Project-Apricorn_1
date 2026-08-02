package com.apricorn.judge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "judge")
public class JudgeProperties {
    private String mode = "stub"; // "docker" or "stub"
    private String javaImage = "eclipse-temurin:21-jdk";
    private String workspaceRoot = System.getProperty("java.io.tmpdir") + "/apricorn-judge";
    private long maxOutputBytes = 1048576L; // 1 MB
    private long compileTimeoutMs = 10000L; // 10s
    private int containerMemoryMb = 256;
    private double containerCpus = 0.5;

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getJavaImage() { return javaImage; }
    public void setJavaImage(String javaImage) { this.javaImage = javaImage; }
    public String getWorkspaceRoot() { return workspaceRoot; }
    public void setWorkspaceRoot(String workspaceRoot) { this.workspaceRoot = workspaceRoot; }
    public long getMaxOutputBytes() { return maxOutputBytes; }
    public void setMaxOutputBytes(long maxOutputBytes) { this.maxOutputBytes = maxOutputBytes; }
    public long getCompileTimeoutMs() { return compileTimeoutMs; }
    public void setCompileTimeoutMs(long compileTimeoutMs) { this.compileTimeoutMs = compileTimeoutMs; }
    public int getContainerMemoryMb() { return containerMemoryMb; }
    public void setContainerMemoryMb(int containerMemoryMb) { this.containerMemoryMb = containerMemoryMb; }
    public double getContainerCpus() { return containerCpus; }
    public void setContainerCpus(double containerCpus) { this.containerCpus = containerCpus; }
}
