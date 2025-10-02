package com.example.gitreview.application.codereview.service;

import com.example.gitreview.domain.codereview.model.aggregate.CodeReview;
import com.example.gitreview.domain.codereview.model.valueobject.CodeDiff;
import com.example.gitreview.domain.codereview.model.valueobject.ReviewResult;
import com.example.gitreview.domain.codereview.model.valueobject.ReviewStrategy;
import com.example.gitreview.domain.codereview.repository.CodeReviewRepository;
import com.example.gitreview.domain.codereview.service.CodeReviewDomainService;
import com.example.gitreview.domain.shared.model.aggregate.Repository;
import com.example.gitreview.infrastructure.claude.ClaudeQueryPort;
import com.example.gitreview.infrastructure.claude.ClaudeQueryResponse;
import com.example.gitreview.infrastructure.git.GitOperationPort;
import com.example.gitreview.domain.shared.exception.ResourceNotFoundException;
import com.example.gitreview.domain.shared.exception.BusinessRuleException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * CodeReviewApplicationService
 * 代码审查应用服务，协调领域服务和基础设施
 */
@Service
@Transactional
public class CodeReviewApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(CodeReviewApplicationService.class);

    @Autowired
    private CodeReviewRepository codeReviewRepository;

    @Autowired
    private CodeReviewDomainService codeReviewDomainService;

    @Autowired
    private GitOperationPort gitOperationPort;

    @Autowired
    private ClaudeQueryPort claudeQueryPort;

    @Autowired
    private com.example.gitreview.infrastructure.context.CodeContextExtractor contextExtractor;

    @Autowired
    private com.example.gitreview.infrastructure.parser.ReviewResultParser reviewResultParser;

    /**
     * 创建代码审查
     * @param repositoryId 仓库ID
     * @param title 审查标题
     * @param description 审查描述
     * @param baseBranch 基础分支
     * @param targetBranch 目标分支
     * @param strategyMode 审查策略模式
     * @param createdBy 创建者
     * @return 代码审查ID
     */
    public Long createCodeReview(Long repositoryId, String title, String description,
                                String baseBranch, String targetBranch, String strategyMode, String createdBy) {
        logger.info("Creating code review for repository {} from {} to {}", repositoryId, baseBranch, targetBranch);

        try {
            // 创建审查策略
            ReviewStrategy strategy = createReviewStrategy(strategyMode);

            // 创建代码审查聚合根
            CodeReview codeReview = new CodeReview(repositoryId, title, description, strategy, createdBy);

            // 保存到仓储
            CodeReview savedReview = codeReviewRepository.save(codeReview);

            logger.info("Created code review with ID: {}", savedReview.getId());
            return savedReview.getId();

        } catch (Exception e) {
            logger.error("Failed to create code review", e);
            throw new RuntimeException("Failed to create code review: " + e.getMessage(), e);
        }
    }

    /**
     * 开始代码审查
     * @param reviewId 审查ID
     * @param repositoryUrl 仓库URL
     * @param username 用户名
     * @param password 密码
     * @param baseBranch 基础分支
     * @param targetBranch 目标分支
     */
    public void startCodeReview(Long reviewId, String repositoryUrl, String username, String password,
                               String baseBranch, String targetBranch) {
        logger.info("Starting code review {}", reviewId);

        try {
            // 获取代码审查聚合根
            CodeReview codeReview = getCodeReviewById(reviewId);

            // 克隆仓库并获取代码差异
            CodeDiff codeDiff = generateCodeDiff(repositoryUrl, username, password, baseBranch, targetBranch,
                                                codeReview.getRepositoryId());

            // 验证是否可以开始审查
            codeReviewDomainService.validateCanStartReview(codeReview, codeDiff);

            // 开始审查
            codeReview.startReview(codeDiff);

            // 保存状态
            codeReviewRepository.save(codeReview);

            // 异步执行审查
            executeReviewAsync(reviewId);

            logger.info("Started code review {} successfully", reviewId);

        } catch (Exception e) {
            logger.error("Failed to start code review {}", reviewId, e);
            markReviewAsFailed(reviewId, e.getMessage());
            throw new RuntimeException("Failed to start code review: " + e.getMessage(), e);
        }
    }

    /**
     * 获取代码审查结果
     * @param reviewId 审查ID
     * @return 审查结果
     */
    @Transactional(readOnly = true)
    public ReviewResult getReviewResult(Long reviewId) {
        logger.debug("Getting review result for {}", reviewId);

        CodeReview codeReview = getCodeReviewById(reviewId);

        if (codeReview.getFinalResult() == null) {
            throw new IllegalStateException("Review result not available for review " + reviewId);
        }

        return codeReview.getFinalResult();
    }

    /**
     * 获取代码审查状态
     * @param reviewId 审查ID
     * @return 审查状态信息
     */
    @Transactional(readOnly = true)
    public CodeReviewStatusInfo getReviewStatus(Long reviewId) {
        logger.debug("Getting review status for {}", reviewId);

        CodeReview codeReview = getCodeReviewById(reviewId);

        return new CodeReviewStatusInfo(
            codeReview.getId(),
            codeReview.getStatus(),
            codeReview.getProgress(),
            codeReview.getSummary(),
            codeReview.getQualityScore(),
            codeReview.hasCriticalIssues()
        );
    }

    /**
     * 取消代码审查
     * @param reviewId 审查ID
     * @param reason 取消原因
     */
    public void cancelCodeReview(Long reviewId, String reason) {
        logger.info("Cancelling code review {} with reason: {}", reviewId, reason);

        try {
            CodeReview codeReview = getCodeReviewById(reviewId);
            codeReview.cancel(reason);
            codeReviewRepository.save(codeReview);

            logger.info("Cancelled code review {} successfully", reviewId);

        } catch (Exception e) {
            logger.error("Failed to cancel code review {}", reviewId, e);
            throw new RuntimeException("Failed to cancel code review: " + e.getMessage(), e);
        }
    }

    /**
     * 重新开始代码审查
     * @param reviewId 审查ID
     */
    public void restartCodeReview(Long reviewId) {
        logger.info("Restarting code review {}", reviewId);

        try {
            CodeReview codeReview = getCodeReviewById(reviewId);

            if (!codeReview.canRetry()) {
                throw new IllegalStateException("Code review " + reviewId + " cannot be restarted");
            }

            codeReview.restart();
            codeReviewRepository.save(codeReview);

            logger.info("Restarted code review {} successfully", reviewId);

        } catch (Exception e) {
            logger.error("Failed to restart code review {}", reviewId, e);
            throw new RuntimeException("Failed to restart code review: " + e.getMessage(), e);
        }
    }

    /**
     * 获取仓库的代码审查列表
     * @param repositoryId 仓库ID
     * @return 代码审查列表
     */
    @Transactional(readOnly = true)
    public List<CodeReview> getRepositoryReviews(Long repositoryId) {
        logger.debug("Getting reviews for repository {}", repositoryId);
        return codeReviewRepository.findByRepositoryId(repositoryId);
    }

    /**
     * 获取用户的代码审查列表
     * @param createdBy 创建者
     * @return 代码审查列表
     */
    @Transactional(readOnly = true)
    public List<CodeReview> getUserReviews(String createdBy) {
        logger.debug("Getting reviews for user {}", createdBy);
        return codeReviewRepository.findByCreatedBy(createdBy);
    }

    /**
     * 删除代码审查
     * @param reviewId 审查ID
     */
    public void deleteCodeReview(Long reviewId) {
        logger.info("Deleting code review {}", reviewId);

        try {
            if (!codeReviewRepository.existsById(reviewId)) {
                throw new ResourceNotFoundException("Code review not found: " + reviewId);
            }

            codeReviewRepository.deleteById(reviewId);
            logger.info("Deleted code review {} successfully", reviewId);

        } catch (Exception e) {
            logger.error("Failed to delete code review {}", reviewId, e);
            throw new RuntimeException("Failed to delete code review: " + e.getMessage(), e);
        }
    }

    // 私有辅助方法

    /**
     * 根据ID获取代码审查
     */
    private CodeReview getCodeReviewById(Long reviewId) {
        Optional<CodeReview> optional = codeReviewRepository.findById(reviewId);
        if (!optional.isPresent()) {
            throw new ResourceNotFoundException("Code review not found: " + reviewId);
        }
        return optional.get();
    }

    /**
     * 创建审查策略
     */
    private ReviewStrategy createReviewStrategy(String strategyMode) {
        if (strategyMode == null || strategyMode.trim().isEmpty()) {
            return ReviewStrategy.standard();
        }

        try {
            ReviewStrategy.ReviewMode mode = ReviewStrategy.ReviewMode.fromCode(strategyMode.toLowerCase());
            return new ReviewStrategy(mode);
        } catch (Exception e) {
            logger.warn("Unknown strategy mode: {}, using standard", strategyMode);
            return ReviewStrategy.standard();
        }
    }

    /**
     * 生成代码差异
     */
    private CodeDiff generateCodeDiff(String repositoryUrl, String username, String password,
                                     String baseBranch, String targetBranch, Long repositoryId) {
        try {
            // 克隆仓库
            java.io.File repoDir = gitOperationPort.cloneRepository(repositoryUrl, username, password, targetBranch);

            // 获取差异
            List<org.eclipse.jgit.diff.DiffEntry> diffEntries = gitOperationPort.getDiffBetweenBranches(
                repoDir, baseBranch, targetBranch);

            // 生成差异内容
            StringBuilder diffContent = new StringBuilder();
            List<CodeDiff.FileChange> fileChanges = new java.util.ArrayList<>();

            for (org.eclipse.jgit.diff.DiffEntry entry : diffEntries) {
                String fileDiff = gitOperationPort.getDiffContent(repoDir, entry);
                diffContent.append(fileDiff).append("\n");

                // 转换为领域对象
                CodeDiff.ChangeType changeType = convertChangeType(entry.getChangeType());
                CodeDiff.FileChange fileChange = new CodeDiff.FileChange(
                    entry.getNewPath(), changeType, 0, 0); // 简化实现，实际应该计算行数
                fileChanges.add(fileChange);
            }

            return new CodeDiff(repositoryId, baseBranch, targetBranch, diffContent.toString(), fileChanges);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate code diff: " + e.getMessage(), e);
        }
    }

    /**
     * 转换变更类型
     */
    private CodeDiff.ChangeType convertChangeType(org.eclipse.jgit.diff.DiffEntry.ChangeType jgitType) {
        switch (jgitType) {
            case ADD:
                return CodeDiff.ChangeType.ADDED;
            case MODIFY:
                return CodeDiff.ChangeType.MODIFIED;
            case DELETE:
                return CodeDiff.ChangeType.DELETED;
            case RENAME:
                return CodeDiff.ChangeType.RENAMED;
            default:
                return CodeDiff.ChangeType.MODIFIED;
        }
    }

    /**
     * 异步执行审查
     */
    private void executeReviewAsync(Long reviewId) {
        // 在实际实现中，这里应该使用异步任务执行器
        // 这里简化为同步调用
        try {
            CodeReview codeReview = getCodeReviewById(reviewId);

            if (!claudeQueryPort.isAvailable()) {
                throw new BusinessRuleException("Claude service is not available");
            }

            // 提取上下文（针对深度审查模式）
            String contextInfo = extractContextForReview(codeReview);

            // 调用Claude进行审查
            ClaudeQueryResponse response = claudeQueryPort.reviewCodeChanges(
                codeReview.getCodeDiff().getDiffContent(),
                "Git代码审查项目", // 项目上下文
                codeReview.getDescription() + "\n\n" + contextInfo, // 提交信息 + 上下文
                codeReview.getStrategy().getMode().getCode()
            );

            if (response.isSuccessful()) {
                // 使用 ReviewResultParser 解析审查结果
                ReviewResult result = reviewResultParser.parse(response.getOutput());

                // 完成审查
                codeReview.completeReview();
                codeReviewRepository.save(codeReview);

                logger.info("Completed code review {} successfully", reviewId);
            } else {
                markReviewAsFailed(reviewId, response.getError());
            }

        } catch (Exception e) {
            logger.error("Failed to execute review {}", reviewId, e);
            markReviewAsFailed(reviewId, e.getMessage());
        }
    }

    /**
     * 提取审查上下文
     */
    private String extractContextForReview(CodeReview codeReview) {
        // 仅对深度审查模式提取上下文
        if (codeReview.getStrategy().getMode() != ReviewStrategy.ReviewMode.DEEP) {
            return "";
        }

        try {
            CodeDiff codeDiff = codeReview.getCodeDiff();
            if (codeDiff == null || codeDiff.getFileChanges().isEmpty()) {
                return "";
            }

            StringBuilder contextBuilder = new StringBuilder();
            contextBuilder.append("## 代码上下文信息\n\n");

            // 只处理 Java 文件
            for (CodeDiff.FileChange fileChange : codeDiff.getFileChanges()) {
                if (!fileChange.getFilePath().endsWith(".java")) {
                    continue;
                }

                try {
                    // 读取文件内容（需要从Git仓库读取）
                    // 这里简化处理，实际需要通过GitOperationPort读取文件
                    // String fileContent = gitOperationPort.readFile(repoDir, fileChange.getFilePath());

                    // 由于无法直接访问仓库，这里先跳过上下文提取
                    // TODO: 需要改进架构，保留仓库克隆以便读取文件内容

                } catch (Exception e) {
                    logger.warn("无法提取文件上下文: {}", fileChange.getFilePath(), e);
                }
            }

            return contextBuilder.toString();

        } catch (Exception e) {
            logger.error("提取上下文失败", e);
            return "";
        }
    }

    /**
     * 标记审查为失败
     */
    private void markReviewAsFailed(Long reviewId, String errorMessage) {
        try {
            CodeReview codeReview = getCodeReviewById(reviewId);
            codeReview.markAsFailed(errorMessage);
            codeReviewRepository.save(codeReview);
        } catch (Exception e) {
            logger.error("Failed to mark review {} as failed", reviewId, e);
        }
    }

    /**
     * 代码审查状态信息
     */
    public static class CodeReviewStatusInfo {
        private final Long id;
        private final CodeReview.ReviewStatus status;
        private final int progress;
        private final String summary;
        private final int qualityScore;
        private final boolean hasCriticalIssues;

        public CodeReviewStatusInfo(Long id, CodeReview.ReviewStatus status, int progress,
                                   String summary, int qualityScore, boolean hasCriticalIssues) {
            this.id = id;
            this.status = status;
            this.progress = progress;
            this.summary = summary;
            this.qualityScore = qualityScore;
            this.hasCriticalIssues = hasCriticalIssues;
        }

        // Getters
        public Long getId() { return id; }
        public CodeReview.ReviewStatus getStatus() { return status; }
        public int getProgress() { return progress; }
        public String getSummary() { return summary; }
        public int getQualityScore() { return qualityScore; }
        public boolean isHasCriticalIssues() { return hasCriticalIssues; }
    }
}