package com.example.gitreview.application.testgen.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * 测试状态DTO
 * 封装测试生成任务的状态信息
 */
public class TestStatusDTO {

    @JsonProperty("taskId")
    private String taskId;

    @JsonProperty("suiteId")
    private Long suiteId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("statusDescription")
    private String statusDescription;

    @JsonProperty("progress")
    private Integer progress;

    @JsonProperty("message")
    private String message;

    @JsonProperty("repositoryId")
    private Long repositoryId;

    @JsonProperty("className")
    private String className;

    @JsonProperty("suiteName")
    private String suiteName;

    @JsonProperty("createTime")
    private LocalDateTime createTime;

    @JsonProperty("updateTime")
    private LocalDateTime updateTime;

    @JsonProperty("testCaseCount")
    private Integer testCaseCount;

    @JsonProperty("testMethodCount")
    private Integer testMethodCount;

    @JsonProperty("coveragePercentage")
    private Double coveragePercentage;

    @JsonProperty("qualityScore")
    private Integer qualityScore;

    @JsonProperty("executionResult")
    private TestExecutionResultDTO executionResult;

    @JsonProperty("canRegenerate")
    private Boolean canRegenerate;

    @JsonProperty("canExecute")
    private Boolean canExecute;

    // 默认构造函数
    public TestStatusDTO() {
    }

    // 测试执行结果内部类
    public static class TestExecutionResultDTO {
        @JsonProperty("totalTests")
        private Integer totalTests;

        @JsonProperty("passedTests")
        private Integer passedTests;

        @JsonProperty("failedTests")
        private Integer failedTests;

        @JsonProperty("skippedTests")
        private Integer skippedTests;

        @JsonProperty("coveragePercentage")
        private Double coveragePercentage;

        @JsonProperty("successRate")
        private Double successRate;

        @JsonProperty("executionTime")
        private LocalDateTime executionTime;

        @JsonProperty("executionReport")
        private String executionReport;

        public TestExecutionResultDTO() {
        }

        public TestExecutionResultDTO(Integer totalTests, Integer passedTests, Integer failedTests,
                                    Integer skippedTests, Double coveragePercentage, String executionReport,
                                    LocalDateTime executionTime) {
            this.totalTests = totalTests;
            this.passedTests = passedTests;
            this.failedTests = failedTests;
            this.skippedTests = skippedTests;
            this.coveragePercentage = coveragePercentage;
            this.executionReport = executionReport;
            this.executionTime = executionTime;
            this.successRate = totalTests > 0 ? (double) passedTests / totalTests * 100 : 0.0;
        }

        // Getters and Setters
        public Integer getTotalTests() { return totalTests; }
        public void setTotalTests(Integer totalTests) { this.totalTests = totalTests; }

        public Integer getPassedTests() { return passedTests; }
        public void setPassedTests(Integer passedTests) { this.passedTests = passedTests; }

        public Integer getFailedTests() { return failedTests; }
        public void setFailedTests(Integer failedTests) { this.failedTests = failedTests; }

        public Integer getSkippedTests() { return skippedTests; }
        public void setSkippedTests(Integer skippedTests) { this.skippedTests = skippedTests; }

        public Double getCoveragePercentage() { return coveragePercentage; }
        public void setCoveragePercentage(Double coveragePercentage) { this.coveragePercentage = coveragePercentage; }

        public Double getSuccessRate() { return successRate; }
        public void setSuccessRate(Double successRate) { this.successRate = successRate; }

        public LocalDateTime getExecutionTime() { return executionTime; }
        public void setExecutionTime(LocalDateTime executionTime) { this.executionTime = executionTime; }

        public String getExecutionReport() { return executionReport; }
        public void setExecutionReport(String executionReport) { this.executionReport = executionReport; }
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusDescription() {
        return statusDescription;
    }

    public void setStatusDescription(String statusDescription) {
        this.statusDescription = statusDescription;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(Long repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getSuiteName() {
        return suiteName;
    }

    public void setSuiteName(String suiteName) {
        this.suiteName = suiteName;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getTestCaseCount() {
        return testCaseCount;
    }

    public void setTestCaseCount(Integer testCaseCount) {
        this.testCaseCount = testCaseCount;
    }

    public Integer getTestMethodCount() {
        return testMethodCount;
    }

    public void setTestMethodCount(Integer testMethodCount) {
        this.testMethodCount = testMethodCount;
    }

    public Double getCoveragePercentage() {
        return coveragePercentage;
    }

    public void setCoveragePercentage(Double coveragePercentage) {
        this.coveragePercentage = coveragePercentage;
    }

    public Integer getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(Integer qualityScore) {
        this.qualityScore = qualityScore;
    }

    public TestExecutionResultDTO getExecutionResult() {
        return executionResult;
    }

    public void setExecutionResult(TestExecutionResultDTO executionResult) {
        this.executionResult = executionResult;
    }

    public Boolean getCanRegenerate() {
        return canRegenerate;
    }

    public void setCanRegenerate(Boolean canRegenerate) {
        this.canRegenerate = canRegenerate;
    }

    public Boolean getCanExecute() {
        return canExecute;
    }

    public void setCanExecute(Boolean canExecute) {
        this.canExecute = canExecute;
    }

    @Override
    public String toString() {
        return "TestStatusDTO{" +
                "taskId='" + taskId + '\'' +
                ", status='" + status + '\'' +
                ", progress=" + progress +
                ", className='" + className + '\'' +
                ", testCaseCount=" + testCaseCount +
                ", qualityScore=" + qualityScore +
                '}';
    }
}