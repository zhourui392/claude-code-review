package com.example.gitreview.domain.workflow.model;

/**
 * 工作流状态枚举
 *
 * @author zhourui(V33215020)
 * @since 2025/10/04
 */
public enum WorkflowStatus {

    DRAFT("草稿"),
    SPEC_GENERATING("生成规格文档中"),
    SPEC_GENERATED("规格文档已生成"),
    TECH_DESIGN_GENERATING("生成技术方案中"),
    TECH_DESIGN_GENERATED("技术方案已生成"),
    TECH_DESIGN_APPROVED("技术方案已批准"),
    TASK_LIST_GENERATING("生成任务列表中"),
    TASK_LIST_GENERATED("任务列表已生成"),
    CODE_GENERATING("代码生成中"),
    COMPLETED("全部完成"),
    FAILED("失败"),
    CANCELLED("已取消");

    private final String description;

    WorkflowStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
