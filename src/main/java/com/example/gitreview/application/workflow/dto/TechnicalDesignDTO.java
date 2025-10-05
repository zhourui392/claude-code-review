package com.example.gitreview.application.workflow.dto;

import java.time.LocalDateTime;

/**
 * Technical design DTO.
 *
 * @author zhourui(V33215020)
 * @since 2025/10/05
 */
public class TechnicalDesignDTO {
    private String content;
    private int version;
    private boolean approved;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;

    public TechnicalDesignDTO() {}

    public TechnicalDesignDTO(String content, int version, boolean approved, LocalDateTime createdAt, LocalDateTime approvedAt) {
        this.content = content;
        this.version = version;
        this.approved = approved;
        this.createdAt = createdAt;
        this.approvedAt = approvedAt;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }
}
