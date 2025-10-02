package com.example.gitreview.application.testgen.assembler;

import com.example.gitreview.application.testgen.dto.*;
import com.example.gitreview.domain.testgen.model.aggregate.TestSuite;
import com.example.gitreview.domain.testgen.model.entity.TestCase;
import com.example.gitreview.domain.testgen.model.entity.TestMethod;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 测试生成Assembler
 * 负责领域对象与DTO之间的转换
 */
@Component
public class TestGenerationAssembler {

    /**
     * 转换为测试状态DTO
     * @param testSuite 测试套件聚合根
     * @param taskId 任务ID
     * @return 测试状态DTO
     */
    public TestStatusDTO toTestStatusDTO(TestSuite testSuite, String taskId) {
        TestStatusDTO dto = new TestStatusDTO();

        dto.setTaskId(taskId);
        dto.setSuiteId(testSuite.getId());
        dto.setStatus(testSuite.getStatus().name());
        dto.setStatusDescription(testSuite.getStatusDescription());
        dto.setProgress(testSuite.getProgress());
        dto.setRepositoryId(testSuite.getRepositoryId());
        dto.setClassName(testSuite.getTargetClass().getSimpleName());
        dto.setSuiteName(testSuite.getSuiteName());
        dto.setCreateTime(testSuite.getCreateTime());
        dto.setUpdateTime(testSuite.getUpdateTime());
        dto.setTestCaseCount(testSuite.getTestCases().size());
        dto.setTestMethodCount(testSuite.getTotalTestMethods());
        dto.setCoveragePercentage(testSuite.getCoveragePercentage());
        dto.setQualityScore(testSuite.getQualityScore());
        dto.setCanRegenerate(testSuite.canRegenerate());
        dto.setCanExecute(testSuite.canExecuteTests());

        // 设置执行结果
        if (testSuite.getExecutionResult() != null) {
            dto.setExecutionResult(toTestExecutionResultDTO(testSuite.getExecutionResult()));
        }

        // 设置消息
        dto.setMessage(generateStatusMessage(testSuite));

        return dto;
    }

    /**
     * 转换为测试套件DTO
     * @param testSuite 测试套件聚合根
     * @return 测试套件DTO
     */
    public TestSuiteDTO toTestSuiteDTO(TestSuite testSuite) {
        TestSuiteDTO dto = new TestSuiteDTO();

        dto.setId(testSuite.getId());
        dto.setRepositoryId(testSuite.getRepositoryId());
        dto.setSuiteName(testSuite.getSuiteName());
        dto.setDescription(testSuite.getDescription());
        dto.setClassName(testSuite.getTargetClass().getSimpleName());
        dto.setPackageName(testSuite.getTargetClass().getPackageName());
        dto.setStatus(testSuite.getStatus().name());
        dto.setStatusDescription(testSuite.getStatusDescription());
        dto.setProgress(testSuite.getProgress());
        dto.setCreateTime(testSuite.getCreateTime());
        dto.setUpdateTime(testSuite.getUpdateTime());
        dto.setCreatedBy(testSuite.getCreatedBy());
        dto.setTestCaseCount(testSuite.getTestCases().size());
        dto.setTestMethodCount(testSuite.getTotalTestMethods());
        dto.setCoveragePercentage(testSuite.getCoveragePercentage());
        dto.setQualityScore(testSuite.getQualityScore());

        // 设置测试模板信息
        if (testSuite.getTemplate() != null) {
            dto.setTestType(testSuite.getTemplate().getTestType().name().toLowerCase());
            dto.setQualityLevel(testSuite.getTemplate().getQualityLevel());
            dto.setMockFramework(testSuite.getTemplate().getMockFramework());
            dto.setAssertionFramework(testSuite.getTemplate().getAssertionFramework());
        }

        dto.setHasExecutionResult(testSuite.getExecutionResult() != null);
        dto.setCanRegenerate(testSuite.canRegenerate());
        dto.setCanExecute(testSuite.canExecuteTests());

        return dto;
    }

    /**
     * 转换为测试生成结果DTO
     * @param testSuite 测试套件聚合根
     * @param taskId 任务ID
     * @return 测试生成结果DTO
     */
    public TestGenerationResultDTO toTestGenerationResultDTO(TestSuite testSuite, String taskId) {
        TestGenerationResultDTO dto = new TestGenerationResultDTO();

        dto.setTaskId(taskId);
        dto.setSuiteId(testSuite.getId());
        dto.setSuiteName(testSuite.getSuiteName());
        dto.setStatus(testSuite.getStatus().name());
        dto.setClassName(testSuite.getTargetClass().getSimpleName());
        dto.setPackageName(testSuite.getTargetClass().getPackageName());
        dto.setGenerateTime(testSuite.getUpdateTime());

        // 转换测试用例
        List<TestGenerationResultDTO.TestCaseDTO> testCaseDTOs = testSuite.getTestCases().stream()
                .map(this::toTestCaseDTO)
                .collect(Collectors.toList());
        dto.setTestCases(testCaseDTOs);

        // 设置摘要信息
        dto.setSummary(createTestSummaryDTO(testSuite));

        // 设置生成的代码（实际项目中需要从代码生成器获取）
        dto.setGeneratedCode(generateTestCode(testSuite));

        // 设置执行结果
        if (testSuite.getExecutionResult() != null) {
            dto.setExecutionResult(toTestExecutionResultDTO(testSuite.getExecutionResult()));
        }

        // 设置编译结果（模拟）
        dto.setCompilationResult(createMockCompilationResult());

        return dto;
    }

