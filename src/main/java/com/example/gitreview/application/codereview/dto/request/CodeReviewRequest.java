package com.example.gitreview.application.codereview.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 代码审查创建请求DTO
 */
public class CodeReviewRequest {

    @NotNull(message = "Repository ID cannot be null")
    private Long repositoryId;

    @NotBlank(message = "Title cannot be blank")
    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @NotBlank(message = "Base branch cannot be blank")
    private String baseBranch;

    @NotBlank(message = "Target branch cannot be blank")
    private String targetBranch;

    @NotBlank(message = "Strategy mode cannot be blank")
    private String strategyMode;

    @NotBlank(message = "Created by cannot be blank")
    private String createdBy;

    // 仓库连接信息
    @NotBlank(message = "Repository URL cannot be blank")
    private String repositoryUrl;

    private String username;
    private String password;

    // 默认构造函数
    public CodeReviewRequest() {}

    // 全参构造函数
    public CodeReviewRequest(Long repositoryId, String title, String description,
                            String baseBranch, String targetBranch, String strategyMode,
                            String createdBy, String repositoryUrl, String username, String password) {
        this.repositoryId = repositoryId;
        this.title = title;
        this.description = description;
        this.baseBranch = baseBranch;
        this.targetBranch = targetBranch;
        this.strategyMode = strategyMode;
        this.createdBy = createdBy;
        this.repositoryUrl = repositoryUrl;
        this.username = username;
        this.password = password;
    }

    // Getters and Setters
    public Long getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(Long repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBaseBranch() {
        return baseBranch;
    }

    public void setBaseBranch(String baseBranch) {
        this.baseBranch = baseBranch;
    }

    public String getTargetBranch() {
        return targetBranch;
    }

    public void setTargetBranch(String targetBranch) {
        this.targetBranch = targetBranch;
    }

    public String getStrategyMode() {
        return strategyMode;
    }

    public void setStrategyMode(String strategyMode) {
        this.strategyMode = strategyMode;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "CodeReviewRequest{" +
                "repositoryId=" + repositoryId +
                ", title='" + title + '\'' +
                ", baseBranch='" + baseBranch + '\'' +
                ", targetBranch='" + targetBranch + '\'' +
                ", strategyMode='" + strategyMode + '\'' +
                ", createdBy='" + createdBy + '\'' +
                '}';
    }
}