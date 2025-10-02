package com.example.gitreview.domain.codereview.service;

import com.example.gitreview.domain.codereview.model.aggregate.CodeReview;
import com.example.gitreview.domain.codereview.model.valueobject.CodeDiff;
import com.example.gitreview.domain.codereview.model.valueobject.ReviewResult;
import com.example.gitreview.domain.codereview.model.valueobject.ReviewStrategy;
import com.example.gitreview.domain.shared.exception.BusinessRuleException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * CodeReviewDomainService
 * 代码审查领域服务，包含复杂的业务逻辑和规则
 */
@Service
public class CodeReviewDomainService {

    /**
     * 计算问题优先级（P0-P3）
     * @param severity 严重程度
     * @param category 问题类别
     * @param description 问题描述
     * @param filePath 文件路径
     * @return 优先级
     */
    public ReviewResult.IssuePriority calculateIssuePriority(
            ReviewResult.IssueSeverity severity,
            String category,
            String description,
            String filePath) {

        // P0: CRITICAL + (安全问题 OR 数据问题 OR 核心业务)
        if (severity == ReviewResult.IssueSeverity.CRITICAL) {
            if (isSecurityRelated(category, description) ||
                isDataRelated(category, description) ||
                isCoreBusiness(filePath)) {
                return ReviewResult.IssuePriority.P0;
            }
            // CRITICAL 但非核心问题 -> P1
            return ReviewResult.IssuePriority.P1;
        }

        // P1: MAJOR + (安全或性能问题)
        if (severity == ReviewResult.IssueSeverity.MAJOR) {
            if (isSecurityRelated(category, description) ||
                isPerformanceRelated(category, description)) {
                return ReviewResult.IssuePriority.P1;
            }
            // MAJOR 但非安全/性能 -> P2
            return ReviewResult.IssuePriority.P2;
        }

        // P2: MINOR + 重要模块
        if (severity == ReviewResult.IssueSeverity.MINOR) {
            if (isImportantModule(filePath)) {
                return ReviewResult.IssuePriority.P2;
            }
            // 普通 MINOR -> P3
            return ReviewResult.IssuePriority.P3;
        }

        // P3: INFO 级别问题
        return ReviewResult.IssuePriority.P3;
    }

    /**
     * 检查是否为安全相关问题
     */
    private boolean isSecurityRelated(String category, String description) {
        String lowerCategory = category.toLowerCase();
        String lowerDescription = description.toLowerCase();

        return lowerCategory.contains("安全") || lowerCategory.contains("security") ||
               lowerDescription.contains("sql注入") || lowerDescription.contains("sql injection") ||
               lowerDescription.contains("xss") || lowerDescription.contains("csrf") ||
               lowerDescription.contains("注入") || lowerDescription.contains("injection") ||
               lowerDescription.contains("权限") || lowerDescription.contains("auth") ||
               lowerDescription.contains("密码") || lowerDescription.contains("password") ||
               lowerDescription.contains("加密") || lowerDescription.contains("encrypt") ||
               lowerDescription.contains("漏洞") || lowerDescription.contains("vulnerability");
    }

    /**
     * 检查是否为数据相关问题
     */
    private boolean isDataRelated(String category, String description) {
        String lowerCategory = category.toLowerCase();
        String lowerDescription = description.toLowerCase();

        return lowerCategory.contains("数据") || lowerCategory.contains("data") ||
               lowerDescription.contains("数据丢失") || lowerDescription.contains("data loss") ||
               lowerDescription.contains("数据泄露") || lowerDescription.contains("data leak") ||
               lowerDescription.contains("数据损坏") || lowerDescription.contains("corruption") ||
               lowerDescription.contains("事务") || lowerDescription.contains("transaction");
    }

    /**
     * 检查是否为性能相关问题
     */
    private boolean isPerformanceRelated(String category, String description) {
        String lowerCategory = category.toLowerCase();
        String lowerDescription = description.toLowerCase();

        return lowerCategory.contains("性能") || lowerCategory.contains("performance") ||
               lowerDescription.contains("n+1") || lowerDescription.contains("内存泄漏") ||
               lowerDescription.contains("memory leak") || lowerDescription.contains("慢查询") ||
               lowerDescription.contains("slow query") || lowerDescription.contains("死锁") ||
               lowerDescription.contains("deadlock") || lowerDescription.contains("超时") ||
               lowerDescription.contains("timeout");
    }

    /**
     * 检查是否为核心业务文件
     */
    private boolean isCoreBusiness(String filePath) {
        if (filePath == null) {
            return false;
        }
        String lowerPath = filePath.toLowerCase();

        // 核心业务关键词
        return lowerPath.contains("payment") || lowerPath.contains("支付") ||
               lowerPath.contains("order") || lowerPath.contains("订单") ||
               lowerPath.contains("account") || lowerPath.contains("账户") ||
               lowerPath.contains("transaction") || lowerPath.contains("交易") ||
               lowerPath.contains("wallet") || lowerPath.contains("钱包") ||
               lowerPath.contains("finance") || lowerPath.contains("财务");
    }

