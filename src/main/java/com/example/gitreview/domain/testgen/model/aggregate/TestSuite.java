package com.example.gitreview.domain.testgen.model.aggregate;

import com.example.gitreview.domain.testgen.model.entity.TestCase;
import com.example.gitreview.domain.testgen.model.entity.TestMethod;
import com.example.gitreview.domain.testgen.model.valueobject.JavaClass;
import com.example.gitreview.domain.testgen.model.valueobject.TestTemplate;
import com.example.gitreview.domain.shared.exception.BusinessRuleException;
import com.example.gitreview.domain.shared.exception.ValidationException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * TestSuite聚合根
 * 测试套件的核心领域模型，管理测试用例的生成、验证和执行
 */
public class TestSuite {

    private Long id;
    private Long repositoryId;
    private String suiteName;
    private String description;
    private JavaClass targetClass;
    private TestTemplate template;
    private GenerationStatus status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String createdBy;

    // 聚合内的实体
    private final List<TestCase> testCases = new ArrayList<>();
    private TestExecutionResult executionResult;

    public enum GenerationStatus {
        PENDING,       // 待生成
        GENERATING,    // 生成中
        GENERATED,     // 已生成
        VALIDATING,    // 验证中
        VALIDATED,     // 已验证
        FAILED,        // 生成失败
        COMPLETED      // 完成
    }

    /**
     * 测试执行结果
     */
    public static class TestExecutionResult {
        private final int totalTests;
        private final int passedTests;
        private final int failedTests;
        private final int skippedTests;
        private final double coveragePercentage;
        private final String executionReport;
        private final LocalDateTime executionTime;

        public TestExecutionResult(int totalTests, int passedTests, int failedTests, int skippedTests,
                                 double coveragePercentage, String executionReport) {
            this.totalTests = totalTests;
            this.passedTests = passedTests;
            this.failedTests = failedTests;
            this.skippedTests = skippedTests;
            this.coveragePercentage = coveragePercentage;
            this.executionReport = executionReport;
            this.executionTime = LocalDateTime.now();
        }

        public int getTotalTests() { return totalTests; }
        public int getPassedTests() { return passedTests; }
        public int getFailedTests() { return failedTests; }
        public int getSkippedTests() { return skippedTests; }
        public double getCoveragePercentage() { return coveragePercentage; }
        public String getExecutionReport() { return executionReport; }
        public LocalDateTime getExecutionTime() { return executionTime; }

        public boolean allTestsPassed() { return failedTests == 0 && totalTests > 0; }
        public double getSuccessRate() { return totalTests > 0 ? (double) passedTests / totalTests * 100 : 0; }

        @Override
        public String toString() {
            return String.format("Tests: %d, Passed: %d, Failed: %d, Coverage: %.1f%%",
                               totalTests, passedTests, failedTests, coveragePercentage);
        }
    }

    // 构造函数
    protected TestSuite() {
        // JPA需要的默认构造函数
    }

    public TestSuite(Long repositoryId, String suiteName, String description,
                    JavaClass targetClass, TestTemplate template, String createdBy) {
        this.repositoryId = Objects.requireNonNull(repositoryId, "Repository ID cannot be null");
        this.suiteName = validateSuiteName(suiteName);
        this.description = description;
        this.targetClass = Objects.requireNonNull(targetClass, "Target class cannot be null");
        this.template = Objects.requireNonNull(template, "Test template cannot be null");
        this.createdBy = validateCreatedBy(createdBy);
        this.status = GenerationStatus.PENDING;
        this.createTime = LocalDateTime.now();
        this.updateTime = this.createTime;
    }

    // 业务方法