    /**
     * 转换测试用例
     */
    private TestGenerationResultDTO.TestCaseDTO toTestCaseDTO(TestCase testCase) {
        TestGenerationResultDTO.TestCaseDTO dto = new TestGenerationResultDTO.TestCaseDTO();

        dto.setName(testCase.getName());
        dto.setDescription(testCase.getDescription());
        dto.setTestType(testCase.getTestType().name().toLowerCase());
        dto.setSetupCode(testCase.getSetupCode());
        dto.setTeardownCode(testCase.getTeardownCode());

        // 转换测试方法
        List<TestGenerationResultDTO.TestMethodDTO> methodDTOs = testCase.getTestMethods().stream()
                .map(this::toTestMethodDTO)
                .collect(Collectors.toList());
        dto.setMethods(methodDTOs);

        return dto;
    }

    /**
     * 转换测试方法
     */
    private TestGenerationResultDTO.TestMethodDTO toTestMethodDTO(TestMethod testMethod) {
        TestGenerationResultDTO.TestMethodDTO dto = new TestGenerationResultDTO.TestMethodDTO();

        dto.setName(testMethod.getName());
        dto.setDescription(testMethod.getDescription());
        dto.setSourceMethod(testMethod.getTargetMethod());
        dto.setTestCode(testMethod.getTestCode());
        dto.setAnnotations(testMethod.getAnnotations());
        dto.setExpectedExceptions(testMethod.getExpectedExceptions());

        return dto;
    }

    /**
     * 转换测试执行结果
     */
    private TestStatusDTO.TestExecutionResultDTO toTestExecutionResultDTO(TestSuite.TestExecutionResult result) {
        return new TestStatusDTO.TestExecutionResultDTO(
                result.getTotalTests(),
                result.getPassedTests(),
                result.getFailedTests(),
                result.getSkippedTests(),
                result.getCoveragePercentage(),
                result.getExecutionReport(),
                result.getExecutionTime()
        );
    }

    /**
     * 创建测试摘要DTO
     */
    private TestGenerationResultDTO.TestSummaryDTO createTestSummaryDTO(TestSuite testSuite) {
        TestGenerationResultDTO.TestSummaryDTO summary = new TestGenerationResultDTO.TestSummaryDTO();

        summary.setTotalTestCases(testSuite.getTestCases().size());
        summary.setTotalTestMethods(testSuite.getTotalTestMethods());
        summary.setCoveragePercentage(testSuite.getCoveragePercentage());
        summary.setQualityScore(testSuite.getQualityScore());
        summary.setEstimatedCodeLines(estimateCodeLines(testSuite));

        // 设置使用的框架
        if (testSuite.getTemplate() != null) {
            summary.setFrameworks(List.of(
                    testSuite.getTemplate().getMockFramework(),
                    testSuite.getTemplate().getAssertionFramework()
            ));
            summary.setDependencies(testSuite.getTemplate().getAdditionalDependencies());
        }

        return summary;
    }

    /**
     * 生成状态消息
     */
    private String generateStatusMessage(TestSuite testSuite) {
        switch (testSuite.getStatus()) {
            case PENDING:
                return "测试生成任务已创建，等待开始";
            case GENERATING:
                return "正在生成测试代码...";
            case GENERATED:
                return "测试代码生成完成，可以进行验证";
            case VALIDATING:
                return "正在验证测试代码...";
            case VALIDATED:
                return "测试验证完成";
            case COMPLETED:
                return "测试生成和验证全部完成";
            case FAILED:
                return "测试生成失败，请检查日志";
            default:
                return "未知状态";
        }
    }

    /**
     * 生成测试代码（模拟）
     */
    private String generateTestCode(TestSuite testSuite) {
        StringBuilder codeBuilder = new StringBuilder();

        // 包声明
        codeBuilder.append("package ").append(testSuite.getTargetClass().getPackageName()).append(";\n\n");

        // 导入语句
        codeBuilder.append("import org.junit.jupiter.api.Test;\n");
        codeBuilder.append("import static org.junit.jupiter.api.Assertions.*;\n\n");

        // 类声明
        String testClassName = testSuite.getTargetClass().getSimpleName() + "Test";
        codeBuilder.append("public class ").append(testClassName).append(" {\n\n");

        // 生成测试方法
        for (TestCase testCase : testSuite.getTestCases()) {
            for (TestMethod testMethod : testCase.getTestMethods()) {
                codeBuilder.append("    @Test\n");
                codeBuilder.append("    public void ").append(testMethod.getName()).append("() {\n");
                codeBuilder.append("        // ").append(testMethod.getDescription()).append("\n");
                codeBuilder.append(testMethod.getTestCode());
                codeBuilder.append("    }\n\n");
            }
        }

        codeBuilder.append("}");

        return codeBuilder.toString();
    }

    /**
     * 估算代码行数
     */
    private Integer estimateCodeLines(TestSuite testSuite) {
        int baseLines = 20; // 基础代码行数（包声明、导入、类声明等）
        int methodLines = testSuite.getTotalTestMethods() * 8; // 每个测试方法估算8行
        return baseLines + methodLines;
    }

    /**
     * 创建模拟编译结果
     */
    private TestGenerationResultDTO.CompilationResultDTO createMockCompilationResult() {
        return new TestGenerationResultDTO.CompilationResultDTO(
                true,
                List.of(),
                List.of("Warning: Some test methods may need manual adjustment"),
                500L
        );
    }
}