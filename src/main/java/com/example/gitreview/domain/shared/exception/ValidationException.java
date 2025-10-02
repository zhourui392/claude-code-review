package com.example.gitreview.domain.shared.exception;

/**
 * 验证异常
 * 当领域对象验证失败时抛出
 */
public class ValidationException extends DomainException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}