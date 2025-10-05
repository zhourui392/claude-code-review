package com.example.gitreview.application.workflow.dto;

/**
 * 工作流进度响应DTO
 *
 * @author zhourui(V33215020)
 * @since 2025/10/04
 */
public class WorkflowProgressDTO {

    private Long workflowId;
    private String status;
    private int progress;
    private String currentStage;
    private int completedTasks;
    private int totalTasks;

    public WorkflowProgressDTO() {
    }

    public WorkflowProgressDTO(Long workflowId, String status, int progress, String currentStage,
                               int completedTasks, int totalTasks) {
        this.workflowId = workflowId;
        this.status = status;
        this.progress = progress;
        this.currentStage = currentStage;
        this.completedTasks = completedTasks;
        this.totalTasks = totalTasks;
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

    public int getCompletedTasks() {
        return completedTasks;
    }

    public void setCompletedTasks(int completedTasks) {
        this.completedTasks = completedTasks;
    }

    public int getTotalTasks() {
        return totalTasks;
    }

    public void setTotalTasks(int totalTasks) {
        this.totalTasks = totalTasks;
    }
}
