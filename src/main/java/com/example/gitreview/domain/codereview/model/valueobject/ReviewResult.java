package com.example.gitreview.domain.codereview.model.valueobject;

import com.example.gitreview.domain.shared.exception.ValidationException;

import java.time.LocalDateTime;
import java.util.*;

/**
 * ReviewResult值对象
 * 表示代码审查结果的领域概念
 */
public class ReviewResult {

    private final String summary;
    private final String detailedReport;
    private final int qualityScore;
    private final List<Issue> issues;
    private final List<Suggestion> suggestions;
    private final ReviewMetrics metrics;
    private final LocalDateTime createTime;

    /**
     * 问题严重级别
     */
    public enum IssueSeverity {
        CRITICAL,    // 严重
        MAJOR,       // 重要
        MINOR,       // 次要
        INFO         // 信息
    }

    /**
     * 问题优先级（P0-P3）
     */
    public enum IssuePriority {
        P0("P0", "阻断性", "必须立即修复，影响核心功能或安全", "🔴", 1),
        P1("P1", "严重", "需在下次发布前修复", "🟠", 2),
        P2("P2", "一般", "建议修复，不影响发布", "🟡", 3),
        P3("P3", "建议", "优化建议，可延后处理", "⚪", 4);

        private final String code;
        private final String displayName;
        private final String description;
        private final String emoji;
        private final int level;

        IssuePriority(String code, String displayName, String description, String emoji, int level) {
            this.code = code;
            this.displayName = displayName;
            this.description = description;
            this.emoji = emoji;
            this.level = level;
        }

        public String getCode() { return code; }
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public String getEmoji() { return emoji; }
        public int getLevel() { return level; }

        public boolean isBlocking() {
            return this == P0;
        }

        public boolean isCritical() {
            return this == P0 || this == P1;
        }

        public static IssuePriority fromCode(String code) {
            for (IssuePriority priority : values()) {
                if (priority.code.equalsIgnoreCase(code)) {
                    return priority;
                }
            }
            throw new ValidationException("Unknown priority code: " + code);
        }

        @Override
        public String toString() {
            return emoji + " " + code + " - " + displayName;
        }
    }

    /**
     * 修复建议
     */
    public static class FixSuggestion {
        private final String rootCause;
        private final String fixApproach;
        private final String codeExample;
        private final String testStrategy;
        private final int estimatedMinutes;
        private final List<String> references;

        public FixSuggestion(String rootCause, String fixApproach, String codeExample,
                           String testStrategy, int estimatedMinutes, List<String> references) {
            this.rootCause = rootCause;
            this.fixApproach = fixApproach;
            this.codeExample = codeExample;
            this.testStrategy = testStrategy;
            this.estimatedMinutes = Math.max(0, estimatedMinutes);
            this.references = references != null ? Collections.unmodifiableList(new ArrayList<>(references)) : Collections.emptyList();
        }

        public String getRootCause() { return rootCause; }
        public String getFixApproach() { return fixApproach; }
        public String getCodeExample() { return codeExample; }
        public String getTestStrategy() { return testStrategy; }
        public int getEstimatedMinutes() { return estimatedMinutes; }
        public List<String> getReferences() { return references; }

        @Override
        public String toString() {
            return "FixSuggestion{" +
                   "rootCause='" + rootCause + '\'' +
                   ", estimatedMinutes=" + estimatedMinutes +
                   '}';
        }
    }

    /**
     * 审查问题
     */
    public static class Issue {
        private final String filePath;
        private final int lineNumber;
        private final IssueSeverity severity;
        private final IssuePriority priority;
        private final String category;
        private final String description;
        private final String codeSnippet;
        private final String impact;
        private final FixSuggestion fixSuggestion;

        // 保留原有构造函数用于向后兼容
        @Deprecated
        public Issue(String filePath, int lineNumber, IssueSeverity severity,
                    String category, String description, String suggestion) {
            this(filePath, lineNumber, severity, null, category, description, null, null,
                 suggestion != null ? new FixSuggestion(null, suggestion, null, null, 0, null) : null);
        }

