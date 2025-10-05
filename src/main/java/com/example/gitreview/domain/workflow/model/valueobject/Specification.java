package com.example.gitreview.domain.workflow.model.valueobject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 规格文档值对象
 *
 * @author zhourui(V33215020)
 * @since 2025/10/04
 */
public class Specification {

    private final String prdContent;
    private final List<String> documentPaths;
    private final String generatedContent;
    private final LocalDateTime generatedAt;

    @JsonCreator
    public Specification(
            @JsonProperty("prdContent") String prdContent,
            @JsonProperty("documentPaths") List<String> documentPaths,
            @JsonProperty("generatedContent") String generatedContent,
            @JsonProperty("generatedAt") LocalDateTime generatedAt) {
        this.prdContent = prdContent;
        this.documentPaths = documentPaths;
        this.generatedContent = generatedContent;
        this.generatedAt = generatedAt;
    }

    /**
     * 验证规格文档内容完整性
     */
    public void validateContent() {
        if (prdContent == null || prdContent.trim().isEmpty()) {
            throw new IllegalArgumentException("PRD内容不能为空");
        }
        if (generatedContent == null || generatedContent.trim().isEmpty()) {
            throw new IllegalArgumentException("生成的规格文档内容不能为空");
        }
    }

    public String getPrdContent() {
        return prdContent;
    }

    public List<String> getDocumentPaths() {
        return documentPaths;
    }

    public String getGeneratedContent() {
        return generatedContent;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Specification that = (Specification) o;
        return Objects.equals(prdContent, that.prdContent) &&
                Objects.equals(documentPaths, that.documentPaths) &&
                Objects.equals(generatedContent, that.generatedContent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prdContent, documentPaths, generatedContent);
    }
}
