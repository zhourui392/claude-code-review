package com.example.gitreview.domain.shared.exception;

/**
 * 资源未找到异常 - 领域层版本
 */
public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResourceNotFoundException(String resourceType, Object resourceId) {
        super(String.format("%s with id '%s' not found", resourceType, resourceId));
    }
}