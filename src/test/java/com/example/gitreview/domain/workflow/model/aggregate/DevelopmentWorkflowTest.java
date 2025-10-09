package com.example.gitreview.domain.workflow.model.aggregate;

import com.example.gitreview.domain.workflow.exception.InvalidWorkflowTransitionException;
import com.example.gitreview.domain.workflow.model.TaskStatus;
import com.example.gitreview.domain.workflow.model.WorkflowStatus;
import com.example.gitreview.domain.workflow.model.valueobject.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DevelopmentWorkflow 聚合根测试
 * 测试工作流状态转换和业务逻辑
 *
 * @author zhourui(V33215020)
 * @since 2025/10/04
 */
public class DevelopmentWorkflowTest {

    @Test
    void should_CreateWorkflow_when_ValidParameters() {
        DevelopmentWorkflow workflow = DevelopmentWorkflow.create(
                "测试工作流",
                1L,
                "zhourui"
        );

        assertEquals("测试工作流", workflow.getName());
        assertEquals(1L, workflow.getRepositoryId());
        assertEquals("zhourui", workflow.getCreatedBy());
        assertEquals(WorkflowStatus.DRAFT, workflow.getStatus());
        assertEquals(0, workflow.getProgress());
        assertEquals("草稿", workflow.getCurrentStage());
        assertNotNull(workflow.getCreatedAt());
        assertNotNull(workflow.getUpdatedAt());
    }

    @Test
    void should_TransitionToSpecGenerating_when_StartSpecGeneration() {
        DevelopmentWorkflow workflow = DevelopmentWorkflow.create("测试", 1L, "zhourui");

        workflow.startSpecGeneration();

        assertEquals(WorkflowStatus.SPEC_GENERATING, workflow.getStatus());
        assertEquals("生成规格文档中", workflow.getCurrentStage());
    }

    @Test
    void should_CompleteSpecGeneration_when_ValidSpecification() {
        DevelopmentWorkflow workflow = DevelopmentWorkflow.create("测试", 1L, "zhourui");
        workflow.startSpecGeneration();

        Specification spec = new Specification(
                "PRD内容",
                List.of("doc1.md"),
                "生成的规格文档内容",
                LocalDateTime.now()
        );

        workflow.completeSpecGeneration(spec);

        assertEquals(WorkflowStatus.SPEC_GENERATED, workflow.getStatus());
        assertEquals("规格文档已生成", workflow.getCurrentStage());
        assertEquals(20, workflow.getProgress());
        assertEquals(spec, workflow.getSpecification());
    }

    @Test
    void should_ThrowException_when_InvalidStateTransition() {
        DevelopmentWorkflow workflow = DevelopmentWorkflow.create("测试", 1L, "zhourui");

        assertThrows(InvalidWorkflowTransitionException.class, () -> {
            workflow.startTechDesign();
        });
    }

    @Test
    void should_GenerateTechDesign_when_SpecGenerated() {
        DevelopmentWorkflow workflow = DevelopmentWorkflow.create("测试", 1L, "zhourui");
        workflow.startSpecGeneration();

        Specification spec = new Specification("PRD", List.of(), "Spec", LocalDateTime.now());
        workflow.completeSpecGeneration(spec);

        workflow.startTechDesign();
        assertEquals(WorkflowStatus.TECH_DESIGN_GENERATING, workflow.getStatus());

        TechnicalDesign design = new TechnicalDesign(
                "技术方案内容",
                1,
                false,
                LocalDateTime.now(),
                null
        );

        workflow.completeTechDesign(design);
        assertEquals(WorkflowStatus.TECH_DESIGN_GENERATED, workflow.getStatus());
        assertEquals(40, workflow.getProgress());
    }

    @Test
    void should_UpdateTechDesign_when_InTechDesignGeneratedState() {
        DevelopmentWorkflow workflow = createWorkflowWithTechDesign();

        workflow.updateTechDesign("更新的技术方案内容");

        assertEquals(2, workflow.getTechnicalDesign().getVersion());
        assertFalse(workflow.getTechnicalDesign().isApproved());
    }

