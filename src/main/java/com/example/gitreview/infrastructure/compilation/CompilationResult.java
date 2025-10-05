package com.example.gitreview.infrastructure.compilation;

/**
 * 编译结果
 *
 * @author zhourui(V33215020)
 * @since 2025/10/05
 */
public class CompilationResult {

    private final boolean success;
    private final String message;
    private final String output;
    private final long executionTimeMs;

    private CompilationResult(boolean success, String message, String output, long executionTimeMs) {
        this.success = success;
        this.message = message;
        this.output = output;
        this.executionTimeMs = executionTimeMs;
    }

    public static CompilationResult success(String message, String output, long executionTimeMs) {
        return new CompilationResult(true, message, output, executionTimeMs);
    }

    public static CompilationResult failure(String message, String output, long executionTimeMs) {
        return new CompilationResult(false, message, output, executionTimeMs);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getOutput() {
        return output;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }
}
