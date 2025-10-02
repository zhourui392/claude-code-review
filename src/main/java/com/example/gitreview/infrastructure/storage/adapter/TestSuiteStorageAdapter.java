package com.example.gitreview.infrastructure.storage.adapter;

import com.example.gitreview.domain.testgen.model.aggregate.TestSuite;
import com.example.gitreview.domain.testgen.model.valueobject.JavaClass;
import com.example.gitreview.domain.testgen.repository.TestSuiteRepository;
import com.example.gitreview.infrastructure.storage.json.JsonStorageAdapter;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * TestSuite持久化适配器
 * 基于JSON文件实现TestSuite的持久化操作
 */
@Component
public class TestSuiteStorageAdapter implements TestSuiteRepository {

    private static final Logger logger = LoggerFactory.getLogger(TestSuiteStorageAdapter.class);

    @Value("${json.storage.testsuite.file:data/test-suites.json}")
    private String storageFile;

    private final JsonStorageAdapter<TestSuite> storageAdapter;

    public TestSuiteStorageAdapter() {
        this.storageAdapter = new JsonStorageAdapter<>();
    }

    @PostConstruct
    public void init() {
        // 配置存储适配器
        storageAdapter.setStorageFile(storageFile);
        storageAdapter.configure(
                TestSuite.class,
                new TypeReference<List<TestSuite>>() {},
                TestSuite::getId,
                "setId"
        );
        storageAdapter.init();
        logger.info("TestSuiteStorageAdapter initialized with file: {}", storageFile);
    }

    @Override
    public TestSuite save(TestSuite testSuite) {
        logger.debug("Saving TestSuite: {}", testSuite.getId());
        return storageAdapter.save(testSuite);
    }

    @Override
    public Optional<TestSuite> findById(Long id) {
        logger.debug("Finding TestSuite by ID: {}", id);
        return storageAdapter.findById(id);
    }

