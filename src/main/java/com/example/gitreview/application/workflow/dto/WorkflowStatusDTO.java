package com.example.gitreview.application.workflow.dto;

import java.time.LocalDateTime;

/**
 * Workflow status DTO.
 *
 * @author zhourui(V33215020)
 * @since 2025/10/05
 */
public class WorkflowStatusDTO {
    private Long id;
    private String name;
    private String status;
    private int progress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public WorkflowStatusDTO() {}

    public WorkflowStatusDTO(Long id, String name, String status, int progress, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.progress = progress;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
