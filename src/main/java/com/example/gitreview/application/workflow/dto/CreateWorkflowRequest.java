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

    private String architecture;
    private String codingStyle;
    private String namingConvention;
    private String commentLanguage;
    private Integer maxMethodLines;
    private Integer maxParameters;

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

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    public String getCodingStyle() {
        return codingStyle;
    }

    public void setCodingStyle(String codingStyle) {
        this.codingStyle = codingStyle;
    }

    public String getNamingConvention() {
        return namingConvention;
    }

    public void setNamingConvention(String namingConvention) {
        this.namingConvention = namingConvention;
    }

    public String getCommentLanguage() {
        return commentLanguage;
    }

    public void setCommentLanguage(String commentLanguage) {
        this.commentLanguage = commentLanguage;
    }

    public Integer getMaxMethodLines() {
        return maxMethodLines;
    }

    public void setMaxMethodLines(Integer maxMethodLines) {
        this.maxMethodLines = maxMethodLines;
    }

    public Integer getMaxParameters() {
        return maxParameters;
    }

    public void setMaxParameters(Integer maxParameters) {
        this.maxParameters = maxParameters;
    }
}
