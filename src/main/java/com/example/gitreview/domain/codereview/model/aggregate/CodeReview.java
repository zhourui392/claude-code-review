package com.example.gitreview.domain.codereview.model.aggregate;

import com.example.gitreview.domain.codereview.model.entity.ReviewSession;
import com.example.gitreview.domain.codereview.model.valueobject.CodeDiff;
import com.example.gitreview.domain.codereview.model.valueobject.ReviewResult;
import com.example.gitreview.domain.codereview.model.valueobject.ReviewStrategy;
import com.example.gitreview.domain.shared.exception.BusinessRuleException;
import com.example.gitreview.domain.shared.exception.ValidationException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * CodeReview聚合根
 * 代码审查的核心领域模型，管理审查会话和结果
 */
public class CodeReview {

    private Long id;
    private Long repositoryId;
    private String title;
    private String description;
    private ReviewStrategy strategy;
    private ReviewStatus status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String createdBy;

    // 聚合内的实体和值对象
    private final List<ReviewSession> sessions = new ArrayList<>();
    private CodeDiff codeDiff;
    private ReviewResult finalResult;

    public enum ReviewStatus {
        PENDING,      // 待审查
        IN_PROGRESS,  // 审查中
        COMPLETED,    // 已完成
        FAILED,       // 审查失败
        CANCELLED     // 已取消
    }

    // 构造函数
    protected CodeReview() {
        // JPA需要的默认构造函数
    }

    public CodeReview(Long repositoryId, String title, String description,
                     ReviewStrategy strategy, String createdBy) {
        this.repositoryId = Objects.requireNonNull(repositoryId, "Repository ID cannot be null");
        this.title = validateTitle(title);
        this.description = description;
        this.strategy = Objects.requireNonNull(strategy, "Review strategy cannot be null");
        this.createdBy = validateCreatedBy(createdBy);
        this.status = ReviewStatus.PENDING;
        this.createTime = LocalDateTime.now();
        this.updateTime = this.createTime;
    }

    // 业务方法

    /**
     * 开始代码审查
     * @param codeDiff 代码差异
     */
    public void startReview(CodeDiff codeDiff) {
        if (status != ReviewStatus.PENDING) {
            throw new BusinessRuleException("Cannot start review in current status: " + status);
        }

        this.codeDiff = Objects.requireNonNull(codeDiff, "Code diff cannot be null");
        this.status = ReviewStatus.IN_PROGRESS;
        this.updateTime = LocalDateTime.now();

        // 创建审查会话
        ReviewSession session = new ReviewSession(this.id, strategy, codeDiff);
        sessions.add(session);
    }

    /**
     * 添加审查会话
     * @param sessionId 会话ID
     * @param strategy 审查策略
     * @param codeDiff 代码差异
     * @return 创建的审查会话
     */
    public ReviewSession addSession(String sessionId, ReviewStrategy strategy, CodeDiff codeDiff) {
        if (status == ReviewStatus.COMPLETED || status == ReviewStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot add session to " + status.name().toLowerCase() + " review");
        }

        ReviewSession session = new ReviewSession(sessionId, this.id, strategy, codeDiff);
        sessions.add(session);
        this.updateTime = LocalDateTime.now();

        return session;
    }

    /**
     * 完成审查会话
     * @param sessionId 会话ID
     * @param result 审查结果
     */
    public void completeSession(String sessionId, ReviewResult result) {
        ReviewSession session = findSessionById(sessionId);
        if (session == null) {
            throw new ValidationException("Session not found: " + sessionId);
        }

        session.complete(result);
        this.updateTime = LocalDateTime.now();

        // 检查是否所有会话都已完成
        if (areAllSessionsCompleted()) {
            completeReview();
        }
    }