        // 新的完整构造函数
        public Issue(String filePath, int lineNumber, IssueSeverity severity, IssuePriority priority,
                    String category, String description, String codeSnippet, String impact,
                    FixSuggestion fixSuggestion) {
            this.filePath = validateFilePath(filePath);
            this.lineNumber = Math.max(0, lineNumber);
            this.severity = Objects.requireNonNull(severity, "Severity cannot be null");
            this.priority = priority; // 可以为空，后续通过领域服务计算
            this.category = validateCategory(category);
            this.description = validateDescription(description);
            this.codeSnippet = codeSnippet; // 可以为空
            this.impact = impact; // 可以为空
            this.fixSuggestion = fixSuggestion; // 可以为空
        }

        private String validateFilePath(String filePath) {
            if (filePath == null || filePath.trim().isEmpty()) {
                throw new ValidationException("File path cannot be null or empty");
            }
            return filePath.trim();
        }

        private String validateCategory(String category) {
            if (category == null || category.trim().isEmpty()) {
                throw new ValidationException("Category cannot be null or empty");
            }
            return category.trim();
        }

        private String validateDescription(String description) {
            if (description == null || description.trim().isEmpty()) {
                throw new ValidationException("Description cannot be null or empty");
            }
            return description.trim();
        }

        public String getFilePath() { return filePath; }
        public int getLineNumber() { return lineNumber; }
        public IssueSeverity getSeverity() { return severity; }
        public IssuePriority getPriority() { return priority; }
        public String getCategory() { return category; }
        public String getDescription() { return description; }
        public String getCodeSnippet() { return codeSnippet; }
        public String getImpact() { return impact; }
        public FixSuggestion getFixSuggestion() { return fixSuggestion; }

        // 向后兼容的方法
        @Deprecated
        public String getSuggestion() {
            return fixSuggestion != null ? fixSuggestion.getFixApproach() : null;
        }

        public boolean isCritical() {
            return severity == IssueSeverity.CRITICAL;
        }

        public boolean isMajor() {
            return severity == IssueSeverity.MAJOR || severity == IssueSeverity.CRITICAL;
        }

        public boolean isBlocking() {
            return priority != null && priority.isBlocking();
        }

        public boolean isHighPriority() {
            return priority != null && priority.isCritical();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (priority != null) {
                sb.append(priority.getEmoji()).append(" ");
            }
            sb.append("[").append(severity).append("] ");
            sb.append(filePath).append(":").append(lineNumber);
            sb.append(" - ").append(category).append(": ").append(description);
            return sb.toString();
        }
    }

    /**
     * 审查建议
     */
    public static class Suggestion {
        private final String category;
        private final String description;
        private final int priority; // 1-10, 10最高

        public Suggestion(String category, String description, int priority) {
            this.category = validateCategory(category);
            this.description = validateDescription(description);
            this.priority = Math.max(1, Math.min(10, priority));
        }

        private String validateCategory(String category) {
            if (category == null || category.trim().isEmpty()) {
                throw new ValidationException("Category cannot be null or empty");
            }
            return category.trim();
        }

        private String validateDescription(String description) {
            if (description == null || description.trim().isEmpty()) {
                throw new ValidationException("Description cannot be null or empty");
            }
            return description.trim();
        }

        public String getCategory() { return category; }
        public String getDescription() { return description; }
        public int getPriority() { return priority; }

        public boolean isHighPriority() {
            return priority >= 8;
        }

        @Override
        public String toString() {
            return String.format("[P%d] %s: %s", priority, category, description);
        }
    }

    /**
     * 审查指标
     */
    public static class ReviewMetrics {
        private final int totalIssues;
        private final int criticalIssues;
        private final int majorIssues;
        private final int minorIssues;
        private final int p0Issues;
        private final int p1Issues;
        private final int p2Issues;
        private final int p3Issues;
        private final int totalSuggestions;
        private final int highPrioritySuggestions;
        private final double codeQualityIndex;

        public ReviewMetrics(List<Issue> issues, List<Suggestion> suggestions) {
            this.totalIssues = issues.size();
            this.criticalIssues = (int) issues.stream().filter(Issue::isCritical).count();
            this.majorIssues = (int) issues.stream().filter(i -> i.getSeverity() == IssueSeverity.MAJOR).count();
            this.minorIssues = (int) issues.stream().filter(i -> i.getSeverity() == IssueSeverity.MINOR).count();

            // P0-P3 统计
            this.p0Issues = (int) issues.stream().filter(i -> i.getPriority() == IssuePriority.P0).count();
            this.p1Issues = (int) issues.stream().filter(i -> i.getPriority() == IssuePriority.P1).count();
            this.p2Issues = (int) issues.stream().filter(i -> i.getPriority() == IssuePriority.P2).count();
            this.p3Issues = (int) issues.stream().filter(i -> i.getPriority() == IssuePriority.P3).count();

            this.totalSuggestions = suggestions.size();
            this.highPrioritySuggestions = (int) suggestions.stream().filter(Suggestion::isHighPriority).count();
            this.codeQualityIndex = calculateQualityIndex();
        }

