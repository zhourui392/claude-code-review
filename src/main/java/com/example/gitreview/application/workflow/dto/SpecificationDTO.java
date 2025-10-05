package com.example.gitreview.application.workflow.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Specification DTO.
 *
 * @author zhourui(V33215020)
 * @since 2025/10/05
 */
public class SpecificationDTO {
    private String generatedContent;
    private String prdContent;
    private List<String> documentPaths;
    private LocalDateTime generatedAt;

    public SpecificationDTO() {}

    public SpecificationDTO(String generatedContent, String prdContent, List<String> documentPaths, LocalDateTime generatedAt) {
        this.generatedContent = generatedContent;
        this.prdContent = prdContent;
        this.documentPaths = documentPaths;
        this.generatedAt = generatedAt;
    }

    public String getGeneratedContent() {
        return generatedContent;
    }

    public void setGeneratedContent(String generatedContent) {
        this.generatedContent = generatedContent;
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
