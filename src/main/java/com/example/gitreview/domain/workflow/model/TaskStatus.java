package com.example.gitreview.domain.workflow.model;

/**
 * 任务状态枚举
 *
 * @author zhourui(V33215020)
 * @since 2025/10/04
 */
public enum TaskStatus {

    PENDING("待执行"),
    IN_PROGRESS("执行中"),
    COMPLETED("已完成"),
    FAILED("失败"),
    SKIPPED("跳过");

    private final String description;

    TaskStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