        private double calculateQualityIndex() {
            // 基于问题严重程度和优先级计算质量指数
            double deduction = p0Issues * 4.0 + p1Issues * 3.0 + p2Issues * 1.5 + p3Issues * 0.5;
            return Math.max(0.0, Math.min(10.0, 10.0 - deduction * 0.1));
        }

        public int getTotalIssues() { return totalIssues; }
        public int getCriticalIssues() { return criticalIssues; }
        public int getMajorIssues() { return majorIssues; }
        public int getMinorIssues() { return minorIssues; }
        public int getP0Issues() { return p0Issues; }
        public int getP1Issues() { return p1Issues; }
        public int getP2Issues() { return p2Issues; }
        public int getP3Issues() { return p3Issues; }
        public int getTotalSuggestions() { return totalSuggestions; }
        public int getHighPrioritySuggestions() { return highPrioritySuggestions; }
        public double getCodeQualityIndex() { return codeQualityIndex; }

        @Override
        public String toString() {
            return String.format("Issues: %d (P0: %d, P1: %d, P2: %d, P3: %d), Suggestions: %d, Quality: %.1f",
                               totalIssues, p0Issues, p1Issues, p2Issues, p3Issues, totalSuggestions, codeQualityIndex);
        }
    }

    // 构造函数
    public ReviewResult(String summary, String detailedReport, int qualityScore,
                       List<Issue> issues, List<Suggestion> suggestions) {
        this.summary = validateSummary(summary);
        this.detailedReport = validateDetailedReport(detailedReport);
        this.qualityScore = validateQualityScore(qualityScore);
        this.issues = Collections.unmodifiableList(new ArrayList<>(
            Objects.requireNonNull(issues, "Issues cannot be null")));
        this.suggestions = Collections.unmodifiableList(new ArrayList<>(
            Objects.requireNonNull(suggestions, "Suggestions cannot be null")));
        this.metrics = new ReviewMetrics(this.issues, this.suggestions);
        this.createTime = LocalDateTime.now();
    }

    // 静态工厂方法
    public static ReviewResult success(String summary, String detailedReport, int qualityScore) {
        return new ReviewResult(summary, detailedReport, qualityScore,
                              Collections.emptyList(), Collections.emptyList());
    }

    public static ReviewResult withIssues(String summary, String detailedReport, int qualityScore,
                                         List<Issue> issues) {
        return new ReviewResult(summary, detailedReport, qualityScore,
                              issues, Collections.emptyList());
    }

    public static ReviewResult complete(String summary, String detailedReport, int qualityScore,
                                      List<Issue> issues, List<Suggestion> suggestions) {
        return new ReviewResult(summary, detailedReport, qualityScore, issues, suggestions);
    }

    // 业务方法

    /**
     * 检查是否有严重问题
     * @return 是否有严重问题
     */
    public boolean hasCriticalIssues() {
        return metrics.getCriticalIssues() > 0;
    }

    /**
     * 检查是否有重要问题
     * @return 是否有重要问题
     */
    public boolean hasMajorIssues() {
        return metrics.getMajorIssues() > 0 || hasCriticalIssues();
    }

    /**
     * 检查审查质量是否合格
     * @return 是否合格（质量分数>=60）
     */
    public boolean isQualityAcceptable() {
        return qualityScore >= 60 && !hasCriticalIssues();
    }

    /**
     * 获取质量等级
     * @return 质量等级描述
     */
    public String getQualityGrade() {
        if (qualityScore >= 90) return "优秀";
        if (qualityScore >= 80) return "良好";
        if (qualityScore >= 70) return "中等";
        if (qualityScore >= 60) return "及格";
        return "不及格";
    }

