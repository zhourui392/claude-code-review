package com.example.gitreview.domain.codereview.repository;

import com.example.gitreview.domain.codereview.model.aggregate.CodeReview;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * CodeReviewRepository
 * 代码审查仓储接口，定义持久化操作
 */
public interface CodeReviewRepository {

    /**
     * 保存代码审查
     * @param codeReview 代码审查聚合根
     * @return 保存后的代码审查
     */
    CodeReview save(CodeReview codeReview);

    /**
     * 根据ID查找代码审查
     * @param id 代码审查ID
     * @return 代码审查（如果存在）
     */
    Optional<CodeReview> findById(Long id);

    /**
     * 根据仓库ID查找代码审查列表
     * @param repositoryId 仓库ID
     * @return 代码审查列表
     */
    List<CodeReview> findByRepositoryId(Long repositoryId);

    /**
     * 根据状态查找代码审查列表
     * @param status 审查状态
     * @return 代码审查列表
     */
    List<CodeReview> findByStatus(CodeReview.ReviewStatus status);

    /**
     * 根据创建者查找代码审查列表
     * @param createdBy 创建者
     * @return 代码审查列表
     */
    List<CodeReview> findByCreatedBy(String createdBy);

    /**
     * 查找指定时间范围内的代码审查
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 代码审查列表
     */
    List<CodeReview> findByCreateTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查找正在进行中的代码审查
     * @return 进行中的代码审查列表
     */
    List<CodeReview> findInProgress();

    /**
     * 查找需要重试的失败审查
     * @return 可重试的失败审查列表
     */
    List<CodeReview> findRetryableFailures();

    /**
     * 根据仓库ID和分支查找代码审查
     * @param repositoryId 仓库ID
     * @param baseBranch 基础分支
     * @param targetBranch 目标分支
     * @return 代码审查列表
     */
    List<CodeReview> findByRepositoryAndBranches(Long repositoryId, String baseBranch, String targetBranch);

    /**
     * 删除代码审查
     * @param id 代码审查ID
     * @return 是否删除成功
     */
    boolean deleteById(Long id);

    /**
     * 检查代码审查是否存在
     * @param id 代码审查ID
     * @return 是否存在
     */
    boolean existsById(Long id);

    /**
     * 获取代码审查总数
     * @return 总数
     */
    long count();

    /**
     * 获取指定仓库的代码审查数量
     * @param repositoryId 仓库ID
     * @return 代码审查数量
     */
    long countByRepositoryId(Long repositoryId);

    /**
     * 获取指定状态的代码审查数量
     * @param status 审查状态
     * @return 代码审查数量
     */
    long countByStatus(CodeReview.ReviewStatus status);

    /**
     * 批量更新状态
     * @param ids 代码审查ID列表
     * @param newStatus 新状态
     * @return 更新的记录数
     */
    int updateStatusBatch(List<Long> ids, CodeReview.ReviewStatus newStatus);

    /**
     * 删除指定时间之前的代码审查记录
     * @param beforeTime 时间阈值
     * @return 删除的记录数
     */
    int deleteOldRecords(LocalDateTime beforeTime);
}