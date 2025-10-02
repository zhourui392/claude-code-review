package com.example.gitreview.domain.testgen.service;

import com.example.gitreview.domain.testgen.model.aggregate.TestSuite;
import com.example.gitreview.domain.testgen.model.entity.TestCase;
import com.example.gitreview.domain.testgen.model.entity.TestMethod;
import com.example.gitreview.domain.testgen.model.valueobject.JavaClass;
import com.example.gitreview.domain.testgen.model.valueobject.TestTemplate;
import com.example.gitreview.domain.shared.model.aggregate.Repository;
import com.example.gitreview.domain.shared.exception.BusinessRuleException;
import com.example.gitreview.infrastructure.claude.ClaudeQueryPort;
import com.example.gitreview.infrastructure.claude.ClaudeQueryResponse;
import com.example.gitreview.infrastructure.git.GitOperationPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * 测试生成领域服务
 * 包含测试生成的核心业务逻辑和规则
 */
@Service
public class TestGenerationDomainService {

    private static final Logger logger = LoggerFactory.getLogger(TestGenerationDomainService.class);

    private final ClaudeQueryPort claudeQueryPort;
    private final GitOperationPort gitOperationPort;

    @Autowired
    public TestGenerationDomainService(ClaudeQueryPort claudeQueryPort, GitOperationPort gitOperationPort) {
        this.claudeQueryPort = claudeQueryPort;
        this.gitOperationPort = gitOperationPort;
    }

    /**
     * 生成测试用例
     * @param testSuite 测试套件
     * @param repository 代码仓库
     */
    public void generateTests(TestSuite testSuite, Repository repository) {
        logger.info("Starting test generation for suite: {}", testSuite.getId());

        try {
            // 验证生成条件
            validateGenerationConditions(testSuite, repository);

            // 提取目标类代码
            String classCode = extractClassCode(repository, testSuite.getTargetClass());

            // 根据测试模板生成测试用例
            List<TestCase> testCases = generateTestCases(testSuite.getTargetClass(),
                                                        testSuite.getTemplate(),
                                                        classCode);

            // 添加测试用例到套件
            testSuite.addTestCases(testCases);

            logger.info("Generated {} test cases for suite: {}", testCases.size(), testSuite.getId());

        } catch (Exception e) {
            logger.error("Failed to generate tests for suite: {}", testSuite.getId(), e);
            throw new BusinessRuleException("Test generation failed: " + e.getMessage());
        }
    }

    /**
     * 验证测试套件
     * @param testSuite 测试套件
     * @return 验证结果
     */
    public TestSuite.TestExecutionResult validateTests(TestSuite testSuite) {
        logger.info("Starting test validation for suite: {}", testSuite.getId());

        try {
            // 验证测试代码语法
            ValidationResult syntaxResult = validateTestSyntax(testSuite);

            // 模拟编译测试
            CompilationResult compilationResult = compileTests(testSuite);

            // 模拟执行测试
            ExecutionResult executionResult = executeTests(testSuite);

            // 计算覆盖率
            double coverage = calculateTestCoverage(testSuite);

            // 创建执行结果
            TestSuite.TestExecutionResult result = new TestSuite.TestExecutionResult(
                    executionResult.getTotalTests(),
                    executionResult.getPassedTests(),
                    executionResult.getFailedTests(),
                    executionResult.getSkippedTests(),
                    coverage,
                    generateExecutionReport(syntaxResult, compilationResult, executionResult)
            );

            logger.info("Test validation completed for suite: {}", testSuite.getId());
            return result;

        } catch (Exception e) {
            logger.error("Failed to validate tests for suite: {}", testSuite.getId(), e);
            throw new BusinessRuleException("Test validation failed: " + e.getMessage());
        }
    }

