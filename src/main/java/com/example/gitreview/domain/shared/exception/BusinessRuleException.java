package com.example.gitreview.domain.shared.exception;

/**
 * 业务规则异常
 * 当违反业务规则时抛出
 */
public class BusinessRuleException extends DomainException {

    public BusinessRuleException(String message) {
        super(message);
    }

    public BusinessRuleException(String message, Throwable cause) {
        super(message, cause);
    }
}