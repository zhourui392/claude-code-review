package com.example.gitreview.domain.workflow.exception;

/**
 * 工作流未找到异常
 *
 * @author zhourui(V33215020)
 * @since 2025/10/04
 */
public class WorkflowNotFoundException extends RuntimeException {

    private final Long workflowId;

    public WorkflowNotFoundException(Long workflowId) {
        super(String.format("工作流不存在，ID: %d", workflowId));
        this.workflowId = workflowId;
    }

    public Long getWorkflowId() {
        return workflowId;
    }
}