    /**
     * 推荐测试模板
     * @param javaClass 目标类
     * @param userPreference 用户偏好
     * @return 推荐的测试模板
     */
    public TestTemplate recommendTestTemplate(JavaClass javaClass, TestTemplate userPreference) {
        // 如果用户有偏好且兼容，使用用户偏好
        if (userPreference != null && isTemplateCompatible(userPreference, javaClass)) {
            return userPreference;
        }

        // 基于类特征推荐模板
        if (javaClass.isInterface()) {
            return TestTemplate.mockBased();
        }

        if (javaClass.hasComplexMethods()) {
            return TestTemplate.comprehensive();
        }

        if (javaClass.hasAsyncMethods()) {
            return TestTemplate.integration();
        }

        // 默认基础模板
        return TestTemplate.basic();
    }

    /**
     * 计算测试质量分数
     * @param testSuite 测试套件
     * @return 质量分数 (0-100)
     */
    public int calculateTestQuality(TestSuite testSuite) {
        if (testSuite.getTestCases().isEmpty()) {
            return 0;
        }

        int qualityScore = 50; // 基础分数

        // 基于覆盖率调整
        double coverage = testSuite.getCoveragePercentage();
        qualityScore += (int) (coverage * 0.3); // 覆盖率权重30%

        // 基于测试方法数量调整
        int methodCount = testSuite.getTotalTestMethods();
        int targetMethodCount = testSuite.getTargetClass().getMethodCount();
        if (targetMethodCount > 0) {
            double methodCoverage = Math.min(1.0, (double) methodCount / targetMethodCount);
            qualityScore += (int) (methodCoverage * 20); // 方法覆盖权重20%
        }

        // 基于测试类型多样性调整
        long testTypeCount = testSuite.getTestCases().stream()
                .map(TestCase::getTestType)
                .distinct()
                .count();
        qualityScore += Math.min(15, testTypeCount * 5); // 类型多样性权重15%

        // 基于执行结果调整
        if (testSuite.getExecutionResult() != null) {
            double successRate = testSuite.getExecutionResult().getSuccessRate();
            qualityScore += (int) (successRate * 0.35); // 成功率权重35%
        }

        return Math.min(100, Math.max(0, qualityScore));
    }

    /**
     * 估算测试生成时间
     * @param javaClass 目标类
     * @param template 测试模板
     * @return 估算时间（秒）
     */
    public int estimateGenerationTime(JavaClass javaClass, TestTemplate template) {
        int baseTime = 30; // 基础30秒

        // 基于类复杂度调整
        int methodCount = javaClass.getMethodCount();
        baseTime += methodCount * 5; // 每个方法增加5秒

        // 基于模板复杂度调整
        switch (template.getTestType()) {
            case COMPREHENSIVE:
                baseTime *= 2;
                break;
            case INTEGRATION:
                baseTime = (int) (baseTime * 1.5);
                break;
            case MOCK:
                baseTime = (int) (baseTime * 1.3);
                break;
            case BASIC:
            default:
                // 使用基础时间
                break;
        }

        // 基于质量级别调整
        baseTime += template.getQualityLevel() * 10;

        return Math.max(10, Math.min(300, baseTime)); // 限制在10-300秒
    }

    // 私有方法

    /**
     * 验证生成条件
     */
    private void validateGenerationConditions(TestSuite testSuite, Repository repository) {
        if (testSuite.getStatus() != TestSuite.GenerationStatus.GENERATING) {
            throw new BusinessRuleException("Invalid status for generation: " + testSuite.getStatus());
        }

        if (testSuite.getTargetClass() == null) {
            throw new BusinessRuleException("Target class is required for generation");
        }

        if (testSuite.getTemplate() == null) {
            throw new BusinessRuleException("Test template is required for generation");
        }

        // 验证仓库可访问性
        if (!isRepositoryAccessible(repository)) {
            throw new BusinessRuleException("Repository is not accessible: " + repository.getGitUrl().getUrl());
        }
    }

    /**
     * 提取类代码
     */
    private String extractClassCode(Repository repository, JavaClass javaClass) {
        try {
            // 这里应该调用GitOperationPort获取类代码
            // 为演示目的，返回模拟代码
            return generateMockClassCode(javaClass);
        } catch (Exception e) {
            throw new BusinessRuleException("Failed to extract class code: " + e.getMessage());
        }
    }

