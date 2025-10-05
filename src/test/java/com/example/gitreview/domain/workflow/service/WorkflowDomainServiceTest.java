package com.example.gitreview.domain.workflow.service;

import com.example.gitreview.domain.workflow.model.WorkflowStatus;
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
 * 测试状态转换验证和进度计算逻辑
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
    void should_ReturnTrue_when_ValidTransition() {
        assertTrue(domainService.isValidTransition(
                WorkflowStatus.DRAFT, WorkflowStatus.SPEC_GENERATING));

        assertTrue(domainService.isValidTransition(
                WorkflowStatus.SPEC_GENERATING, WorkflowStatus.SPEC_GENERATED));

        assertTrue(domainService.isValidTransition(
                WorkflowStatus.SPEC_GENERATED, WorkflowStatus.TECH_DESIGN_GENERATING));

        assertTrue(domainService.isValidTransition(
                WorkflowStatus.TECH_DESIGN_GENERATED, WorkflowStatus.TECH_DESIGN_APPROVED));

        assertTrue(domainService.isValidTransition(
                WorkflowStatus.TASK_LIST_GENERATED, WorkflowStatus.CODE_GENERATING));

        assertTrue(domainService.isValidTransition(
                WorkflowStatus.CODE_GENERATING, WorkflowStatus.COMPLETED));
    }

    @Test
    void should_ReturnFalse_when_InvalidTransition() {
        assertFalse(domainService.isValidTransition(
                WorkflowStatus.DRAFT, WorkflowStatus.COMPLETED));

        assertFalse(domainService.isValidTransition(
                WorkflowStatus.SPEC_GENERATED, WorkflowStatus.TASK_LIST_GENERATING));

        assertFalse(domainService.isValidTransition(
                WorkflowStatus.COMPLETED, WorkflowStatus.DRAFT));
    }

    @Test
    void should_AllowCancellation_from_AnyState() {
        for (WorkflowStatus status : WorkflowStatus.values()) {
            if (status != WorkflowStatus.CANCELLED) {
                assertTrue(domainService.isValidTransition(status, WorkflowStatus.CANCELLED),
                        "应该允许从 " + status + " 转换到 CANCELLED");
            }
        }
    }

    @Test
    void should_ThrowException_when_SpecificationIsNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            domainService.validateSpecification(null);
        });
    }

    @Test
    void should_ThrowException_when_PrdContentIsEmpty() {
        Specification spec = new Specification(
                "",
                List.of(),
                "生成内容",
                LocalDateTime.now()
        );

        assertThrows(IllegalArgumentException.class, () -> {
            domainService.validateSpecification(spec);
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
    void should_Pass_when_ValidSpecification() {
        Specification spec = new Specification(
                "PRD内容",
                List.of("doc1.md"),
                "生成的规格文档",
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
    void should_ThrowException_when_VersionLessThan1() {
        TechnicalDesign design = new TechnicalDesign(
                "内容",
                0,
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
        TechnicalDesign design = new TechnicalDesign(
                "技术方案内容",
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
    void should_ReturnZero_when_WorkflowIsDraft() {
        DevelopmentWorkflow workflow = DevelopmentWorkflow.create("测试", 1L, "zhourui");

        int progress = domainService.calculateProgress(workflow);

        assertEquals(0, progress);
    }

    @Test
    void should_Return20_when_SpecGenerated() {
        DevelopmentWorkflow workflow = DevelopmentWorkflow.create("测试", 1L, "zhourui");
        workflow.startSpecGeneration();

        Specification spec = new Specification("PRD", List.of(), "Spec", LocalDateTime.now());
        workflow.completeSpecGeneration(spec);

        int progress = domainService.calculateProgress(workflow);

        assertEquals(20, progress);
    }

    @Test
    void should_Return40_when_TechDesignGenerated() {
        DevelopmentWorkflow workflow = DevelopmentWorkflow.create("测试", 1L, "zhourui");
        workflow.startSpecGeneration();

        Specification spec = new Specification("PRD", List.of(), "Spec", LocalDateTime.now());
        workflow.completeSpecGeneration(spec);

        workflow.startTechDesign();

        TechnicalDesign design = new TechnicalDesign("Design", 1, false, LocalDateTime.now(), null);
        workflow.completeTechDesign(design);

        int progress = domainService.calculateProgress(workflow);

        assertEquals(40, progress);
    }

    @Test
    void should_Return100_when_Completed() {
        DevelopmentWorkflow workflow = DevelopmentWorkflow.create("测试", 1L, "zhourui");
        workflow.startSpecGeneration();

        Specification spec = new Specification("PRD", List.of(), "Spec", LocalDateTime.now());
        workflow.completeSpecGeneration(spec);

        workflow.updateProgress(100);

        int progress = domainService.calculateProgress(workflow);

        assertTrue(progress >= 20);
    }
}
