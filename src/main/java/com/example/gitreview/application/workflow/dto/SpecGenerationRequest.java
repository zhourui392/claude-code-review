package com.example.gitreview.application.workflow.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 规格文档生成请求DTO
 *
 * @author zhourui(V33215020)
 * @since 2025/10/04
 */
public class SpecGenerationRequest {

    @NotEmpty(message = "PRD内容不能为空")
    private String prdContent;

    private List<String> documentPaths;

    public SpecGenerationRequest() {
    }

    public SpecGenerationRequest(String prdContent, List<String> documentPaths) {
        this.prdContent = prdContent;
        this.documentPaths = documentPaths;
    }

    public String getPrdContent() {
        return prdContent;
    }

    public void setPrdContent(String prdContent) {
        this.prdContent = prdContent;
    }

    public List<String> getDocumentPaths() {
        return documentPaths;
    }

    public void setDocumentPaths(List<String> documentPaths) {
        this.documentPaths = documentPaths;
    }
}
