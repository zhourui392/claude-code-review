package com.example.gitreview.infrastructure.claude.exception;

/**
 * Claude服务异常
 * 封装Claude AI相关操作的异常信息
 */
public class ClaudeServiceException extends RuntimeException {

    public ClaudeServiceException(String message) {
        super(message);
    }

    public ClaudeServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClaudeServiceException(Throwable cause) {
        super(cause);
    }
}