    @Test
    void should_ApproveTechDesign_when_TechDesignGenerated() {
        DevelopmentWorkflow workflow = createWorkflowWithTechDesign();

        workflow.approveTechDesign();

        assertEquals(WorkflowStatus.TECH_DESIGN_APPROVED, workflow.getStatus());
        assertTrue(workflow.getTechnicalDesign().isApproved());
        assertNotNull(workflow.getTechnicalDesign().getApprovedAt());
    }

    @Test
    void should_GenerateTaskList_when_TechDesignApproved() {
        DevelopmentWorkflow workflow = createWorkflowWithApprovedTechDesign();

        workflow.startTaskListGeneration();
        assertEquals(WorkflowStatus.TASK_LIST_GENERATING, workflow.getStatus());

        List<Task> tasks = new ArrayList<>();
        tasks.add(new Task("P0-1", "任务1"));

        TaskList taskList = new TaskList("任务列表内容", tasks, LocalDateTime.now());

        workflow.completeTaskListGeneration(taskList);
        assertEquals(WorkflowStatus.TASK_LIST_GENERATED, workflow.getStatus());
        assertEquals(60, workflow.getProgress());
    }

    @Test
    void should_StartCodeGeneration_when_TaskListGenerated() {
        DevelopmentWorkflow workflow = createWorkflowWithTaskList();

        workflow.startCodeGeneration();

        assertEquals(WorkflowStatus.CODE_GENERATING, workflow.getStatus());
        assertEquals("代码生成中", workflow.getCurrentStage());
    }

    @Test
    void should_CompleteTask_when_CodeGenerating() {
        DevelopmentWorkflow workflow = createWorkflowWithTaskList();
        workflow.startCodeGeneration();

        workflow.completeTask("P0-1", "生成的代码内容");

        Task completedTask = workflow.getTaskList().getTaskById("P0-1").orElse(null);
        assertNotNull(completedTask);
        assertEquals(TaskStatus.COMPLETED, completedTask.getStatus());
        assertEquals("生成的代码内容", completedTask.getGeneratedCode());
    }

    @Test
    void should_MarkAsFailed_when_ErrorOccurs() {
        DevelopmentWorkflow workflow = DevelopmentWorkflow.create("测试", 1L, "zhourui");

        workflow.markAsFailed("测试失败原因");

        assertEquals(WorkflowStatus.FAILED, workflow.getStatus());
        assertTrue(workflow.getCurrentStage().contains("失败"));
    }

    @Test
    void should_Cancel_when_UserCancels() {
        DevelopmentWorkflow workflow = DevelopmentWorkflow.create("测试", 1L, "zhourui");

        workflow.cancel("用户取消");

        assertEquals(WorkflowStatus.CANCELLED, workflow.getStatus());
        assertTrue(workflow.getCurrentStage().contains("已取消"));
    }

    @Test
    void should_UpdateProgress_when_Called() {
        DevelopmentWorkflow workflow = DevelopmentWorkflow.create("测试", 1L, "zhourui");

        workflow.updateProgress(50);

        assertEquals(50, workflow.getProgress());
    }

    private DevelopmentWorkflow createWorkflowWithTechDesign() {
        DevelopmentWorkflow workflow = DevelopmentWorkflow.create("测试", 1L, "zhourui");
        workflow.startSpecGeneration();

        Specification spec = new Specification("PRD", List.of(), "Spec", LocalDateTime.now());
        workflow.completeSpecGeneration(spec);

        workflow.startTechDesign();

        TechnicalDesign design = new TechnicalDesign("技术方案", 1, false, LocalDateTime.now(), null);
        workflow.completeTechDesign(design);

        return workflow;
    }

    private DevelopmentWorkflow createWorkflowWithApprovedTechDesign() {
        DevelopmentWorkflow workflow = createWorkflowWithTechDesign();
        workflow.approveTechDesign();
        return workflow;
    }

    private DevelopmentWorkflow createWorkflowWithTaskList() {
        DevelopmentWorkflow workflow = createWorkflowWithApprovedTechDesign();
        workflow.startTaskListGeneration();

        List<Task> tasks = new ArrayList<>();
        tasks.add(new Task("P0-1", "任务1"));

        TaskList taskList = new TaskList("任务列表", tasks, LocalDateTime.now());
        workflow.completeTaskListGeneration(taskList);

        return workflow;
    }
}
