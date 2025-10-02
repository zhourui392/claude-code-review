package com.example.gitreview.infrastructure.claude;

/**
 * Claude查询响应
 * 统一的Claude查询响应格式
 */
public class ClaudeQueryResponse {

    private final boolean successful;
    private final String output;
    private final String error;
    private final long executionTimeMs;
    private final String command;

    private ClaudeQueryResponse(boolean successful, String output, String error,
                               long executionTimeMs, String command) {
        this.successful = successful;
        this.output = output;
        this.error = error;
        this.executionTimeMs = executionTimeMs;
        this.command = command;
    }

    /**
     * 创建成功响应
     */
    public static ClaudeQueryResponse success(String output, long executionTimeMs, String command) {
        return new ClaudeQueryResponse(true, output, null, executionTimeMs, command);
    }

    /**
     * 创建失败响应
     */
    public static ClaudeQueryResponse failure(int exitCode, String error, long executionTimeMs, String command) {
        return new ClaudeQueryResponse(false, null, error, executionTimeMs, command);
    }

    /**
     * 创建异常响应
     */
    public static ClaudeQueryResponse exception(Exception e, String command) {
        return new ClaudeQueryResponse(false, null, e.getMessage(), 0, command);
    }

    // Getters
    public boolean isSuccessful() {
        return successful;
    }

    public String getOutput() {
        return output;
    }

    public String getError() {
        return error;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public String getCommand() {
        return command;
    }

    @Override
    public String toString() {
        return "ClaudeQueryResponse{" +
                "successful=" + successful +
                ", output='" + (output != null ? output.substring(0, Math.min(100, output.length())) + "..." : "null") + '\'' +
                ", error='" + error + '\'' +
                ", executionTimeMs=" + executionTimeMs +
                ", command='" + command + '\'' +
                '}';
    }
}