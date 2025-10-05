package com.example.gitreview.exception;

import com.example.gitreview.domain.shared.exception.BusinessRuleException;
import com.example.gitreview.domain.shared.exception.ResourceNotFoundException;
import com.example.gitreview.domain.shared.exception.ValidationException;
import com.example.gitreview.domain.workflow.exception.InvalidWorkflowTransitionException;
import com.example.gitreview.domain.workflow.exception.WorkflowNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 统一处理应用程序中的异常
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理静态资源未找到异常（如 favicon.ico）
     * 不记录日志，直接返回 204 No Content
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResourceFoundException(NoResourceFoundException e) {
        // 对 favicon.ico 等静态资源的404请求不记录日志，避免日志污染
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * 处理一般异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception e) {
        logger.error("Unexpected error occurred", e);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "ERROR");
        response.put("message", "An unexpected error occurred");
        response.put("error", e.getMessage());
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        logger.warn("Invalid argument provided: {}", e.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "BAD_REQUEST");
        response.put("message", "Invalid request parameters");
        response.put("error", e.getMessage());
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 处理业务规则异常
     */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessRuleException(BusinessRuleException e) {
        logger.warn("Business rule violation: {}", e.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "BUSINESS_RULE_VIOLATION");
        response.put("message", "Business rule violation");
        response.put("error", e.getMessage());
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 处理资源未找到异常
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(ResourceNotFoundException e) {
        logger.warn("Resource not found: {}", e.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "NOT_FOUND");
        response.put("message", "Requested resource not found");
        response.put("error", e.getMessage());
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * 处理验证异常
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(ValidationException e) {
        logger.warn("Validation failed: {}", e.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "VALIDATION_FAILED");
        response.put("message", "Request validation failed");
        response.put("error", e.getMessage());
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 处理工作流未找到异常
     */
    @ExceptionHandler(WorkflowNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleWorkflowNotFoundException(WorkflowNotFoundException e) {
        logger.warn("Workflow not found: workflowId={}", e.getWorkflowId());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "NOT_FOUND");
        response.put("message", "Workflow not found");
        response.put("error", e.getMessage());
        response.put("workflowId", e.getWorkflowId());
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * 处理非法工作流状态转换异常
     */
    @ExceptionHandler(InvalidWorkflowTransitionException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidWorkflowTransitionException(InvalidWorkflowTransitionException e) {
        logger.warn("Invalid workflow transition: from={} to={}", e.getFrom(), e.getTo());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "INVALID_TRANSITION");
        response.put("message", "Invalid workflow state transition");
        response.put("error", e.getMessage());
        response.put("fromStatus", e.getFrom().name());
        response.put("toStatus", e.getTo().name());
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}