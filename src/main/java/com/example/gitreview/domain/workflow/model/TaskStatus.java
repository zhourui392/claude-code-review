package com.example.gitreview.domain.workflow.model;

/**
 * Task status enumeration.
 *
 * @author zhourui(V33215020)
 * @since 2025/10/05
 */
public enum TaskStatus {
    /**
     * Task is pending execution
     */
    PENDING,

    /**
     * Task is currently in progress
     */
    IN_PROGRESS,

    /**
     * Task completed successfully
     */
    COMPLETED,

    /**
     * Task execution failed
     */
    FAILED
}
