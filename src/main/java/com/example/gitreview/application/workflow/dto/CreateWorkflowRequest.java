package com.example.gitreview.application.workflow.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * 创建工作流请求DTO
 *
 * @author zhourui(V33215020)
 * @since 2025/10/04
 */
public class CreateWorkflowRequest {

    @NotEmpty(message = "工作流名称不能为空")
    private String name;

    @NotNull(message = "仓库ID不能为空")
    private Long repositoryId;

    @NotEmpty(message = "创建者不能为空")
    private String createdBy;

    public CreateWorkflowRequest() {
    }

    public CreateWorkflowRequest(String name, Long repositoryId, String createdBy) {
        this.name = name;
        this.repositoryId = repositoryId;
        this.createdBy = createdBy;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(Long repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
