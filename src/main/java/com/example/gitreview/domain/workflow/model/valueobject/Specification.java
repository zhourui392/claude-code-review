package com.example.gitreview.domain.workflow.model.valueobject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
