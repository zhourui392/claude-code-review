package com.example.gitreview.application.workflow.dto;

import java.util.List;

/**
 * Specification generation request DTO.
 *
 * @author zhourui(V33215020)
 * @since 2025/10/05
 */
public class SpecGenerationRequest {
    private String prdContent;
    private List<String> documentPaths;

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
