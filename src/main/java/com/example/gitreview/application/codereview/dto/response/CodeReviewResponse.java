package com.example.gitreview.application.codereview.dto.response;

import com.example.gitreview.domain.codereview.model.aggregate.CodeReview;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 代码审查响应DTO
 */
public class CodeReviewResponse {

    private Long id;
    private Long repositoryId;
    private String title;
    private String description;
    private String status;
    private String statusDescription;
    private int progress;
    private int qualityScore;
    private boolean hasCriticalIssues;
    private String summary;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String createdBy;

    // 审查策略信息
    private String strategyMode;
    private String strategyDescription;

    // 代码差异信息
    private String baseBranch;
    private String targetBranch;
    private Integer totalFiles;
    private Integer addedLines;
    private Integer deletedLines;

    // 审查结果信息
    private List<IssueInfo> issues;
    private List<SuggestionInfo> suggestions;

    // 嵌套类：问题信息
    public static class IssueInfo {
        private String filePath;
        private int lineNumber;
        private String severity;
        private String category;
        private String description;
        private String suggestion;

        // 构造函数
        public IssueInfo() {}

        public IssueInfo(String filePath, int lineNumber, String severity,
                        String category, String description, String suggestion) {
            this.filePath = filePath;
            this.lineNumber = lineNumber;
            this.severity = severity;
            this.category = category;
            this.description = description;
            this.suggestion = suggestion;
        }

        // Getters and Setters
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public int getLineNumber() { return lineNumber; }
        public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getSuggestion() { return suggestion; }
        public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    }

    // 嵌套类：建议信息
    public static class SuggestionInfo {
        private String category;
        private String description;
        private int priority;

        // 构造函数
        public SuggestionInfo() {}

        public SuggestionInfo(String category, String description, int priority) {
            this.category = category;
            this.description = description;
            this.priority = priority;
        }

        // Getters and Setters
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public int getPriority() { return priority; }
        public void setPriority(int priority) { this.priority = priority; }
    }

    // 默认构造函数
    public CodeReviewResponse() {}

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(Long repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusDescription() {
        return statusDescription;
    }

    public void setStatusDescription(String statusDescription) {
        this.statusDescription = statusDescription;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(int qualityScore) {
        this.qualityScore = qualityScore;
    }

    public boolean isHasCriticalIssues() {
        return hasCriticalIssues;
    }

    public void setHasCriticalIssues(boolean hasCriticalIssues) {
        this.hasCriticalIssues = hasCriticalIssues;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getStrategyMode() {
        return strategyMode;
    }

    public void setStrategyMode(String strategyMode) {
        this.strategyMode = strategyMode;
    }

    public String getStrategyDescription() {
        return strategyDescription;
    }

    public void setStrategyDescription(String strategyDescription) {
        this.strategyDescription = strategyDescription;
    }

    public String getBaseBranch() {
        return baseBranch;
    }

    public void setBaseBranch(String baseBranch) {
        this.baseBranch = baseBranch;
    }

    public String getTargetBranch() {
        return targetBranch;
    }

    public void setTargetBranch(String targetBranch) {
        this.targetBranch = targetBranch;
    }

    public Integer getTotalFiles() {
        return totalFiles;
    }

    public void setTotalFiles(Integer totalFiles) {
        this.totalFiles = totalFiles;
    }

    public Integer getAddedLines() {
        return addedLines;
    }

    public void setAddedLines(Integer addedLines) {
        this.addedLines = addedLines;
    }

    public Integer getDeletedLines() {
        return deletedLines;
    }

    public void setDeletedLines(Integer deletedLines) {
        this.deletedLines = deletedLines;
    }

    public List<IssueInfo> getIssues() {
        return issues;
    }

    public void setIssues(List<IssueInfo> issues) {
        this.issues = issues;
    }

    public List<SuggestionInfo> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<SuggestionInfo> suggestions) {
        this.suggestions = suggestions;
    }

    @Override
    public String toString() {
        return "CodeReviewResponse{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", status='" + status + '\'' +
                ", progress=" + progress +
                ", qualityScore=" + qualityScore +
                '}';
    }
}