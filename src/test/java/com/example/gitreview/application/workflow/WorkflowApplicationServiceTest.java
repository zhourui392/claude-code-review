package com.example.gitreview.application.workflow;

import com.example.gitreview.application.workflow.dto.*;
import com.example.gitreview.domain.workflow.exception.WorkflowNotFoundException;
import com.example.gitreview.domain.workflow.model.WorkflowStatus;
import com.example.gitreview.domain.workflow.model.aggregate.DevelopmentWorkflow;
import com.example.gitreview.domain.workflow.model.valueobject.Specification;
import com.example.gitreview.domain.workflow.repository.WorkflowRepository;
import com.example.gitreview.domain.workflow.service.WorkflowDomainService;
import com.example.gitreview.infrastructure.claude.ClaudeQueryPort;
import com.example.gitreview.infrastructure.git.GitOperationPort;
import com.example.gitreview.infrastructure.parser.TaskListParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WorkflowApplicationService 集成测试
 * 使用 Mockito 模拟外部依赖，测试服务编排逻辑
 *
 * @author zhourui(V33215020)
 * @since 2025/10/04
 */
@ExtendWith(MockitoExtension.class)
class WorkflowApplicationServiceTest {

    @Mock
    private WorkflowRepository workflowRepository;

    @Mock
    private WorkflowDomainService workflowDomainService;

    @Mock
    private ClaudeQueryPort claudeQueryPort;

    @Mock
    private GitOperationPort gitOperationPort;

    @Mock
    private TaskListParser taskListParser;

    @InjectMocks
    private WorkflowApplicationService workflowApplicationService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void should_create_workflow_successfully() {
        CreateWorkflowRequest request = new CreateWorkflowRequest(
                "测试工作流",
                1L,
                "zhourui"
        );

        DevelopmentWorkflow workflow = DevelopmentWorkflow.create(
                request.getName(),
                request.getRepositoryId(),
                request.getCreatedBy()
        );
        workflow.setId(1L);

        when(workflowRepository.save(any(DevelopmentWorkflow.class))).thenReturn(workflow);

        Long workflowId = workflowApplicationService.createWorkflow(request);

        assertThat(workflowId).isEqualTo(1L);
        verify(workflowRepository, times(1)).save(any(DevelopmentWorkflow.class));
    }

    @Test
    void should_get_workflow_status() {
        Long workflowId = 1L;
        DevelopmentWorkflow workflow = DevelopmentWorkflow.create("测试", 1L, "zhourui");
        workflow.setId(workflowId);

        when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(workflow));

        WorkflowStatusDTO status = workflowApplicationService.getWorkflowStatus(workflowId);

        assertThat(status).isNotNull();
        assertThat(status.getId()).isEqualTo(workflowId);
        assertThat(status.getName()).isEqualTo("测试");
        assertThat(status.getStatus()).isEqualTo(WorkflowStatus.DRAFT.name());
        verify(workflowRepository, times(1)).findById(workflowId);
    }

    @Test
    void should_throw_exception_when_workflow_not_found() {
        Long workflowId = 999L;
        when(workflowRepository.findById(workflowId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workflowApplicationService.getWorkflowStatus(workflowId))
                .isInstanceOf(WorkflowNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void should_get_workflow_progress() {
        Long workflowId = 1L;
        DevelopmentWorkflow workflow = DevelopmentWorkflow.create("测试", 1L, "zhourui");
        workflow.setId(workflowId);
        workflow.updateProgress(50);

        when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(workflow));

        WorkflowProgressDTO progress = workflowApplicationService.getProgress(workflowId);

        assertThat(progress).isNotNull();
        assertThat(progress.getWorkflowId()).isEqualTo(workflowId);
        assertThat(progress.getProgress()).isEqualTo(50);
        assertThat(progress.getStatus()).isEqualTo(WorkflowStatus.DRAFT.name());
    }

    @Test
    void should_cancel_workflow() {
        Long workflowId = 1L;
        String reason = "用户取消";
        DevelopmentWorkflow workflow = DevelopmentWorkflow.create("测试", 1L, "zhourui");
        workflow.setId(workflowId);

        when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(workflow));
        when(workflowRepository.save(any(DevelopmentWorkflow.class))).thenReturn(workflow);

        workflowApplicationService.cancelWorkflow(workflowId, reason);

        assertThat(workflow.getStatus()).isEqualTo(WorkflowStatus.CANCELLED);
        assertThat(workflow.getCurrentStage()).contains(reason);
        verify(workflowRepository, times(1)).save(workflow);
    }

    @Test
    void should_get_specification() {
        Long workflowId = 1L;
        DevelopmentWorkflow workflow = DevelopmentWorkflow.create("测试", 1L, "zhourui");
        workflow.setId(workflowId);
        workflow.startSpecGeneration();

        Specification spec = new Specification(
                "PRD内容",
                Arrays.asList("doc1.md"),
                "生成的规格文档",
                null
        );
        workflow.completeSpecGeneration(spec);

        when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(workflow));

        SpecificationDTO specDTO = workflowApplicationService.getSpecification(workflowId);

        assertThat(specDTO).isNotNull();
        assertThat(specDTO.getContent()).isEqualTo("生成的规格文档");
    }

    @Test
    void should_get_all_workflows() {
        DevelopmentWorkflow workflow1 = DevelopmentWorkflow.create("工作流1", 1L, "zhourui");
        workflow1.setId(1L);
        DevelopmentWorkflow workflow2 = DevelopmentWorkflow.create("工作流2", 2L, "zhourui");
        workflow2.setId(2L);

        when(workflowRepository.findAll()).thenReturn(Arrays.asList(workflow1, workflow2));

        List<WorkflowStatusDTO> workflows = workflowApplicationService.getAllWorkflows();

        assertThat(workflows).hasSize(2);
        assertThat(workflows.get(0).getName()).isEqualTo("工作流1");
        assertThat(workflows.get(1).getName()).isEqualTo("工作流2");
    }

    @Test
    void should_verify_workflow_creation_flow() {
        CreateWorkflowRequest request = new CreateWorkflowRequest(
                "测试工作流",
                1L,
                "zhourui"
        );

        DevelopmentWorkflow mockWorkflow = DevelopmentWorkflow.create(
                request.getName(),
                request.getRepositoryId(),
                request.getCreatedBy()
        );
        mockWorkflow.setId(100L);

        when(workflowRepository.save(any(DevelopmentWorkflow.class))).thenReturn(mockWorkflow);

        Long workflowId = workflowApplicationService.createWorkflow(request);

        assertThat(workflowId).isEqualTo(100L);

        verify(workflowRepository).save(argThat(wf ->
                wf.getName().equals("测试工作流") &&
                wf.getRepositoryId().equals(1L) &&
                wf.getCreatedBy().equals("zhourui") &&
                wf.getStatus() == WorkflowStatus.DRAFT
        ));
    }
}
