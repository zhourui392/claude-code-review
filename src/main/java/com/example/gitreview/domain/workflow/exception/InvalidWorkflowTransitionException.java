package com.example.gitreview.domain.workflow.exception;

import com.example.gitreview.domain.workflow.model.WorkflowStatus;

/**
 * Exception thrown when workflow transition is invalid.
 *
 * @author zhourui(V33215020)
 * @since 2025/10/05
 */
public class InvalidWorkflowTransitionException extends RuntimeException {

    private final WorkflowStatus from;
    private final WorkflowStatus to;

    public InvalidWorkflowTransitionException(WorkflowStatus currentStatus, WorkflowStatus targetStatus) {
        super(String.format("Invalid workflow transition from %s to %s", currentStatus, targetStatus));
        this.from = currentStatus;
        this.to = targetStatus;
    }

    public WorkflowStatus getFrom() {
        return from;
    }

    public WorkflowStatus getTo() {
        return to;
    }
}
