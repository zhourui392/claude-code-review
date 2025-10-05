package com.example.gitreview.infrastructure.claude;

import java.util.List;

/**
 * Claude Code 执行结果
 *
 * @author zhourui(V33215020)
 * @since 2025/10/05
 */
public class ClaudeCodeResult {

    private final boolean success;
    private final String message;
    private final List<String> modifiedFiles;
    private final long executionTimeMs;

    private ClaudeCodeResult(boolean success, String message,
                             List<String> modifiedFiles, long executionTimeMs) {
        this.success = success;
        this.message = message;
        this.modifiedFiles = modifiedFiles;
        this.executionTimeMs = executionTimeMs;
    }

    public static ClaudeCodeResult success(String message, List<String> modifiedFiles, long executionTimeMs) {
        return new ClaudeCodeResult(true, message, modifiedFiles, executionTimeMs);
    }

    public static ClaudeCodeResult failure(String message, long executionTimeMs) {
        return new ClaudeCodeResult(false, message, List.of(), executionTimeMs);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public List<String> getModifiedFiles() {
        return modifiedFiles;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }
}