    @Override
    public List<TestSuite> findByRepositoryId(Long repositoryId) {
        logger.debug("Finding TestSuites by repository ID: {}", repositoryId);
        return storageAdapter.findAll().stream()
                .filter(suite -> repositoryId.equals(suite.getRepositoryId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<TestSuite> findByRepositoryIdAndCreatedBy(Long repositoryId, String createdBy) {
        logger.debug("Finding TestSuites by repository ID: {} and createdBy: {}", repositoryId, createdBy);
        return storageAdapter.findAll().stream()
                .filter(suite -> repositoryId.equals(suite.getRepositoryId()) &&
                               createdBy.equals(suite.getCreatedBy()))
                .collect(Collectors.toList());
    }

    @Override
    public List<TestSuite> findByStatus(TestSuite.GenerationStatus status) {
        logger.debug("Finding TestSuites by status: {}", status);
        return storageAdapter.findAll().stream()
                .filter(suite -> status.equals(suite.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public List<TestSuite> findByCreatedBy(String createdBy) {
        logger.debug("Finding TestSuites by createdBy: {}", createdBy);
        return storageAdapter.findAll().stream()
                .filter(suite -> createdBy.equals(suite.getCreatedBy()))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<TestSuite> findBySuiteName(String suiteName) {
        logger.debug("Finding TestSuite by suite name: {}", suiteName);
        return storageAdapter.findAll().stream()
                .filter(suite -> suiteName.equals(suite.getSuiteName()))
                .findFirst();
    }

    @Override
    public List<TestSuite> findByTargetClass(JavaClass targetClass) {
        logger.debug("Finding TestSuites by target class: {}", targetClass);
        return storageAdapter.findAll().stream()
                .filter(suite -> targetClass.equals(suite.getTargetClass()))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<TestSuite> findByRepositoryIdAndTargetClass(Long repositoryId, JavaClass targetClass) {
        logger.debug("Finding TestSuite by repository {} and target class {}", repositoryId, targetClass);
        return storageAdapter.findAll().stream()
                .filter(suite -> repositoryId.equals(suite.getRepositoryId()) &&
                               targetClass.equals(suite.getTargetClass()))
                .findFirst();
    }

    @Override
    public List<TestSuite> findByCreateTimeBetween(LocalDateTime startTime, LocalDateTime endTime) {
        logger.debug("Finding TestSuites between {} and {}", startTime, endTime);
        return storageAdapter.findAll().stream()
                .filter(suite -> {
                    LocalDateTime createTime = suite.getCreateTime();
                    return createTime != null &&
                           !createTime.isBefore(startTime) &&
                           !createTime.isAfter(endTime);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<TestSuite> findInProgress() {
        logger.debug("Finding in-progress TestSuites");
        return storageAdapter.findAll().stream()
                .filter(suite -> suite.getStatus() == TestSuite.GenerationStatus.GENERATING ||
                               suite.getStatus() == TestSuite.GenerationStatus.VALIDATING)
                .collect(Collectors.toList());
    }

    @Override
    public List<TestSuite> findCompleted() {
        logger.debug("Finding completed TestSuites");
        return findByStatus(TestSuite.GenerationStatus.COMPLETED);
    }

    @Override
    public List<TestSuite> findRetryableFailures() {
        logger.debug("Finding retryable failed TestSuites");
        return storageAdapter.findAll().stream()
                .filter(TestSuite::canRegenerate)
                .collect(Collectors.toList());
    }

    @Override
    public List<TestSuite> findByQualityScoreRange(int minScore, int maxScore) {
        logger.debug("Finding TestSuites with quality score between {} and {}", minScore, maxScore);
        return storageAdapter.findAll().stream()
                .filter(suite -> {
                    int score = suite.getQualityScore();
                    return score >= minScore && score <= maxScore;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<TestSuite> findByCoverageRange(double minCoverage, double maxCoverage) {
        logger.debug("Finding TestSuites with coverage between {}% and {}%", minCoverage, maxCoverage);
        return storageAdapter.findAll().stream()
                .filter(suite -> {
                    double coverage = suite.getCoveragePercentage();
                    return coverage >= minCoverage && coverage <= maxCoverage;
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean deleteById(Long id) {
        logger.debug("Deleting TestSuite by ID: {}", id);
        return storageAdapter.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return storageAdapter.existsById(id);
    }

    @Override
    public boolean existsBySuiteName(String suiteName) {
        return findBySuiteName(suiteName).isPresent();
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
    public long countByStatus(TestSuite.GenerationStatus status) {
        return findByStatus(status).size();
    }

    @Override
    public long countByCreatedBy(String createdBy) {
        return findByCreatedBy(createdBy).size();
    }

    @Override
    public int updateStatusBatch(List<Long> ids, TestSuite.GenerationStatus newStatus) {
        logger.debug("Updating status to {} for {} test suites", newStatus, ids.size());
        int updateCount = 0;

        for (Long id : ids) {
            Optional<TestSuite> suiteOpt = findById(id);
            if (suiteOpt.isPresent()) {
                TestSuite suite = suiteOpt.get();
                try {
                    // 根据新状态执行相应的业务操作
                    switch (newStatus) {
                        case FAILED:
                            suite.markAsFailed("Batch operation");
                            break;
                        case PENDING:
                            suite.restart();
                            break;
                        default:
                            // 对于其他状态，直接设置可能不安全，跳过
                            continue;
                    }
                    save(suite);
                    updateCount++;
                } catch (Exception e) {
                    logger.warn("Failed to update status for test suite {}: {}", id, e.getMessage());
                }
            }
        }

        return updateCount;
    }

    @Override
    public int deleteOldRecords(LocalDateTime beforeTime) {
        logger.debug("Deleting TestSuites before {}", beforeTime);
        List<TestSuite> allSuites = storageAdapter.findAll();
        List<TestSuite> toDelete = allSuites.stream()
                .filter(suite -> suite.getCreateTime().isBefore(beforeTime))
                .collect(Collectors.toList());

        int deleteCount = 0;
        for (TestSuite suite : toDelete) {
            if (deleteById(suite.getId())) {
                deleteCount++;
            }
        }

        return deleteCount;
    }

    @Override
    public List<TestSuite> findBySuiteNameContaining(String namePattern) {
        logger.debug("Finding TestSuites containing name pattern: {}", namePattern);
        return storageAdapter.findAll().stream()
                .filter(suite -> suite.getSuiteName() != null &&
                               suite.getSuiteName().toLowerCase().contains(namePattern.toLowerCase()))
                .collect(Collectors.toList());
    }

    @Override
    public List<TestSuite> findRecentlyUpdated(int limit) {
        logger.debug("Finding {} recently updated test suites", limit);
        return storageAdapter.findAll().stream()
                .sorted(Comparator.comparing(TestSuite::getUpdateTime, Comparator.reverseOrder()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<TestSuite> findByRepositoryIdAndStatus(Long repositoryId, TestSuite.GenerationStatus status) {
        logger.debug("Finding TestSuites by repository {} and status {}", repositoryId, status);
        return storageAdapter.findAll().stream()
                .filter(suite -> repositoryId.equals(suite.getRepositoryId()) &&
                               status.equals(suite.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public List<TestSuite> findHighQualitySuites() {
        logger.debug("Finding high quality test suites (score > 80)");
        return findByQualityScoreRange(80, 100);
    }

    @Override
    public List<TestSuite> findLowCoverageSuites() {
        logger.debug("Finding low coverage test suites (coverage < 50%)");
        return findByCoverageRange(0.0, 50.0);
    }
}