package com.example.gitreview.application.workflow.api;

import com.example.gitreview.application.workflow.WorkflowApplicationService;
import com.example.gitreview.application.workflow.dto.*;
import com.example.gitreview.domain.workflow.exception.WorkflowNotFoundException;
import com.example.gitreview.domain.workflow.model.WorkflowStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * WorkflowController REST API 测试
 * 使用 MockMvc 测试 HTTP 接口
 *
 * @author zhourui(V33215020)
 * @since 2025/10/04
 */
@WebMvcTest(WorkflowController.class)
class WorkflowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WorkflowApplicationService workflowApplicationService;

    @Test
    void should_create_workflow_successfully() throws Exception {
        CreateWorkflowRequest request = new CreateWorkflowRequest(
                "测试工作流",
                1L,
                "zhourui"
        );

        when(workflowApplicationService.createWorkflow(any(CreateWorkflowRequest.class)))
                .thenReturn(1L);

        mockMvc.perform(post("/api/workflow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflowId").value(1))
                .andExpect(jsonPath("$.message").exists());

        verify(workflowApplicationService, times(1)).createWorkflow(any(CreateWorkflowRequest.class));
    }

    @Test
    void should_get_all_workflows() throws Exception {
        WorkflowStatusDTO workflow1 = new WorkflowStatusDTO(
                1L, "工作流1", WorkflowStatus.DRAFT.name(), 0,
                LocalDateTime.now(), LocalDateTime.now()
        );
        WorkflowStatusDTO workflow2 = new WorkflowStatusDTO(
                2L, "工作流2", WorkflowStatus.SPEC_GENERATING.name(), 10,
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(workflowApplicationService.getAllWorkflows())
                .thenReturn(Arrays.asList(workflow1, workflow2));

        mockMvc.perform(get("/api/workflow"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("工作流1"))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    void should_get_workflow_status() throws Exception {
        WorkflowStatusDTO status = new WorkflowStatusDTO(
                1L, "测试工作流", WorkflowStatus.SPEC_GENERATED.name(), 20,
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(workflowApplicationService.getWorkflowStatus(1L)).thenReturn(status);

        mockMvc.perform(get("/api/workflow/1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("测试工作流"))
                .andExpect(jsonPath("$.status").value(WorkflowStatus.SPEC_GENERATED.name()))
                .andExpect(jsonPath("$.progress").value(20));
    }

    @Test
    void should_return_404_when_workflow_not_found() throws Exception {
        when(workflowApplicationService.getWorkflowStatus(999L))
                .thenThrow(new WorkflowNotFoundException(999L));

        mockMvc.perform(get("/api/workflow/999/status"))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_get_workflow_progress() throws Exception {
        WorkflowProgressDTO progress = new WorkflowProgressDTO(
                1L,
                WorkflowStatus.CODE_GENERATING.name(),
                75,
                "代码生成中",
                5,
                10
        );

        when(workflowApplicationService.getProgress(1L)).thenReturn(progress);

        mockMvc.perform(get("/api/workflow/1/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflowId").value(1))
                .andExpect(jsonPath("$.status").value(WorkflowStatus.CODE_GENERATING.name()))
                .andExpect(jsonPath("$.progress").value(75))
                .andExpect(jsonPath("$.currentStage").value("代码生成中"))
                .andExpect(jsonPath("$.completedTasks").value(5))
                .andExpect(jsonPath("$.totalTasks").value(10));
    }

    @Test
    void should_cancel_workflow() throws Exception {
        doNothing().when(workflowApplicationService).cancelWorkflow(anyLong(), anyString());

        mockMvc.perform(post("/api/workflow/1/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"用户取消\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        verify(workflowApplicationService, times(1)).cancelWorkflow(eq(1L), eq("用户取消"));
    }

    @Test
    void should_generate_specification() throws Exception {
        SpecGenerationRequest request = new SpecGenerationRequest(
                "PRD内容",
                Arrays.asList("doc1.md", "doc2.md")
        );

        doNothing().when(workflowApplicationService)
                .generateSpecification(anyLong(), any(SpecGenerationRequest.class));

        mockMvc.perform(post("/api/workflow/1/spec/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").exists());

        verify(workflowApplicationService, times(1))
                .generateSpecification(eq(1L), any(SpecGenerationRequest.class));
    }

    @Test
    void should_get_specification() throws Exception {
        SpecificationDTO spec = new SpecificationDTO(
                "生成的规格文档内容",
                "原始PRD内容",
                Collections.emptyList(),
                LocalDateTime.now()
        );

        when(workflowApplicationService.getSpecification(1L)).thenReturn(spec);

        mockMvc.perform(get("/api/workflow/1/spec"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("生成的规格文档内容"))
                .andExpect(jsonPath("$.generatedAt").exists());
    }

    @Test
    void should_approve_tech_design() throws Exception {
        doNothing().when(workflowApplicationService).approveTechnicalDesign(anyLong());

        mockMvc.perform(post("/api/workflow/1/tech-design/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        verify(workflowApplicationService, times(1)).approveTechnicalDesign(1L);
    }

    @Test
    void should_start_code_generation() throws Exception {
        doNothing().when(workflowApplicationService).startCodeGeneration(anyLong());

        mockMvc.perform(post("/api/workflow/1/code-generation/start"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").exists());

        verify(workflowApplicationService, times(1)).startCodeGeneration(1L);
    }
}
