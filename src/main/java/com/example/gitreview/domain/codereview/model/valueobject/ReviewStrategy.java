package com.example.gitreview.domain.codereview.model.valueobject;

import com.example.gitreview.domain.shared.exception.ValidationException;

import java.util.Objects;

/**
 * ReviewStrategy值对象
 * 表示代码审查策略的领域概念
 */
public class ReviewStrategy {

    private final ReviewMode mode;
    private final int maxRetries;
    private final int timeoutMinutes;
    private final boolean includeContext;
    private final boolean enableDeepAnalysis;
    private final String customPrompt;

    public enum ReviewMode {
        QUICK("quick", "快速审查", "重点关注严重问题和明显错误"),
        STANDARD("standard", "标准审查", "平衡关注代码质量、功能正确性、最佳实践"),
        DEEP("deep", "深度审查", "结合上下文的全面深度分析，P0-P3问题分级"),
        SECURITY("security", "安全审查", "重点关注安全漏洞、注入攻击、数据泄露等安全问题"),
        ARCHITECTURE("architecture", "架构审查", "重点关注设计模式、代码结构、模块划分、可维护性"),
        PERFORMANCE("performance", "性能审查", "重点关注性能瓶颈、资源消耗、算法复杂度、并发问题"),
        COMPREHENSIVE("comprehensive", "全面审查", "深入检查所有方面，包括功能、安全、性能、可维护性、测试覆盖");

        private final String code;
        private final String displayName;
        private final String description;

        ReviewMode(String code, String displayName, String description) {
            this.code = code;
            this.displayName = displayName;
            this.description = description;
        }

        public String getCode() { return code; }
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }

        public static ReviewMode fromCode(String code) {
            for (ReviewMode mode : values()) {
                if (mode.code.equals(code)) {
                    return mode;
                }
            }
            throw new ValidationException("Unknown review mode code: " + code);
        }