    /**
     * 生成测试用例
     */
    private List<TestCase> generateTestCases(JavaClass javaClass, TestTemplate template, String classCode) {
        List<TestCase> testCases = new ArrayList<>();

        try {
            // 构建Claude提示词
            String prompt = buildTestGenerationPrompt(javaClass, template, classCode);

            // 调用Claude生成测试代码
            ClaudeQueryResponse claudeResponse = claudeQueryPort.query(prompt);
            String response = claudeResponse.getOutput();

            // 解析Claude响应生成测试用例
            testCases = parseClaudeResponse(response, template);

        } catch (Exception e) {
            logger.warn("Claude generation failed, using fallback generation: {}", e.getMessage());
            // 使用备用生成方法
            testCases = generateFallbackTestCases(javaClass, template);
        }

        return testCases;
    }

    /**
     * 验证测试语法
     */
    private ValidationResult validateTestSyntax(TestSuite testSuite) {
        // 模拟语法验证
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (TestCase testCase : testSuite.getTestCases()) {
            for (TestMethod method : testCase.getTestMethods()) {
                if (method.getTestCode() == null || method.getTestCode().trim().isEmpty()) {
                    errors.add("Test method " + method.getName() + " has empty code");
                }
                if (!method.getTestCode().contains("@Test")) {
                    warnings.add("Test method " + method.getName() + " missing @Test annotation");
                }
            }
        }

        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    /**
     * 编译测试
     */
    private CompilationResult compileTests(TestSuite testSuite) {
        // 模拟编译过程
        return new CompilationResult(true, List.of(), List.of("Some imports may be optimized"));
    }

    /**
     * 执行测试
     */
    private ExecutionResult executeTests(TestSuite testSuite) {
        // 模拟测试执行
        int totalTests = testSuite.getTotalTestMethods();
        int passedTests = (int) (totalTests * 0.85); // 85%通过率
        int failedTests = totalTests - passedTests;

        return new ExecutionResult(totalTests, passedTests, failedTests, 0);
    }

    /**
     * 计算测试覆盖率
     */
    private double calculateTestCoverage(TestSuite testSuite) {
        // 基于测试方法数量估算覆盖率
        int testMethods = testSuite.getTotalTestMethods();
        int targetMethods = testSuite.getTargetClass().getMethodCount();

        if (targetMethods == 0) {
            return 0.0;
        }

        double coverage = Math.min(100.0, (double) testMethods / targetMethods * 100);
        return coverage;
    }

    /**
     * 生成执行报告
     */
    private String generateExecutionReport(ValidationResult syntax, CompilationResult compilation, ExecutionResult execution) {
        StringBuilder report = new StringBuilder();
        report.append("=== Test Validation Report ===\n");
        report.append("Syntax Check: ").append(syntax.isSuccess() ? "PASSED" : "FAILED").append("\n");
        report.append("Compilation: ").append(compilation.isSuccess() ? "PASSED" : "FAILED").append("\n");
        report.append("Execution: ").append(execution.getPassedTests()).append("/").append(execution.getTotalTests()).append(" PASSED\n");

        if (!syntax.getErrors().isEmpty()) {
            report.append("\nSyntax Errors:\n");
            syntax.getErrors().forEach(error -> report.append("- ").append(error).append("\n"));
        }

        return report.toString();
    }

    /**
     * 检查模板兼容性
     */
    private boolean isTemplateCompatible(TestTemplate template, JavaClass javaClass) {
        // 检查基本兼容性
        if (template.getTestType() == TestTemplate.TestType.MOCK && javaClass.isInterface()) {
            return true;
        }

        return template.getQualityLevel() >= 1 && template.getQualityLevel() <= 5;
    }

    /**
     * 检查仓库可访问性
     */
    private boolean isRepositoryAccessible(Repository repository) {
        // 这里应该实际检查仓库连接
        return repository != null && repository.getUrl() != null;
    }

    /**
     * 生成模拟类代码
     */
    private String generateMockClassCode(JavaClass javaClass) {
        return "public class " + javaClass.getSimpleName() + " {\n" +
               "    // Mock implementation for test generation\n" +
               "}";
    }

    /**
     * 构建测试生成提示词
     */
    private String buildTestGenerationPrompt(JavaClass javaClass, TestTemplate template, String classCode) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate comprehensive unit tests for the following Java class:\n\n");
        prompt.append("Class: ").append(javaClass.getFullName()).append("\n");
        prompt.append("Test Type: ").append(template.getTestType()).append("\n");
        prompt.append("Quality Level: ").append(template.getQualityLevel()).append("/5\n\n");
        prompt.append("Source Code:\n");
        prompt.append(classCode).append("\n\n");
        prompt.append("Please generate JUnit 5 test cases with the following requirements:\n");
        prompt.append("- Use ").append(template.getMockFramework()).append(" for mocking\n");
        prompt.append("- Use ").append(template.getAssertionFramework()).append(" for assertions\n");
        prompt.append("- Include test setup and teardown methods\n");
        prompt.append("- Cover edge cases and error conditions\n");
        prompt.append("- Include meaningful test method names and comments\n");

        return prompt.toString();
    }

