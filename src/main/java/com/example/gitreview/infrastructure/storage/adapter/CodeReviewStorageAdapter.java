package com.example.gitreview.infrastructure.storage.adapter;

import com.example.gitreview.domain.codereview.model.aggregate.CodeReview;
import com.example.gitreview.domain.codereview.repository.CodeReviewRepository;
import com.example.gitreview.infrastructure.storage.json.JsonStorageAdapter;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * CodeReview持久化适配器
 * 基于JSON文件实现CodeReview的持久化操作
 */
@Component
public class CodeReviewStorageAdapter implements CodeReviewRepository {

    private static final Logger logger = LoggerFactory.getLogger(CodeReviewStorageAdapter.class);

    @Value("${json.storage.codereview.file:data/code-reviews.json}")
    private String storageFile;

    private final JsonStorageAdapter<CodeReview> storageAdapter;

    public CodeReviewStorageAdapter() {
        this.storageAdapter = new JsonStorageAdapter<>();
    }

    @PostConstruct
    public void init() {
        // 配置存储适配器
        storageAdapter.setStorageFile(storageFile);
        storageAdapter.configure(
                CodeReview.class,
                new TypeReference<List<CodeReview>>() {},
                CodeReview::getId,
                "setId"
        );
        storageAdapter.init();
        logger.info("CodeReviewStorageAdapter initialized with file: {}", storageFile);
    }

    @Override
    public CodeReview save(CodeReview codeReview) {
        logger.debug("Saving CodeReview: {}", codeReview.getId());
        return storageAdapter.save(codeReview);
    }

    @Override
    public Optional<CodeReview> findById(Long id) {
        logger.debug("Finding CodeReview by ID: {}", id);
        return storageAdapter.findById(id);
    }

    @Override
    public List<CodeReview> findByRepositoryId(Long repositoryId) {
        logger.debug("Finding CodeReviews by repository ID: {}", repositoryId);
        return storageAdapter.findAll().stream()
                .filter(review -> repositoryId.equals(review.getRepositoryId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<CodeReview> findByStatus(CodeReview.ReviewStatus status) {
        logger.debug("Finding CodeReviews by status: {}", status);
        return storageAdapter.findAll().stream()
                .filter(review -> status.equals(review.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public List<CodeReview> findByCreatedBy(String createdBy) {
        logger.debug("Finding CodeReviews by createdBy: {}", createdBy);
        return storageAdapter.findAll().stream()
                .filter(review -> createdBy.equals(review.getCreatedBy()))
                .collect(Collectors.toList());
    }

    @Override
    public List<CodeReview> findByCreateTimeBetween(LocalDateTime startTime, LocalDateTime endTime) {
        logger.debug("Finding CodeReviews between {} and {}", startTime, endTime);
        return storageAdapter.findAll().stream()
                .filter(review -> {
                    LocalDateTime createTime = review.getCreateTime();
                    return createTime != null &&
                           !createTime.isBefore(startTime) &&
                           !createTime.isAfter(endTime);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<CodeReview> findInProgress() {
        logger.debug("Finding in-progress CodeReviews");
        return findByStatus(CodeReview.ReviewStatus.IN_PROGRESS);
    }

    @Override
    public List<CodeReview> findRetryableFailures() {
        logger.debug("Finding retryable failed CodeReviews");
        return storageAdapter.findAll().stream()
                .filter(CodeReview::canRetry)
                .collect(Collectors.toList());
    }

    @Override
    public List<CodeReview> findByRepositoryAndBranches(Long repositoryId, String baseBranch, String targetBranch) {
        logger.debug("Finding CodeReviews by repository {} and branches {} -> {}",
                    repositoryId, baseBranch, targetBranch);
        return storageAdapter.findAll().stream()
                .filter(review -> repositoryId.equals(review.getRepositoryId()))
                .filter(review -> {
                    // 这里需要检查CodeDiff中的分支信息
                    // 暂时简化处理，实际项目中可能需要更复杂的查询
                    return review.getCodeDiff() != null;
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean deleteById(Long id) {
        logger.debug("Deleting CodeReview by ID: {}", id);
        return storageAdapter.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return storageAdapter.existsById(id);
    }

    @Override
    public long count() {
        return storageAdapter.count();
    }

    @Override
    public long countByRepositoryId(Long repositoryId) {
        return findByRepositoryId(repositoryId).size();
    }

    @Override
    public long countByStatus(CodeReview.ReviewStatus status) {
        return findByStatus(status).size();
    }

    @Override
    public int updateStatusBatch(List<Long> ids, CodeReview.ReviewStatus newStatus) {
        logger.debug("Updating status to {} for {} reviews", newStatus, ids.size());
        int updateCount = 0;

        for (Long id : ids) {
            Optional<CodeReview> reviewOpt = findById(id);
            if (reviewOpt.isPresent()) {
                CodeReview review = reviewOpt.get();
                try {
                    // 根据新状态执行相应的业务操作
                    switch (newStatus) {
                        case CANCELLED:
                            review.cancel("Batch operation");
                            break;
                        case FAILED:
                            review.markAsFailed("Batch operation");
                            break;
                        case PENDING:
                            review.restart();
                            break;
                        default:
                            // 对于其他状态，直接设置可能不安全，跳过
                            continue;
                    }
                    save(review);
                    updateCount++;
                } catch (Exception e) {
                    logger.warn("Failed to update status for review {}: {}", id, e.getMessage());
                }
            }
        }

        return updateCount;
    }

    @Override
    public int deleteOldRecords(LocalDateTime beforeTime) {
        logger.debug("Deleting CodeReviews before {}", beforeTime);
        List<CodeReview> allReviews = storageAdapter.findAll();
        List<CodeReview> toDelete = allReviews.stream()
                .filter(review -> review.getCreateTime().isBefore(beforeTime))
                .collect(Collectors.toList());

        int deleteCount = 0;
        for (CodeReview review : toDelete) {
            if (deleteById(review.getId())) {
                deleteCount++;
            }
        }

        return deleteCount;
    }
}