package com.example.gitreview.domain.workflow.model.valueobject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Specification value object.
 *
 * @author zhourui(V33215020)
 * @since 2025/10/05
 */
public class Specification {
    private String prdContent;
    private List<String> documentPaths;
    private String generatedContent;
    private LocalDateTime generatedAt;

    // 默认构造函数供Jackson使用
    public Specification() {
        this.documentPaths = new ArrayList<>();
    }

    @JsonCreator
    public Specification(
            @JsonProperty("prdContent") String prdContent,
            @JsonProperty("documentPaths") List<String> documentPaths,
            @JsonProperty("generatedContent") String generatedContent,
            @JsonProperty("generatedAt") LocalDateTime generatedAt) {
        this.prdContent = prdContent;
        this.documentPaths = documentPaths != null ? new ArrayList<>(documentPaths) : new ArrayList<>();
        this.generatedContent = generatedContent;
        this.generatedAt = generatedAt;
    }

    public String getPrdContent() {
        return prdContent;
    }

    public List<String> getDocumentPaths() {
        return Collections.unmodifiableList(documentPaths);
    }

    public String getGeneratedContent() {
        return generatedContent;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }
}
