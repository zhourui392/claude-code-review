package com.example.gitreview.domain.workflow.exception;

import com.example.gitreview.domain.workflow.model.WorkflowStatus;

/**
 * 非法工作流状态转换异常
 *
 * @author zhourui(V33215020)
 * @since 2025/10/04
 */
public class InvalidWorkflowTransitionException extends RuntimeException {

    private final WorkflowStatus from;
    private final WorkflowStatus to;

    public InvalidWorkflowTransitionException(WorkflowStatus from, WorkflowStatus to) {
        super(String.format("无法从状态 %s 转换到 %s", from.getDescription(), to.getDescription()));
        this.from = from;
        this.to = to;
    }

    public WorkflowStatus getFrom() {
        return from;
    }

    public WorkflowStatus getTo() {
        return to;
    }
}
