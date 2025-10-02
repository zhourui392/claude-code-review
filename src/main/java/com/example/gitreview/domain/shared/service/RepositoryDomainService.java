package com.example.gitreview.domain.shared.service;

import com.example.gitreview.domain.shared.model.aggregate.Repository;
import com.example.gitreview.domain.shared.exception.BusinessRuleException;
import com.example.gitreview.domain.shared.exception.ValidationException;
import com.example.gitreview.infrastructure.git.GitOperationPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * 仓库领域服务
 * 包含仓库相关的核心业务逻辑和规则
 */
@Service
public class RepositoryDomainService {

    private static final Logger logger = LoggerFactory.getLogger(RepositoryDomainService.class);

    // Git URL格式验证正则表达式
    private static final Pattern GIT_URL_PATTERN = Pattern.compile(
            "^(https?://|git@)[\\w\\.-]+[/:]([\\w\\.-]+/)*[\\w\\.-]+(\\.git)?/?$"
    );

    private final GitOperationPort gitOperationPort;

    @Autowired
    public RepositoryDomainService(GitOperationPort gitOperationPort) {
        this.gitOperationPort = gitOperationPort;
    }

    /**
     * 验证仓库URL格式
     * @param url 仓库URL
     */
    public void validateRepositoryUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new ValidationException("Repository URL cannot be null or empty");
        }

        String trimmedUrl = url.trim();

        // 检查URL格式
        if (!GIT_URL_PATTERN.matcher(trimmedUrl).matches()) {
            throw new ValidationException("Invalid Git repository URL format: " + url);
        }

        // 检查是否为支持的协议
        if (!isSupportedProtocol(trimmedUrl)) {
            throw new ValidationException("Unsupported protocol. Only HTTP(S) and SSH are supported");
        }

        logger.debug("Repository URL validation passed: {}", url);
    }

    /**
     * 验证仓库访问权限
     * @param repository 仓库聚合根
     */
    public void validateRepositoryAccess(Repository repository) {
        try {
            logger.info("Validating access for repository: {}", repository.getName());

            // 使用Repository自身的连接测试方法
            boolean isAccessible = repository.testConnection();

            if (!isAccessible) {
                throw new BusinessRuleException("Repository is not accessible: " + repository.getUrl());
            }

            logger.info("Repository access validation successful: {}", repository.getName());

        } catch (BusinessRuleException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to validate repository access: {}", repository.getName(), e);
            throw new BusinessRuleException("Failed to validate repository access: " + e.getMessage());
        }
    }

    /**
     * 检查仓库是否可访问
     * @param repository 仓库聚合根
     * @return 是否可访问
     */
    public boolean isRepositoryAccessible(Repository repository) {
        try {
            return repository.testConnection();
        } catch (Exception e) {
            logger.warn("Repository accessibility check failed: {}", repository.getName(), e);
            return false;
        }
    }

    /**
     * 验证是否可以删除仓库
     * @param repository 仓库聚合根
     */
    public void validateCanDelete(Repository repository) {
        // 检查仓库是否有关联的代码审查
        // 实际项目中需要查询相关服务
        // if (hasActiveCodeReviews(repository)) {
        //     throw new BusinessRuleException("Cannot delete repository with active code reviews");
        // }

        // 检查仓库是否有关联的测试套件
        // if (hasActiveTestSuites(repository)) {
        //     throw new BusinessRuleException("Cannot delete repository with active test suites");
        // }

        logger.debug("Repository deletion validation passed: {}", repository.getName());
    }

    /**
     * 计算仓库的复杂度评分
     * @param repository 仓库聚合根
     * @return 复杂度评分 (1-10)
     */
    public int calculateRepositoryComplexity(Repository repository) {
        int complexity = 1; // 基础复杂度

        try {
            // 基于URL判断仓库类型
            String url = repository.getUrl();
            if (url.contains("github.com") || url.contains("gitlab.com")) {
                complexity += 1; // 公共仓库通常较复杂
            }

            // 基于名称判断项目类型
            String name = repository.getName().toLowerCase();
            if (name.contains("enterprise") || name.contains("platform")) {
                complexity += 2; // 企业级项目更复杂
            }
            if (name.contains("microservice") || name.contains("service")) {
                complexity += 1; // 微服务项目稍复杂
            }

            // 基于访问次数判断使用频率
            if (repository.getAccessCount() > 100) {
                complexity += 1; // 高频使用的仓库通常较复杂
            }

        } catch (Exception e) {
            logger.warn("Error calculating repository complexity: {}", repository.getName(), e);
        }

        return Math.min(10, Math.max(1, complexity));
    }

    /**
     * 推荐分支策略
     * @param repository 仓库聚合根
     * @return 推荐的分支策略
     */
    public String recommendBranchStrategy(Repository repository) {
        int complexity = calculateRepositoryComplexity(repository);

        if (complexity >= 8) {
            return "Git Flow"; // 复杂项目使用Git Flow
        } else if (complexity >= 5) {
            return "GitHub Flow"; // 中等复杂度使用GitHub Flow
        } else {
            return "Feature Branch"; // 简单项目使用特性分支
        }
    }

    /**
     * 估算代码审查时间
     * @param repository 仓库聚合根
     * @param changeLines 变更行数
     * @return 估算审查时间（分钟）
     */
    public int estimateReviewTime(Repository repository, int changeLines) {
        int baseTime = 5; // 基础时间5分钟
        int complexity = calculateRepositoryComplexity(repository);

        // 基于复杂度调整基础时间
        baseTime += complexity * 2;

        // 基于变更行数计算时间
        int reviewTime = baseTime + (changeLines / 10); // 每10行代码增加1分钟

        // 设置合理的上下限
        return Math.min(120, Math.max(5, reviewTime)); // 5-120分钟
    }

    /**
     * 检查仓库健康状态
     * @param repository 仓库聚合根
     * @return 健康状态描述
     */
    public String checkRepositoryHealth(Repository repository) {
        try {
            boolean isAccessible = isRepositoryAccessible(repository);

            if (!isAccessible) {
                return "Repository is not accessible";
            }

            // 检查最近访问时间
            if (repository.getLastAccessTime() != null) {
                long daysSinceAccess = java.time.temporal.ChronoUnit.DAYS.between(
                        repository.getLastAccessTime().toLocalDate(),
                        java.time.LocalDate.now()
                );

                if (daysSinceAccess > 30) {
                    return "Repository has not been accessed for over 30 days";
                }
            }

            return "Repository is healthy";

        } catch (Exception e) {
            logger.error("Error checking repository health: {}", repository.getName(), e);
            return "Unable to determine repository health";
        }
    }

    /**
     * 生成仓库统计报告
     * @param repository 仓库聚合根
     * @return 统计报告
     */
    public String generateRepositoryReport(Repository repository) {
        StringBuilder report = new StringBuilder();

        report.append("=== Repository Report ===\n");
        report.append("Name: ").append(repository.getName()).append("\n");
        report.append("URL: ").append(repository.getUrl()).append("\n");
        report.append("Status: ").append(repository.getStatus()).append("\n");
        report.append("Created: ").append(repository.getCreateTime()).append("\n");
        report.append("Access Count: ").append(repository.getAccessCount()).append("\n");

        if (repository.getLastAccessTime() != null) {
            report.append("Last Access: ").append(repository.getLastAccessTime()).append("\n");
        }

        report.append("Complexity Score: ").append(calculateRepositoryComplexity(repository)).append("/10\n");
        report.append("Recommended Branch Strategy: ").append(recommendBranchStrategy(repository)).append("\n");
        report.append("Health Status: ").append(checkRepositoryHealth(repository)).append("\n");

        return report.toString();
    }

    // 私有辅助方法

    /**
     * 检查是否为支持的协议
     */
    private boolean isSupportedProtocol(String url) {
        return url.startsWith("http://") ||
               url.startsWith("https://") ||
               url.startsWith("git@");
    }

    /**
     * 检查是否有活跃的代码审查
     */
    private boolean hasActiveCodeReviews(Repository repository) {
        // 实际项目中需要查询代码审查服务
        return false;
    }

    /**
     * 检查是否有活跃的测试套件
     */
    private boolean hasActiveTestSuites(Repository repository) {
        // 实际项目中需要查询测试套件服务
        return false;
    }
}