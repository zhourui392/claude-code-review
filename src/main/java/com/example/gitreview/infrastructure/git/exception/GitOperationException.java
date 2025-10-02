package com.example.gitreview.infrastructure.git.exception;

/**
 * Git操作异常
 * 封装Git相关操作的异常信息
 */
public class GitOperationException extends RuntimeException {

    public GitOperationException(String message) {
        super(message);
    }

    public GitOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public GitOperationException(Throwable cause) {
        super(cause);
    }
}