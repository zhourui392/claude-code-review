package com.example.gitreview.domain.workflow.model.valueobject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Technical design value object.
 *
 * @author zhourui(V33215020)
 * @since 2025/10/05
 */
public class TechnicalDesign {
    private String content;
    private int version;
    private boolean approved;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;

    // 默认构造函数供Jackson使用
    public TechnicalDesign() {
    }

    @JsonCreator
    public TechnicalDesign(
            @JsonProperty("content") String content,
            @JsonProperty("version") int version,
            @JsonProperty("approved") boolean approved,
            @JsonProperty("createdAt") LocalDateTime createdAt,
            @JsonProperty("approvedAt") LocalDateTime approvedAt) {
        this.content = content;
        this.version = version;
        this.approved = approved;
        this.createdAt = createdAt;
        this.approvedAt = approvedAt;
    }

    public String getContent() {
        return content;
    }

    public int getVersion() {
        return version;
    }

    public boolean isApproved() {
        return approved;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public TechnicalDesign approve() {
        return new TechnicalDesign(this.content, this.version, true, this.createdAt, LocalDateTime.now());
    }

    public TechnicalDesign createNewVersion(String newContent) {
        return new TechnicalDesign(newContent, this.version + 1, false, LocalDateTime.now(), null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TechnicalDesign that = (TechnicalDesign) o;
        return version == that.version &&
                Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content, version);
    }
}
