package com.example.gitreview.domain.workflow.service;

import com.example.gitreview.domain.workflow.model.aggregate.DevelopmentWorkflow;
import com.example.gitreview.domain.workflow.model.valueobject.Specification;
import com.example.gitreview.domain.workflow.model.valueobject.TechnicalDesign;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WorkflowDomainService 测试
 * 测试进度计算和验证逻辑
 *
 * @author zhourui(V33215020)
 * @since 2025/10/04
 */
public class WorkflowDomainServiceTest {

    private WorkflowDomainService domainService;

    @BeforeEach
    void setUp() {
        domainService = new WorkflowDomainService();
    }

    @Test
    void should_ValidateWorkflowName() {
        assertTrue(domainService.validateWorkflowName("测试工作流"));
        assertFalse(domainService.validateWorkflowName(""));
        assertFalse(domainService.validateWorkflowName(null));
        assertFalse(domainService.validateWorkflowName("   "));
    }

    @Test
    void should_CalculateProgress() {
        assertEquals(0, domainService.calculateProgress(0, 10));
        assertEquals(50, domainService.calculateProgress(5, 10));
        assertEquals(100, domainService.calculateProgress(10, 10));
        assertEquals(0, domainService.calculateProgress(0, 0));
    }

    @Test
    void should_ThrowException_when_SpecificationIsNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            domainService.validateSpecification(null);
        });
    }

    @Test
    void should_ThrowException_when_GeneratedContentIsEmpty() {
        Specification spec = new Specification(
                "PRD内容",
                List.of(),
                "",
                LocalDateTime.now()
        );

        assertThrows(IllegalArgumentException.class, () -> {
            domainService.validateSpecification(spec);
        });
    }

    @Test
    void should_ThrowException_when_GeneratedContentIsTooShort() {
        Specification spec = new Specification(
                "PRD内容",
                List.of(),
                "短",
                LocalDateTime.now()
        );

        assertThrows(IllegalArgumentException.class, () -> {
            domainService.validateSpecification(spec);
        });
    }

    @Test
    void should_Pass_when_ValidSpecification() {
        String longContent = "生成的规格文档".repeat(20);
        Specification spec = new Specification(
                "PRD内容",
                List.of("doc1.md"),
                longContent,
                LocalDateTime.now()
        );

        assertDoesNotThrow(() -> {
            domainService.validateSpecification(spec);
        });
    }

    @Test
    void should_ThrowException_when_TechnicalDesignIsNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            domainService.validateTechnicalDesign(null);
        });
    }

    @Test
    void should_ThrowException_when_TechDesignContentIsEmpty() {
        TechnicalDesign design = new TechnicalDesign(
                "",
                1,
                false,
                LocalDateTime.now(),
                null
        );

        assertThrows(IllegalArgumentException.class, () -> {
            domainService.validateTechnicalDesign(design);
        });
    }

    @Test
    void should_ThrowException_when_TechDesignContentIsTooShort() {
        TechnicalDesign design = new TechnicalDesign(
                "短",
                1,
                false,
                LocalDateTime.now(),
                null
        );

        assertThrows(IllegalArgumentException.class, () -> {
            domainService.validateTechnicalDesign(design);
        });
    }

    @Test
    void should_Pass_when_ValidTechnicalDesign() {
        String longContent = "技术方案内容".repeat(20);
        TechnicalDesign design = new TechnicalDesign(
                longContent,
                1,
                false,
                LocalDateTime.now(),
                null
        );

        assertDoesNotThrow(() -> {
            domainService.validateTechnicalDesign(design);
        });
    }

    @Test
    void should_ReturnZero_when_WorkflowHasNoTasks() {
        DevelopmentWorkflow workflow = DevelopmentWorkflow.create("测试", 1L, "zhourui");

        int progress = domainService.calculateProgress(workflow);

        assertEquals(0, progress);
    }
}
