package com.example.gitreview.domain.workflow.exception;

/**
 * Exception thrown when workflow is not found.
 *
 * @author zhourui(V33215020)
 * @since 2025/10/05
 */
public class WorkflowNotFoundException extends RuntimeException {

    private final Long workflowId;

    public WorkflowNotFoundException(Long workflowId) {
        super("Workflow not found: " + workflowId);
        this.workflowId = workflowId;
    }

    public Long getWorkflowId() {
        return workflowId;
    }
}
