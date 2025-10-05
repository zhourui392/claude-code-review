package com.example.gitreview.domain.workflow.model.valueobject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 技术方案值对象
 *
 * @author zhourui(V33215020)
 * @since 2025/10/04
 */
public class TechnicalDesign {

    private final String content;
    private final int version;
    private final boolean approved;
    private final LocalDateTime createdAt;
    private final LocalDateTime approvedAt;

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

    /**
     * 创建新版本技术方案
     *
     * @param newContent 新的技术方案内容
     * @return 新版本的技术方案
     */
    public TechnicalDesign createNewVersion(String newContent) {
        return new TechnicalDesign(newContent, this.version + 1, false, LocalDateTime.now(), null);
    }

    /**
     * 批准技术方案
     *
     * @return 已批准的技术方案
     */
    public TechnicalDesign approve() {
        return new TechnicalDesign(this.content, this.version, true, this.createdAt, LocalDateTime.now());
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TechnicalDesign that = (TechnicalDesign) o;
        return version == that.version && Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content, version);
    }
}
