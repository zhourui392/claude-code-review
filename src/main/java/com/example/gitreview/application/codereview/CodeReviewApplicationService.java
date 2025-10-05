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
import org.springframework.scheduling.annotation.Async;
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

    @Autowired
    private com.example.gitreview.infrastructure.workspace.TempWorkspaceManager workspaceManager;

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
        String workspaceId = null;
        try {
            // 创建临时工作空间（用于代码审查）
            workspaceId = "code-review-" + System.currentTimeMillis();
            workspaceManager.createWorkspace(workspaceId);

            // 克隆仓库到工作空间
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

            CodeDiff codeDiff = new CodeDiff(repositoryId, baseBranch, targetBranch, diffContent.toString(), fileChanges);
            codeDiff.setWorkspaceId(workspaceId);

            return codeDiff;

        } catch (Exception e) {
            // 如果创建失败，清理工作空间
            if (workspaceId != null) {
                try {
                    workspaceManager.cleanupWorkspace(workspaceManager.getWorkspaceFile(workspaceId));
                } catch (Exception cleanupEx) {
                    logger.error("清理工作空间失败: {}", workspaceId, cleanupEx);
                }
            }
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
     * 使用异步线程池执行代码审查，并实时更新进度
     */
    @Async("reviewExecutor")
    public void executeReviewAsync(Long reviewId) {
        logger.info("开始异步执行代码审查: {}", reviewId);

        try {
            // 0% - 开始审查
            CodeReview codeReview = getCodeReviewById(reviewId);
            updateReviewProgress(reviewId, 0, "开始代码审查");

            // 10% - 检查服务可用性
            if (!claudeQueryPort.isAvailable()) {
                throw new BusinessRuleException("Claude service is not available");
            }
            updateReviewProgress(reviewId, 10, "检查Claude服务");

            // 30% - 提取上下文（针对深度审查模式）
            logger.info("提取代码上下文: {}", reviewId);
            String contextInfo = extractContextForReview(codeReview);
            updateReviewProgress(reviewId, 30, "提取代码上下文");

            // 50% - 调用Claude进行审查
            logger.info("调用Claude进行代码审查: {}", reviewId);
            updateReviewProgress(reviewId, 50, "Claude分析中");

            ClaudeQueryResponse response = claudeQueryPort.reviewCodeChanges(
                codeReview.getCodeDiff().getDiffContent(),
                "Git代码审查项目", // 项目上下文
                codeReview.getDescription() + "\n\n" + contextInfo, // 提交信息 + 上下文
                codeReview.getStrategy().getMode().getCode()
            );

            // 80% - 解析审查结果
            updateReviewProgress(reviewId, 80, "解析审查结果");

            if (response.isSuccessful()) {
                // 使用 ReviewResultParser 解析审查结果
                ReviewResult result = reviewResultParser.parse(response.getOutput());

                // 90% - 保存结果
                updateReviewProgress(reviewId, 90, "保存审查结果");

                // 完成审查
                codeReview.completeReview();
                codeReviewRepository.save(codeReview);

                // 100% - 完成
                logger.info("异步代码审查完成: {}", reviewId);
            } else {
                markReviewAsFailed(reviewId, response.getError());
            }

        } catch (Exception e) {
            logger.error("异步审查执行失败: {}", reviewId, e);
            markReviewAsFailed(reviewId, e.getMessage());
        }
    }

    /**
     * 更新审查进度
     * @param reviewId 审查ID
     * @param progress 进度百分比
     * @param stepDescription 当前步骤描述
     */
    private void updateReviewProgress(Long reviewId, int progress, String stepDescription) {
        try {
            CodeReview codeReview = getCodeReviewById(reviewId);
            codeReview.updateProgress(progress);
            codeReviewRepository.save(codeReview);
            logger.debug("审查进度更新: {} - {}% - {}", reviewId, progress, stepDescription);
        } catch (Exception e) {
            logger.warn("更新审查进度失败: {}", reviewId, e);
            // 不抛出异常，避免影响主流程
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

            // 检查是否有工作空间ID
            String workspaceId = codeDiff.getWorkspaceId();
            if (workspaceId == null) {
                logger.warn("CodeDiff 没有关联工作空间，无法提取上下文");
                return "";
            }

            // 获取工作空间目录
            java.io.File workspaceDir = workspaceManager.getWorkspaceFile(workspaceId);
            if (workspaceDir == null || !workspaceDir.exists()) {
                logger.warn("工作空间不存在: {}", workspaceId);
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
                    // 读取文件内容
                    java.io.File file = new java.io.File(workspaceDir, fileChange.getFilePath());
                    if (!file.exists()) {
                        logger.warn("文件不存在: {}", file.getPath());
                        continue;
                    }

                    String fileContent = gitOperationPort.readFileContent(file);

                    // 从CodeDiff中提取变更的行号
                    List<Integer> changedLines = extractChangedLinesFromDiff(
                            codeDiff.getDiffContent(),
                            fileChange.getFilePath()
                    );

                    // 使用 CodeContextExtractor 提取上下文
                    com.example.gitreview.infrastructure.context.FileContext fileContext =
                        contextExtractor.extractContext(fileChange.getFilePath(), fileContent, changedLines);

                    if (!fileContext.isEmpty()) {
                        contextBuilder.append(fileContext.toPromptString());
                        contextBuilder.append("\n");
                    }

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
     * 从Git Diff中提取变更的行号
     * @param diffContent Git Diff完整内容
     * @param filePath 目标文件路径
     * @return 变更的行号列表
     */
    private List<Integer> extractChangedLinesFromDiff(String diffContent, String filePath) {
        List<Integer> changedLines = new java.util.ArrayList<>();

        if (diffContent == null || diffContent.isEmpty()) {
            return changedLines;
        }

        try {
            // 分割diff内容为行
            String[] lines = diffContent.split("\n");

            boolean inTargetFile = false;
            int currentLineNumber = 0;

            for (String line : lines) {
                // 检查是否是目标文件的diff块
                if (line.startsWith("diff --git") || line.startsWith("--- ") || line.startsWith("+++ ")) {
                    if (line.contains(filePath)) {
                        inTargetFile = true;
                    } else if (line.startsWith("diff --git")) {
                        inTargetFile = false;
                    }
                    continue;
                }

                if (!inTargetFile) {
                    continue;
                }

                // 解析 @@ 行号标记
                // 格式: @@ -45,7 +45,8 @@ 或 @@ -45 +45,8 @@
                if (line.startsWith("@@")) {
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("@@\\s+-\\d+(?:,\\d+)?\\s+\\+(\\d+)(?:,\\d+)?\\s+@@");
                    java.util.regex.Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        currentLineNumber = Integer.parseInt(matcher.group(1));
                    }
                    continue;
                }

                // 处理变更行
                if (currentLineNumber > 0) {
                    if (line.startsWith("+") && !line.startsWith("+++")) {
                        // 新增行或修改后的行
                        changedLines.add(currentLineNumber);
                        currentLineNumber++;
                    } else if (line.startsWith("-") && !line.startsWith("---")) {
                        // 删除的行，不增加行号
                        // 不添加到changedLines，因为这些行已经不存在
                    } else if (line.startsWith(" ") || (!line.startsWith("+") && !line.startsWith("-"))) {
                        // 上下文行（未变更的行）
                        currentLineNumber++;
                    }
                }
            }

            logger.debug("从Diff中提取到 {} 个变更行号: {}", changedLines.size(), filePath);

        } catch (Exception e) {
            logger.warn("解析Diff内容失败: {}", filePath, e);
        }

        return changedLines;
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