    /**
     * 完成代码审查
     */
    public void completeReview() {
        if (status != ReviewStatus.IN_PROGRESS) {
            throw new BusinessRuleException("Cannot complete review in current status: " + status);
        }

        if (sessions.isEmpty()) {
            throw new BusinessRuleException("Cannot complete review without any sessions");
        }

        // 合并所有会话的结果
        this.finalResult = mergeSessionResults();
        this.status = ReviewStatus.COMPLETED;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 取消代码审查
     * @param reason 取消原因
     */
    public void cancel(String reason) {
        if (status == ReviewStatus.COMPLETED) {
            throw new BusinessRuleException("Cannot cancel completed review");
        }

        this.status = ReviewStatus.CANCELLED;
        this.updateTime = LocalDateTime.now();

        // 取消所有进行中的会话
        sessions.forEach(session -> {
            if (!session.isCompleted()) {
                session.cancel(reason);
            }
        });
    }

    /**
     * 标记审查失败
     * @param errorMessage 错误信息
     */
    public void markAsFailed(String errorMessage) {
        this.status = ReviewStatus.FAILED;
        this.updateTime = LocalDateTime.now();

        // 如果有最终结果，更新错误信息
        if (this.finalResult != null) {
            this.finalResult = this.finalResult.withError(errorMessage);
        }
    }

    /**
     * 重新开始审查
     */
    public void restart() {
        if (status == ReviewStatus.COMPLETED) {
            throw new BusinessRuleException("Cannot restart completed review");
        }

        this.status = ReviewStatus.PENDING;
        this.updateTime = LocalDateTime.now();
        this.finalResult = null;

        // 清除所有会话
        sessions.clear();
    }

    /**
     * 更新审查信息
     * @param title 新标题
     * @param description 新描述
     */
    public void updateInfo(String title, String description) {
        if (status == ReviewStatus.COMPLETED) {
            throw new BusinessRuleException("Cannot update completed review");
        }

        this.title = validateTitle(title);
        this.description = description;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 获取审查进度
     * @return 进度百分比 (0-100)
     */
    public int getProgress() {
        if (sessions.isEmpty()) {
            return status == ReviewStatus.COMPLETED ? 100 : 0;
        }

        long completedSessions = sessions.stream()
                .mapToLong(session -> session.isCompleted() ? 1 : 0)
                .sum();

        return (int) ((completedSessions * 100) / sessions.size());
    }

    /**
     * 获取审查质量分数
     * @return 质量分数 (0-100)
     */
    public int getQualityScore() {
        if (finalResult == null) {
            return 0;
        }
        return finalResult.getQualityScore();
    }

    /**
     * 检查是否有严重问题
     * @return 是否有严重问题
     */
    public boolean hasCriticalIssues() {
        if (finalResult == null) {
            return false;
        }
        return finalResult.hasCriticalIssues();
    }

    /**
     * 获取审查摘要
     * @return 审查摘要
     */
    public String getSummary() {
        if (finalResult == null) {
            return "Review not completed";
        }
        return finalResult.getSummary();
    }

    /**
     * 检查审查是否可以重试
     * @return 是否可以重试
     */
    public boolean canRetry() {
        return status == ReviewStatus.FAILED ||
               (status == ReviewStatus.IN_PROGRESS && sessions.stream().anyMatch(s -> !s.isCompleted()));
    }

    // 私有辅助方法

    private String validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new ValidationException("Review title cannot be null or empty");
        }
        if (title.length() > 200) {
            throw new ValidationException("Review title cannot exceed 200 characters");
        }
        return title.trim();
    }

    private String validateCreatedBy(String createdBy) {
        if (createdBy == null || createdBy.trim().isEmpty()) {
            throw new ValidationException("CreatedBy cannot be null or empty");
        }
        return createdBy.trim();
    }

    private ReviewSession findSessionById(String sessionId) {
        return sessions.stream()
                .filter(session -> sessionId.equals(session.getSessionId()))
                .findFirst()
                .orElse(null);
    }

    private boolean areAllSessionsCompleted() {
        return !sessions.isEmpty() && sessions.stream().allMatch(ReviewSession::isCompleted);
    }

    private ReviewResult mergeSessionResults() {
        if (sessions.isEmpty()) {
            throw new IllegalStateException("No sessions to merge");
        }

        // 简单合并策略：取最后一个完成的会话结果
        return sessions.stream()
                .filter(ReviewSession::isCompleted)
                .map(ReviewSession::getResult)
                .filter(Objects::nonNull)
                .reduce((first, second) -> second)
                .orElseThrow(() -> new IllegalStateException("No completed session results found"));
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Long getRepositoryId() {
        return repositoryId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public ReviewStrategy getStrategy() {
        return strategy;
    }

    public ReviewStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public List<ReviewSession> getSessions() {
        return new ArrayList<>(sessions);
    }

    public CodeDiff getCodeDiff() {
        return codeDiff;
    }

    public ReviewResult getFinalResult() {
        return finalResult;
    }

    // 用于持久化的setter（仅限基础设施层使用）
    public void setId(Long id) {
        this.id = id;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CodeReview that = (CodeReview) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "CodeReview{" +
                "id=" + id +
                ", repositoryId=" + repositoryId +
                ", title='" + title + '\'' +
                ", status=" + status +
                ", progress=" + getProgress() + "%" +
                '}';
    }
}