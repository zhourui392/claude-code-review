package com.example.gitreview.application.testgen.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 测试生成结果DTO
 * 封装完整的测试生成结果信息
 */
public class TestGenerationResultDTO {

    @JsonProperty("taskId")
    private String taskId;

    @JsonProperty("suiteId")
    private Long suiteId;

    @JsonProperty("suiteName")
    private String suiteName;

    @JsonProperty("status")
    private String status;

    @JsonProperty("className")
    private String className;

    @JsonProperty("packageName")
    private String packageName;

    @JsonProperty("generateTime")
    private LocalDateTime generateTime;

    @JsonProperty("testCases")
    private List<TestCaseDTO> testCases;

    @JsonProperty("summary")
    private TestSummaryDTO summary;

    @JsonProperty("generatedCode")
    private String generatedCode;

    @JsonProperty("compilationResult")
    private CompilationResultDTO compilationResult;

    @JsonProperty("executionResult")
    private TestStatusDTO.TestExecutionResultDTO executionResult;

    // 内部DTO类
    public static class TestCaseDTO {
        @JsonProperty("name")
        private String name;

        @JsonProperty("description")
        private String description;

        @JsonProperty("testType")
        private String testType;

        @JsonProperty("methods")
        private List<TestMethodDTO> methods;

        @JsonProperty("setupCode")
        private String setupCode;

        @JsonProperty("teardownCode")
        private String teardownCode;

        public TestCaseDTO() {}

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getTestType() { return testType; }
        public void setTestType(String testType) { this.testType = testType; }

        public List<TestMethodDTO> getMethods() { return methods; }
        public void setMethods(List<TestMethodDTO> methods) { this.methods = methods; }

        public String getSetupCode() { return setupCode; }
        public void setSetupCode(String setupCode) { this.setupCode = setupCode; }

        public String getTeardownCode() { return teardownCode; }
        public void setTeardownCode(String teardownCode) { this.teardownCode = teardownCode; }
    }

    public static class TestMethodDTO {
        @JsonProperty("name")
        private String name;

        @JsonProperty("description")
        private String description;

        @JsonProperty("sourceMethod")
        private String sourceMethod;

        @JsonProperty("testCode")
        private String testCode;

        @JsonProperty("annotations")
        private List<String> annotations;

        @JsonProperty("expectedExceptions")
        private List<String> expectedExceptions;

        public TestMethodDTO() {}

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getSourceMethod() { return sourceMethod; }
        public void setSourceMethod(String sourceMethod) { this.sourceMethod = sourceMethod; }

        public String getTestCode() { return testCode; }
        public void setTestCode(String testCode) { this.testCode = testCode; }

        public List<String> getAnnotations() { return annotations; }
        public void setAnnotations(List<String> annotations) { this.annotations = annotations; }

        public List<String> getExpectedExceptions() { return expectedExceptions; }
        public void setExpectedExceptions(List<String> expectedExceptions) { this.expectedExceptions = expectedExceptions; }
    }

    public static class TestSummaryDTO {
        @JsonProperty("totalTestCases")
        private Integer totalTestCases;

        @JsonProperty("totalTestMethods")
        private Integer totalTestMethods;

        @JsonProperty("coveragePercentage")
        private Double coveragePercentage;

        @JsonProperty("qualityScore")
        private Integer qualityScore;

        @JsonProperty("estimatedCodeLines")
        private Integer estimatedCodeLines;

        @JsonProperty("frameworks")
        private List<String> frameworks;

        @JsonProperty("dependencies")
        private List<String> dependencies;

        public TestSummaryDTO() {}

        // Getters and Setters
        public Integer getTotalTestCases() { return totalTestCases; }
        public void setTotalTestCases(Integer totalTestCases) { this.totalTestCases = totalTestCases; }

        public Integer getTotalTestMethods() { return totalTestMethods; }
        public void setTotalTestMethods(Integer totalTestMethods) { this.totalTestMethods = totalTestMethods; }

        public Double getCoveragePercentage() { return coveragePercentage; }
        public void setCoveragePercentage(Double coveragePercentage) { this.coveragePercentage = coveragePercentage; }

        public Integer getQualityScore() { return qualityScore; }
        public void setQualityScore(Integer qualityScore) { this.qualityScore = qualityScore; }

        public Integer getEstimatedCodeLines() { return estimatedCodeLines; }
        public void setEstimatedCodeLines(Integer estimatedCodeLines) { this.estimatedCodeLines = estimatedCodeLines; }

        public List<String> getFrameworks() { return frameworks; }
        public void setFrameworks(List<String> frameworks) { this.frameworks = frameworks; }

        public List<String> getDependencies() { return dependencies; }
        public void setDependencies(List<String> dependencies) { this.dependencies = dependencies; }
    }

    public static class CompilationResultDTO {
        @JsonProperty("success")
        private Boolean success;

        @JsonProperty("errors")
        private List<String> errors;

        @JsonProperty("warnings")
        private List<String> warnings;

        @JsonProperty("compilationTime")
        private Long compilationTime;

        public CompilationResultDTO() {}

        public CompilationResultDTO(Boolean success, List<String> errors, List<String> warnings, Long compilationTime) {
            this.success = success;
            this.errors = errors;
            this.warnings = warnings;
            this.compilationTime = compilationTime;
        }

        // Getters and Setters
        public Boolean getSuccess() { return success; }
        public void setSuccess(Boolean success) { this.success = success; }

        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }

        public List<String> getWarnings() { return warnings; }
        public void setWarnings(List<String> warnings) { this.warnings = warnings; }

        public Long getCompilationTime() { return compilationTime; }
        public void setCompilationTime(Long compilationTime) { this.compilationTime = compilationTime; }
    }

    // 默认构造函数
    public TestGenerationResultDTO() {
    }

    // Getters and Setters
    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public Long getSuiteId() {
        return suiteId;
    }

    public void setSuiteId(Long suiteId) {
        this.suiteId = suiteId;
    }

    public String getSuiteName() {
        return suiteName;
    }

    public void setSuiteName(String suiteName) {
        this.suiteName = suiteName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public LocalDateTime getGenerateTime() {
        return generateTime;
    }

    public void setGenerateTime(LocalDateTime generateTime) {
        this.generateTime = generateTime;
    }

    public List<TestCaseDTO> getTestCases() {
        return testCases;
    }

    public void setTestCases(List<TestCaseDTO> testCases) {
        this.testCases = testCases;
    }

    public TestSummaryDTO getSummary() {
        return summary;
    }

    public void setSummary(TestSummaryDTO summary) {
        this.summary = summary;
    }

    public String getGeneratedCode() {
        return generatedCode;
    }

    public void setGeneratedCode(String generatedCode) {
        this.generatedCode = generatedCode;
    }

    public CompilationResultDTO getCompilationResult() {
        return compilationResult;
    }

    public void setCompilationResult(CompilationResultDTO compilationResult) {
        this.compilationResult = compilationResult;
    }

    public TestStatusDTO.TestExecutionResultDTO getExecutionResult() {
        return executionResult;
    }

    public void setExecutionResult(TestStatusDTO.TestExecutionResultDTO executionResult) {
        this.executionResult = executionResult;
    }

    @Override
    public String toString() {
        return "TestGenerationResultDTO{" +
                "taskId='" + taskId + '\'' +
                ", suiteName='" + suiteName + '\'' +
                ", status='" + status + '\'' +
                ", className='" + className + '\'' +
                ", testCases=" + (testCases != null ? testCases.size() : 0) +
                '}';
    }
}