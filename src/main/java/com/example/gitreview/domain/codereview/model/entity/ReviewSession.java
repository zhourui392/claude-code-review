package com.example.gitreview.domain.codereview.model.entity;

import com.example.gitreview.domain.codereview.model.valueobject.CodeDiff;
import com.example.gitreview.domain.codereview.model.valueobject.ReviewResult;
import com.example.gitreview.domain.codereview.model.valueobject.ReviewStrategy;
import com.example.gitreview.domain.shared.exception.BusinessRuleException;
import com.example.gitreview.domain.shared.exception.ValidationException;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * ReviewSession实体
 * 代表一次具体的审查会话
 */
public class ReviewSession {

    private String sessionId;
    private Long codeReviewId;
    private ReviewStrategy strategy;
    private CodeDiff codeDiff;
    private ReviewResult result;
    private SessionStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String errorMessage;
    private int retryCount;

    public enum SessionStatus {
        CREATED,      // 已创建
        RUNNING,      // 运行中
        COMPLETED,    // 已完成
        FAILED,       // 失败
        CANCELLED     // 已取消
    }

    // 构造函数
    protected ReviewSession() {
        // JPA需要的默认构造函数
    }

    public ReviewSession(Long codeReviewId, ReviewStrategy strategy, CodeDiff codeDiff) {
        this(UUID.randomUUID().toString(), codeReviewId, strategy, codeDiff);
    }

    public ReviewSession(String sessionId, Long codeReviewId, ReviewStrategy strategy, CodeDiff codeDiff) {
        this.sessionId = validateSessionId(sessionId);
        this.codeReviewId = Objects.requireNonNull(codeReviewId, "Code review ID cannot be null");
        this.strategy = Objects.requireNonNull(strategy, "Review strategy cannot be null");
        this.codeDiff = Objects.requireNonNull(codeDiff, "Code diff cannot be null");
        this.status = SessionStatus.CREATED;
        this.startTime = LocalDateTime.now();
        this.retryCount = 0;
    }

    // 业务方法

    /**
     * 开始审查会话
     */
    public void start() {
        if (status != SessionStatus.CREATED) {
            throw new BusinessRuleException("Session can only be started from CREATED status, current: " + status);
        }
        this.status = SessionStatus.RUNNING;
        this.startTime = LocalDateTime.now();
    }

    /**
     * 完成审查会话
     * @param result 审查结果
     */
    public void complete(ReviewResult result) {
        if (status != SessionStatus.RUNNING) {
            throw new BusinessRuleException("Session can only be completed from RUNNING status, current: " + status);
        }

        this.result = Objects.requireNonNull(result, "Review result cannot be null");
        this.status = SessionStatus.COMPLETED;
        this.endTime = LocalDateTime.now();
        this.errorMessage = null;
    }

    /**
     * 标记会话失败
     * @param errorMessage 错误信息
     */
    public void fail(String errorMessage) {
        if (status == SessionStatus.COMPLETED) {
            throw new BusinessRuleException("Cannot fail a completed session");
        }

        this.status = SessionStatus.FAILED;
        this.endTime = LocalDateTime.now();
        this.errorMessage = validateErrorMessage(errorMessage);
    }

    /**
     * 取消审查会话
     * @param reason 取消原因
     */
    public void cancel(String reason) {
        if (status == SessionStatus.COMPLETED) {
            throw new BusinessRuleException("Cannot cancel a completed session");
        }

        this.status = SessionStatus.CANCELLED;
        this.endTime = LocalDateTime.now();
        this.errorMessage = reason;
    }

    /**
     * 重试审查会话
     */
    public void retry() {
        if (status != SessionStatus.FAILED) {
            throw new BusinessRuleException("Can only retry failed sessions, current: " + status);
        }

        if (retryCount >= getMaxRetries()) {
            throw new BusinessRuleException("Maximum retry count exceeded: " + retryCount);
        }

        this.status = SessionStatus.CREATED;
        this.result = null;
        this.endTime = null;
        this.errorMessage = null;
        this.retryCount++;
    }

    /**
     * 获取执行时长（毫秒）
     * @return 执行时长，如果未结束返回-1
     */
    public long getExecutionTimeMs() {
        if (startTime == null) {
            return -1;
        }

        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        return java.time.Duration.between(startTime, end).toMillis();
    }

    /**
     * 检查是否已完成
     * @return 是否已完成
     */
    public boolean isCompleted() {
        return status == SessionStatus.COMPLETED;
    }

    /**
     * 检查是否可以重试
     * @return 是否可以重试
     */
    public boolean canRetry() {
        return status == SessionStatus.FAILED && retryCount < getMaxRetries();
    }

    /**
     * 检查是否正在运行
     * @return 是否正在运行
     */
    public boolean isRunning() {
        return status == SessionStatus.RUNNING;
    }

    /**
     * 获取会话摘要
     * @return 会话摘要
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Session ").append(sessionId.substring(0, 8)).append("... ");
        summary.append("Status: ").append(status);

        if (isCompleted() && result != null) {
            summary.append(", Quality: ").append(result.getQualityScore());
        }

        if (status == SessionStatus.FAILED && errorMessage != null) {
            summary.append(", Error: ").append(errorMessage.substring(0, Math.min(50, errorMessage.length())));
        }

        return summary.toString();
    }

    /**
     * 获取会话状态描述
     * @return 状态描述
     */
    public String getStatusDescription() {
        switch (status) {
            case CREATED:
                return "等待开始";
            case RUNNING:
                return "审查中";
            case COMPLETED:
                return "已完成";
            case FAILED:
                return "失败" + (retryCount > 0 ? " (重试" + retryCount + "次)" : "");
            case CANCELLED:
                return "已取消";
            default:
                return "未知状态";
        }
    }

    /**
     * 获取最大重试次数
     * @return 最大重试次数
     */
    private int getMaxRetries() {
        return strategy.getMaxRetries();
    }

    // 私有验证方法
    private String validateSessionId(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new ValidationException("Session ID cannot be null or empty");
        }
        return sessionId.trim();
    }

    private String validateErrorMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.trim().isEmpty()) {
            return "Unknown error";
        }
        return errorMessage.trim();
    }

    // Getters
    public String getSessionId() {
        return sessionId;
    }

    public Long getCodeReviewId() {
        return codeReviewId;
    }

    public ReviewStrategy getStrategy() {
        return strategy;
    }

    public CodeDiff getCodeDiff() {
        return codeDiff;
    }

    public ReviewResult getResult() {
        return result;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getRetryCount() {
        return retryCount;
    }

    // 用于持久化的setter（仅限基础设施层使用）
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReviewSession that = (ReviewSession) o;
        return Objects.equals(sessionId, that.sessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId);
    }

    @Override
    public String toString() {
        return "ReviewSession{" +
                "sessionId='" + sessionId + '\'' +
                ", codeReviewId=" + codeReviewId +
                ", status=" + status +
                ", strategy=" + strategy.getMode() +
                ", executionTime=" + getExecutionTimeMs() + "ms" +
                '}';
    }
}