    /**
     * 开始测试生成
     */
    public void startGeneration() {
        if (status != GenerationStatus.PENDING) {
            throw new BusinessRuleException("Cannot start generation in current status: " + status);
        }

        this.status = GenerationStatus.GENERATING;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 添加测试用例
     * @param testCase 测试用例
     */
    public void addTestCase(TestCase testCase) {
        if (status != GenerationStatus.GENERATING && status != GenerationStatus.GENERATED) {
            throw new BusinessRuleException("Cannot add test case in current status: " + status);
        }

        Objects.requireNonNull(testCase, "Test case cannot be null");
        testCases.add(testCase);
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 批量添加测试用例
     * @param newTestCases 测试用例列表
     */
    public void addTestCases(List<TestCase> newTestCases) {
        Objects.requireNonNull(newTestCases, "Test cases cannot be null");
        for (TestCase testCase : newTestCases) {
            addTestCase(testCase);
        }
    }

    /**
     * 完成测试生成
     */
    public void completeGeneration() {
        if (status != GenerationStatus.GENERATING) {
            throw new BusinessRuleException("Cannot complete generation in current status: " + status);
        }

        if (testCases.isEmpty()) {
            throw new BusinessRuleException("Cannot complete generation without any test cases");
        }

        this.status = GenerationStatus.GENERATED;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 开始测试验证
     */
    public void startValidation() {
        if (status != GenerationStatus.GENERATED) {
            throw new BusinessRuleException("Cannot start validation in current status: " + status);
        }

        this.status = GenerationStatus.VALIDATING;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 完成测试验证
     * @param executionResult 执行结果
     */
    public void completeValidation(TestExecutionResult executionResult) {
        if (status != GenerationStatus.VALIDATING) {
            throw new BusinessRuleException("Cannot complete validation in current status: " + status);
        }

        this.executionResult = Objects.requireNonNull(executionResult, "Execution result cannot be null");
        this.status = GenerationStatus.VALIDATED;
        this.updateTime = LocalDateTime.now();

        // 如果所有测试通过，标记为完成
        if (executionResult.allTestsPassed()) {
            this.status = GenerationStatus.COMPLETED;
        }
    }

    /**
     * 标记为失败
     * @param reason 失败原因
     */
    public void markAsFailed(String reason) {
        this.status = GenerationStatus.FAILED;
        this.updateTime = LocalDateTime.now();

        // 可以将失败原因记录到执行结果中
        if (reason != null && !reason.trim().isEmpty()) {
            this.executionResult = new TestExecutionResult(0, 0, 0, 0, 0.0, "Generation failed: " + reason);
        }
    }

    /**
     * 重新开始生成
     */
    public void restart() {
        if (status == GenerationStatus.COMPLETED) {
            throw new BusinessRuleException("Cannot restart completed test suite");
        }

        this.status = GenerationStatus.PENDING;
        this.updateTime = LocalDateTime.now();
        this.executionResult = null;
        this.testCases.clear();
    }

    /**
     * 更新测试套件信息
     * @param suiteName 新名称
     * @param description 新描述
     */
    public void updateInfo(String suiteName, String description) {
        if (status == GenerationStatus.COMPLETED) {
            throw new BusinessRuleException("Cannot update completed test suite");
        }

        this.suiteName = validateSuiteName(suiteName);
        this.description = description;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 更新测试模板
     * @param newTemplate 新的测试模板
     */
    public void updateTemplate(TestTemplate newTemplate) {
        if (status == GenerationStatus.GENERATING || status == GenerationStatus.VALIDATING) {
            throw new BusinessRuleException("Cannot update template while processing");
        }

        this.template = Objects.requireNonNull(newTemplate, "Test template cannot be null");
        this.updateTime = LocalDateTime.now();

        // 如果已经有测试用例，需要重新生成
        if (!testCases.isEmpty() && status != GenerationStatus.PENDING) {
            this.status = GenerationStatus.PENDING;
            this.testCases.clear();
            this.executionResult = null;
        }
    }

    /**
     * 获取测试生成进度
     * @return 进度百分比 (0-100)
     */
    public int getProgress() {
        switch (status) {
            case PENDING:
                return 0;
            case GENERATING:
                return 25;
            case GENERATED:
                return 50;
            case VALIDATING:
                return 75;
            case VALIDATED:
            case COMPLETED:
                return 100;
            case FAILED:
                return 0;
            default:
                return 0;
        }
    }

    /**
     * 获取测试覆盖率
     * @return 覆盖率百分比
     */
    public double getCoveragePercentage() {
        if (executionResult != null) {
            return executionResult.getCoveragePercentage();
        }

        // 基于目标类的方法数量估算覆盖率
        int targetMethods = targetClass.getMethodCount();
        int testMethods = getTotalTestMethods();

        if (targetMethods == 0) {
            return 0.0;
        }

        return Math.min(100.0, (double) testMethods / targetMethods * 100);
    }

    /**
     * 获取测试方法总数
     * @return 测试方法总数
     */
    public int getTotalTestMethods() {
        return testCases.stream()
                .mapToInt(testCase -> testCase.getTestMethods().size())
                .sum();
    }

    /**
     * 检查是否可以执行测试
     * @return 是否可以执行
     */
    public boolean canExecuteTests() {
        return status == GenerationStatus.GENERATED || status == GenerationStatus.VALIDATED;
    }

    /**
     * 检查是否可以重新生成
     * @return 是否可以重新生成
     */
    public boolean canRegenerate() {
        return status == GenerationStatus.FAILED ||
               (status == GenerationStatus.VALIDATED && !executionResult.allTestsPassed());
    }

    /**
     * 获取测试质量分数
     * @return 质量分数 (0-100)
     */
    public int getQualityScore() {
        if (executionResult == null || status != GenerationStatus.COMPLETED) {
            return 0;
        }

        // 基于成功率和覆盖率计算质量分数
        double successRate = executionResult.getSuccessRate();
        double coverageRate = getCoveragePercentage();

        return (int) Math.round((successRate * 0.7 + coverageRate * 0.3));
    }

    /**
     * 生成测试套件摘要
     * @return 测试套件摘要
     */
    public String generateSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("测试套件: ").append(suiteName).append("\n");
        summary.append("目标类: ").append(targetClass.getFullName()).append("\n");
        summary.append("状态: ").append(getStatusDescription()).append("\n");
        summary.append("测试用例数: ").append(testCases.size()).append("\n");
        summary.append("测试方法数: ").append(getTotalTestMethods()).append("\n");

        if (executionResult != null) {
            summary.append("执行结果: ").append(executionResult.toString()).append("\n");
        }

        summary.append("质量分数: ").append(getQualityScore()).append("/100");

        return summary.toString();
    }

    /**
     * 获取状态描述
     * @return 状态描述
     */
    public String getStatusDescription() {
        switch (status) {
            case PENDING:
                return "等待生成";
            case GENERATING:
                return "生成中";
            case GENERATED:
                return "已生成";
            case VALIDATING:
                return "验证中";
            case VALIDATED:
                return "已验证";
            case FAILED:
                return "生成失败";
            case COMPLETED:
                return "已完成";
            default:
                return "未知状态";
        }
    }

    /**
     * 获取特定类型的测试用例
     * @param testType 测试类型
     * @return 测试用例列表
     */
    public List<TestCase> getTestCasesByType(TestCase.TestType testType) {
        return testCases.stream()
                .filter(testCase -> testCase.getTestType() == testType)
                .collect(Collectors.toList());
    }

    // 私有验证方法
    private String validateSuiteName(String suiteName) {
        if (suiteName == null || suiteName.trim().isEmpty()) {
            throw new ValidationException("Suite name cannot be null or empty");
        }
        if (suiteName.length() > 100) {
            throw new ValidationException("Suite name cannot exceed 100 characters");
        }
        return suiteName.trim();
    }

    private String validateCreatedBy(String createdBy) {
        if (createdBy == null || createdBy.trim().isEmpty()) {
            throw new ValidationException("CreatedBy cannot be null or empty");
        }
        return createdBy.trim();
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Long getRepositoryId() {
        return repositoryId;
    }

    public String getSuiteName() {
        return suiteName;
    }

    public String getDescription() {
        return description;
    }

    public JavaClass getTargetClass() {
        return targetClass;
    }

    public TestTemplate getTemplate() {
        return template;
    }

    public GenerationStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public List<TestCase> getTestCases() {
        return new ArrayList<>(testCases);
    }

    public TestExecutionResult getExecutionResult() {
        return executionResult;
    }

    // 用于持久化的setter（仅限基础设施层使用）
    public void setId(Long id) {
        this.id = id;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public void setStatus(GenerationStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestSuite testSuite = (TestSuite) o;
        return Objects.equals(id, testSuite.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "TestSuite{" +
                "id=" + id +
                ", suiteName='" + suiteName + '\'' +
                ", targetClass=" + targetClass.getSimpleName() +
                ", status=" + status +
                ", testCases=" + testCases.size() +
                ", quality=" + getQualityScore() +
                '}';
    }
}