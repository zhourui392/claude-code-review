package com.example.gitreview.domain.workflow.model.valueobject;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.*;

/**
 * Specification 值对象单元测试
 *
 * @author zhourui(V33215020)
 * @since 2025/10/04
 */
class SpecificationTest {

    @Test
    void should_create_specification_with_all_fields() {
        Specification spec = new Specification(
                "PRD内容",
                Arrays.asList("doc1.md", "doc2.md"),
                "生成的规格文档",
                LocalDateTime.now()
        );

        assertThat(spec.getPrdContent()).isEqualTo("PRD内容");
        assertThat(spec.getDocumentPaths()).containsExactly("doc1.md", "doc2.md");
        assertThat(spec.getGeneratedContent()).isEqualTo("生成的规格文档");
        assertThat(spec.getGeneratedAt()).isNotNull();
    }

    @Test
    void should_create_specification_with_empty_document_paths() {
        Specification spec = new Specification(
                "PRD内容",
                Collections.emptyList(),
                "生成的规格文档",
                LocalDateTime.now()
        );

        assertThat(spec.getDocumentPaths()).isEmpty();
    }

    @Test
    void should_create_specification_with_null_document_paths() {
        Specification spec = new Specification(
                "PRD内容",
                null,
                "生成的规格文档",
                LocalDateTime.now()
        );

        assertThat(spec.getDocumentPaths()).isNotNull();
        assertThat(spec.getDocumentPaths()).isEmpty();
    }

    @Test
    void should_be_equal_when_same_content() {
        LocalDateTime now = LocalDateTime.now();
        Specification spec1 = new Specification(
                "PRD内容",
                Arrays.asList("doc1.md"),
                "生成内容",
                now
        );
        Specification spec2 = new Specification(
                "PRD内容",
                Arrays.asList("doc1.md"),
                "生成内容",
                now.plusHours(1)
        );

        assertThat(spec1).isEqualTo(spec2);
        assertThat(spec1.hashCode()).isEqualTo(spec2.hashCode());
    }

    @Test
    void should_not_be_equal_when_different_content() {
        Specification spec1 = new Specification(
                "PRD1",
                Collections.emptyList(),
                "生成内容1",
                LocalDateTime.now()
        );
        Specification spec2 = new Specification(
                "PRD2",
                Collections.emptyList(),
                "生成内容2",
                LocalDateTime.now()
        );

        assertThat(spec1).isNotEqualTo(spec2);
    }
}