    /**
     * 检查是否为重要模块
     */
    private boolean isImportantModule(String filePath) {
        if (filePath == null) {
            return false;
        }
        String lowerPath = filePath.toLowerCase();

        // 重要模块关键词
        return lowerPath.contains("service") || lowerPath.contains("controller") ||
               lowerPath.contains("repository") || lowerPath.contains("dao") ||
               lowerPath.contains("manager") || lowerPath.contains("processor");
    }

    /**
     * 验证代码审查是否可以开始
     * @param codeReview 代码审查聚合根
     * @param codeDiff 代码差异
     * @throws BusinessRuleException 如果不满足业务规则
     */
    public void validateCanStartReview(CodeReview codeReview, CodeDiff codeDiff) {
        // 检查审查状态
        if (codeReview.getStatus() != CodeReview.ReviewStatus.PENDING) {
            throw new BusinessRuleException(
                "Cannot start review in current status: " + codeReview.getStatus());
        }

        // 检查代码差异
        if (codeDiff.isEmpty()) {
            throw new BusinessRuleException(
                "Cannot review empty code diff");
        }

        // 检查策略与差异的兼容性
        validateStrategyCompatibility(codeReview.getStrategy(), codeDiff);
    }

    /**
     * 验证审查策略与代码差异的兼容性
     * @param strategy 审查策略
     * @param codeDiff 代码差异
     */
    public void validateStrategyCompatibility(ReviewStrategy strategy, CodeDiff codeDiff) {
        // 检查大型变更的策略适用性
        if (codeDiff.isLargeChange() && !strategy.isSuitableForLargeChanges(codeDiff.getStats().getTotalChangedLines())) {
            throw new BusinessRuleException(
                "Review strategy " + strategy.getMode().getDisplayName() +
                " is not suitable for large changes (" + codeDiff.getStats().getTotalChangedLines() + " lines)");
        }

        // 检查差异大小是否适合审查
        int maxSizeBytes = getMaxSizeForStrategy(strategy);
        if (!codeDiff.isSuitableForReview(maxSizeBytes)) {
            throw new BusinessRuleException(
                "Code diff is too large for review strategy " + strategy.getMode().getDisplayName());
        }
    }

    /**
     * 选择最佳审查策略
     * @param codeDiff 代码差异
     * @param userPreference 用户偏好（可为null）
     * @return 推荐的审查策略
     */
    public ReviewStrategy recommendStrategy(CodeDiff codeDiff, ReviewStrategy userPreference) {
        // 如果用户有明确偏好且兼容，则使用用户偏好
        if (userPreference != null) {
            try {
                validateStrategyCompatibility(userPreference, codeDiff);
                return userPreference;
            } catch (BusinessRuleException e) {
                // 用户偏好不兼容，继续推荐最佳策略
            }
        }

        // 基于代码差异特征推荐策略
        if (codeDiff.isEmpty()) {
            return ReviewStrategy.quick();
        }

        // 大型变更使用快速或标准模式
        if (codeDiff.isLargeChange()) {
            return codeDiff.getStats().getTotalChangedLines() > 1000 ?
                   ReviewStrategy.quick() : ReviewStrategy.standard();
        }

        // 安全相关文件变更使用安全模式
        if (hasSecurityRelevantChanges(codeDiff)) {
            return ReviewStrategy.security();
        }

        // 性能相关文件变更使用性能模式
        if (hasPerformanceRelevantChanges(codeDiff)) {
            return ReviewStrategy.performance();
        }

        // 架构相关文件变更使用架构模式
        if (hasArchitectureRelevantChanges(codeDiff)) {
            return ReviewStrategy.architecture();
        }

        // 复杂度高的变更使用全面模式
        if (codeDiff.getComplexityScore() >= 7) {
            return ReviewStrategy.comprehensive();
        }

        // 默认使用标准模式
        return ReviewStrategy.standard();
    }

    /**
     * 计算审查优先级
     * @param codeReview 代码审查
     * @return 优先级分数 (1-10, 10最高)
     */
    public int calculateReviewPriority(CodeReview codeReview) {
        int priority = 5; // 基础优先级

        CodeDiff codeDiff = codeReview.getCodeDiff();
        if (codeDiff == null) {
            return priority;
        }

        // 基于变更大小调整优先级
        int changedLines = codeDiff.getStats().getTotalChangedLines();
        if (changedLines > 1000) {
            priority += 2; // 大型变更优先级高
        } else if (changedLines > 500) {
            priority += 1;
        }

        // 基于文件类型调整优先级
        if (codeDiff.hasJavaChanges()) {
            priority += 1; // Java代码变更优先级较高
        }

        // 基于安全相关性调整优先级
        if (hasSecurityRelevantChanges(codeDiff)) {
            priority += 3; // 安全相关变更优先级最高
        }

        // 基于审查策略调整优先级
        ReviewStrategy strategy = codeReview.getStrategy();
        if (strategy.getMode().isSecurityFocused()) {
            priority += 2;
        } else if (strategy.getMode().isComprehensive()) {
            priority += 1;
        }

        return Math.min(10, Math.max(1, priority));
    }

