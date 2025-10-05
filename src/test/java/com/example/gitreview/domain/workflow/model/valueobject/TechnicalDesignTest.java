package com.example.gitreview.domain.workflow.model.valueobject;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * TechnicalDesign 值对象单元测试
 *
 * @author zhourui(V33215020)
 * @since 2025/10/04
 */
class TechnicalDesignTest {

    @Test
    void should_create_technical_design_with_all_fields() {
        LocalDateTime now = LocalDateTime.now();
        TechnicalDesign design = new TechnicalDesign(
                "技术方案内容",
                1,
                false,
                now,
                null
        );

        assertThat(design.getContent()).isEqualTo("技术方案内容");
        assertThat(design.getVersion()).isEqualTo(1);
        assertThat(design.isApproved()).isFalse();
        assertThat(design.getCreatedAt()).isEqualTo(now);
        assertThat(design.getApprovedAt()).isNull();
    }

    @Test
    void should_create_new_version_with_incremented_version_number() {
        TechnicalDesign design = new TechnicalDesign(
                "原内容",
                1,
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        TechnicalDesign newVersion = design.createNewVersion("新内容");

        assertThat(newVersion.getContent()).isEqualTo("新内容");
        assertThat(newVersion.getVersion()).isEqualTo(2);
        assertThat(newVersion.isApproved()).isFalse();
        assertThat(newVersion.getApprovedAt()).isNull();
        assertThat(design.getVersion()).isEqualTo(1);
    }

    @Test
    void should_preserve_original_when_creating_new_version() {
        TechnicalDesign design = new TechnicalDesign(
                "原内容",
                1,
                false,
                LocalDateTime.now(),
                null
        );

        TechnicalDesign newVersion = design.createNewVersion("新内容");

        assertThat(design.getContent()).isEqualTo("原内容");
        assertThat(design.getVersion()).isEqualTo(1);
        assertThat(newVersion).isNotSameAs(design);
    }

    @Test
    void should_approve_design_and_set_approved_time() {
        TechnicalDesign design = new TechnicalDesign(
                "技术方案内容",
                1,
                false,
                LocalDateTime.now(),
                null
        );

        TechnicalDesign approved = design.approve();

        assertThat(approved.isApproved()).isTrue();
        assertThat(approved.getApprovedAt()).isNotNull();
        assertThat(approved.getContent()).isEqualTo(design.getContent());
        assertThat(approved.getVersion()).isEqualTo(design.getVersion());
        assertThat(design.isApproved()).isFalse();
    }

    @Test
    void should_preserve_original_when_approving() {
        TechnicalDesign design = new TechnicalDesign(
                "技术方案内容",
                1,
                false,
                LocalDateTime.now(),
                null
        );

        TechnicalDesign approved = design.approve();

        assertThat(design.isApproved()).isFalse();
        assertThat(design.getApprovedAt()).isNull();
        assertThat(approved).isNotSameAs(design);
    }

    @Test
    void should_be_equal_when_same_content_and_version() {
        LocalDateTime now = LocalDateTime.now();
        TechnicalDesign design1 = new TechnicalDesign(
                "内容",
                1,
                false,
                now,
                null
        );
        TechnicalDesign design2 = new TechnicalDesign(
                "内容",
                1,
                true,
                now.plusHours(1),
                now
        );

        assertThat(design1).isEqualTo(design2);
        assertThat(design1.hashCode()).isEqualTo(design2.hashCode());
    }

    @Test
    void should_not_be_equal_when_different_version() {
        TechnicalDesign design1 = new TechnicalDesign(
                "内容",
                1,
                false,
                LocalDateTime.now(),
                null
        );
        TechnicalDesign design2 = new TechnicalDesign(
                "内容",
                2,
                false,
                LocalDateTime.now(),
                null
        );

        assertThat(design1).isNotEqualTo(design2);
    }

    @Test
    void should_not_be_equal_when_different_content() {
        TechnicalDesign design1 = new TechnicalDesign(
                "内容1",
                1,
                false,
                LocalDateTime.now(),
                null
        );
        TechnicalDesign design2 = new TechnicalDesign(
                "内容2",
                1,
                false,
                LocalDateTime.now(),
                null
        );

        assertThat(design1).isNotEqualTo(design2);
    }
}
