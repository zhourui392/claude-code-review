package com.example.gitreview.domain.shared.exception;

/**
 * 领域异常基类
 * 所有领域相关异常的基类
 */
public abstract class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}