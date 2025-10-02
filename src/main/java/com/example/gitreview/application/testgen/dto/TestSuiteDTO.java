package com.example.gitreview.application.testgen.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * 测试套件DTO
 * 封装测试套件的基本信息
 */
public class TestSuiteDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("repositoryId")
    private Long repositoryId;

    @JsonProperty("suiteName")
    private String suiteName;

    @JsonProperty("description")
    private String description;

    @JsonProperty("className")
    private String className;

    @JsonProperty("packageName")
    private String packageName;

    @JsonProperty("status")
    private String status;

    @JsonProperty("statusDescription")
    private String statusDescription;

    @JsonProperty("progress")
    private Integer progress;

    @JsonProperty("createTime")
    private LocalDateTime createTime;

    @JsonProperty("updateTime")
    private LocalDateTime updateTime;

    @JsonProperty("createdBy")
    private String createdBy;

    @JsonProperty("testCaseCount")
    private Integer testCaseCount;

    @JsonProperty("testMethodCount")
    private Integer testMethodCount;

    @JsonProperty("coveragePercentage")
    private Double coveragePercentage;

    @JsonProperty("qualityScore")
    private Integer qualityScore;

    @JsonProperty("testType")
    private String testType;

    @JsonProperty("qualityLevel")
    private Integer qualityLevel;

    @JsonProperty("mockFramework")
    private String mockFramework;

    @JsonProperty("assertionFramework")
    private String assertionFramework;

    @JsonProperty("hasExecutionResult")
    private Boolean hasExecutionResult;

    @JsonProperty("canRegenerate")
    private Boolean canRegenerate;

    @JsonProperty("canExecute")
    private Boolean canExecute;

    // 默认构造函数
    public TestSuiteDTO() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(Long repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getSuiteName() {
        return suiteName;
    }

    public void setSuiteName(String suiteName) {
        this.suiteName = suiteName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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

    public String getTestType() {
        return testType;
    }

    public void setTestType(String testType) {
        this.testType = testType;
    }

    public Integer getQualityLevel() {
        return qualityLevel;
    }

    public void setQualityLevel(Integer qualityLevel) {
        this.qualityLevel = qualityLevel;
    }

    public String getMockFramework() {
        return mockFramework;
    }

    public void setMockFramework(String mockFramework) {
        this.mockFramework = mockFramework;
    }

    public String getAssertionFramework() {
        return assertionFramework;
    }

    public void setAssertionFramework(String assertionFramework) {
        this.assertionFramework = assertionFramework;
    }

    public Boolean getHasExecutionResult() {
        return hasExecutionResult;
    }

    public void setHasExecutionResult(Boolean hasExecutionResult) {
        this.hasExecutionResult = hasExecutionResult;
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
        return "TestSuiteDTO{" +
                "id=" + id +
                ", suiteName='" + suiteName + '\'' +
                ", className='" + className + '\'' +
                ", status='" + status + '\'' +
                ", testCaseCount=" + testCaseCount +
                ", qualityScore=" + qualityScore +
                '}';
    }
}