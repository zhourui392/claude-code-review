package com.example.gitreview.application.workflow.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 规格文档响应DTO
 *
 * @author zhourui(V33215020)
 * @since 2025/10/04
 */
public class SpecificationDTO {

    private String content;
    private String prdContent;
    private List<String> documentPaths;
    private LocalDateTime generatedAt;

    public SpecificationDTO() {
    }

    public SpecificationDTO(String content, String prdContent, List<String> documentPaths, LocalDateTime generatedAt) {
        this.content = content;
        this.prdContent = prdContent;
        this.documentPaths = documentPaths;
        this.generatedAt = generatedAt;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}
