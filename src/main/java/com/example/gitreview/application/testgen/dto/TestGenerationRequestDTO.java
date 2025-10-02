package com.example.gitreview.application.testgen.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.util.List;

/**
 * 测试生成请求DTO
 * 封装前端传递的测试生成请求参数
 */
public class TestGenerationRequestDTO {

    @NotNull(message = "Repository ID is required")
    @JsonProperty("repositoryId")
    private Long repositoryId;

    @NotNull(message = "Class name is required")
    @Size(min = 1, max = 200, message = "Class name must be between 1 and 200 characters")
    @JsonProperty("className")
    private String className;

    @JsonProperty("packageName")
    private String packageName;

    @NotNull(message = "Branch is required")
    @Size(min = 1, max = 100, message = "Branch must be between 1 and 100 characters")
    @JsonProperty("branch")
    private String branch;

    @JsonProperty("testType")
    private String testType = "basic"; // basic, comprehensive, mock, integration

    @Min(value = 1, message = "Quality level must be at least 1")
    @Max(value = 5, message = "Quality level must be at most 5")
    @JsonProperty("qualityLevel")
    private Integer qualityLevel = 3;

    @JsonProperty("mockFramework")
    private String mockFramework = "mockito";

    @JsonProperty("assertionFramework")
    private String assertionFramework = "junit";

    @JsonProperty("description")
    private String description;

    @JsonProperty("additionalDependencies")
    private List<String> additionalDependencies;

    @JsonProperty("includePrivateMethods")
    private Boolean includePrivateMethods = false;

    @JsonProperty("generateSetupTeardown")
    private Boolean generateSetupTeardown = true;

    @JsonProperty("generateDataProviders")
    private Boolean generateDataProviders = false;

    @JsonProperty("coverageTarget")
    private Double coverageTarget = 80.0;

    // 默认构造函数
    public TestGenerationRequestDTO() {
    }

    // 全参构造函数
    public TestGenerationRequestDTO(Long repositoryId, String className, String packageName, String branch,
                                  String testType, Integer qualityLevel, String mockFramework,
                                  String assertionFramework, String description, List<String> additionalDependencies) {
        this.repositoryId = repositoryId;
        this.className = className;
        this.packageName = packageName;
        this.branch = branch;
        this.testType = testType;
        this.qualityLevel = qualityLevel;
        this.mockFramework = mockFramework;
        this.assertionFramework = assertionFramework;
        this.description = description;
        this.additionalDependencies = additionalDependencies;
    }

    // Getters and Setters
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

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getAdditionalDependencies() {
        return additionalDependencies;
    }

    public void setAdditionalDependencies(List<String> additionalDependencies) {
        this.additionalDependencies = additionalDependencies;
    }

    public Boolean getIncludePrivateMethods() {
        return includePrivateMethods;
    }

    public void setIncludePrivateMethods(Boolean includePrivateMethods) {
        this.includePrivateMethods = includePrivateMethods;
    }

    public Boolean getGenerateSetupTeardown() {
        return generateSetupTeardown;
    }

    public void setGenerateSetupTeardown(Boolean generateSetupTeardown) {
        this.generateSetupTeardown = generateSetupTeardown;
    }

    public Boolean getGenerateDataProviders() {
        return generateDataProviders;
    }

    public void setGenerateDataProviders(Boolean generateDataProviders) {
        this.generateDataProviders = generateDataProviders;
    }

    public Double getCoverageTarget() {
        return coverageTarget;
    }

    public void setCoverageTarget(Double coverageTarget) {
        this.coverageTarget = coverageTarget;
    }

    @Override
    public String toString() {
        return "TestGenerationRequestDTO{" +
                "repositoryId=" + repositoryId +
                ", className='" + className + '\'' +
                ", packageName='" + packageName + '\'' +
                ", branch='" + branch + '\'' +
                ", testType='" + testType + '\'' +
                ", qualityLevel=" + qualityLevel +
                ", mockFramework='" + mockFramework + '\'' +
                ", assertionFramework='" + assertionFramework + '\'' +
                ", description='" + description + '\'' +
                ", coverageTarget=" + coverageTarget +
                '}';
    }
}