        public boolean isQuick() { return this == QUICK; }
        public boolean isComprehensive() { return this == COMPREHENSIVE; }
        public boolean isSecurityFocused() { return this == SECURITY; }
        public boolean isPerformanceFocused() { return this == PERFORMANCE; }
    }

    // 构造函数
    public ReviewStrategy(ReviewMode mode) {
        this(mode, getDefaultMaxRetries(mode), getDefaultTimeoutMinutes(mode),
             getDefaultIncludeContext(mode), getDefaultEnableDeepAnalysis(mode), null);
    }

    public ReviewStrategy(ReviewMode mode, int maxRetries, int timeoutMinutes,
                         boolean includeContext, boolean enableDeepAnalysis, String customPrompt) {
        this.mode = Objects.requireNonNull(mode, "Review mode cannot be null");
        this.maxRetries = validateMaxRetries(maxRetries);
        this.timeoutMinutes = validateTimeoutMinutes(timeoutMinutes);
        this.includeContext = includeContext;
        this.enableDeepAnalysis = enableDeepAnalysis;
        this.customPrompt = customPrompt; // 可以为空
    }

    // 静态工厂方法
    public static ReviewStrategy quick() {
        return new ReviewStrategy(ReviewMode.QUICK);
    }

    public static ReviewStrategy standard() {
        return new ReviewStrategy(ReviewMode.STANDARD);
    }

    public static ReviewStrategy deep() {
        return new ReviewStrategy(ReviewMode.DEEP, 3, 60, true, true, null);
    }

    public static ReviewStrategy security() {
        return new ReviewStrategy(ReviewMode.SECURITY);
    }

    public static ReviewStrategy architecture() {
        return new ReviewStrategy(ReviewMode.ARCHITECTURE);
    }

    public static ReviewStrategy performance() {
        return new ReviewStrategy(ReviewMode.PERFORMANCE);
    }

    public static ReviewStrategy comprehensive() {
        return new ReviewStrategy(ReviewMode.COMPREHENSIVE);
    }

    public static ReviewStrategy custom(String customPrompt) {
        return new ReviewStrategy(ReviewMode.STANDARD, 3, 30, true, true, customPrompt);
    }

    // 业务方法

    /**
     * 检查是否需要深度分析
     * @return 是否需要深度分析
     */
    public boolean requiresDeepAnalysis() {
        return enableDeepAnalysis || mode.isComprehensive();
    }

    /**
     * 检查是否需要包含项目上下文
     * @return 是否需要包含上下文
     */
    public boolean requiresContext() {
        return includeContext || mode.isComprehensive() || mode.isSecurityFocused();
    }

    /**
     * 获取审查重点描述
     * @return 审查重点描述
     */
    public String getFocusDescription() {
        if (hasCustomPrompt()) {
            return "自定义审查重点";
        }
        return mode.getDescription();
    }

    /**
     * 检查是否有自定义提示词
     * @return 是否有自定义提示词
     */
    public boolean hasCustomPrompt() {
        return customPrompt != null && !customPrompt.trim().isEmpty();
    }

    /**
     * 获取有效的提示词
     * @return 提示词内容
     */
    public String getEffectivePrompt() {
        if (hasCustomPrompt()) {
            return customPrompt.trim();
        }
        return mode.getDescription();
    }

    /**
     * 估算审查时间（分钟）
     * @param codeLines 代码行数
     * @return 估算时间（分钟）
     */
    public int estimateReviewTime(int codeLines) {
        double baseMinutes;
        switch (mode) {
            case QUICK:
                baseMinutes = codeLines * 0.01; // 1分钟/100行
                break;
            case COMPREHENSIVE:
                baseMinutes = codeLines * 0.05; // 1分钟/20行
                break;
            case SECURITY:
            case ARCHITECTURE:
            case PERFORMANCE:
                baseMinutes = codeLines * 0.03; // 1分钟/33行
                break;
            case STANDARD:
            default:
                baseMinutes = codeLines * 0.02; // 1分钟/50行
                break;
        }

        // 深度分析增加时间
        if (enableDeepAnalysis) {
            baseMinutes *= 1.5;
        }

        // 包含上下文增加时间
        if (includeContext) {
            baseMinutes *= 1.2;
        }

        return Math.max(1, (int) Math.ceil(baseMinutes));
    }

    /**
     * 检查策略是否适合大型变更
     * @param totalLines 总变更行数
     * @return 是否适合
     */
    public boolean isSuitableForLargeChanges(int totalLines) {
        if (totalLines > 1000) {
            // 大型变更建议使用快速或标准模式
            return mode == ReviewMode.QUICK || mode == ReviewMode.STANDARD;
        }
        return true; // 小型变更适合所有模式
    }

    /**
     * 创建策略副本并修改参数
     * @param newMaxRetries 新的最大重试次数
     * @return 新的策略实例
     */
    public ReviewStrategy withMaxRetries(int newMaxRetries) {
        return new ReviewStrategy(mode, newMaxRetries, timeoutMinutes, includeContext, enableDeepAnalysis, customPrompt);
    }

    /**
     * 创建策略副本并修改超时时间
     * @param newTimeoutMinutes 新的超时时间
     * @return 新的策略实例
     */
    public ReviewStrategy withTimeout(int newTimeoutMinutes) {
        return new ReviewStrategy(mode, maxRetries, newTimeoutMinutes, includeContext, enableDeepAnalysis, customPrompt);
    }

    /**
     * 创建策略副本并启用深度分析
     * @return 新的策略实例
     */
    public ReviewStrategy withDeepAnalysis() {
        return new ReviewStrategy(mode, maxRetries, timeoutMinutes, includeContext, true, customPrompt);
    }

    /**
     * 创建策略副本并包含上下文
     * @return 新的策略实例
     */
    public ReviewStrategy withContext() {
        return new ReviewStrategy(mode, maxRetries, timeoutMinutes, true, enableDeepAnalysis, customPrompt);
    }

    // 私有方法
    private static int getDefaultMaxRetries(ReviewMode mode) {
        switch (mode) {
            case QUICK:
                return 1;
            case COMPREHENSIVE:
                return 5;
            default:
                return 3;
        }
    }

    private static int getDefaultTimeoutMinutes(ReviewMode mode) {
        switch (mode) {
            case QUICK:
                return 10;
            case COMPREHENSIVE:
                return 60;
            default:
                return 30;
        }
    }

    private static boolean getDefaultIncludeContext(ReviewMode mode) {
        return mode == ReviewMode.COMPREHENSIVE || mode == ReviewMode.SECURITY || mode == ReviewMode.ARCHITECTURE;
    }

    private static boolean getDefaultEnableDeepAnalysis(ReviewMode mode) {
        return mode == ReviewMode.COMPREHENSIVE || mode == ReviewMode.SECURITY || mode == ReviewMode.PERFORMANCE;
    }

    private int validateMaxRetries(int maxRetries) {
        if (maxRetries < 0 || maxRetries > 10) {
            throw new ValidationException("Max retries must be between 0 and 10");
        }
        return maxRetries;
    }

    private int validateTimeoutMinutes(int timeoutMinutes) {
        if (timeoutMinutes < 1 || timeoutMinutes > 120) {
            throw new ValidationException("Timeout must be between 1 and 120 minutes");
        }
        return timeoutMinutes;
    }

    // Getters
    public ReviewMode getMode() {
        return mode;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public int getTimeoutMinutes() {
        return timeoutMinutes;
    }

    public boolean isIncludeContext() {
        return includeContext;
    }

    public boolean isEnableDeepAnalysis() {
        return enableDeepAnalysis;
    }

    public String getCustomPrompt() {
        return customPrompt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReviewStrategy that = (ReviewStrategy) o;
        return maxRetries == that.maxRetries &&
               timeoutMinutes == that.timeoutMinutes &&
               includeContext == that.includeContext &&
               enableDeepAnalysis == that.enableDeepAnalysis &&
               mode == that.mode &&
               Objects.equals(customPrompt, that.customPrompt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, maxRetries, timeoutMinutes, includeContext, enableDeepAnalysis, customPrompt);
    }

    @Override
    public String toString() {
        return "ReviewStrategy{" +
                "mode=" + mode.getDisplayName() +
                ", maxRetries=" + maxRetries +
                ", timeoutMinutes=" + timeoutMinutes +
                ", includeContext=" + includeContext +
                ", enableDeepAnalysis=" + enableDeepAnalysis +
                ", hasCustomPrompt=" + hasCustomPrompt() +
                '}';
    }
}