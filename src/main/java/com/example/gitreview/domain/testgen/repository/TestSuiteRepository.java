package com.example.gitreview.domain.testgen.repository;

import com.example.gitreview.domain.testgen.model.aggregate.TestSuite;
import com.example.gitreview.domain.testgen.model.valueobject.JavaClass;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * TestSuiteRepository
 * 测试套件聚合根的仓储接口，定义持久化操作
 */
public interface TestSuiteRepository {

    /**
     * 保存测试套件
     * @param testSuite 测试套件聚合根
     * @return 保存后的测试套件
     */
    TestSuite save(TestSuite testSuite);

    /**
     * 根据ID查找测试套件
     * @param id 测试套件ID
     * @return 测试套件（如果存在）
     */
    Optional<TestSuite> findById(Long id);

    /**
     * 根据仓库ID查找测试套件列表
     * @param repositoryId 仓库ID
     * @return 测试套件列表
     */
    List<TestSuite> findByRepositoryId(Long repositoryId);

    /**
     * 根据仓库ID和创建者查找测试套件列表
     * @param repositoryId 仓库ID
     * @param createdBy 创建者
     * @return 测试套件列表
     */
    List<TestSuite> findByRepositoryIdAndCreatedBy(Long repositoryId, String createdBy);

    /**
     * 根据状态查找测试套件列表
     * @param status 生成状态
     * @return 测试套件列表
     */
    List<TestSuite> findByStatus(TestSuite.GenerationStatus status);

    /**
     * 根据创建者查找测试套件列表
     * @param createdBy 创建者
     * @return 测试套件列表
     */
    List<TestSuite> findByCreatedBy(String createdBy);

    /**
     * 根据套件名称查找测试套件
     * @param suiteName 套件名称
     * @return 测试套件（如果存在）
     */
    Optional<TestSuite> findBySuiteName(String suiteName);

    /**
     * 根据目标类查找测试套件列表
     * @param targetClass 目标Java类
     * @return 测试套件列表
     */
    List<TestSuite> findByTargetClass(JavaClass targetClass);

    /**
     * 根据仓库ID和目标类查找测试套件
     * @param repositoryId 仓库ID
     * @param targetClass 目标Java类
     * @return 测试套件（如果存在）
     */
    Optional<TestSuite> findByRepositoryIdAndTargetClass(Long repositoryId, JavaClass targetClass);

    /**
     * 查找指定时间范围内的测试套件
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 测试套件列表
     */
    List<TestSuite> findByCreateTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查找正在生成中的测试套件
     * @return 生成中的测试套件列表
     */
    List<TestSuite> findInProgress();

    /**
     * 查找已完成的测试套件
     * @return 已完成的测试套件列表
     */
    List<TestSuite> findCompleted();

    /**
     * 查找需要重试的失败套件
     * @return 可重试的失败套件列表
     */
    List<TestSuite> findRetryableFailures();

    /**
     * 根据质量分数范围查找测试套件
     * @param minScore 最低分数
     * @param maxScore 最高分数
     * @return 测试套件列表
     */
    List<TestSuite> findByQualityScoreRange(int minScore, int maxScore);

    /**
     * 根据覆盖率范围查找测试套件
     * @param minCoverage 最低覆盖率
     * @param maxCoverage 最高覆盖率
     * @return 测试套件列表
     */
    List<TestSuite> findByCoverageRange(double minCoverage, double maxCoverage);

    /**
     * 删除测试套件
     * @param id 测试套件ID
     * @return 是否删除成功
     */
    boolean deleteById(Long id);

    /**
     * 检查测试套件是否存在
     * @param id 测试套件ID
     * @return 是否存在
     */
    boolean existsById(Long id);

    /**
     * 检查指定名称的测试套件是否存在
     * @param suiteName 套件名称
     * @return 是否存在
     */
    boolean existsBySuiteName(String suiteName);

    /**
     * 获取测试套件总数
     * @return 总数
     */
    long count();

    /**
     * 获取指定仓库的测试套件数量
     * @param repositoryId 仓库ID
     * @return 测试套件数量
     */
    long countByRepositoryId(Long repositoryId);

    /**
     * 获取指定状态的测试套件数量
     * @param status 生成状态
     * @return 测试套件数量
     */
    long countByStatus(TestSuite.GenerationStatus status);

    /**
     * 获取指定创建者的测试套件数量
     * @param createdBy 创建者
     * @return 测试套件数量
     */
    long countByCreatedBy(String createdBy);

    /**
     * 批量更新状态
     * @param ids 测试套件ID列表
     * @param newStatus 新状态
     * @return 更新的记录数
     */
    int updateStatusBatch(List<Long> ids, TestSuite.GenerationStatus newStatus);

    /**
     * 删除指定时间之前的测试套件记录
     * @param beforeTime 时间阈值
     * @return 删除的记录数
     */
    int deleteOldRecords(LocalDateTime beforeTime);

    /**
     * 根据套件名称模糊查询
     * @param namePattern 名称模式
     * @return 匹配的测试套件列表
     */
    List<TestSuite> findBySuiteNameContaining(String namePattern);

    /**
     * 获取最近更新的测试套件
     * @param limit 限制数量
     * @return 最近更新的测试套件列表
     */
    List<TestSuite> findRecentlyUpdated(int limit);

    /**
     * 根据仓库ID和状态查找测试套件
     * @param repositoryId 仓库ID
     * @param status 生成状态
     * @return 测试套件列表
     */
    List<TestSuite> findByRepositoryIdAndStatus(Long repositoryId, TestSuite.GenerationStatus status);

    /**
     * 查找高质量的测试套件（质量分数>80）
     * @return 高质量测试套件列表
     */
    List<TestSuite> findHighQualitySuites();

    /**
     * 查找低覆盖率的测试套件（覆盖率<50%）
     * @return 低覆盖率测试套件列表
     */
    List<TestSuite> findLowCoverageSuites();
}