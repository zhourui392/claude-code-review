package com.example.gitreview.domain.workflow.model;

/**
 * Workflow status enumeration.
 *
 * @author zhourui(V33215020)
 * @since 2025/10/05
 */
public enum WorkflowStatus {
    DRAFT,
    SPEC_GENERATING,
    SPEC_GENERATED,
    TECH_DESIGN_GENERATING,
    TECH_DESIGN_GENERATED,
    TECH_DESIGN_APPROVED,
    TASK_LIST_GENERATING,
    TASK_LIST_GENERATED,
    CODE_GENERATING,
    COMPLETED,
    FAILED,
    CANCELLED
}
