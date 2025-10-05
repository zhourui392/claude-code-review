package com.example.gitreview.exception;

import com.example.gitreview.application.workflow.WorkflowApplicationService;
import com.example.gitreview.application.workflow.api.WorkflowController;
import com.example.gitreview.domain.workflow.exception.InvalidWorkflowTransitionException;
import com.example.gitreview.domain.workflow.exception.WorkflowNotFoundException;
import com.example.gitreview.domain.workflow.model.WorkflowStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 全局异常处理器测试
 * 测试工作流相关异常的标准化响应
 *
 * @author zhourui(V33215020)
 * @since 2025/10/04
 */
@WebMvcTest({WorkflowController.class, GlobalExceptionHandler.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkflowApplicationService workflowApplicationService;

    @Test
    void should_handle_workflow_not_found_exception() throws Exception {
        when(workflowApplicationService.getWorkflowStatus(anyLong()))
                .thenThrow(new WorkflowNotFoundException(999L));

        mockMvc.perform(get("/api/workflow/999/status"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Workflow not found"))
                .andExpect(jsonPath("$.workflowId").value(999))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void should_handle_invalid_workflow_transition_exception() throws Exception {
        when(workflowApplicationService.getWorkflowStatus(anyLong()))
                .thenThrow(new InvalidWorkflowTransitionException(
                        WorkflowStatus.COMPLETED,
                        WorkflowStatus.DRAFT
                ));

        mockMvc.perform(get("/api/workflow/1/status"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("INVALID_TRANSITION"))
                .andExpect(jsonPath("$.message").value("Invalid workflow state transition"))
                .andExpect(jsonPath("$.fromStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.toStatus").value("DRAFT"))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void should_return_standard_error_format_for_workflow_not_found() throws Exception {
        when(workflowApplicationService.getWorkflowStatus(anyLong()))
                .thenThrow(new WorkflowNotFoundException(123L));

        mockMvc.perform(get("/api/workflow/123/status"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").isString())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.error").isString())
                .andExpect(jsonPath("$.workflowId").isNumber())
                .andExpect(jsonPath("$.timestamp").isNumber());
    }

    @Test
    void should_return_standard_error_format_for_invalid_transition() throws Exception {
        when(workflowApplicationService.getWorkflowStatus(anyLong()))
                .thenThrow(new InvalidWorkflowTransitionException(
                        WorkflowStatus.DRAFT,
                        WorkflowStatus.COMPLETED
                ));

        mockMvc.perform(get("/api/workflow/1/status"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").isString())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.error").isString())
                .andExpect(jsonPath("$.fromStatus").isString())
                .andExpect(jsonPath("$.toStatus").isString())
                .andExpect(jsonPath("$.timestamp").isNumber());
    }
}