    /**
     * 按严重程度获取问题列表
     * @param severity 严重程度
     * @return 问题列表
     */
    public List<Issue> getIssuesBySeverity(IssueSeverity severity) {
        return issues.stream()
                .filter(issue -> issue.getSeverity() == severity)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 按优先级获取问题列表
     * @param priority 优先级
     * @return 问题列表
     */
    public List<Issue> getIssuesByPriority(IssuePriority priority) {
        return issues.stream()
                .filter(issue -> issue.getPriority() == priority)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 获取阻断性问题（P0）
     * @return P0问题列表
     */
    public List<Issue> getBlockingIssues() {
        return getIssuesByPriority(IssuePriority.P0);
    }

    /**
     * 获取高优先级问题（P0 + P1）
     * @return 高优先级问题列表
     */
    public List<Issue> getHighPriorityIssues() {
        return issues.stream()
                .filter(Issue::isHighPriority)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 按优先级获取建议列表
     * @param minPriority 最小优先级
     * @return 建议列表
     */
    public List<Suggestion> getSuggestionsByPriority(int minPriority) {
        return suggestions.stream()
                .filter(suggestion -> suggestion.getPriority() >= minPriority)
                .sorted((s1, s2) -> Integer.compare(s2.getPriority(), s1.getPriority()))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 生成简要报告
     * @return 简要报告
     */
    public String generateBriefReport() {
        StringBuilder report = new StringBuilder();
        report.append("审查结果摘要\n");
        report.append("质量评分: ").append(qualityScore).append("/100 (").append(getQualityGrade()).append(")\n");
        report.append("指标统计: ").append(metrics.toString()).append("\n");

        if (hasCriticalIssues()) {
            report.append("⚠️ 发现严重问题，需要立即修复\n");
        }

        return report.toString();
    }

    /**
     * 添加错误信息
     * @param errorMessage 错误信息
     * @return 新的ReviewResult
     */
    public ReviewResult withError(String errorMessage) {
        String newSummary = summary + "\n\n错误信息: " + errorMessage;
        return new ReviewResult(newSummary, detailedReport, 0, issues, suggestions);
    }

    /**
     * 合并其他审查结果
     * @param other 其他审查结果
     * @return 合并后的结果
     */
    public ReviewResult merge(ReviewResult other) {
        String mergedSummary = summary + "\n\n" + other.summary;
        String mergedReport = detailedReport + "\n\n" + other.detailedReport;
        int mergedScore = (qualityScore + other.qualityScore) / 2;

        List<Issue> mergedIssues = new ArrayList<>(issues);
        mergedIssues.addAll(other.issues);

        List<Suggestion> mergedSuggestions = new ArrayList<>(suggestions);
        mergedSuggestions.addAll(other.suggestions);

        return new ReviewResult(mergedSummary, mergedReport, mergedScore, mergedIssues, mergedSuggestions);
    }

    // 私有验证方法
    private String validateSummary(String summary) {
        if (summary == null || summary.trim().isEmpty()) {
            throw new ValidationException("Summary cannot be null or empty");
        }
        return summary.trim();
    }

    private String validateDetailedReport(String detailedReport) {
        if (detailedReport == null) {
            return "";
        }
        return detailedReport;
    }

    private int validateQualityScore(int qualityScore) {
        if (qualityScore < 0 || qualityScore > 100) {
            throw new ValidationException("Quality score must be between 0 and 100");
        }
        return qualityScore;
    }

    // Getters
    public String getSummary() {
        return summary;
    }

    public String getDetailedReport() {
        return detailedReport;
    }

    public int getQualityScore() {
        return qualityScore;
    }

    public List<Issue> getIssues() {
        return issues;
    }

    public List<Suggestion> getSuggestions() {
        return suggestions;
    }

    public ReviewMetrics getMetrics() {
        return metrics;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReviewResult that = (ReviewResult) o;
        return qualityScore == that.qualityScore &&
               Objects.equals(summary, that.summary) &&
               Objects.equals(detailedReport, that.detailedReport) &&
               Objects.equals(issues, that.issues) &&
               Objects.equals(suggestions, that.suggestions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(summary, detailedReport, qualityScore, issues, suggestions);
    }

    @Override
    public String toString() {
        return "ReviewResult{" +
                "qualityScore=" + qualityScore +
                ", grade='" + getQualityGrade() + '\'' +
                ", metrics=" + metrics +
                ", acceptable=" + isQualityAcceptable() +
                '}';
    }
}