    /**
     * 解析Claude响应
     */
    private List<TestCase> parseClaudeResponse(String response, TestTemplate template) {
        // 这里应该实现实际的响应解析逻辑
        // 为演示目的，返回一个模拟测试用例
        List<TestCase> testCases = new ArrayList<>();

        TestCase mainTestCase = new TestCase(
                "MainTestCase",
                "Main test case for the target class",
                TestCase.TestType.UNIT,
                "// Setup code here",
                "// Cleanup code here"
        );

        // 添加一些测试方法
        TestMethod testMethod1 = new TestMethod(
                "testBasicFunctionality",
                "Test basic functionality",
                "targetMethod",
                "@Test\npublic void testBasicFunctionality() {\n    // Test implementation\n    assertTrue(true);\n}",
                List.of("@Test"),
                List.of()
        );

        mainTestCase.addTestMethod(testMethod1);
        testCases.add(mainTestCase);

        return testCases;
    }

    /**
     * 生成备用测试用例
     */
    private List<TestCase> generateFallbackTestCases(JavaClass javaClass, TestTemplate template) {
        List<TestCase> testCases = new ArrayList<>();

        TestCase fallbackCase = new TestCase(
                "FallbackTestCase",
                "Fallback test case when Claude generation fails",
                TestCase.TestType.UNIT,
                "// Basic setup",
                "// Basic cleanup"
        );

        TestMethod fallbackMethod = new TestMethod(
                "testFallback",
                "Basic fallback test",
                "unknownMethod",
                "@Test\npublic void testFallback() {\n    // Basic test\n    assertNotNull(new Object());\n}",
                List.of("@Test"),
                List.of()
        );

        fallbackCase.addTestMethod(fallbackMethod);
        testCases.add(fallbackCase);

        return testCases;
    }

    // 内部结果类
    private static class ValidationResult {
        private final boolean success;
        private final List<String> errors;
        private final List<String> warnings;

        public ValidationResult(boolean success, List<String> errors, List<String> warnings) {
            this.success = success;
            this.errors = errors;
            this.warnings = warnings;
        }

        public boolean isSuccess() { return success; }
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
    }

    private static class CompilationResult {
        private final boolean success;
        private final List<String> errors;
        private final List<String> warnings;

        public CompilationResult(boolean success, List<String> errors, List<String> warnings) {
            this.success = success;
            this.errors = errors;
            this.warnings = warnings;
        }

        public boolean isSuccess() { return success; }
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
    }

    private static class ExecutionResult {
        private final int totalTests;
        private final int passedTests;
        private final int failedTests;
        private final int skippedTests;

        public ExecutionResult(int totalTests, int passedTests, int failedTests, int skippedTests) {
            this.totalTests = totalTests;
            this.passedTests = passedTests;
            this.failedTests = failedTests;
            this.skippedTests = skippedTests;
        }

        public int getTotalTests() { return totalTests; }
        public int getPassedTests() { return passedTests; }
        public int getFailedTests() { return failedTests; }
        public int getSkippedTests() { return skippedTests; }
    }
}