    /**
     * 验证审查结果质量
     * @param result 审查结果
     * @param strategy 审查策略
     * @return 是否通过质量检查
     */
    public boolean validateResultQuality(ReviewResult result, ReviewStrategy strategy) {
        // 检查基本质量要求
        if (result.getQualityScore() < 0 || result.getQualityScore() > 100) {
            return false;
        }

        // 检查摘要内容
        if (result.getSummary() == null || result.getSummary().trim().length() < 10) {
            return false;
        }

        // 根据策略检查结果完整性
        switch (strategy.getMode()) {
            case COMPREHENSIVE:
                // 全面审查需要详细的问题和建议
                return result.getIssues().size() > 0 || result.getSuggestions().size() > 0;
            case SECURITY:
                // 安全审查需要检查安全相关问题
                return hasSecurityIssues(result);
            case QUICK:
                // 快速审查只需要基本的摘要
                return true;
            default:
                // 标准审查需要基本的质量评估
                return result.getQualityScore() > 0;
        }
    }

    /**
     * 合并多个审查结果
     * @param results 审查结果列表
     * @return 合并后的结果
     */
    public ReviewResult mergeResults(List<ReviewResult> results) {
        if (results.isEmpty()) {
            throw new BusinessRuleException("Cannot merge empty result list");
        }

        if (results.size() == 1) {
            return results.get(0);
        }

        // 合并逻辑：使用最新的结果作为基础，合并其他结果的问题和建议
        ReviewResult base = results.get(results.size() - 1);
        for (int i = results.size() - 2; i >= 0; i--) {
            base = base.merge(results.get(i));
        }

        return base;
    }

    /**
     * 估算审查完成时间
     * @param strategy 审查策略
     * @param codeDiff 代码差异
     * @return 估算完成时间（分钟）
     */
    public int estimateCompletionTime(ReviewStrategy strategy, CodeDiff codeDiff) {
        if (codeDiff == null || codeDiff.isEmpty()) {
            return 1; // 最少1分钟
        }

        int baseTime = strategy.estimateReviewTime(codeDiff.getStats().getTotalChangedLines());

        // 基于复杂度调整时间
        int complexity = codeDiff.getComplexityScore();
        if (complexity >= 8) {
            baseTime = (int) (baseTime * 1.5);
        } else if (complexity >= 6) {
            baseTime = (int) (baseTime * 1.2);
        }

        // 基于文件类型调整时间
        if (codeDiff.hasJavaChanges()) {
            baseTime = (int) (baseTime * 1.1); // Java代码需要更多时间
        }

        return Math.max(1, baseTime);
    }

    // 私有辅助方法

    /**
     * 检查是否有安全相关的变更
     */
    private boolean hasSecurityRelevantChanges(CodeDiff codeDiff) {
        return codeDiff.getFileChanges().stream().anyMatch(change -> {
            String path = change.getFilePath().toLowerCase();
            return path.contains("security") || path.contains("auth") ||
                   path.contains("login") || path.contains("password") ||
                   path.contains("token") || path.contains("crypto") ||
                   path.contains("ssl") || path.contains("certificate");
        });
    }

    /**
     * 检查是否有性能相关的变更
     */
    private boolean hasPerformanceRelevantChanges(CodeDiff codeDiff) {
        return codeDiff.getFileChanges().stream().anyMatch(change -> {
            String path = change.getFilePath().toLowerCase();
            return path.contains("cache") || path.contains("pool") ||
                   path.contains("queue") || path.contains("async") ||
                   path.contains("thread") || path.contains("concurrent") ||
                   path.contains("performance") || path.contains("optimization");
        });
    }

    /**
     * 检查是否有架构相关的变更
     */
    private boolean hasArchitectureRelevantChanges(CodeDiff codeDiff) {
        return codeDiff.getFileChanges().stream().anyMatch(change -> {
            String path = change.getFilePath().toLowerCase();
            return path.contains("config") || path.contains("application.") ||
                   path.contains("pom.xml") || path.contains("build.gradle") ||
                   path.contains("dockerfile") || path.contains("schema") ||
                   change.getChangeType() == CodeDiff.ChangeType.ADDED &&
                   change.getFilePath().endsWith(".java");
        });
    }

    /**
     * 检查结果是否包含安全问题
     */
    private boolean hasSecurityIssues(ReviewResult result) {
        return result.getIssues().stream().anyMatch(issue -> {
            String category = issue.getCategory().toLowerCase();
            String description = issue.getDescription().toLowerCase();
            return category.contains("security") || category.contains("安全") ||
                   description.contains("security") || description.contains("安全") ||
                   description.contains("vulnerability") || description.contains("漏洞");
        });
    }

    /**
     * 获取策略对应的最大差异大小
     */
    private int getMaxSizeForStrategy(ReviewStrategy strategy) {
        switch (strategy.getMode()) {
            case QUICK:
                return 50 * 1024; // 50KB
            case COMPREHENSIVE:
                return 500 * 1024; // 500KB
            default:
                return 200 * 1024; // 200KB
        }
    }
}