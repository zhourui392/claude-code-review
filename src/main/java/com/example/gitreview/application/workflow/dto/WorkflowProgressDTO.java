package com.example.gitreview.application.workflow.dto;

import java.util.List;

/**
 * Workflow progress DTO.
 *
 * @author zhourui(V33215020)
 * @since 2025/10/05
 */
public class WorkflowProgressDTO {
    private Long workflowId;
    private String status;
    private int progress;
    private String currentStage;
    private int completedTasksCount;
    private int totalTasksCount;

    public WorkflowProgressDTO() {}

    public WorkflowProgressDTO(Long workflowId, String status, int progress, String currentStage, int completedTasksCount, int totalTasksCount) {
        this.workflowId = workflowId;
        this.status = status;
        this.progress = progress;
        this.currentStage = currentStage;
        this.completedTasksCount = completedTasksCount;
        this.totalTasksCount = totalTasksCount;
    }

    public Long getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(Long workflowId) {
        this.workflowId = workflowId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public String getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(String currentStage) {
        this.currentStage = currentStage;
    }

    public int getCompletedTasksCount() {
        return completedTasksCount;
    }

    public void setCompletedTasksCount(int completedTasksCount) {
        this.completedTasksCount = completedTasksCount;
    }

    public int getTotalTasksCount() {
        return totalTasksCount;
    }

    public void setTotalTasksCount(int totalTasksCount) {
        this.totalTasksCount = totalTasksCount;
    }
}
